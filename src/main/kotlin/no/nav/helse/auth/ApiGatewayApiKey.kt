package no.nav.helse.auth

data class ApiGatewayApiKey(
    val value: String,
    val headerKey: String = "x-nav-apiKey"
)