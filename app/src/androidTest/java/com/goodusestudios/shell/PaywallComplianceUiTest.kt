package com.goodusestudios.weldinggaswallet

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import com.goodusestudios.weldinggaswallet.data.BillingProduct
import com.goodusestudios.weldinggaswallet.data.BillingStatus
import com.goodusestudios.weldinggaswallet.data.BillingUiState
import com.goodusestudios.weldinggaswallet.ui.PaywallScreen
import com.goodusestudios.weldinggaswallet.ui.StoreProductKind
import org.junit.Rule
import org.junit.Test

class PaywallComplianceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun purchaseSurface_hasRequiredControls_andNoAppLogo() {
        compose.setContent {
            MaterialTheme {
                PaywallScreen(
                    billing = BillingUiState(
                        status = BillingStatus.Ready,
                        products = listOf(
                            BillingProduct(
                                id = "test.monthly",
                                title = "Monthly",
                                description = "Test product",
                                formattedPrice = "USD 1.99 / month",
                                kind = StoreProductKind.Subscription,
                                available = true,
                            ),
                        ),
                    ),
                    benefits = listOf("Unlimited access"),
                    onRetry = {},
                    onRestore = {},
                    onPurchase = {},
                    onPrivacy = {},
                    onTerms = {},
                )
            }
        }

        compose.onNodeWithTag("shell-paywall").assertExists()
        compose.onNodeWithTag("shell-purchase").assertExists()
        compose.onNodeWithTag("shell-restore").assertExists()
        compose.onNodeWithTag("shell-privacy").assertExists()
        compose.onNodeWithTag("shell-terms").assertExists()
        compose.onAllNodes(hasTestTag("shell-app-logo")).assertCountEquals(0)
    }
}
