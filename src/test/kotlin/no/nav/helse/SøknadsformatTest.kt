package no.nav.helse

import no.nav.helse.dokument.Søknadsformat
import no.nav.helse.prosessering.v1.melding.*
import org.skyscreamer.jsonassert.JSONAssert
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
        ))

        val forventetSøknad =
            //language=json
            """
            {
              "søknadId": "d559c242-e95f-4dda-8d59-7d0b06985bb3",
              "mottatt": "2018-01-02T03:04:05.000000006Z",
              "språk": "nb",
              "søker": {
                "fødselsnummer": "02119970078",
                "fornavn": "Ola",
                "mellomnavn": "Mellomnavn",
                "etternavn": "Nordmann",
                "fødselsdato": "2018-01-24",
                "aktørId": "123456"
              },
              "id": "123456789",
              "arbeidssituasjon": [
                "FRILANSER",
                "SELVSTENDIG_NÆRINGSDRIVENDE"
              ],
              "annenForelder": {
                "navn": "Berit",
                "fnr": "02119970078",
                "situasjon": "FENGSEL",
                "situasjonBeskrivelse": "Sitter i fengsel..",
                "periodeOver6Måneder": false,
                "periodeFraOgMed": "2020-01-01",
                "periodeTilOgMed": "2020-10-01"
              },
              "antallBarn": 2,
              "fødselsårBarn": [
                2005,
                2013
              ],
              "medlemskap": {
                "harBoddIUtlandetSiste12Mnd": true,
                "utenlandsoppholdSiste12Mnd": [
                  {
                    "fraOgMed": "2020-01-01",
                    "tilOgMed": "2020-01-10",
                    "landkode": "DE",
                    "landnavn": "Tyskland"
                  },
                  {
                    "fraOgMed": "2020-01-01",
                    "tilOgMed": "2020-01-10",
                    "landkode": "SWE",
                    "landnavn": "Sverige"
                  }
                ],
                "skalBoIUtlandetNeste12Mnd": true,
                "utenlandsoppholdNeste12Mnd": [
                  {
                    "fraOgMed": "2020-10-01",
                    "tilOgMed": "2020-10-10",
                    "landkode": "BR",
                    "landnavn": "Brasil"
                  }
                ]
              },
              "harForståttRettigheterOgPlikter": true,
              "harBekreftetOpplysninger": true
            }
            """.trimIndent()

        JSONAssert.assertEquals(forventetSøknad, String(json), true)
    }
}
