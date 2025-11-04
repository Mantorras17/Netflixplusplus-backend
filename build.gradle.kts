plugins {
    id("java")
}

group = "org.netflixpp"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}



repositories {
    mavenCentral()
}

dependencies {
    // Jersey (JAX-RS) and Jetty HTTP container
    implementation("org.glassfish.jersey.core:jersey-server:3.1.5")
    implementation("org.glassfish.jersey.containers:jersey-container-jetty-http:3.1.5")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.1.5")

    // Jakarta JAX-RS API
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    // Jackson (explicit, though jersey-media-json-jackson already brings it)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    // Database driver (MariaDB)
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // Tests
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.test {
    useJUnitPlatform()
}