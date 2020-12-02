package no.nav.helse

import no.nav.helse.dokument.tilServiceDiscoveryUrl
import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals

class ServiceDiscoveryUrlTest {

    @Test
    fun `Sjekker at gammel url blir gjort om korrekt service-discovery url`(){
        val gammelUrl = URI("https://k9-dokument.nais.preprod.local/v1/dokument/123.456")
        val serviceDiscoveryBaseUrl = URI("http://k9-dokument/v1/dokument")

        val forventetServiceDiscoveryUrl = URI("http://k9-dokument/v1/dokument/123.456")

        val nyUrl = gammelUrl.tilServiceDiscoveryUrl(serviceDiscoveryBaseUrl)

        assertEquals(forventetServiceDiscoveryUrl, nyUrl)
    }

}