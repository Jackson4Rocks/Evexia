package com.jackson4rocks.evexia.ui.components

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jackson4rocks.evexia.util.FlowAiClient

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PuterAiWebHost() {
    val context = LocalContext.current
    val signInDialogVisible by FlowAiClient.showSignIn.collectAsState()
    val assetLoader = remember(context) { FlowAiClient.assetLoader(context) }
    var mainWebView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { viewContext ->
            WebView(viewContext).apply {
                visibility = View.INVISIBLE
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isFocusable = false
                isClickable = false
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(FlowAiClient.bridge(), "Evexia")
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean =
                        request.url.host != ASSET_HOST || request.url.scheme != "https"

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ) = assetLoader.shouldInterceptRequest(request.url)

                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript(
                            "if (window.evexiaPuterRefresh) window.evexiaPuterRefresh();",
                            null,
                        )
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: android.webkit.WebResourceError,
                    ) {
                        if (request.isForMainFrame) {
                            FlowAiClient.onStatus(false, "Couldn't load Puter.js. Check your internet connection and retry.")
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message,
                    ) = openPuterPopup(view, resultMsg)
                }
                FlowAiClient.attach(this)
                mainWebView = this
                loadUrl("https://appassets.androidplatform.net/assets/puter-bridge.html")
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            PuterPopupWindows.dismissAll()
            mainWebView?.let {
                FlowAiClient.detach(it)
                it.stopLoading()
                it.destroy()
            }
        }
    }

    if (signInDialogVisible) {
        Dialog(
            onDismissRequest = { FlowAiClient.dismissSignIn() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(FlowAiClient.bridge(), "Evexia")
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean =
                                request.url.host != ASSET_HOST || request.url.scheme != "https"

                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest,
                            ) = assetLoader.shouldInterceptRequest(request.url)
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message,
                            ) = openPuterPopup(view, resultMsg)
                        }
                        loadUrl("https://appassets.androidplatform.net/assets/puter-signin.html")
                    }
                },
                onRelease = {
                    it.stopLoading()
                    it.destroy()
                },
            )
        }
    }
}

private fun openPuterPopup(view: WebView, resultMsg: android.os.Message): Boolean {
    val progress = ProgressBar(view.context)
    val loadError = TextView(view.context).apply {
        text = "Puter sign-in couldn't load. Check your internet connection and try again."
        gravity = Gravity.CENTER
        visibility = View.GONE
        setPadding(32, 32, 32, 32)
    }
    val popupView = WebView(view.context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        setBackgroundColor(android.graphics.Color.WHITE)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message,
            ) = openPuterPopup(view, resultMsg)

            override fun onCloseWindow(window: WebView) {
                PuterPopupWindows.dismiss(window)
            }
        }
    }
    val container = FrameLayout(view.context).apply {
        setBackgroundColor(android.graphics.Color.WHITE)
        addView(
            popupView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        addView(
            progress,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
        addView(
            loadError,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
    val dialog = Dialog(view.context).apply {
        setContentView(container)
        setOnDismissListener {
            PuterPopupWindows.remove(popupView)
            popupView.stopLoading()
            popupView.destroy()
        }
        show()
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }
    popupView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ) = false

        override fun onPageCommitVisible(view: WebView, url: String) {
            progress.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: android.webkit.WebResourceError,
        ) {
            if (request.isForMainFrame) {
                progress.visibility = View.GONE
                loadError.visibility = View.VISIBLE
            }
        }
    }
    PuterPopupWindows.add(popupView, dialog)
    val transport = resultMsg.obj as WebView.WebViewTransport
    transport.webView = popupView
    resultMsg.sendToTarget()
    return true
}

private object PuterPopupWindows {
    private val dialogs = mutableMapOf<WebView, Dialog>()

    fun add(view: WebView, dialog: Dialog) {
        dialogs[view] = dialog
    }

    fun remove(view: WebView) {
        dialogs.remove(view)
    }

    fun dismiss(view: WebView) {
        dialogs[view]?.dismiss()
    }

    fun dismissAll() {
        dialogs.values.toList().forEach(Dialog::dismiss)
        dialogs.clear()
    }
}

private const val ASSET_HOST = "appassets.androidplatform.net"
