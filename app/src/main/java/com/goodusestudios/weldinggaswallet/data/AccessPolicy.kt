package com.goodusestudios.weldinggaswallet.data

import com.goodusestudios.weldinggaswallet.ui.MonetizationMode
import com.goodusestudios.weldinggaswallet.ui.PurchaseProduct
import com.goodusestudios.weldinggaswallet.ui.StoreProductKind
import com.goodusestudios.weldinggaswallet.ui.requiredProductKind
import com.goodusestudios.weldinggaswallet.ui.usesUsageCap

data class FeatureAccess(
    val allowed: Boolean,
    val remainingFreeActions: Int?,
    val reason: AccessDenialReason? = null,
)

enum class AccessDenialReason { PurchaseRequired, UsageCapReached }

fun resolveFeatureAccess(
    mode: MonetizationMode,
    successfulActionCount: Int,
    freeSuccessfulActions: Int,
    entitled: Boolean,
): FeatureAccess {
    if (entitled) return FeatureAccess(allowed = true, remainingFreeActions = null)
    if (mode.usesUsageCap) {
        val remaining = (freeSuccessfulActions - successfulActionCount).coerceAtLeast(0)
        return FeatureAccess(
            allowed = remaining > 0,
            remainingFreeActions = remaining,
            reason = if (remaining == 0) AccessDenialReason.UsageCapReached else null,
        )
    }
    return when (mode) {
        MonetizationMode.OneTimeUnlock,
        MonetizationMode.Subscription -> FeatureAccess(false, null, AccessDenialReason.PurchaseRequired)
        else -> FeatureAccess(true, null)
    }
}

fun hasEntitlementForMode(
    mode: MonetizationMode,
    entitledProductIds: Set<String>,
    products: List<PurchaseProduct>,
): Boolean {
    val requiredKind = mode.requiredProductKind ?: return false
    return products.any { it.kind == requiredKind && it.id in entitledProductIds }
}

fun productsForMode(mode: MonetizationMode, products: List<PurchaseProduct>): List<PurchaseProduct> =
    mode.requiredProductKind?.let { kind -> products.filter { it.kind == kind } }.orEmpty()

fun cachedEntitlementIsUsable(
    productKind: StoreProductKind,
    verifiedAtEpochMillis: Long,
    nowEpochMillis: Long,
    subscriptionGraceHours: Int,
): Boolean = when {
    verifiedAtEpochMillis <= 0 || nowEpochMillis < verifiedAtEpochMillis -> false
    productKind == StoreProductKind.OneTime -> true
    else ->
        nowEpochMillis - verifiedAtEpochMillis <= subscriptionGraceHours * 60L * 60L * 1000L
}

fun nextSuccessfulActionIds(current: Set<String>, actionId: String, cap: Int): Set<String> = when {
    actionId.isBlank() || actionId.length > 128 || cap < 1 || current.size >= cap || actionId in current -> current
    else -> current + actionId
}
