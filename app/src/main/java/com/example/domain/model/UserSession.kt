package com.example.domain.model

data class UserSession(val uid: String, val email: String, val displayName: String, val photoUrl: String? = null)
