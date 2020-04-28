package com.ysoftsafeqmobileprintsampleapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ysoftsafeqmobileprintsampleapp.sdk.Discovery
import com.ysoftsafeqmobileprintsampleapp.sdk.Login
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.HttpUrl

class LoginActivity : AppCompatActivity() {

    val DELIVERY_ENDPOINT_EUI = "eui"
    val DELIVERY_ENDPOINT_MIG = "mig"

    var deliveryEndpoint = DELIVERY_ENDPOINT_MIG
    var sharedArrayList: ArrayList<String> = arrayListOf()
    lateinit var discoveryClass: Discovery

    var sharedUri = ""

    fun getUri(): String {
        return server_edittext.text.toString()
    }

    fun getLogin(): String {
        return username_edittext.text.toString()
    }

    fun getPassword(): String {
        return password_edittext.text.toString()
    }

    private fun setUrl(value: String) {
        runOnUiThread {
            server_edittext.setText(value)
        }
    }

    private fun getUrl(suffix: String): String {
        val uri = getUri()

        if (this.deliveryEndpoint == DELIVERY_ENDPOINT_MIG) {
            val items = uri.split("://")
            if (items.size > 1) {
                return "https://" + items[1] + suffix
            }
        }

        return uri + suffix
    }
    private val discoveryCallback = object : Discovery.DiscoveryCallback {
        override fun showDialog(title: String, message: String) {
            this@LoginActivity.runOnUiThread {
                val alertDialogBuilder = AlertDialog.Builder(this@LoginActivity)
                alertDialogBuilder.setTitle(title)
                alertDialogBuilder.setMessage(message)
                alertDialogBuilder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                val alert = alertDialogBuilder.create()
                alert.show()
            }
        }

        override fun promptUserForUrlConfirmation(url: HttpUrl) {
            this@LoginActivity.runOnUiThread {
                val alertDialogBuilder = AlertDialog.Builder(this@LoginActivity)
                alertDialogBuilder.setTitle("Discovery")
                alertDialogBuilder.setMessage(url.toString())
                alertDialogBuilder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                    setUrl(url.toString())
                    dialog.dismiss()
                }
                alertDialogBuilder.setNegativeButton(android.R.string.no) { dialog, _ ->
                    discoveryClass.verifyDomain()
                }
                val alert = alertDialogBuilder.create()
                alert.show()
            }
        }

        override fun hideDiscoveringBtn(flag: Boolean) {
            this@LoginActivity.runOnUiThread {
                runOnUiThread {
                    if (flag) {
                        discovery_button.text = "Discover"
                    } else {
                        discovery_button.text = "Discovering"
                    }
                    setAllButtons(flag)
                }
            }
        }
    }

    private val loginCallback = object : Login.LoginCallback {
        override fun showLoginProgressBar(flag: Boolean) {
            //indicate progress to user
            setAllButtons(!flag)
            runOnUiThread {
                if (flag) {
                    login_button.text = getString(R.string.logging_in)
                } else {
                    login_button.text = getString(R.string.login)
                }
            }

        }

        override fun invokeUploadActivity(token: String) {
            intent = Intent(this@LoginActivity, UploadActivity::class.java)
            intent.putExtra("deliveryEndpoint", deliveryEndpoint)
            intent.putExtra("token", token)
            intent.putExtra("serverUri", getUri())
            intent.putExtra("sharedUri", sharedUri)
            intent.putExtra("sharedArrayList", sharedArrayList)

            val sharedPref = getSharedPreferences("loginFlag", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("login", true)
                apply()
            }

            startActivity(intent)
            finish()
        }

        override fun savePreferences() {
            val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putString(getString(R.string.saved_server_uri_key), getUri())
                putString(getString(R.string.saved_user_login_key), getLogin())
                putString(getString(R.string.saved_user_password_key), getPassword())
                apply()
            }
        }

        override fun clearPreferences() {
            val shared = getPreferences(Context.MODE_PRIVATE) ?: return
            shared.edit().clear().apply()
        }

        override fun showDialog(title: String, message: String) {
            this@LoginActivity.runOnUiThread {
                val alertDialogBuilder = AlertDialog.Builder(this@LoginActivity)
                alertDialogBuilder.setTitle(title)
                alertDialogBuilder.setMessage(message)
                alertDialogBuilder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }

                val alert = alertDialogBuilder.create()
                alert.show()

            }
        }

    }

    private fun setAllButtons(flag: Boolean) {
        runOnUiThread {
            server_edittext.isEnabled = flag
            username_edittext.isEnabled = flag
            password_edittext.isEnabled = flag
            login_button.isEnabled = flag
            discovery_button.isEnabled = flag
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login_button.setOnClickListener {
            if (evaluateEmptyTextFields()) {
                val loginClass = Login(
                    loginCallback,
                    this,
                    getUrl(""),
                    getLogin(),
                    getPassword(),
                    true
                )

                loginClass.downloadLoginPage()
                deliveryEndpoint = loginClass.deliveryEndpoint
            } else {
                loginCallback.showDialog("Login Failed", "Empty fields detected")
            }
        }

        discoveryClass = Discovery(discoveryCallback, this)
        discovery_button.setOnClickListener {
            discoveryClass.serverName = server_edittext.text.toString()
            discoveryClass.discoverServer()
        }
    }

    private fun evaluateEmptyTextFields(): Boolean {
        return (server_edittext.text.isNotEmpty() && username_edittext.text.isNotEmpty()
                && password_edittext.text.isNotEmpty())
    }


}