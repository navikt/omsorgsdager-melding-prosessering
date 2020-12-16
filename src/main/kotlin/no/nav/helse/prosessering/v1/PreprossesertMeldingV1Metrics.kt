package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import no.nav.helse.prosessering.v1.melding.PreprossesertMelding
import org.slf4j.LoggerFactory

private val generelCounter = Counter.build()
    .name("generel_counter")
    .help("Generel counter")
    .labelNames("spm", "svar")
    .register()

internal fun PreprossesertMelding.reportMetrics(){
    val logger = LoggerFactory.getLogger("no.nav.helse.prosessering.v1.Metrics")

    if(this.fordeling != null){
        if(fordeling.samværsavtale.isEmpty()){
            logger.info("Metrikk: Øker 'vedlegg' 'Nei'")
            generelCounter.labels("vedlegg", "Nei").inc()
        } else {
            logger.info("Metrikk: Øker 'vedlegg' 'Ja'")
            generelCounter.labels("vedlegg", "Ja").inc()
        }
    }

}
