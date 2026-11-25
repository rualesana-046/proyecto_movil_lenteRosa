package com.lenterosa.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.lenterosa.app.BuildConfig
import com.lenterosa.app.account.AccountApi
import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.account.CredentialManagerGoogleIdTokenProvider
import com.lenterosa.app.account.FirebaseAuthGateway
import com.lenterosa.app.account.GoogleIdTokenProvider
import com.lenterosa.app.discreet.AndroidHelpLocation
import com.lenterosa.app.discreet.DataStoreDiscreetStore
import com.lenterosa.app.discreet.DiscreetStore
import com.lenterosa.app.discreet.HelpLocation
import com.lenterosa.app.map.AndroidDeviceLocation
import com.lenterosa.app.map.DeviceLocation
import com.lenterosa.app.network.AndroidNetworkStatus
import com.lenterosa.app.network.NetworkStatus
import com.lenterosa.app.offline.AndroidConnectivity
import com.lenterosa.app.offline.CacheDao
import com.lenterosa.app.offline.CacheDatabase
import com.lenterosa.app.offline.Connectivity
import com.lenterosa.app.offline.MapCacheCleaner
import com.lenterosa.app.offline.MapLibreCacheCleaner
import com.lenterosa.app.offline.OfflineCache
import com.lenterosa.app.offline.RoomOfflineCache
import com.lenterosa.app.onboarding.DataStoreOnboardingStore
import com.lenterosa.app.onboarding.OnboardingStore
import com.lenterosa.app.places.PlacesApi
import com.lenterosa.app.saved.SavedApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun firebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    // Sin interceptor de registro: los tokens nunca se escriben en los logs.
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun accountApi(retrofit: Retrofit): AccountApi = retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun placesApi(retrofit: Retrofit): PlacesApi = retrofit.create(PlacesApi::class.java)

    @Provides
    @Singleton
    fun savedApi(retrofit: Retrofit): SavedApi = retrofit.create(SavedApi::class.java)

    /** Caché sin conexión (RF-10). No entra en copias de seguridad ni transferencias (TD-03). */
    @Provides
    @Singleton
    fun cacheDatabase(@ApplicationContext context: Context): CacheDatabase =
        Room.databaseBuilder(context, CacheDatabase::class.java, "cache_sin_conexion.db").build()

    @Provides
    fun cacheDao(database: CacheDatabase): CacheDao = database.cache()

    /** Ajustes locales de la app (por ahora, si la bienvenida ya se completó). Nunca datos personales. */
    @Provides
    @Singleton
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("ajustes") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun authGateway(implementation: FirebaseAuthGateway): AuthGateway

    @Binds
    abstract fun googleIdTokenProvider(implementation: CredentialManagerGoogleIdTokenProvider): GoogleIdTokenProvider

    @Binds
    abstract fun networkStatus(implementation: AndroidNetworkStatus): NetworkStatus

    @Binds
    abstract fun deviceLocation(implementation: AndroidDeviceLocation): DeviceLocation

    @Binds
    abstract fun onboardingStore(implementation: DataStoreOnboardingStore): OnboardingStore

    @Binds
    abstract fun offlineCache(implementation: RoomOfflineCache): OfflineCache

    @Binds
    abstract fun mapCacheCleaner(implementation: MapLibreCacheCleaner): MapCacheCleaner

    @Binds
    abstract fun connectivity(implementation: AndroidConnectivity): Connectivity

    @Binds
    abstract fun discreetStore(implementation: DataStoreDiscreetStore): DiscreetStore

    @Binds
    abstract fun helpLocation(implementation: AndroidHelpLocation): HelpLocation
}
