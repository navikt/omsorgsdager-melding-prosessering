package no.nav.helse

import no.nav.helse.prosessering.v1.melding.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

object SøknadUtils {

    fun gyldigSøknad(
        søkerFødselsnummer: String = "02119970078",
        søknadId: String = UUID.randomUUID().toString(),
        mottatt: ZonedDateTime = ZonedDateTime.now()
    ) = Melding(
        språk = "nb",
        søknadId = søknadId,
        mottatt = mottatt,
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = søkerFødselsnummer,
            fødselsdato = LocalDate.now().minusDays(1000),
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
        arbeidssituasjon = listOf(Arbeidssituasjon.FRILANSER),
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true,
        antallDagerBruktEtter1Juli = 1,
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
    )

}
