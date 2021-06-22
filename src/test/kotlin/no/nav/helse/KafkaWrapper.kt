package no.nav.helse

import junit.framework.Assert.assertEquals
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.felles.Metadata
import no.nav.helse.prosessering.v1.asynkron.Data
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import no.nav.helse.prosessering.v1.asynkron.Topics.CLEANUP
import no.nav.helse.prosessering.v1.asynkron.Topics.K9_RAPID_V2
import no.nav.helse.prosessering.v1.asynkron.Topics.MOTTATT
import no.nav.helse.prosessering.v1.asynkron.Topics.PREPROSSESERT
import no.nav.helse.prosessering.v1.asynkron.omsorgsdagerMeldingKonfigurertMapper
import no.nav.helse.prosessering.v1.melding.Melding
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*

private const val username = "srvkafkaclient"
private const val password = "kafkaclient"

object KafkaWrapper {
    fun bootstrap(): KafkaEnvironment {
        val kafkaEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = true,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames = listOf(
                MOTTATT.name,
                PREPROSSESERT.name,
                CLEANUP.name,
                K9_RAPID_V2.name
            )
        )
        return kafkaEnvironment
    }
}

private fun KafkaEnvironment.testConsumerProperties(groupId: String): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
        )
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    }
}

private fun KafkaEnvironment.testProducerProperties(clientId: String): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";"
        )
        put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
    }
}


fun KafkaEnvironment.k9RapidKonsumer(): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(
        testConsumerProperties("K9RapidKonsumer"),
        StringDeserializer(),
        StringDeserializer()
    )
    consumer.subscribe(listOf(K9_RAPID_V2.name))
    return consumer
}

fun KafkaEnvironment.meldingsProducer() = KafkaProducer(
    testProducerProperties("MidlertidigAleneTestProducer"),
    MOTTATT.keySerializer,
    MOTTATT.serDes
)

fun KafkaConsumer<String, String>.hentK9RapidMelding(
    id: String,
    maxWaitInSeconds: Long = 20
): String {
    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    while (System.currentTimeMillis() < end) {
        seekToBeginning(assignment())
        val entries = poll(Duration.ofSeconds(1))
            .records(K9_RAPID_V2.name)
            .filter { it.key() == id }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first().value()
        }
    }
    throw IllegalStateException("Fant ikke opprettet oppgave for søknad $id etter $maxWaitInSeconds sekunder.")
}

fun KafkaProducer<String, TopicEntry>.leggTilMottak(soknad: Melding) {
    send(
        ProducerRecord(
            MOTTATT.name,
            soknad.søknadId,
            TopicEntry(
                metadata = Metadata(
                    version = 1,
                    correlationId = UUID.randomUUID().toString()
                ),
                data = Data(omsorgsdagerMeldingKonfigurertMapper().writeValueAsString(soknad))
            )
        )
    ).get()
}

fun KafkaEnvironment.username() = username
fun KafkaEnvironment.password() = password
