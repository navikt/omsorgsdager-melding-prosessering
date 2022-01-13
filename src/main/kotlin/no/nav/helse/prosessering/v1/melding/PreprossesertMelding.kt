package no.nav.helse.prosessering.v1.melding

import java.time.ZonedDateTime

data class PreprossesertMelding(
    val id: String,
    val søknadId: String,
    val språk: String,
    val mottatt: ZonedDateTime,
    val dokumentId: List<List<String>>,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean,
    val søker: Søker,
    val mottakerFnr: String,
    val mottakerNavn: String,
    val harAleneomsorg: Boolean,
    val harUtvidetRett: Boolean,
    val erYrkesaktiv: Boolean,
    val arbeiderINorge: Boolean,
    val arbeidssituasjon: List<Arbeidssituasjon>,
    val antallDagerBruktIÅr: Int? = null,
    val barn: List<Barn>,
    val type: Meldingstype,
    val korona: KoronaOverføringMelding? = null,
    val overføring: OverføringsMelding? = null,
    val fordeling: FordelingsMelding? = null
) {
    internal constructor(
        melding: Melding,
        dokumentId: List<List<String>>
    ) : this(
        id = melding.id,
        søknadId = melding.søknadId,
        språk = melding.språk,
        mottatt = melding.mottatt,
        dokumentId = dokumentId,
        harForståttRettigheterOgPlikter = melding.harForståttRettigheterOgPlikter,
        harBekreftetOpplysninger = melding.harBekreftetOpplysninger,
        søker = melding.søker,
        mottakerFnr = melding.mottakerFnr,
        mottakerNavn = melding.mottakerNavn,
        harAleneomsorg = melding.harAleneomsorg,
        harUtvidetRett = melding.harUtvidetRett,
        erYrkesaktiv = melding.harUtvidetRett,
        arbeiderINorge = melding.arbeiderINorge,
        arbeidssituasjon = melding.arbeidssituasjon,
        antallDagerBruktIÅr = melding.antallDagerBruktIÅr,
        barn = melding.barn,
        type = melding.type,
        korona = melding.korona,
        overføring = melding.overføring,
        fordeling = melding.fordeling
    )

    override fun toString(): String {
        return "PreprossesertMelding(id='$id', søknadId='$søknadId', språk='$språk', mottatt=$mottatt)"
    }

}
