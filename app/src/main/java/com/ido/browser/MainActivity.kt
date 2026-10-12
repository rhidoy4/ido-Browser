package com.ido.browser

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.net.URLEncoder
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private var desktopMode = false
    private var darkMode = false
    private var privateMode = false
    private val blockedHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "adservice.google.com",
        "adnxs.com", "taboola.com", "outbrain.com", "popads.net"
    )
    private val prefs by lazy { getSharedPreferences("ido_browser", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        configureWebView()
        val last = prefs.getString("last_url", "https://www.google.com") ?: "https://www.google.com"
        loadAddress(last)
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(28, 30, 33))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.rgb(40, 42, 46))
        }
        val brand = TextView(this).apply {
            text = "ido"
            textSize = 22f
            setTextColor(Color.rgb(170, 120, 255))
            setPadding(dp(4), 0, dp(8), 0)
        }
        address = EditText(this).apply {
            hint = "Search or enter address"
            singleLine = true
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(dp(10), 0, dp(10), 0)
            setBackgroundColor(Color.rgb(60, 62, 67))
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO || event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    loadAddress(text.toString()); true
                } else false
            }
        }
        val go = button("GO") { loadAddress(address.text.toString()) }
        top.addView(brand)
        top.addView(address, LinearLayout.LayoutParams(0, dp(44), 1f))
        top.addView(go)
        root.addView(top)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(40, 42, 46))
        }
        listOf(
            "‹" to { if (web.canGoBack()) web.goBack() else Unit },
            "›" to { if (web.canGoForward()) web.goForward() else Unit },
            "＋ Tab" to { newTab() },
            "↓ Files" to { openDownloads() },
            "⋮ Menu" to { showMenu() }
        ).forEach { (label, action) ->
            val b = button(label, action)
            bottom.addView(b, LinearLayout.LayoutParams(0, dp(50), 1f))
        }
        root.addView(bottom)
        status = TextView(this).apply {
            text = "Ready • Media downloads depend on the website"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            setBackgroundColor(Color.rgb(35, 37, 40))
        }
        root.addView(status)
        setContentView(root)
    }

    private fun configureWebView() {
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.setSupportMultipleWindows(false)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.settings.mediaPlaybackRequiresUserGesture = true
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) status.text = title
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("magnet:", true)) {
                    Toast.makeText(this@MainActivity, "Magnet link detected. Torrent engine is not included in this starter build.", Toast.LENGTH_LONG).show()
                    return true
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                    return true
                }
                address.setText(url)
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                val host = request?.url?.host?.lowercase(Locale.ROOT) ?: return null
                if (blockedHosts.any { host == it || host.endsWith(".$it") }) {
                    return android.webkit.WebResourceResponse("text/plain", "utf-8", null)
                }
                return null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (!url.isNullOrBlank()) {
                    address.setText(url)
                    if (!privateMode) prefs.edit().putString("last_url", url).apply()
                }
            }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            startDownload(url, userAgent, contentDisposition, mimeType)
        })
    }

    private fun loadAddress(raw: String) {
        var value = raw.trim()
        if (value.isEmpty()) return
        if (!value.contains("://") && value.contains(".") && !value.contains(" ")) value = "https://$value"
        else if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")
        }
        address.setText(value)
        web.loadUrl(value)
    }

    private fun startDownload(url: String, userAgent: String?, disposition: String?, mimeType: String?) {
        try {
            if (url.startsWith("blob:", true) || url.startsWith("data:", true)) {
                Toast.makeText(this, "এই ধরনের Blob/Data মিডিয়া এই সংস্করণে সরাসরি ডাউনলোড করা যায় না।", Toast.LENGTH_LONG).show()
                return
            }
            val filename = URLUtil.guessFileName(url, disposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(filename)
                .setDescription("Downloading with ido Browser")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrBlank()) request.addRequestHeader("Cookie", cookies)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Download started: $filename", Toast.LENGTH_LONG).show()
            status.text = "Download started: $filename"
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showMenu() {
        val items = arrayOf(
            "Bookmarks: save current page",
            "History: show recent pages",
            "Desktop site: ${if (desktopMode) "ON" else "OFF"}",
            "Dark mode: ${if (darkMode) "ON" else "OFF"}",
            "Incognito: ${if (privateMode) "ON" else "OFF"}",
            "Refresh page",
            "Open downloads"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("ido Browser")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val url = web.url ?: return@setItems
                        val bookmarks = prefs.getStringSet("bookmarks", emptySet())!!.toMutableSet()
                        bookmarks.add(url)
                        prefs.edit().putStringSet("bookmarks", bookmarks).apply()
                        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
                    }
                    1 -> showSavedList("history", "History")
                    2 -> {
                        desktopMode = !desktopMode
                        web.settings.userAgentString = if (desktopMode)
                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"
                        else null
                        web.settings.useWideViewPort = desktopMode
                        web.settings.loadWithOverviewMode = desktopMode
                        web.reload()
                    }
                    3 -> {
                        darkMode = !darkMode
                        web.evaluateJavascript(
                            if (darkMode) "document.documentElement.style.filter='invert(1) hue-rotate(180deg)';" 
                            else "document.documentElement.style.filter='none';", null)
                    }
                    4 -> {
                        privateMode = !privateMode
                        if (privateMode) {
                            web.clearHistory()
                            web.clearCache(true)
                            CookieManager.getInstance().removeAllCookies(null)
                            Toast.makeText(this, "Private mode ON (basic; not a full privacy guarantee)", Toast.LENGTH_LONG).show()
                        } else Toast.makeText(this, "Private mode OFF", Toast.LENGTH_SHORT).show()
                    }
                    5 -> web.reload()
                    6 -> openDownloads()
                }
            }.show()
    }

    private fun showSavedList(key: String, title: String) {
        val entries = if (key == "history") {
            listOfNotNull(web.url)
        } else prefs.getStringSet(key, emptySet())?.toList() ?: emptyList()
        if (entries.isEmpty()) {
            Toast.makeText(this, "No saved entries yet", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this).setTitle(title).setItems(entries.toTypedArray()) { _, which ->
            loadAddress(entries[which])
        }.show()
    }

    private fun newTab() {
        // Lightweight starter behavior: open a fresh home page in the current WebView.
        loadAddress("https://www.google.com")
        Toast.makeText(this, "Starter version: tab button opens a fresh page; independent tab switching will be added later.", Toast.LENGTH_LONG).show()
    }

    private fun openDownloads() {
        try {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        } catch (_: Exception) {
            Toast.makeText(this, "Open the Downloads app to view downloaded files.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(48, 50, 55))
            setPadding(dp(3), 0, dp(3), 0)
            setOnClickListener { action() }
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        if (::web.isInitialized) web.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::web.isInitialized) web.onResume()
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
