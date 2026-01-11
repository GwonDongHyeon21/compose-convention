val updateDetektRules by tasks.registering(Copy::class) {
    group = "verification"
    description = "Downloads custom detekt rules for IDE integration"

    // 1. 'detektPlugins' 의존성 목록 중에서
    // 2. 이름에 'compose-convention'이 포함된 파일(내 라이브러리)만 찾아서
    from(project.configurations.getByName("detektPlugins").files.filter {
        it.name.contains("compose-convention")
    })

    // 3. 앱 프로젝트의 'config/detekt/rules' 폴더로 복사하고
    into(project.rootProject.file("config/detekt/rules"))

    // 4. 파일 이름을 'custom-rules.jar'로 고정 (IDE 설정이 깨지지 않게)
    rename { "custom-rules.jar" }
}

// 5. 앱이 빌드되기 전에 이 작업을 무조건 먼저 실행 (자동화)
tasks.named("preBuild").configure {
    dependsOn(updateDetektRules)
}