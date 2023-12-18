plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "wahtari"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    runtimeOnly("com.h2database:h2")
    implementation("org.flywaydb:flyway-core:10.3.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("com.dslplatform:dsl-json:2.0.2")
    annotationProcessor("com.dslplatform:dsl-json:2.0.2")
    runtimeOnly("javax.json.bind:javax.json.bind-api:1.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
