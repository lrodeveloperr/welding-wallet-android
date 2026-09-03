package com.goodusestudios.weldinggaswallet.data

import android.app.Activity
import android.content.Context
import android.util.Base64
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.goodusestudios.weldinggaswallet.ui.PurchaseProduct
import com.goodusestudios.weldinggaswallet.ui.StoreProductKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

data class BillingProduct(
    val id: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val kind: StoreProductKind,
    val available: Boolean,
)

enum class BillingStatus { Connecting, Ready, Unavailable }
data class BillingUiState(
    val status: BillingStatus = BillingStatus.Connecting,
    val products: List<BillingProduct> = emptyList(),
    val entitledProductIds: Set<String> = emptySet(),
    val pending: Boolean = false,
    val working: Boolean = false,
    val message: String? = null,
) {
    val entitled: Boolean get() = entitledProductIds.isNotEmpty()
}

/** A verifier may call a trusted service. Returning false is always fail-closed. */
fun interface PurchaseVerifier {
    suspend fun verify(purchase: Purchase): Boolean
}

/** Local-first verifier backed by the Play Console licensing public key. */
class PlaySignaturePurchaseVerifier(private val base64PublicKey: String) : PurchaseVerifier {
    override suspend fun verify(purchase: Purchase): Boolean = runCatching {
        if (base64PublicKey.isBlank() || purchase.signature.isBlank()) return false
        val keyBytes = Base64.decode(base64PublicKey, Base64.DEFAULT)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
        Signature.getInstance("SHA1withRSA").run {
            initVerify(publicKey)
            update(purchase.originalJson.toByteArray(Charsets.UTF_8))
            verify(Base64.decode(purchase.signature, Base64.DEFAULT))
        }
    }.getOrDefault(false)
}

class BillingController(
    context: Context,
    private val configuredProducts: List<PurchaseProduct>,
    private val stateStore: ShellStateStore,
    private val subscriptionGraceHours: Int,
    private val verifier: PurchaseVerifier,
    private val now: () -> Long = System::currentTimeMillis,
) : PurchasesUpdatedListener {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val detailsById = mutableMapOf<String, ProductDetails>()
    private val configuredById = configuredProducts.associateBy { it.id }
    private val _state = MutableStateFlow(BillingUiState(products = fallbackProducts()))
    val state: StateFlow<BillingUiState> = _state.asStateFlow()
    private val readyActions = mutableListOf<() -> Unit>()
    private var cachedVerificationTimes: Map<String, Long> = emptyMap()
    private var connecting = false

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        controllerScope.launch {
            stateStore.state.collect { applyCachedEntitlement(it) }
        }
    }

    fun connect() {
        if (configuredProducts.isEmpty()) {
            _state.value = _state.value.copy(status = BillingStatus.Ready)
            return
        }
        if (billingClient.isReady) {
            refreshProducts()
            queryOwnedPurchases(showRestoreMessage = false)
            return
        }
        if (connecting) return
        connecting = true
        _state.value = _state.value.copy(status = BillingStatus.Connecting, message = null)
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.value = _state.value.copy(status = BillingStatus.Ready)
                    val actions = readyActions.toList()
                    readyActions.clear()
                    refreshProducts()
                    queryOwnedPurchases(showRestoreMessage = false)
                    actions.forEach { it() }
                } else {
                    readyActions.clear()
                    _state.value = _state.value.copy(
                        status = BillingStatus.Unavailable,
                        message = result.debugMessage.ifBlank { "Google Play purchases are unavailable." },
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                _state.value = _state.value.copy(status = BillingStatus.Connecting)
            }
        })
    }

    fun restore() = whenReady { queryOwnedPurchases(showRestoreMessage = true) }

    fun launchPurchase(activity: Activity, productId: String) = whenReady {
        if (productId !in configuredById) {
            _state.value = _state.value.copy(message = "The selected product is not configured for this app.")
            return@whenReady
        }
        val details = detailsById[productId]
        if (details == null) {
            _state.value = _state.value.copy(message = "This product is not active for the installed Play build.")
            return@whenReady
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (details.productType == BillingClient.ProductType.SUBS) {
            val token = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (token == null) {
                _state.value = _state.value.copy(message = "No eligible subscription offer is available.")
                return@whenReady
            }
            productParams.setOfferToken(token)
        }
        _state.value = _state.value.copy(working = true, message = null)
        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams.build())).build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.value = _state.value.copy(working = false, message = result.debugMessage)
        }
    }

    private fun whenReady(action: () -> Unit) {
        if (billingClient.isReady) action() else {
            readyActions += action
            connect()
        }
    }

    private fun refreshProducts() {
        configuredProducts.groupBy { it.kind }.forEach { (kind, products) ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it.id)
                        .setProductType(kind.playType())
                        .build()
                })
                .build()
            billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryResult.productDetailsList.forEach { detailsById[it.productId] = it }
                    _state.value = _state.value.copy(products = configuredProducts.map(::toBillingProduct), message = null)
                } else {
                    _state.value = _state.value.copy(message = result.debugMessage)
                }
            }
        }
    }

    private fun queryOwnedPurchases(showRestoreMessage: Boolean) {
        _state.value = _state.value.copy(working = true, message = null)
        val types = configuredProducts.map { it.kind }.distinct()
        if (types.isEmpty()) {
            _state.value = _state.value.copy(working = false)
            return
        }
        val owned = mutableListOf<Purchase>()
        var remaining = types.size
        var allQueriesSucceeded = true
        var failureMessage: String? = null
        types.forEach { kind ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(kind.playType()).build(),
            ) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    owned += purchases
                } else {
                    allQueriesSucceeded = false
                    failureMessage = result.debugMessage
                }
                remaining--
                if (remaining == 0) {
                    if (allQueriesSucceeded) {
                        processPurchases(owned, authoritative = true)
                    } else {
                        _state.value = _state.value.copy(working = false, message = failureMessage)
                    }
                    if (showRestoreMessage && allQueriesSucceeded && owned.none { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                        _state.value = _state.value.copy(message = "No active purchases were found.")
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty(), authoritative = false)
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = _state.value.copy(working = false)
            else -> _state.value = _state.value.copy(working = false, message = result.debugMessage)
        }
    }

    private fun processPurchases(purchases: List<Purchase>, authoritative: Boolean) {
        controllerScope.launch {
            val pending = purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }
            val verified = purchases.filter { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.isNotEmpty() &&
                    purchase.products.all { it in configuredById } &&
                    verifier.verify(purchase)
            }
            val verificationTime = now()
            val verifiedTimes = verified.flatMap { it.products }.associateWith { verificationTime }
            val resultingTimes = if (authoritative) verifiedTimes else cachedVerificationTimes + verifiedTimes
            if (authoritative || verifiedTimes.isNotEmpty()) {
                stateStore.replaceEntitlements(resultingTimes)
            }
            _state.value = _state.value.copy(
                entitledProductIds = resultingTimes.keys,
                pending = pending,
                working = false,
                message = when {
                    pending -> "Purchase pending. Access starts after Google Play confirms payment."
                    purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } && verifiedTimes.isEmpty() ->
                        "Purchase verification failed. Access was not granted."
                    else -> null
                },
            )
            verified.filterNot { it.isAcknowledged }.forEach(::acknowledge)
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.value = _state.value.copy(message = result.debugMessage)
            }
        }
    }

    private fun applyCachedEntitlement(snapshot: ShellPersistentState) {
        val validTimes = snapshot.entitlementVerifiedAtByProduct.filter { (productId, verifiedAt) ->
            configuredById[productId]?.let { product ->
                cachedEntitlementIsUsable(
                    product.kind,
                    verifiedAt,
                    now(),
                    subscriptionGraceHours,
                )
            } == true
        }
        cachedVerificationTimes = validTimes
        _state.value = _state.value.copy(entitledProductIds = validTimes.keys)
    }

    private fun toBillingProduct(config: PurchaseProduct): BillingProduct {
        val details = detailsById[config.id]
        val price = when (config.kind) {
            StoreProductKind.OneTime -> details?.oneTimePurchaseOfferDetails?.formattedPrice
            StoreProductKind.Subscription -> details?.subscriptionOfferDetails?.firstOrNull()
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        }
        return BillingProduct(
            id = config.id,
            title = details?.title ?: config.fallbackTitle,
            description = details?.description.orEmpty(),
            formattedPrice = price ?: config.fallbackPrice,
            kind = config.kind,
            available = details != null,
        )
    }

    private fun fallbackProducts() = configuredProducts.map(::toBillingProduct)
    private fun StoreProductKind.playType() =
        if (this == StoreProductKind.Subscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP

    fun close() {
        controllerScope.cancel()
        billingClient.endConnection()
    }
}
