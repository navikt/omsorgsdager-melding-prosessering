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
            melding = SøknadUtils.gyldigSøknad(søknadId = id)
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
