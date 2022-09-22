package com.vdotok.icsdks.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 12:12 PM in 2021
 */
object ApiClient {

//    private val BASE_URL: String = "https://stenant.vdotok.dev/"
//    private val BASE_URL: String = "https://q-tenant.vdotok.dev/"
    //    private val BASE_URL: String = "https://stenant.vdotok.dev/"
    private val BASE_URL: String = "https://q-tenant.vdotok.dev/"
    private var retrofitAppClient: Retrofit? = null

    /**
     * Function to set and get retrofit client and set Http interceptor
     * @return Returns a Configured Retrofit object for api connection
     * */
    fun getClient(context: Context): Retrofit {
        if (retrofitAppClient == null) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
            clientBuilder.addInterceptor(interceptor)
            setSSLCert(clientBuilder)

            val client = clientBuilder.build()

            retrofitAppClient = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }
        return retrofitAppClient as Retrofit
    }

    private fun setSSLCert(httpClient: OkHttpClient.Builder) {
        // Load CAs from an InputStream

        try {
            val trustManagers: TrustManager = object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
            val x509TrustManager = trustManagers as X509TrustManager

            // Create an SSLSocketFactory that uses our TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(x509TrustManager), null)
            httpClient.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
    }


    private fun getCertificateFileName(): String? {
        return "vdotok_com.crt"
    }

}