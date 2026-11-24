package com.lenterosa.app.di

import com.google.firebase.auth.FirebaseAuth
import com.lenterosa.app.BuildConfig
import com.lenterosa.app.account.AccountApi
import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.account.CredentialManagerGoogleIdTokenProvider
import com.lenterosa.app.account.FirebaseAuthGateway
import com.lenterosa.app.account.GoogleIdTokenProvider
import com.lenterosa.app.network.AndroidNetworkStatus
import com.lenterosa.app.network.NetworkStatus
import com.lenterosa.app.places.PlacesApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
