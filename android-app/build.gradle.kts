// AGP 9 trae Kotlin integrado (por defecto KGP 2.2.10). Se fija KGP 2.4.21 en el classpath,
// como indican las notas de AGP 9.0, para que coincida con el plugin del compilador de Compose.
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
