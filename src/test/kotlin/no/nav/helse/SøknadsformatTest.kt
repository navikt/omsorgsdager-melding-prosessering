package no.nav.helse

import no.nav.helse.dokument.Søknadsformat
import no.nav.helse.prosessering.v1.melding.*
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test

class SøknadsformatTest {

    @Test
    fun `Søknaden journalføres som JSON uten vedlegg`() {
        val json = Søknadsformat.somJson(SøknadUtils.gyldigSøknad(
            søkerFødselsnummer = "123456789",
            mottatt = ZonedDateTime.parse("2018-01-02T03:04:05Z"),
            søknadId = "d559c242-e95f-4dda-8d59-7d0b06985bb3"
        ).copy(
            fordeling = FordelingsMelding(MottakerType.SAMVÆRSFORELDER, listOf(URL("http://localhost:8080/vedlegg/1"))),
            korona = KoronaOverføringMelding(15)
        )
        )

        val forventetSøknad =
            //language=json
            """
            {
              "id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
              "søknadId": "d559c242-e95f-4dda-8d59-7d0b06985bb3",
              "type": "OVERFORING",
              "mottatt": "2018-01-02T03:04:05Z",
              "språk": "nb",
              "søker": {
                "fødselsnummer": "123456789",
                "fornavn": "Ola",
                "mellomnavn": "Mellomnavn",
                "etternavn": "Nordmann",
                "fødselsdato": "2018-03-09",
                "aktørId": "123456"
              },
              "arbeiderINorge": true,
              "erYrkesaktiv": true,
              "harAleneomsorg": true,
              "harUtvidetRett": true,
              "arbeidssituasjon": [
                "FRILANSER"
              ],
              "antallDagerBruktEtter1Juli": 1,
              "mottakerNavn": "Kari Nordmann",
              "mottakerFnr": "12345678910",
              "barn": [
                {
                  "identitetsnummer": "10987654321",
                  "navn": "Doffen Nordmann",
                  "fødselsdato": "2010-01-01",
                  "aktørId": "654321",
                  "aleneOmOmsorgen": true,
                  "utvidetRett": true
                }
              ],
              "fordeling": {
                "mottakerType": "SAMVÆRSFORELDER",
                "samværsavtale": ["http://localhost:8080/vedlegg/1"]
              },
              "korona": {
                 "antallDagerSomSkalOverføres": 15
              },
              "overføring": {
                 "mottakerType": "EKTEFELLE",
                 "antallDagerSomSkalOverføres": 15
              },
              "harForståttRettigheterOgPlikter": true,
              "harBekreftetOpplysninger": true
            }
            """.trimIndent()

        JSONAssert.assertEquals(forventetSøknad, String(json), true)
    }
}
