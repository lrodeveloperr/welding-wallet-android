package com.goodusestudios.weldinggaswallet.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PropaneTank
import androidx.compose.ui.graphics.vector.ImageVector
import com.goodusestudios.weldinggaswallet.BuildConfig

object ShellConfig {
    const val contractVersion = "2.1.0-welding-wallet"
    val definition = ShellDefinition(
        brand = BrandConfig("Welding Gas Wallet", "lrodeveloperr@gmail.com"),
        legal = LegalConfig(
            version = 1,
            effectiveDate = "2026-09-03",
            privacyUrl = "https://lrodeveloperr.github.io/privacy-policy/welding-gas-wallet/privacy/",
            termsUrl = "https://lrodeveloperr.github.io/privacy-policy/welding-gas-wallet/terms/",
            privacyBody = "The reviewed Privacy Policy opens in your browser.",
            termsBody = "The reviewed Terms of Use open in your browser.",
        ),
        onboarding = OnboardingConfig(OnboardingPresentation.None, false, true, emptyList()),
        monetization = MonetizationConfig(
            initialMode = MonetizationMode.AdsWithSubscription,
            freeSuccessfulActions = 3,
            subscriptionOfflineGraceHours = 72,
            playLicensePublicKey = BuildConfig.PLAY_LICENSE_PUBLIC_KEY,
            products = listOf(PurchaseProduct("com.gooduse.weldinggaswallet.pro.yearly", StoreProductKind.Subscription, "Welding Gas Wallet Pro", "Annual price unavailable")),
            benefits = listOf("Unlimited cylinders", "No advertisements", "Cylinder records stay local"),
        ),
        ads = AdsConfig(BuildConfig.ADMOB_APP_ID, BuildConfig.ADMOB_BANNER_ID),
        destinations = listOf(
            ShellDestination("cylinders", "Cylinders", Icons.Outlined.PropaneTank),
            ShellDestination("activity", "Activity", Icons.Outlined.History),
            ShellDestination("suppliers", "Suppliers", Icons.Outlined.Groups),
        ),
    )
    val appName get() = definition.brand.appName
    val supportEmail get() = definition.brand.supportEmail
    val privacyUrl get() = definition.legal.privacyUrl
    val termsUrl get() = definition.legal.termsUrl
    val demoBannerUnitId get() = definition.ads.bannerUnitId
    val destinations get() = definition.destinations

    fun validationErrors(): List<String> = buildList {
        if (definition.brand.appName.isBlank()) add("brand.appName must not be blank")
        if (!definition.brand.supportEmail.contains('@')) add("brand.supportEmail must be an email address")
        if (definition.destinations.map { it.id }.distinct().size != definition.destinations.size) add("destination IDs must be unique")
        if (definition.monetization.requiredProductKind() == null) add("an annual subscription is required")
    }
}

data class ShellDefinition(val brand: BrandConfig, val legal: LegalConfig, val onboarding: OnboardingConfig, val monetization: MonetizationConfig, val ads: AdsConfig, val destinations: List<ShellDestination>)
data class BrandConfig(val appName: String, val supportEmail: String)
data class LegalConfig(val version: Int, val effectiveDate: String, val privacyUrl: String, val termsUrl: String, val privacyBody: String, val termsBody: String)
enum class OnboardingPresentation { None, SinglePage, Pager }
data class OnboardingConfig(val presentation: OnboardingPresentation, val showBrandMark: Boolean, val requireLegalAcceptance: Boolean, val pages: List<OnboardingPage>)
data class OnboardingPage(val step: String, val title: String, val body: String)
data class MonetizationConfig(val initialMode: MonetizationMode, val freeSuccessfulActions: Int, val subscriptionOfflineGraceHours: Int, val playLicensePublicKey: String, val products: List<PurchaseProduct>, val benefits: List<String>)
data class PurchaseProduct(val id: String, val kind: StoreProductKind, val fallbackTitle: String, val fallbackPrice: String)
enum class StoreProductKind { OneTime, Subscription }
data class AdsConfig(val applicationId: String, val bannerUnitId: String, val tagForUnderAgeOfConsent: Boolean = false)
data class ShellDestination(val id: String, val label: String, val icon: ImageVector)

enum class MonetizationMode { Free, Ads, AdsWithRemovePurchase, AdsWithSubscription, OneTimeUnlock, Subscription, UsageCapWithOneTimeUnlock, UsageCapWithSubscription }
val MonetizationMode.usesAds get() = this == MonetizationMode.Ads || this == MonetizationMode.AdsWithRemovePurchase || this == MonetizationMode.AdsWithSubscription
val MonetizationMode.usesUsageCap get() = this == MonetizationMode.UsageCapWithOneTimeUnlock || this == MonetizationMode.UsageCapWithSubscription
val MonetizationMode.requiredProductKind: StoreProductKind?
    get() = when (this) {
        MonetizationMode.AdsWithRemovePurchase, MonetizationMode.OneTimeUnlock, MonetizationMode.UsageCapWithOneTimeUnlock -> StoreProductKind.OneTime
        MonetizationMode.AdsWithSubscription, MonetizationMode.Subscription, MonetizationMode.UsageCapWithSubscription -> StoreProductKind.Subscription
        MonetizationMode.Free, MonetizationMode.Ads -> null
    }
private fun MonetizationConfig.requiredProductKind() = initialMode.requiredProductKind

enum class SampleContentState { Populated, Empty, Loading, Error }
enum class NavigationMode { BottomBar, Rail, Sidebar }
enum class OnboardingLayoutMode { Anchored, Scrollable }
fun navigationModeForWidth(widthDp: Int) = when { widthDp >= 840 -> NavigationMode.Sidebar; widthDp >= 600 -> NavigationMode.Rail; else -> NavigationMode.BottomBar }
fun onboardingLayoutModeFor(heightDp: Int, fontScale: Float) = if (heightDp < 700 || fontScale > 1.3f) OnboardingLayoutMode.Scrollable else OnboardingLayoutMode.Anchored
