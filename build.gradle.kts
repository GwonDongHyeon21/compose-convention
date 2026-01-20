// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.gwondh.convention"
version = "1.3.0"

dependencies {
    compileOnly("com.android.tools.build:gradle:8.2.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")

    // Detekt API
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.4")

    // Test
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "convention-compose"
            version = project.version.toString()
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}