package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port : Int = 8080,
        omsorgspengerJoarkBaseUrl : String? = wireMockServer?.getOmsorgspengerJoarkBaseUrl(),
        k9MellomlagringServiceDiscovery : String? = wireMockServer?.getK9MellomlagringServiceDiscovery()
    ) : Map<String, String>{
        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.gateways.k9_joark_url","$omsorgspengerJoarkBaseUrl"),
            Pair("nav.gateways.k9_mellomlagring_service_discovery","$k9MellomlagringServiceDiscovery"),
            Pair("nav.authorization.api_gateway.api_key", "verysecret")
        )

        // Clients
        if (wireMockServer != null) {
            map["nav.auth.clients.1.alias"] = "azure-v2"
            map["nav.auth.clients.1.client_id"] = "omsorgsdager-melding-prosessering"
            map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientA.privateKeyJwk
            map["nav.auth.clients.1.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()
            map["nav.auth.scopes.lagre-dokument"] = "k9-mellomlagring/.default"
            map["nav.auth.scopes.slette-dokument"] = "k9-mellomlagring/.default"
            map["nav.auth.scopes.journalfore"] = "omsorgspenger-joark/.default"
        }

        kafkaEnvironment?.let {
            map["nav.kafka.bootstrap_servers"] = it.brokersURL
            map["nav.kafka.username"] = it.username()
            map["nav.kafka.password"] = it.password()
            map["nav.kafka.auto_offset_reset"] = "earliest"
        }

        return map.toMap()
    }
}
