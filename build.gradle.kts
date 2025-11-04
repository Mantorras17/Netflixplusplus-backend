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
    // Jersey (JAX-RS)
    implementation("org.glassfish.jersey.core:jersey-server:3.1.5")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.1.5")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:3.1.5")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:3.1.5")
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.5") // <== AQUI é o que faltava

    // Jetty (Servlet container)
    implementation("org.eclipse.jetty:jetty-server:11.0.24")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.24")
    implementation("org.eclipse.jetty:jetty-webapp:11.0.24")

    // Jakarta APIs
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    // Apache Commons IO
    implementation("commons-io:commons-io:2.15.1")

    // Database drivers
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    implementation("com.datastax.oss:java-driver-core:4.17.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.13")

    // JSON Web Token (JJWT)
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // HK2 (Injeção de dependência - base)
    implementation("org.glassfish.hk2:hk2-api:2.6.1")
    implementation("org.glassfish.hk2:hk2-locator:2.6.1")

    // Tests
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}



tasks.test {
    useJUnitPlatform()
}
