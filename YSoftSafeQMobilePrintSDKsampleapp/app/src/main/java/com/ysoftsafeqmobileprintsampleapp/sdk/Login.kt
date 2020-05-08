package com.ysoftsafeqmobileprintsampleapp.sdk

import android.content.Context
import android.util.Log
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLProtocolException

class Login(
    private val loginCallback: LoginCallback,
    private val context: Context,
    private val serverUrl: String,
    private val username: String,
    private val password: String,
    private val savePreferences: Boolean
) {
    val DELIVERY_ENDPOINT_EUI = "eui"
    val DELIVERY_ENDPOINT_MIG = "mig"

    var deliveryEndpoint = DELIVERY_ENDPOINT_EUI

    interface LoginCallback {
        fun showLoginProgressBar(flag: Boolean)
        fun invokeUploadActivity(token: String)
        fun savePreferences()
        fun clearPreferences()
        fun showDialog(title: String, message: String)
    }

    companion object {
        var client: OkHttpClient = OkHttpClient()
    }

    fun get(url: String, callback: Callback, myclient: OkHttpClient): Call {
        var call: Call? = null
        try {
            val request = Request.Builder()
                .url(url)
                .header("Connection", "close")
                .build()

            call = myclient.newCall(request)
            call.enqueue(callback)

        } catch (ex: IllegalArgumentException) {

        }
        return call!!
    }

    private fun post(url: String, parameters: HashMap<String, String>, callback: Callback): Call {
        val builder = FormBody.Builder()
        val it = parameters.entries.iterator()
        while (it.hasNext()) {
            val pair = it.next() as Map.Entry<*, *>
            builder.add(pair.key.toString(), pair.value.toString())
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .header("Connection", "close")
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    private fun getUrl(suffix: String): String {

        if (this.deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            val items = serverUrl.split("://")
            if (items.size > 1) {
                return "https://" + items[1] + suffix
            }
        }

        return serverUrl + suffix
    }

    private fun getLoginUrl(): String {
        if (deliveryEndpoint == DELIVERY_ENDPOINT_EUI) {
            return getUrl("login")
        }
        if (deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            return getUrl("/Administration")
        }
        return getUrl("")
    }

    fun downloadLoginPage() {
        loginCallback.showLoginProgressBar(true)

        val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
        val trust = CustomTrust()

        client = trust.clientBuilder.cookieJar(cookieJar)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .followRedirects(true)
            .readTimeout(20000, TimeUnit.MILLISECONDS)
            .build()

        determineDeliveryEndpoint()
        val url = getLoginUrl()
        try {
            get(
                url,
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        loginCallback.showLoginProgressBar(false)
                        loginCallback.showDialog(
                            "Login Failed",
                            e.message.toString()
                        )
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d("getLogin", "success")
                        val responseString: String? = response.body?.string()
                        val csrfPattern: Pattern = getLoginPattern()
                        val csrfMatcher: Matcher = csrfPattern.matcher(responseString)

                        if (csrfMatcher.find()) {

                            if (deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
                                postLoginMig()
                            } else {
                                val loginToken = responseString?.substring(
                                    csrfMatcher.start() + 14,
                                    csrfMatcher.end() - 2
                                )
                                if (loginToken != null) {
                                    postLoginEui(loginToken)
                                } else {
                                    loginCallback.showDialog(
                                        "Login Failed",
                                        "Server does not provide EU/MPS interface."
                                    )
                                    loginCallback.showLoginProgressBar(false)
                                }
                            }

                        } else {
                            loginCallback.showDialog(
                                "Login Failed",
                                "Server does not provide interface for print upload."
                            )
                            loginCallback.showLoginProgressBar(false)
                        }
                    }
                }, client
            )
        } catch (ex: KotlinNullPointerException) {
            loginCallback.showDialog(
                "Could not download login page",
                ex.message.toString()
            )
            loginCallback.showLoginProgressBar(false)
            return
        }

    }

    private fun determineDeliveryEndpoint() {
        if (serverUrl.contains("end-user")) {
            this.deliveryEndpoint = DELIVERY_ENDPOINT_EUI
        }  else {
            this.deliveryEndpoint = DELIVERY_ENDPOINT_MIG
        }
    }

    private fun getLoginPattern(): Pattern {
        if (deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            return Pattern.compile("configuration")
        }
        return Pattern.compile("_csrf\".*")
    }

    fun postLoginMig() {
        val loginUrl: String = this.getUrl("/ipp/print")
        val isSavePreferencesChecked = savePreferences
        val ippRequest = IppEmptyRequest("")

        val requestBody = ippRequest.bytes.toRequestBody()

        val token = Credentials.basic(username, password)
        val request: Request = Request.Builder()
            .url(loginUrl)
            .header("Content-Type", "application/ipp")
            .header("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2")
            .header("Authorization", token)

            .post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    if (response.code == 401) {
                        loginCallback.showLoginProgressBar(false)
                        loginCallback.showDialog(
                            "Login Failed",
                            "Invalid credentials"
                        )

                        return
                    }

                    loginCallback.showLoginProgressBar(false)
                    loginCallback.showDialog(
                        "Login Failed",
                        "Communication with server failed. Code:" + response.code
                    )
                    return
                }



                if (isSavePreferencesChecked) {
                    try {
                        loginCallback.savePreferences()
                    } catch (ex: Exception) {
                        loginCallback.showLoginProgressBar(false)
                        loginCallback.showDialog(
                            "Failed to save credentials",
                            ex.message.toString()
                        )
                        return
                    }
                } else {
                    loginCallback.clearPreferences()
                }

                val token = Credentials.basic(username, password)
                loginCallback.invokeUploadActivity(token)


            }

            override fun onFailure(call: Call, e: IOException) {
                loginCallback.showLoginProgressBar(false)
                loginCallback.showDialog(
                    "Login Failed",
                    "Unable to communicate with server. " + e.localizedMessage
                )
            }
        })
    }

    fun postLoginEui(token: String) {

        val loginUrl: String = this.getUrl("j_spring_security_check")
        val isSavePreferencesChecked = savePreferences

        val params = HashMap<String, String>()
        params["username"] = username
        params["password"] = password
        params["_csrf"] = token

        post(loginUrl, params,
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    loginCallback.showLoginProgressBar(false)
                    loginCallback.showDialog(
                        "Login Failed",
                        e.message.toString()
                    )
                }

                override fun onResponse(call: Call, response: Response) {

                    try {
                        val responseString = response.body?.string()
                        val csrfPattern: Pattern = Pattern.compile("Login to YSoft SafeQ")
                        val csrfMatcher: Matcher = csrfPattern.matcher(responseString)
                        if (csrfMatcher.find()) {
                            loginCallback.showLoginProgressBar(false)
                            loginCallback.showDialog(
                                "Login Failed",
                                "Invalid credentials"
                            )

                            return
                        }
                    } catch (ex: SSLProtocolException) {

                    }

                    if (isSavePreferencesChecked) {
                        try {
                            loginCallback.savePreferences()
                        } catch (ex: Exception) {
                            loginCallback.showLoginProgressBar(false)
                            loginCallback.showDialog(
                                "Failed to save credentials",
                                ex.message.toString()
                            )
                            return
                        }
                    } else {
                        loginCallback.clearPreferences()
                    }
                    loginCallback.invokeUploadActivity("")
                }
            })

    }

}