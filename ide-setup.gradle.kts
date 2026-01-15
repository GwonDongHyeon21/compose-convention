import java.net.URI

// JAR 파일 복사
val updateDetektJar by tasks.registering(Copy::class) {
    group = "verification"
    description = "Copies custom detekt rules. Automatically updates when version changes."

    val rulesDir = project.rootProject.file("config/detekt/rules")
    val detektConfig = project.configurations.getByName("detektPlugins")

    from(detektConfig)
    include("*compose-convention*")

    into(rulesDir)
    rename { "custom-rules.jar" }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// YML 파일 업데이트
val updateDetektConfig by tasks.registering {
    group = "verification"
    description = "Downloads detekt.yml if version changed or file is missing."

    val configDir = project.rootProject.file("config/detekt")
    val detektConfig = project.configurations.getByName("detektPlugins")

    val configFile = File(configDir, "detekt.yml")
    val versionFile = File(configDir, "version.txt")

    doLast {
        val artifact = detektConfig.resolvedConfiguration.resolvedArtifacts.find {
            it.name.contains("compose-convention") || it.moduleVersion.id.name.contains("compose-convention")
        }

        val currentVersion = artifact?.moduleVersion?.id?.version ?: "dev"
        val savedVersion = if (versionFile.exists()) versionFile.readText().trim() else ""

        if (currentVersion != savedVersion || !configFile.exists()) {
            println("[Auto-Setup] New version detected! ($savedVersion -> $currentVersion)")
            println("Updating detekt.yml...")

            if (!configDir.exists()) configDir.mkdirs()

            val rawUrl =
                "https://raw.githubusercontent.com/GwonDongHyeon21/compose-convention/refs/heads/dev/detekt.yml"

            runCatching {
                configFile.writeBytes(URI(rawUrl).toURL().readBytes())
                versionFile.writeText(currentVersion)
                println("detekt.yml updated successfully to version $currentVersion")
            }.onFailure {
                println("Update failed: ${it.message}")
            }
        } else {
            println("[Auto-Setup] detekt.yml is up-to-date ($currentVersion).")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(updateDetektJar)
    dependsOn(updateDetektConfig)
}
