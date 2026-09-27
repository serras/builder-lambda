plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("com.serranofp.builder.lambda") version "10.0-test"
}

repositories {
    maven(url = file("../build/local-plugin-repository"))
    mavenCentral()
}

dependencies {
    implementation("org.springframework.ai:spring-ai-ollama:2.0.1")
    implementation("ai.djl.fasttext:fasttext-engine:0.38.0")
}