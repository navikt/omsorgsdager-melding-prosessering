package no.nav.helse

import no.nav.helse.prosessering.v1.PdfV1Generator
import no.nav.helse.prosessering.v1.melding.*
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val fødselsdato = LocalDate.now()
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {

        var id = "1-overføringsmelding-full"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.gyldigSøknad(
                søknadId = id,
                id = "01ERQ0796659C4ANGK5EKQ4FG4"
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-fordelingsmelding-full"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.gyldigSøknad(
                id = "01ERQ08MR3G9YADS8XRQ3R3NSE",
                søknadId = id
            ).copy(
                type = Meldingstype.FORDELING,
                overføring = null,
                fordeling = FordelingsMelding(
                    mottakerType = MottakerType.SAMVÆRSFORELDER,
                    samværsavtale = listOf()
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "3-koronaoverføringsmelding-full"
        pdf = generator.generateSoknadOppsummeringPdf(
            melding = SøknadUtils.gyldigSøknad(
                id = "01ERQ0A8NPGQYJ6AYK3AGR2M0T",
                søknadId = id
            ).copy(
                type = Meldingstype.KORONA,
                overføring = null,
                korona = KoronaOverføringMelding(
                    antallDagerSomSkalOverføres = 10
                )
            )
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    //@Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
