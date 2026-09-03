package com.goodusestudios.weldinggaswallet.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class BackupStatus { Idle, Complete, Error }

data class BackupUiState(
    val status: BackupStatus = BackupStatus.Idle,
    val working: Boolean = false,
    val message: String? = null,
)

/**
 * Optional native backup seam. The default provider is unavailable and performs no
 * network, sign-in, account, or cloud-storage work.
 */
interface BackupProvider {
    val isAvailable: Boolean
    val displayName: String
    val state: StateFlow<BackupUiState>
    fun createBackup()
    fun restoreLatest()
}

object DisabledBackupProvider : BackupProvider {
    override val isAvailable = false
    override val displayName = "Disabled"
    override val state: StateFlow<BackupUiState> = MutableStateFlow(BackupUiState())
    override fun createBackup() = Unit
    override fun restoreLatest() = Unit
}
