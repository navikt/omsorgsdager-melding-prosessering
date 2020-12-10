package no.nav.helse

import no.nav.helse.dokument.Søknadsformat
import no.nav.helse.prosessering.v1.melding.FordelingsMelding
import no.nav.helse.prosessering.v1.melding.KoronaOverføringMelding
import no.nav.helse.prosessering.v1.melding.KoronaStengingsperiode
import no.nav.helse.prosessering.v1.melding.MottakerType
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URL
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class SøknadsformatTest {

    @Test
    fun `Søknaden journalføres som JSON uten vedlegg`() {
        val json = Søknadsformat.somJson(SøknadUtils.gyldigSøknad(
            søkerFødselsnummer = "123456789",
            mottatt = ZonedDateTime.parse("2018-01-02T03:04:05Z"),
            id = "01ERQ0AXTRPDV6NN70NMTYC0EX",
            søknadId = "d559c242-e95f-4dda-8d59-7d0b06985bb3"
        ).copy(
            fordeling = FordelingsMelding(MottakerType.SAMVÆRSFORELDER, listOf(URL("http://localhost:8080/vedlegg/1"))),
            korona = KoronaOverføringMelding(
                antallDagerSomSkalOverføres = 15,
                stengingsperiode = KoronaStengingsperiode(
                    fraOgMed = LocalDate.parse("2020-06-06"),
                    tilOgMed = LocalDate.parse("2020-10-10")
                    )
                )
            )
        )

        val forventetSøknad =
            //language=json
            """
            {
              "id": "01ERQ0AXTRPDV6NN70NMTYC0EX",
              "søknadId": "d559c242-e95f-4dda-8d59-7d0b06985bb3",
              "type": "OVERFORING",
              "mottatt": "2018-01-02T03:04:05Z",
              "språk": "nb",
              "søker": {
                "fødselsnummer": "123456789",
                "fornavn": "Ola",
                "mellomnavn": "Mellomnavn",
                "etternavn": "Nordmann",
                "fødselsdato": "2018-03-10",
                "aktørId": "123456"
              },
              "arbeiderINorge": true,
              "erYrkesaktiv": true,
              "harAleneomsorg": true,
              "harUtvidetRett": true,
              "arbeidssituasjon": [
                "FRILANSER"
              ],
              "antallDagerBruktIÅr": 1,
              "mottakerNavn": "Kari Nordmann",
              "mottakerFnr": "12345678910",
              "barn": [
                {
                  "identitetsnummer": "10987654321",
                  "navn": "Doffen Nordmann",
                  "fødselsdato": "2010-01-01",
                  "aleneOmOmsorgen": true,
                  "utvidetRett": true
                }
              ],
              "fordeling": {
                "mottakerType": "SAMVÆRSFORELDER",
                "samværsavtale": ["http://localhost:8080/vedlegg/1"]
              },
              "korona": {
                 "antallDagerSomSkalOverføres": 15,
                 "stengingsperiode": {
                    "fraOgMed": "2020-06-06", 
                    "tilOgMed": "2020-10-10"
                 }
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
