package no.nav.helse.k9

import no.nav.helse.felles.Metadata
import no.nav.helse.prosessering.v1.asynkron.Cleanup
import no.nav.helse.prosessering.v1.asynkron.Journalfort
import no.nav.helse.prosessering.v1.asynkron.tilK9Behovssekvens
import no.nav.helse.prosessering.v1.melding.*
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class K9BehovssekvensTest {

    companion object {
        val gyldigFodselsnummerA = "02119970078"
        val gyldigFodselsnummerB = "19066672169"
    }

    @Test
    fun `Verifisere at gyldig cleanupMelding blir omgjort til riktig behovssekvensformat`() {
        val forventetBehovssekvensJson =
            //language=json
            """
            {
              "@id": "01ARZ3NDEKTSV4RRFFQ69G5FAV",
              "@type": "Behovssekvens",
              "@versjon": "1",
              "@correlationId": "12345678910",
              "@behovsrekkefølge": [
                "MidlertidigAlene"
              ],
              "@behov": {
                "MidlertidigAlene": {
                  "versjon": "1.0.0",
                  "søker": {
                    "identitetsnummer": "$gyldigFodselsnummerA"
                  },
                  "annenForelder": {
                    "identitetsnummer": "$gyldigFodselsnummerA"
                  },
                  "journalpostIder": [
                    "12345"
                  ],
                  "mottatt": "2020-01-01T12:00:00Z"
                }
              }
            }
        """.trimIndent()

        val (id, løsning) = gyldigCleanupMelding.tilK9Behovssekvens().keyValue
        val behovssekvensLøsningSomJson = JSONObject(løsning)
        behovssekvensLøsningSomJson.remove("@opprettet")
        behovssekvensLøsningSomJson.remove("@sistEndret")

        JSONAssert.assertEquals(forventetBehovssekvensJson, behovssekvensLøsningSomJson, true)
    }

    val gyldigCleanupMelding = Cleanup(
        metadata = Metadata(
            version = 1,
            correlationId = "12345678910",
            requestId = "1111111111"
        ),
        journalførtMelding = Journalfort(
            journalpostId = "12345"
        ),
        melding = PreprossesertMelding(
            melding = Melding(
                språk = "nb",
                søknadId = UUID.randomUUID().toString(),
                mottatt = ZonedDateTime.parse("2020-01-01T12:00:00Z"),
                søker = Søker(
                    aktørId = "123456",
                    fødselsnummer = gyldigFodselsnummerA,
                    fødselsdato = LocalDate.now().minusDays(1000),
                    etternavn = "Nordmann",
                    mellomnavn = "Mellomnavn",
                    fornavn = "Ola"
                ),
                id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                arbeidssituasjon = listOf(Arbeidssituasjon.FRILANSER),
                harBekreftetOpplysninger = true,
                harForståttRettigheterOgPlikter = true,
                antalllDagerBruktIÅr = 1,
                arbeiderINorge = true,
                erYrkesaktiv = true,
                harAleneomsorg = true,
                harUtvidetRett = true,
                mottakerFnr = "12345678910",
                mottakerNavn = "Kari Nordmann",
                barn = listOf(
                    Barn(
                        identitetsnummer = "10987654321",
                        aktørId = "654321",
                        fødselsdato = LocalDate.parse("2010-01-01"),
                        aleneOmOmsorgen = true,
                        utvidetRett = true,
                        navn = "Doffen Nordmann"
                    )
                ),
                type = Meldingstype.OVERFORING,
                overføring = OverføringsMelding(
                    MottakerType.EKTEFELLE,
                    antallDagerSomSkalOverføres = 15
                )
            ),
            dokumentUrls = listOf(listOf(URI.create("http://localhost:8080/vedlegg/1")))
        )
    )
}
