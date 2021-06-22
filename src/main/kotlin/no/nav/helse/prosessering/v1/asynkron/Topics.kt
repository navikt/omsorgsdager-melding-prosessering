package no.nav.helse.prosessering.v1.asynkron

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.felles.Metadata
import no.nav.helse.prosessering.v1.melding.Melding
import no.nav.helse.prosessering.v1.melding.PreprossesertMelding
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.json.JSONObject

data class Data(val rawJson: String)
data class Cleanup(val metadata: Metadata, val melding: PreprossesertMelding, val journalførtMelding: Journalfort)
data class Journalfort(val journalpostId: String)

internal data class Topic(
    val name: String,
    val serDes: SerDes
) {
    val keySerializer = StringSerializer()
    private val keySerde = Serdes.String()
    private val valueSerde = Serdes.serdeFrom(SerDes(), SerDes())
    val consumed = Consumed.with(keySerde, valueSerde)
    val produced = Produced.with(keySerde, valueSerde)
}

internal object Topics {
    val MOTTATT = Topic(
        name = "privat-omsorgsdager-melding-mottatt",
        serDes = SerDes()
    )

    val PREPROSSESERT = Topic(
        name = "privat-omsorgsdager-melding-preprossesert",
        serDes = SerDes()
    )

    val CLEANUP = Topic(
        name = "privat-omsorgsdager-melding-cleanup",
        serDes = SerDes()
    )

    val K9_RAPID_V2 = Topic(
        name = "k9-rapid-v2",
        serDes = SerDes()
    )
}

internal fun TopicEntry.deserialiserTilCleanup(): Cleanup  = omsorgsdagerMeldingKonfigurertMapper().readValue(data.rawJson)
internal fun TopicEntry.deserialiserTilMelding(): Melding = omsorgsdagerMeldingKonfigurertMapper().readValue(data.rawJson)
internal fun TopicEntry.deserialiserTilPreprosessertMelding(): PreprossesertMelding  = omsorgsdagerMeldingKonfigurertMapper().readValue(data.rawJson)
internal fun Any.serialiserTilData() = Data(omsorgsdagerMeldingKonfigurertMapper().writeValueAsString(this))

class SerDes : Serializer<TopicEntry>, Deserializer<TopicEntry> {
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
    override fun serialize(topic: String, entry: TopicEntry): ByteArray = when (topic == Topics.K9_RAPID_V2.name) {
        true -> entry.data.rawJson.toByteArray()
        false -> entry.rawJson.toByteArray()
    }
    override fun deserialize(topic: String, entry: ByteArray): TopicEntry = TopicEntry(String(entry))
}

data class TopicEntry(val rawJson: String) {
    constructor(metadata: Metadata, data: Data) : this(
        JSONObject(
            mapOf(
                "metadata" to JSONObject(
                    mapOf(
                        "version" to metadata.version,
                        "correlationId" to metadata.correlationId
                    )
                ),
                "data" to JSONObject(data.rawJson)
            )
        ).toString()
    )

    private val entityJson = JSONObject(rawJson)
    private val metadataJson = requireNotNull(entityJson.getJSONObject("metadata"))
    private val dataJson = requireNotNull(entityJson.getJSONObject("data"))
    val metadata = Metadata(
        version = requireNotNull(metadataJson.getInt("version")),
        correlationId = requireNotNull(metadataJson.getString("correlationId"))
    )
    val data = Data(dataJson.toString())
}

fun omsorgsdagerMeldingKonfigurertMapper(): ObjectMapper {
    return jacksonObjectMapper().dusseldorfConfigured()
        .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
}
