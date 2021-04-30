import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "1.5.2.5b2dff7"
val k9RapidBehovVersion = "1.91b665d"
val k9RapidOverføreOmsorgsdagerVersion = "1.6d743f4"
val k9RapidFordeleOmsorgsdagerVersion = "1.3265f25"
val k9RapidOverføreKoronaOmsorgsdagerVersion = "1.3265f25"
val ktorVersion = ext.get("ktorVersion").toString()
val slf4jVersion = ext.get("slf4jVersion").toString()
val kotlinxCoroutinesVersion = ext.get("kotlinxCoroutinesVersion").toString()

val openhtmltopdfVersion = "1.0.8"
val kafkaEmbeddedEnvVersion = ext.get("kafkaEmbeddedEnvVersion").toString()
val kafkaVersion = ext.get("kafkaVersion").toString() // Alligned med version fra kafka-embedded-env
val handlebarsVersion = "4.2.0"

val mainClass = "no.nav.helse.OmsorgsdagerMeldingProsesseringKt"

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

buildscript {
    // Henter ut diverse dependency versjoner, i.e. ktorVersion.
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/5b2dff7a11531e487ebdee06b6ae94e0e50287c2/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    // Server
    implementation ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")

    implementation ( "org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxCoroutinesVersion")
    
    // Client
    implementation ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // PDF
    implementation ( "com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    implementation ( "com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")
    implementation ( "org.slf4j:jcl-over-slf4j:$slf4jVersion")
    implementation ( "com.github.jknack:handlebars:$handlebarsVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    // Test
    testImplementation ( "org.apache.kafka:kafka-clients:$kafkaVersion")
    testImplementation ( "no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation ( "no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    implementation(kotlin("stdlib-jdk8"))

    //K9-Rapid
    implementation("no.nav.k9.rapid:behov:$k9RapidBehovVersion")
    implementation("no.nav.k9.rapid:overfore-omsorgsdager:$k9RapidOverføreOmsorgsdagerVersion")
    implementation("no.nav.k9.rapid:fordele-omsorgsdager:$k9RapidFordeleOmsorgsdagerVersion")
    implementation("no.nav.k9.rapid:overfore-korona-omsorgsdager:$k9RapidOverføreKoronaOmsorgsdagerVersion")

}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-format")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")


}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.8.3"
}