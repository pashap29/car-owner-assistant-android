package com.carownerassistant.contract.cloudsync

interface CloudSyncContract {
    suspend fun requestSync(reason: String): Boolean
}
