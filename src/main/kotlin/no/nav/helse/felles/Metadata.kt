package no.nav.helse.felles

data class Metadata(
    val version : Int,
    val correlationId : String,
    val requestId : String
)