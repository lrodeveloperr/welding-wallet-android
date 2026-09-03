package com.goodusestudios.weldinggaswallet.data

import android.app.Activity
import com.goodusestudios.weldinggaswallet.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdConsentState(
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val message: String? = null,
)

class AdConsentController(private val activity: Activity, private val underAge: Boolean) {
    private val consentInformation by lazy { UserMessagingPlatform.getConsentInformation(activity) }
    private val _state = MutableStateFlow(AdConsentState())
    val state: StateFlow<AdConsentState> = _state.asStateFlow()
    private var adsInitialized = false

    fun gather() {
        if (!BuildConfig.SHELL_ADS_ENABLED) return
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(underAge).build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                updateState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error -> updateState(error?.message) }
            },
            { error -> updateState(error.message) },
        )
        updateState()
    }

    fun showPrivacyOptions() {
        if (!BuildConfig.SHELL_ADS_ENABLED) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> updateState(error?.message) }
    }

    private fun updateState(message: String? = null) {
        if (!BuildConfig.SHELL_ADS_ENABLED) return
        val canRequest = consentInformation.canRequestAds()
        if (canRequest && !adsInitialized) {
            adsInitialized = true
            MobileAds.initialize(activity)
        }
        _state.value = AdConsentState(
            canRequestAds = canRequest,
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            message = message,
        )
    }
}
