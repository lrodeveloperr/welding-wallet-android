package com.goodusestudios.weldinggaswallet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The only product-owned surface. Report a stable ID only after a domain action succeeds;
 * retries with the same ID are deduplicated by the shell's persistent usage ledger.
 */
data class FeatureCanvasScope(
    val destinationId: String,
    val sampleState: SampleContentState,
    val expanded: Boolean,
    val remainingFreeActions: Int?,
    val isEntitled: Boolean,
    val reportSuccessfulAction: (stableActionId: String) -> Unit,
    val requestPaywall: () -> Unit,
)

typealias FeatureCanvas = @Composable (FeatureCanvasScope) -> Unit

val DefaultFeatureCanvas: FeatureCanvas = { scope ->
    FeatureScreen(scope.destinationId, scope.sampleState, scope.expanded) { }
}

@Composable
fun AccessLockedScreen(usageCapReached: Boolean, onUpgrade: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            if (usageCapReached) "Free actions used" else "Upgrade required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (usageCapReached) "Upgrade to keep using this feature." else "Activate access to use this feature.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) { Text("View options") }
    }
}
