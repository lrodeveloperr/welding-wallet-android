package com.goodusestudios.weldinggaswallet.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.shellStateDataStore by preferencesDataStore(name = "shell_state")

sealed interface ShellGate {
    data object FullOnboarding : ShellGate
    data object LegalUpdate : ShellGate
    data object Ready : ShellGate
}

data class ShellPersistentState(
    val successfulActionIds: Set<String> = emptySet(),
    val entitlementVerifiedAtByProduct: Map<String, Long> = emptyMap(),
) {
    val entitledProductIds: Set<String> get() = entitlementVerifiedAtByProduct.keys
}

class ShellStateStore(private val context: Context) {
    private val data = context.shellStateDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

    val state: Flow<ShellPersistentState> = data.map { preferences ->
        val legacyTimestamp = preferences[ENTITLEMENT_VERIFIED_AT] ?: 0
        val legacy = preferences[ENTITLED_PRODUCT_IDS].orEmpty().associateWith { legacyTimestamp }
        ShellPersistentState(
            successfulActionIds = preferences[SUCCESSFUL_ACTION_IDS].orEmpty(),
            entitlementVerifiedAtByProduct = parseEntitlementRecords(preferences[ENTITLEMENT_RECORDS].orEmpty()) + legacy,
        )
    }

    fun gate(legalVersion: Int): Flow<ShellGate> = data.map { preferences ->
            resolveShellGate(
                onboardingComplete = preferences[ONBOARDING_COMPLETE] == true,
                acceptedLegalVersion = preferences[ACCEPTED_LEGAL_VERSION],
                requiredLegalVersion = legalVersion,
            )
    }

    suspend fun completeOnboarding(legalVersion: Int) {
        context.shellStateDataStore.edit {
            it[ONBOARDING_COMPLETE] = true
            it[ACCEPTED_LEGAL_VERSION] = legalVersion
        }
    }

    suspend fun acceptLegalUpdate(legalVersion: Int) {
        context.shellStateDataStore.edit { it[ACCEPTED_LEGAL_VERSION] = legalVersion }
    }

    /** Count only unique domain operations after they have completed successfully. */
    suspend fun recordSuccessfulAction(actionId: String, cap: Int) {
        require(actionId.length in 1..128) { "A stable action ID of 1 to 128 characters is required" }
        if (cap < 1) return
        context.shellStateDataStore.edit { preferences ->
            val current = preferences[SUCCESSFUL_ACTION_IDS].orEmpty()
            preferences[SUCCESSFUL_ACTION_IDS] = nextSuccessfulActionIds(current, actionId, cap)
        }
    }

    suspend fun replaceEntitlements(verifiedAtByProduct: Map<String, Long>) {
        context.shellStateDataStore.edit { preferences ->
            preferences.remove(ENTITLED_PRODUCT_IDS)
            preferences.remove(ENTITLEMENT_VERIFIED_AT)
            if (verifiedAtByProduct.isEmpty()) {
                preferences.remove(ENTITLEMENT_RECORDS)
            } else {
                preferences[ENTITLEMENT_RECORDS] = verifiedAtByProduct.mapTo(mutableSetOf()) { (id, timestamp) ->
                    "$id\t$timestamp"
                }
            }
        }
    }

    suspend fun resetOnboarding() {
        context.shellStateDataStore.edit {
            it[ONBOARDING_COMPLETE] = false
            it.remove(ACCEPTED_LEGAL_VERSION)
        }
    }

    private companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val ACCEPTED_LEGAL_VERSION = intPreferencesKey("accepted_legal_version")
        val SUCCESSFUL_ACTION_IDS = stringSetPreferencesKey("successful_action_ids")
        val ENTITLED_PRODUCT_IDS = stringSetPreferencesKey("entitled_product_ids")
        val ENTITLEMENT_VERIFIED_AT = longPreferencesKey("entitlement_verified_at")
        val ENTITLEMENT_RECORDS = stringSetPreferencesKey("entitlement_records_v2")
    }
}

fun parseEntitlementRecords(records: Set<String>): Map<String, Long> = buildMap {
    records.forEach { record ->
        val separator = record.lastIndexOf('\t')
        if (separator > 0) {
            record.substring(separator + 1).toLongOrNull()?.takeIf { it > 0 }?.let { timestamp ->
                put(record.substring(0, separator), timestamp)
            }
        }
    }
}

fun resolveShellGate(
    onboardingComplete: Boolean,
    acceptedLegalVersion: Int?,
    requiredLegalVersion: Int,
): ShellGate = when {
    !onboardingComplete -> ShellGate.FullOnboarding
    acceptedLegalVersion != requiredLegalVersion -> ShellGate.LegalUpdate
    else -> ShellGate.Ready
}
