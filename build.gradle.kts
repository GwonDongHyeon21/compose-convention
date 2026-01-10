// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.gwondh.convention"
version = "1.0.0"

dependencies {
    compileOnly("com.android.tools.build:gradle:8.2.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
}

gradlePlugin {
    plugins {
        create("myComposeConvention") {
            id = "com.gwondh.compose.code-convention"
            implementationClass = "com.gwondh.customcodeconvention.CustomCodeConvention"
        }
    }
}