import java.net.URL

val updateDetektJar by tasks.registering(Copy::class) {
    group = "verification"
    from(project.configurations.getByName("detektPlugins").files.filter {
        it.name.contains("compose-convention")
    })
    into(project.rootProject.file("config/detekt/rules"))
    rename { "custom-rules.jar" }
}

val downloadDetektConfig by tasks.registering {
    group = "verification"
    description = "Downloads default detekt.yml if not exists"

    val configDir = project.rootProject.file("config/detekt")
    val configFile = File(configDir, "detekt.yml")

    val rawUrl =
        "https://raw.githubusercontent.com/GwonDongHyeon21/compose-convention/refs/heads/dev/detekt.yml"

    doLast {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        if (!configFile.exists()) {
            runCatching {
                configFile.writeBytes(URL(rawUrl).readBytes())
            }.onFailure {
                println("Failed to download detekt.yml: ${it.message}")
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(updateDetektJar)
    dependsOn(downloadDetektConfig)
}