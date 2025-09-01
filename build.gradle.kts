import com.google.protobuf.gradle.id

plugins {
    id("org.springframework.boot") version "3.1.7"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.spring") version "2.0.0"
    kotlin("plugin.jpa") version "2.0.0"
    id("com.google.protobuf") version "0.9.3"

}

group = "org.ecommerce"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // --- Spring Boot Starters ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- Database ---
    runtimeOnly("org.postgresql:postgresql")

    // --- Messaging (Kafka for async events) ---
    implementation("org.springframework.kafka:spring-kafka")

    // --- Caching (Redis) ---
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // --- JSON (Kotlin support for Jackson) ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // --- Kotlin core ---
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // --- WebClient ---
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // --- Testing ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}