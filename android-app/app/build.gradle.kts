plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Lee app/google-services.json (no versionado; el CI genera uno de relleno).
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.lenterosa.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lenterosa.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // Mapa (RF-09, ADR 0009): OpenFreeMap por defecto, sin clave. Se puede cambiar de proveedor
        // (por ejemplo Stadia Maps) con propiedades de Gradle, sin tocar el código; la atribución
        // y el nombre del proveedor cambian junto con el estilo.
        buildConfigField("String", "MAP_STYLE_URL", quoted(mapProperty("mapStyleUrl", "https://tiles.openfreemap.org/styles/liberty")))
        // Estilo oscuro para el tema oscuro: un mapa claro de noche deslumbra y llama la atención en la calle.
        buildConfigField("String", "MAP_STYLE_URL_DARK", quoted(mapProperty("mapStyleUrlDark", "https://tiles.openfreemap.org/styles/dark")))
        buildConfigField("String", "MAP_PROVIDER_NAME", quoted(mapProperty("mapProviderName", "OpenFreeMap")))
        buildConfigField(
            "String",
            "MAP_ATTRIBUTION",
            quoted(mapProperty("mapAttribution", "© OpenFreeMap © OpenMapTiles · Datos © colaboradores de OpenStreetMap")),
        )
        buildConfigField(
            "String",
            "MAP_ATTRIBUTION_URL",
            quoted(mapProperty("mapAttributionUrl", "https://www.openstreetmap.org/copyright")),
        )
    }

    buildTypes {
        debug {
            // La API local vista desde el emulador de Android.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            isMinifyEnabled = false
            // Se reemplaza por la URL pública de la API cuando exista el despliegue (RNF-05).
            buildConfigField("String", "API_BASE_URL", "\"https://api.lente-rosa.invalid/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // En las pruebas de JVM, las clases de Android (por ejemplo Log) devuelven valores por defecto.
        unitTests.isReturnDefaultValues = true
        // Robolectric necesita los recursos para las pruebas de UI en la JVM.
        unitTests.isIncludeAndroidResources = true
    }
}

// Robolectric usa una versión de Android que normalmente descarga por su cuenta durante las pruebas.
// Aquí la descarga Gradle (con su caché, también en el CI) y Robolectric trabaja sin conexión.
val robolectricAndroidAll: Configuration by configurations.creating

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.maplibre.android)
    implementation(libs.haze.glass)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    robolectricAndroidAll(libs.robolectric.android.all)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    val androidAll: FileCollection = robolectricAndroidAll
    inputs.files(androidAll).withPropertyName("robolectricAndroidAll")
    jvmArgumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                "-Drobolectric.offline=true",
                "-Drobolectric.dependency.dir=${androidAll.singleFile.parentFile.absolutePath}",
            )
        },
    )
}

// Propiedad de Gradle `lenteRosa.<nombre>` (por ejemplo en ~/.gradle/gradle.properties o con -P), o el valor por defecto.
fun mapProperty(name: String, default: String): String =
    providers.gradleProperty("lenteRosa.$name").getOrElse(default)

fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
