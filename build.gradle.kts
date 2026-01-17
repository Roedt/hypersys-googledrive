import io.quarkus.gradle.tasks.QuarkusDev
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.allopen") version "2.3.0"
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation("io.quarkiverse.googlecloudservices:quarkus-google-cloud-secret-manager")
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:quarkus-google-cloud-services-bom:${quarkusPlatformVersion}"))
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")

    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")

    implementation(enforcedPlatform("com.google.auth:google-auth-library-bom:1.30.1"))

    implementation("com.google.apis:google-api-services-drive:v3-rev20251210-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http")
}

group = "no.roedt"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
tasks.withType<QuarkusDev> {
    args = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        javaParameters = true
        jvmTarget.set(JvmTarget.JVM_25)
        kotlinDaemonJvmArgs = listOf("--add-opens java.base/java.lang=ALL-UNNAMED")
        /*
        Denne er for å unngå unødige advarsler om https://youtrack.jetbrains.com/issue/KT-73255
        Vi bruker egentlig bare konstruktør-varianten, men vil egentlig helst holde oss til kotlin sin standardvariant
        Så når dette er blitt standarden i kotlin - som det skal bli - så kan vi skru av denne
         */
        freeCompilerArgs = listOf("-Xannotation-default-target=param-property", "-Xjvm-default=all")
    }
}