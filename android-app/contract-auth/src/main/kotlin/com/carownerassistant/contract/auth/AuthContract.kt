package com.carownerassistant.contract.auth

interface AuthContract {
    suspend fun isSignedIn(): Boolean
}
