package com.wasim.cookieretrieve

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myWebView = findViewById(R.id.webview)
        myWebView.settings.javaScriptEnabled = true

        // Set a custom WebViewClient
        myWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Tell the WebView to load the new URL
                view?.loadUrl(url ?: "")
                return true // Returning true indicates that the method has handled the URL
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectJavaScript(myWebView);
            }
        }

        myWebView.loadUrl("https://www.bathandbodyworks.com/my-account/edit-profile") // Load the initial URL.

        val getCookiesButton: Button = findViewById(R.id.button_get_cookie)
        getCookiesButton.setOnClickListener {
            getCurrentPageCookies()
        }
    }

    private fun injectJavaScript(view: WebView?) {
        val inputStream: InputStream = assets.open("script.js")
        val buffer = inputStream.readBytes()
        val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
        view?.loadUrl("javascript:(function() {" +
                "var parent = document.getElementsByTagName('head').item(0);" +
                "var script = document.createElement('script');" +
                "script.type = 'text/javascript';" +
                // Decoding the base64 encoded script
                "script.innerHTML = decodeURIComponent(escape(window.atob('" + encoded + "')));" +
                "parent.appendChild(script)" +
                "})()")
    }

    private fun getCurrentPageCookies() {
        val currentUrl = myWebView.url // Get the current URL from the WebView.
        currentUrl?.let { url ->
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrEmpty()) {

                val ck = parseCookies(cookies, ".publix.com")

                // Convert the list of cookies to JSON
                val gson = Gson()
                val json = gson.toJson(mapOf("cookies" to ck))

                copyToClipboard("Cookies", json)
                Toast.makeText(this, "Cookies copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No cookies found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun parseCookies(cookieString: String, domain: String): List<Cookie> {
        return cookieString.split("; ").map { cookie ->
            val (name, value) = cookie.split("=", limit = 2)
            Cookie(domain, name.trim(), "$name=$value")
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}

data class Cookie(
    val domain: String,
    val name: String,
    val value: String
)