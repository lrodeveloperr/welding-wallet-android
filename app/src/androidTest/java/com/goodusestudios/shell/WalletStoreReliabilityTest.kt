package com.goodusestudios.weldinggaswallet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.goodusestudios.weldinggaswallet.wallet.CylinderStatus
import com.goodusestudios.weldinggaswallet.wallet.Relationship
import com.goodusestudios.weldinggaswallet.wallet.WalletStore
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WalletStoreReliabilityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun clearStore() {
        context.getSharedPreferences("welding_wallet_v2", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun freeLimitIsEnforcedInsideStoreAndUndoCannotBypassIt() {
        val store = WalletStore(context)
        repeat(3) { index -> assertNotNull(store.addCylinder("Argon", 80.0, "ft3", null, Relationship.Owned, "FREE-$index")) }
        assertNull(store.addCylinder("Oxygen", 40.0, "ft3", null, Relationship.Owned, "FOURTH"))
        val first = store.state.value.activeCylinders.first()
        store.delete(first.id)
        assertNotNull(store.addCylinder("Oxygen", 40.0, "ft3", null, Relationship.Owned, "REPLACEMENT"))
        store.undoDelete()
        assertEquals(4, store.state.value.activeCylinders.size)
        assertFalse(store.setStatus(first.id, CylinderStatus.Low, false))
    }

    @Test fun malformedRestoreCannotEraseExistingWallet() {
        val store = WalletStore(context)
        assertNotNull(store.addCylinder("Argon", 80.0, "ft3", null, Relationship.Owned, "SAFE"))
        assertFalse(store.restoreJson("{\"format\":\"welding-gas-wallet\",\"version\":2,\"cylinders\":["))
        assertEquals(listOf("SAFE"), store.state.value.cylinders.map { it.serial })
    }

    @Test fun invalidNumbersAndOverflowingCostsAreRejected() {
        val store = WalletStore(context)
        assertNull(store.addCylinder("Argon", Double.POSITIVE_INFINITY, "ft3", null, Relationship.Owned, "INF"))
        val cylinder = store.addCylinder("Argon", 80.0, "ft3", null, Relationship.Owned, "VALID")!!
        assertFalse(store.recordService(cylinder.id, com.goodusestudios.weldinggaswallet.wallet.ActivityKind.Cost, BigDecimal("999999999999999999999999"), "USD", System.currentTimeMillis()))
    }
}
