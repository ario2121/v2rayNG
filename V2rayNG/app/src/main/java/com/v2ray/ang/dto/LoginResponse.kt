package com.v2ray.ang.dto

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val username: String,
    val expire_date: String?,
    val subscription_url: String
)
