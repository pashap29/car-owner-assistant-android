package com.carownerassistant.core.navigation

object AppRoutes {
    const val GARAGE = "garage"
    const val FUEL = "fuel"
    const val EXPENSE = "expense"
    const val SERVICE = "service"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    const val MILEAGE_LEDGER = "mileage_ledger"
    const val MILEAGE_ENTRY = "mileage_entry"
    const val BACKUP_RESTORE = "backup_restore"
    const val VEHICLE_HANDBOOK = "vehicle_handbook"
}

val BottomTabRoutes = listOf(
    AppRoutes.FUEL,
    AppRoutes.EXPENSE,
    AppRoutes.SERVICE,
    AppRoutes.STATISTICS,
    AppRoutes.SETTINGS,
)
