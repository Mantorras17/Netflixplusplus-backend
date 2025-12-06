plugins {
    java
    id("application")
}

group = "org.netflixpp"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("org.netflixpp.Main")
}


repositories {
    mavenCentral()
}

val jerseyVersion = "3.1.5"
val jettyVersion = "11.0.24"
val log4jVersion = "2.22.1"
val jjwtVersion = "0.11.5"
val nettyVersion = "4.1.110.Final"
val slf4jVersion = "2.0.13"

dependencies {
    // Jersey (JAX-RS) - Jakarta
    implementation("org.glassfish.jersey.core:jersey-server:$jerseyVersion")
    implementation("org.glassfish.jersey.containers:jersey-container-servlet-core:$jerseyVersion")
    implementation("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:$jerseyVersion")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:$jerseyVersion")

    // Jetty (Servlet container) - Jakarta
    implementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
    implementation("org.eclipse.jetty:jetty-webapp:$jettyVersion")

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

    // Logging (Log4j2 + SLF4J binding)
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    // Netty for P2P mesh components
    implementation("io.netty:netty-all:$nettyVersion")

    // JSON Web Token (JJWT)
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // HK2 (Dependency Injection)
    implementation("org.glassfish.hk2:hk2-api:2.6.1")
    implementation("org.glassfish.hk2:hk2-locator:2.6.1")

    // Tests
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
