package no.nav.helse.dokument

import no.nav.helse.felles.CorrelationId
import no.nav.helse.prosessering.v1.melding.Melding
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

private val logger: Logger = LoggerFactory.getLogger("nav.DokumentService")

class DokumentService(
    private val dokumentGateway: DokumentGateway
) {
    private suspend fun lagreDokument(
        dokument: DokumentGateway.Dokument,
        correlationId: CorrelationId
    ) : URI {
        return dokumentGateway.lagreDokmenter(
            dokumenter = setOf(dokument),
            correlationId = correlationId
        ).first()
    }

    internal suspend fun lagreSoknadsOppsummeringPdf(
        pdf : ByteArray,
        dokumentEier: DokumentGateway.DokumentEier,
        correlationId: CorrelationId,
        dokumentbeskrivelse: String
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                eier = dokumentEier,
                content = pdf,
                contentType = "application/pdf",
                title = dokumentbeskrivelse
            ),
            correlationId = correlationId
        )
    }

    internal suspend fun lagreSoknadsMelding(
        melding: Melding,
        dokumentEier: DokumentGateway.DokumentEier,
        correlationId: CorrelationId
    ) : URI {
        return lagreDokument(
            dokument = DokumentGateway.Dokument(
                eier = dokumentEier,
                content = Søknadsformat.somJson(melding),
                contentType = "application/json",
                title = "Omsorgsdager - Søknad om å bli regnet som alene som JSON"
            ),
            correlationId = correlationId
        )
    }

    internal suspend fun slettDokumeter(
        dokumentIdBolks: List<List<String>>,
        dokumentEier: DokumentGateway.DokumentEier,
        correlationId : CorrelationId
    ) {
        dokumentGateway.slettDokmenter(
            dokumentId = dokumentIdBolks.flatten(),
            dokumentEier = dokumentEier,
            correlationId = correlationId
        )

    }

}

