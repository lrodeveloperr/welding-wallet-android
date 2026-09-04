package com.goodusestudios.weldinggaswallet

import com.goodusestudios.weldinggaswallet.data.AccessDenialReason
import com.goodusestudios.weldinggaswallet.data.cachedEntitlementIsUsable
import com.goodusestudios.weldinggaswallet.data.hasEntitlementForMode
import com.goodusestudios.weldinggaswallet.data.nextSuccessfulActionIds
import com.goodusestudios.weldinggaswallet.data.parseEntitlementRecords
import com.goodusestudios.weldinggaswallet.data.resolveFeatureAccess
import com.goodusestudios.weldinggaswallet.ui.MonetizationMode
import com.goodusestudios.weldinggaswallet.ui.PurchaseProduct
import com.goodusestudios.weldinggaswallet.ui.StoreProductKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationAccessTest {
    @Test fun freeAndAdProfilesNeverGateTheFeature() {
        listOf(MonetizationMode.Free, MonetizationMode.Ads, MonetizationMode.AdsWithRemovePurchase, MonetizationMode.AdsWithSubscription).forEach { mode ->
            assertTrue(mode.name, resolveFeatureAccess(mode, 99, 5, entitled = false).allowed)
        }
    }

    @Test fun fullUnlockProfilesFailClosed() {
        listOf(MonetizationMode.OneTimeUnlock, MonetizationMode.Subscription).forEach { mode ->
            val access = resolveFeatureAccess(mode, 0, 5, entitled = false)
            assertFalse(mode.name, access.allowed)
            assertEquals(AccessDenialReason.PurchaseRequired, access.reason)
        }
    }

    @Test fun bothUsageCapProfilesAllowExactlyTheConfiguredFreeCount() {
        listOf(MonetizationMode.UsageCapWithOneTimeUnlock, MonetizationMode.UsageCapWithSubscription).forEach { mode ->
            assertEquals(1, resolveFeatureAccess(mode, 4, 5, false).remainingFreeActions)
            val blocked = resolveFeatureAccess(mode, 5, 5, false)
            assertFalse(blocked.allowed)
            assertEquals(AccessDenialReason.UsageCapReached, blocked.reason)
        }
    }

    @Test fun entitlementOverridesAllFeatureGates() {
        MonetizationMode.entries.forEach { mode ->
            val access = resolveFeatureAccess(mode, 100, 5, entitled = true)
            assertTrue(mode.name, access.allowed)
            assertNull(access.remainingFreeActions)
        }
    }

    @Test fun usageLedgerDeduplicatesRetriesAndNeverExceedsCap() {
        val once = nextSuccessfulActionIds(emptySet(), "job-42", 2)
        val retry = nextSuccessfulActionIds(once, "job-42", 2)
        val full = nextSuccessfulActionIds(retry, "job-43", 2)
        assertEquals(once, retry)
        assertEquals(setOf("job-42", "job-43"), full)
        assertEquals(full, nextSuccessfulActionIds(full, "job-44", 2))
        assertEquals(full, nextSuccessfulActionIds(full, "x".repeat(129), 3))
    }

    @Test fun entitlementsAreScopedToTheProfileProductKind() {
        val products = listOf(
            PurchaseProduct("lifetime", StoreProductKind.OneTime, "Lifetime", "$10"),
            PurchaseProduct("subscription", StoreProductKind.Subscription, "Subscription", "$20"),
        )
        assertTrue(hasEntitlementForMode(MonetizationMode.OneTimeUnlock, setOf("lifetime"), products))
        assertFalse(hasEntitlementForMode(MonetizationMode.Subscription, setOf("lifetime"), products))
        assertTrue(hasEntitlementForMode(MonetizationMode.Subscription, setOf("subscription"), products))
    }

    @Test fun subscriptionCacheExpiresWhileOneTimeCachePersists() {
        val hour = 60L * 60L * 1000L
        assertTrue(cachedEntitlementIsUsable(StoreProductKind.Subscription, hour, 72 * hour, 72))
        assertFalse(cachedEntitlementIsUsable(StoreProductKind.Subscription, hour, 74 * hour, 72))
        assertTrue(cachedEntitlementIsUsable(StoreProductKind.OneTime, hour, 10_000 * hour, 0))
        assertFalse(cachedEntitlementIsUsable(StoreProductKind.OneTime, 0, hour, 0))
    }

    @Test fun entitlementRecordsKeepIndependentVerificationTimes() {
        assertEquals(
            mapOf("lifetime" to 100L, "subscription" to 200L),
            parseEntitlementRecords(setOf("lifetime\t100", "subscription\t200", "invalid")),
        )
    }
}
