import java.net.URI

val detektConfigDir = project.rootProject.file("config/detekt")
val detektRulesDir = detektConfigDir.resolve("rules")
val versionFile = detektConfigDir.resolve("version.txt")
val configFile = detektConfigDir.resolve("detekt.yml")

val setupDetekt by tasks.registering {
    group = "verification"
    description = "Updates detekt rules (JAR & YML) if the dependency version changes."

    doLast {
        val artifact = project
            .configurations
            .getByName("detektPlugins")
            .resolvedConfiguration.resolvedArtifacts.find {
                it.name.contains("compose-convention") || it.moduleVersion.id.name.contains("compose-convention")
            }

        if (artifact == null) {
            logger.warn("'compose-convention' 라이브러리를 찾을 수 없습니다. detektPlugins에 추가되었는지 확인하세요.")
            return@doLast
        }

        val currentVersion = artifact.moduleVersion.id.version
        val savedVersion = if (versionFile.exists()) versionFile.readText().trim() else ""

        // 버전이 다르거나, 필수 파일이 없으면 업데이트 실행
        if (currentVersion != savedVersion || !configFile.exists() || !detektRulesDir.exists()) {
            println("[Detekt Setup] 새로운 버전 감지! ($savedVersion -> $currentVersion)")
            println("업데이트를 진행합니다...")

            // 폴더가 없으면 생성
            if (!detektRulesDir.exists()) {
                detektRulesDir.mkdirs()
            } else {
                detektRulesDir.listFiles()?.forEach { it.delete() }
            }

            // JAR 파일 복사 (원본 이름 그대로 사용)
            val sourceJar = artifact.file
            val targetJar = detektRulesDir.resolve(sourceJar.name)

            try {
                sourceJar.copyTo(targetJar, overwrite = true)
                println("JAR 파일 업데이트 완료: ${targetJar.name}")
            } catch (e: Exception) {
                println("JAR 복사 실패: ${e.message}")
            }

            // YML 파일 다운로드
            val rawUrl =
                "https://raw.githubusercontent.com/GwonDongHyeon21/compose-convention/refs/heads/dev/detekt.yml"

            runCatching {
                configFile.writeBytes(URI(rawUrl).toURL().readBytes())
                println("detekt.yml 다운로드 완료")
            }.onFailure {
                println("detekt.yml 다운로드 실패 (기존 파일 유지): ${it.message}")
            }

            versionFile.writeText(currentVersion)
            println("Detekt 설정이 버전 $currentVersion (으)로 업데이트 되었습니다.\n")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(setupDetekt)
}