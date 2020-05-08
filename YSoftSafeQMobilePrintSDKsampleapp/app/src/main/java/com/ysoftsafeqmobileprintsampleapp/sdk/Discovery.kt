package com.ysoftsafeqmobileprintsampleapp.sdk

import android.net.wifi.WifiManager
import android.util.Log
import android.webkit.URLUtil
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import javax.net.ssl.HostnameVerifier
import kotlin.collections.ArrayList

class Discovery(
    private val discoveryCallback: DiscoveryCallback
) {

    private inner class IppDiscoveryListener : ServiceListener {


        override fun serviceAdded(event: ServiceEvent) {
            println("Service added: " + event.info)
        }

        override fun serviceRemoved(event: ServiceEvent) {
            println("Service removed: " + event.info)
        }

        override fun serviceResolved(event: ServiceEvent) {
            println("Service resolved: " + event.info)
            printInfoAboutServiceEvent(event)
        }


        private fun printInfoAboutServiceEvent(ev: ServiceEvent) {

            val builderAddresses = StringBuilder()
            for (addrs in ev.info.inet4Addresses) {
                builderAddresses.append(
                    java.lang.String.format(
                        Locale.ENGLISH,
                        "%d.%d.%d.%d",
                        addrs.address[0],
                        addrs.address[1],
                        addrs.address[2],
                        addrs.address[3]
                    )
                )
                builderAddresses.append(", ")
            }

            val builder = StringBuilder()
            val e = ev.info.propertyNames
            while (e.hasMoreElements()) {
                val name = e.nextElement()
                builder.append(name).append(" : ").append(ev.info.getPropertyString(name))
                    .append(", ")
            }
            println(builder)

            val domaininUrl:String = "http://" + ev.info.address.toString() + ":631/" +  ev.info.getPropertyString("rp")
            println(domaininUrl)

            if (domaininUrl.toHttpUrlOrNull() != null) {
                ippDomainsForVerification?.add(domaininUrl)
            }
        }

    }

    interface DiscoveryCallback {
        fun showDialog(title: String, message: String)
        fun promptUserForUrlConfirmation(url: HttpUrl)
        fun hideDiscoveringBtn(flag: Boolean)
    }

    var wifiManager: WifiManager? = null
    var serverName: String = ""
    private val trust = CustomTrust()
    private val domainsForVerification: ArrayList<String> = ArrayList()
    private val ippDomainsForVerification: ArrayList<String> = ArrayList()
    private var currentUrl: String? = null
    var jmdns: JmDNS? = null
    var discoveryThread: Thread? = null

    private fun getUrl(suffix: String): String {
        val items = serverName.split("://")
        if (items.size > 1) {
            return "https://" + items[1] + suffix
        }
        return serverName + suffix
    }

    companion object {
        var discovery_client: OkHttpClient = OkHttpClient()
    }

    private fun getLocalIpAddress(): InetAddress? {

        val wifiInfo = wifiManager?.connectionInfo
        val ipAddress = wifiInfo?.ipAddress
        var address: InetAddress? = null
        try {
            if (ipAddress != null) {
                address = InetAddress.getByName(
                    java.lang.String.format(
                        Locale.ENGLISH,
                        "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )
                )
            }
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        return address
    }

    private var mIppDiscoveryListener : IppDiscoveryListener? = null
    fun startIppDiscovery() {

        if (jmdns != null) {
            return
        }
        this.discoveryThread = Thread {
            try {
                mIppDiscoveryListener = IppDiscoveryListener()

                val addr: InetAddress? = getLocalIpAddress()
                val hostname = addr?.hostName
                this.jmdns = JmDNS.create(addr, hostname)
                this.jmdns?.addServiceListener("_ipp._tcp.local.", mIppDiscoveryListener)

            } catch (e: UnknownHostException) {
                System.out.println(e.message)
            } catch (e: IOException) {
                System.out.println(e.message)
            }
        }
        this.discoveryThread?.start()
    }

    fun discoverServer() {
        discoveryCallback.hideDiscoveringBtn(false)
        discovery_client = trust.clientBuilder
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .followRedirects(true)
            .callTimeout(4, TimeUnit.SECONDS) //for ysoft.local, as the server does not respond
            .connectTimeout(4, TimeUnit.SECONDS)
            .build()

        domainsForVerification.clear()

        if (serverName.isNotEmpty()) {

            val enteredUrl = serverName
            if (!URLUtil.isValidUrl(getUrl(enteredUrl))) {

                if (!enteredUrl.contains("safeq6")) {
                    domainsForVerification.add("https://safeq6.$enteredUrl:8050")
                    domainsForVerification.add("https://safeq6.$enteredUrl")
                    domainsForVerification.add("https://safeq6.$enteredUrl/end-user/ui/")
                    domainsForVerification.add("https://safeq6.$enteredUrl:9443/end-user/ui/")
                }

                domainsForVerification.add("https://$enteredUrl:8050")
                domainsForVerification.add("https://$enteredUrl")
                domainsForVerification.add("https://$enteredUrl/end-user/ui/")
                domainsForVerification.add("https://$enteredUrl:9443/end-user/ui/")

            } else {
                val currentDomain = URL(serverName).host

                if (!currentDomain.contains("safeq6")) {
                    domainsForVerification.add("https://safeq6.$currentDomain:8050")
                    domainsForVerification.add("https://safeq6.$currentDomain")
                    domainsForVerification.add("https://safeq6.$currentDomain/end-user/ui/")
                    domainsForVerification.add("https://safeq6.$currentDomain:9443/end-user/ui/")
                }

                domainsForVerification.add("https://$currentDomain:8050")
                domainsForVerification.add("https://$currentDomain")
                domainsForVerification.add("https://$currentDomain/end-user/ui/")
                domainsForVerification.add("https://$currentDomain:9443/end-user/ui/")

            }

        }

        // merge domains from mDNS search
        domainsForVerification.addAll(ippDomainsForVerification)

        if (domainsForVerification.size == 0) {
            discoveryCallback.showDialog("No server discovered",
                "Please try to enter your domain (e.g. ysoft.local) into Server textfield and press Discover button again."
            )
            discoveryCallback.hideDiscoveringBtn(true)
            return
        }

        verifyDomain()

    }

    fun verifyDomain() {

        if (domainsForVerification.size == 0) {
            discoveryCallback.showDialog("Discovery", "No print server found")
            discoveryCallback.hideDiscoveringBtn(true)
            return
        }
        val domain = domainsForVerification[0]
        domainsForVerification.removeAt(0)
        currentUrl = domain
        findServerInDomain(domain)
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
            discoveryCallback.showDialog("Server discovery failed", ex.message.toString())
            discoveryCallback.hideDiscoveringBtn(true)
        }
        return call!!
    }

    private fun findServerInDomain(url: String) {

        try {
            get(
                url,
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.i("fail", e.toString())
                        serverDiscoveryFailed()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.code == 404) {
                            serverDiscoveryFailed()
                            return
                        }

                        val responseString: String? = response.body?.string()

                        if (responseString == null) {
                            serverDiscoveryFailed()
                            return
                        }

                        if (responseString.startsWith("MIG")) {
                            discoveryCallback.promptUserForUrlConfirmation(response.request.url)
                        } else if (responseString.startsWith("AP hello")) {
                            // SQ5 AP = MiG
                            discoveryCallback.promptUserForUrlConfirmation(response.request.url)
                        } else if (response.request.url.toString().contains("631")) {
                            // CUPS delivery
                            discoveryCallback.promptUserForUrlConfirmation(response.request.url)
                        } else {
                            val csrfPattern: Pattern = Pattern.compile("_csrf\".*")
                            val csrfMatcher: Matcher = csrfPattern.matcher(responseString)
                            if (csrfMatcher.find()) {
                                val loginToken = responseString?.substring(
                                    csrfMatcher.start() + 14,
                                    csrfMatcher.end() - 2
                                )
                                if (loginToken.isNotEmpty()) {
                                    discoveryCallback.promptUserForUrlConfirmation(response.request.url)
                                } else {
                                    discoveryCallback.showDialog("Discovery failed", "Server does not provide EU/MPS interface.")
                                    //discoveryCallback.showLoginProgressBar(false)
                                }
                            } else {
                                serverDiscoveryFailed()
                            }
                        }
                    }
                }, discovery_client
            )
        } catch (ex: NullPointerException) {
            return
        }

    }

    fun serverDiscoveryFailed() {
        verifyDomain()
    }

}