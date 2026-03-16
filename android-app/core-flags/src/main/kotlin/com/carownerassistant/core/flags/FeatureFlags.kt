package com.carownerassistant.core.flags

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeatureFlag(val key: String, val defaultValue: Boolean) {
    MAP_PLACES_ENABLED("map_places_enabled", false),
    ADVANCED_ANALYTICS_ENABLED("advanced_analytics_enabled", false),
    ENCRYPTED_BACKUP_ENABLED("encrypted_backup_enabled", false),
    FUTURE_AUTH_CONTRACT_ENABLED("future_auth_contract_enabled", false),
    FUTURE_CLOUD_SYNC_CONTRACT_ENABLED("future_cloud_sync_contract_enabled", false),
    FUTURE_PREMIUM_CONTRACT_ENABLED("future_premium_contract_enabled", false),
    FUTURE_FAMILY_CONTRACT_ENABLED("future_family_contract_enabled", false),
}

interface FeatureFlagRepository {
    val flags: StateFlow<Map<FeatureFlag, Boolean>>
    fun isEnabled(flag: FeatureFlag): Boolean
    fun setLocalOverride(flag: FeatureFlag, enabled: Boolean)
}

class InMemoryFeatureFlagRepository : FeatureFlagRepository {
    private val state = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue },
    )

    override val flags: StateFlow<Map<FeatureFlag, Boolean>> = state.asStateFlow()

    override fun isEnabled(flag: FeatureFlag): Boolean = state.value[flag] ?: flag.defaultValue

    override fun setLocalOverride(flag: FeatureFlag, enabled: Boolean) {
        state.value = state.value.toMutableMap().apply { put(flag, enabled) }
    }
}
