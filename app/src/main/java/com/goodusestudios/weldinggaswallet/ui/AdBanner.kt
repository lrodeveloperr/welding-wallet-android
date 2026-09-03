package com.goodusestudios.weldinggaswallet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.goodusestudios.weldinggaswallet.BuildConfig

@Composable
fun AdaptiveAdBanner(canRequestAds: Boolean, modifier: Modifier = Modifier) {
    if (!BuildConfig.SHELL_ADS_ENABLED) return
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        if (!canRequestAds) {
            Box(Modifier.fillMaxWidth().height(adSize.height.dp))
            return@BoxWithConstraints
        }
        val adView = remember(widthDp, ShellConfig.demoBannerUnitId) {
            AdView(context).apply {
                adUnitId = ShellConfig.demoBannerUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        Box(Modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
        }
    }
}
