package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import java.util.*

private const val k9JoarkBaseUrl = "/helse-reverse-proxy/k9-joark-mock"
private const val k9MellomlagringServiceDiscovery = "/k9-mellomlagring-mock"

fun WireMockBuilder.navnOppslagConfig() = wireMockConfiguration {

}

internal fun WireMockServer.stubLagreDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9MellomlagringServiceDiscovery.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withHeader("Location", "${getK9MellomlagringServiceDiscovery()}/v1/dokument/${UUID.randomUUID()}")
                .withStatus(201)
        )
    )
    return this
}

internal fun WireMockServer.stubSlettDokument(): WireMockServer {
    WireMock.stubFor(
        WireMock.delete(WireMock.urlPathMatching(".*$k9MellomlagringServiceDiscovery.*")).willReturn(
            WireMock.aResponse()
                .withStatus(204)
        )
    )
    return this
}

internal fun WireMockServer.stubJournalfor(responseCode: Int = 201, path: String): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(".*$k9JoarkBaseUrl/$path.*")).willReturn(
            WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(
                    """
                    {
                        "journal_post_id" : "9101112"
                    }
                    """.trimIndent()
                )
                .withStatus(responseCode)
        )
    )
    return this
}

private fun WireMockServer.stubHealthEndpoint(
    path: String
): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching(".*$path")).willReturn(
            WireMock.aResponse()
                .withStatus(200)
        )
    )
    return this
}

internal fun WireMockServer.stubK9MellomlagringHealth() = stubHealthEndpoint("$k9MellomlagringServiceDiscovery/health")
internal fun WireMockServer.stubK9JoarkHealth() = stubHealthEndpoint("$k9JoarkBaseUrl/health")

internal fun WireMockServer.getK9JoarkBaseUrl() = baseUrl() + k9JoarkBaseUrl
internal fun WireMockServer.getK9MellomlagringServiceDiscovery() = baseUrl() + k9MellomlagringServiceDiscovery
