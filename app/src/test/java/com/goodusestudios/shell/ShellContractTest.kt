package com.goodusestudios.weldinggaswallet

import com.goodusestudios.weldinggaswallet.data.ShellGate
import com.goodusestudios.weldinggaswallet.data.resolveShellGate
import com.goodusestudios.weldinggaswallet.ui.ShellConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellContractTest {
    @Test fun shippedTemplateConfigurationIsInternallyValid() =
        assertTrue(ShellConfig.validationErrors().joinToString(), ShellConfig.validationErrors().isEmpty())

    @Test fun shippedSubscriptionUsesAnnualStorefrontPricing() {
        val product = ShellConfig.definition.monetization.products.single()
        assertEquals("com.gooduse.weldinggaswallet.pro.yearly", product.id)
        assertEquals("Annual price unavailable", product.fallbackPrice)
    }

    @Test fun firstRunRequiresFullOnboarding() =
        assertEquals(ShellGate.FullOnboarding, resolveShellGate(false, null, 1))

    @Test fun legalVersionChangeRequiresOnlyLegalUpdate() =
        assertEquals(ShellGate.LegalUpdate, resolveShellGate(true, 1, 2))

    @Test fun currentAcceptanceOpensTheApp() =
        assertEquals(ShellGate.Ready, resolveShellGate(true, 2, 2))
}
