package com.goodusestudios.weldinggaswallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodusestudios.weldinggaswallet.data.AdConsentController
import com.goodusestudios.weldinggaswallet.ui.ShellApp
import com.goodusestudios.weldinggaswallet.ui.ShellConfig
import com.goodusestudios.weldinggaswallet.ui.ShellTheme
import com.goodusestudios.weldinggaswallet.wallet.WalletStore
import com.goodusestudios.weldinggaswallet.wallet.weldingWalletFeature
import androidx.compose.runtime.remember

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val adConsent = AdConsentController(this, ShellConfig.definition.ads.tagForUnderAgeOfConsent)
        setContent {
            val consentState = adConsent.state.collectAsStateWithLifecycle().value
            val walletStore = remember { WalletStore(applicationContext) }
            ShellTheme {
                ShellApp(
                    canRequestAds = consentState.canRequestAds,
                    privacyOptionsRequired = consentState.privacyOptionsRequired,
                    onPrivacyOptions = adConsent::showPrivacyOptions,
                    walletStore = walletStore,
                    featureCanvas = weldingWalletFeature(walletStore),
                )
            }
        }
        adConsent.gather()
    }
}
