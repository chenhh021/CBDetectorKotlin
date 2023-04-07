import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.repository.redhat.com/ga/")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.antlr:antlr4:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation("junit:junit:4.11")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    implementation("com.google.inject.extensions:guice-assistedinject:3.0")
//    implementation("org.eclipse.jdt.core.compiler:ecj:4.2.2")
//    implementation("org.eclipse.jdt.core.compiler:ecj:4.20.0.redhat-00002")



    implementation("org.apache.commons:commons-lang3:3.1")
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.lucene:lucene-core:8.11.2")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")

    implementation("commons-io:commons-io:2.5")
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:3.10.0")
}

tasks.withType(JavaCompile::class.java){
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}