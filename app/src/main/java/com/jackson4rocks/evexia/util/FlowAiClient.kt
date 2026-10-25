package com.jackson4rocks.evexia.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.webkit.WebViewAssetLoader
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class FlowAiTurn(
    val role: String,
    val text: String,
)

class FlowAiException(
    message: String,
) : IOException(message)

enum class FlowAiAvailability {
    CHECKING,
    SIGN_IN_REQUIRED,
    ERROR,
    READY,
}

/** Bridges the in-app WebView to Puter.js, which performs chat in the cloud. */
object FlowAiClient {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestIds = AtomicLong()
    private val pendingRequests = ConcurrentHashMap<String, CancellableContinuation<String>>()
    private val progressCallbacks = ConcurrentHashMap<String, (String) -> Unit>()
    private val _availability = MutableStateFlow(FlowAiAvailability.CHECKING)
    val availability: StateFlow<FlowAiAvailability> = _availability.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _showSignIn = MutableStateFlow(false)
    val showSignIn: StateFlow<Boolean> = _showSignIn.asStateFlow()

    @Volatile
    private var webView: WebView? = null

    fun attach(view: WebView) {
        if (webView !== view) {
            _availability.value = FlowAiAvailability.CHECKING
            _message.value = null
        }
        webView = view
    }

    fun detach(view: WebView) {
        if (webView === view) webView = null
        pendingRequests.entries.toList().forEach { (id, continuation) ->
            if (pendingRequests.remove(id, continuation)) {
                fail(continuation, FlowAiException("Evexia AI closed before the online reply finished."))
            }
        }
    }

    fun onProgress(id: String, text: String) {
        if (text.length > 100_000) return
        mainHandler.post { progressCallbacks[id]?.invoke(text) }
    }

    fun signIn() {
        mainHandler.post { _showSignIn.value = true }
    }

    fun dismissSignIn() {
        mainHandler.post { _showSignIn.value = false }
    }

    fun signOut() {
        evaluate("window.evexiaPuterSignOut && window.evexiaPuterSignOut()")
    }

    fun retryConnection() {
        mainHandler.post {
            _availability.value = FlowAiAvailability.CHECKING
            _message.value = null
            webView?.reload()
        }
    }

    fun onStatus(signedIn: Boolean, error: String?) {
        mainHandler.post {
            _availability.value = when {
                error != null -> FlowAiAvailability.ERROR
                signedIn -> FlowAiAvailability.READY
                else -> FlowAiAvailability.SIGN_IN_REQUIRED
            }
            _message.value = error
            if (signedIn) _showSignIn.value = false
        }
    }

    fun onReply(id: String, text: String?, error: String?) {
        val continuation = pendingRequests.remove(id) ?: return
        progressCallbacks.remove(id)
        mainHandler.post {
            if (error != null) {
                fail(continuation, FlowAiException(error))
            } else if (text.isNullOrBlank()) {
                fail(continuation, FlowAiException("Puter AI returned an empty response. Try again."))
            } else {
                succeed(continuation, text)
            }
        }
    }

    suspend fun generate(
        turns: List<FlowAiTurn>,
        thinkMore: Boolean,
        onProgress: (String) -> Unit = {},
    ): String = suspendCancellableCoroutine { continuation ->
        if (_availability.value != FlowAiAvailability.READY) {
            fail(continuation, FlowAiException("Sign in to Puter to use online Evexia AI."))
            return@suspendCancellableCoroutine
        }
        val id = requestIds.incrementAndGet().toString()
        pendingRequests[id] = continuation
        progressCallbacks[id] = onProgress
        continuation.invokeOnCancellation {
            pendingRequests.remove(id)
            progressCallbacks.remove(id)
        }

        val messages = JSONArray().apply {
            turns.forEach { turn ->
                put(
                    JSONObject()
                        .put("role", if (turn.role == "assistant") "assistant" else "user")
                        .put("content", turn.text),
                )
            }
        }
        val model = if (thinkMore) "gpt-5.6-luna" else "gemini-3.1-flash-lite"
        val script = "window.evexiaPuterChat(" +
            "${JSONObject.quote(id)},${JSONObject.quote(messages.toString())}," +
            "${JSONObject.quote(model)},${if (thinkMore) "true" else "false"})"
        mainHandler.post {
            val currentWebView = webView
            if (currentWebView == null) {
                if (pendingRequests.remove(id, continuation)) {
                    fail(continuation, FlowAiException("Open Evexia AI to connect to Puter."))
                }
            } else {
                currentWebView.evaluateJavascript(script, null)
            }
        }
    }

    fun bridge(): Any = PuterBridge()

    fun assetLoader(context: Context): WebViewAssetLoader =
        WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(context.applicationContext),
            )
            .build()

    private fun evaluate(script: String) {
        mainHandler.post { webView?.evaluateJavascript(script, null) }
    }

    private fun fail(continuation: CancellableContinuation<String>, error: Throwable) {
        continuation.resumeWith(Result.failure(error))
    }

    private fun succeed(continuation: CancellableContinuation<String>, text: String) {
        continuation.resumeWith(Result.success(text))
    }

    @Keep
    private class PuterBridge {
        @JavascriptInterface
        @Keep
        fun onStatus(signedIn: Boolean, error: String?) {
            FlowAiClient.onStatus(signedIn, error)
        }

        @JavascriptInterface
        @Keep
        fun onReply(id: String, text: String?, error: String?) {
            if (id.length <= 20 && (text?.length ?: 0) <= 100_000 && (error?.length ?: 0) <= 2_000) {
                FlowAiClient.onReply(id, text, error)
            }
        }

        @JavascriptInterface
        @Keep
        fun onProgress(id: String, text: String) {
            if (id.length <= 20 && text.length <= 100_000) {
                FlowAiClient.onProgress(id, text)
            }
        }

        @JavascriptInterface
        @Keep
        fun onSignInClosed() {
            FlowAiClient.dismissSignIn()
        }
    }

    suspend fun generate(
        context: Context,
        turns: List<FlowAiTurn>,
        thinkMore: Boolean = false,
        onProgress: (String) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        val cleanTurns = turns.filter { it.text.isNotBlank() }.takeLast(if (thinkMore) 16 else 8)
        if (cleanTurns.none { it.role == "user" }) {
            throw FlowAiException("Ask Evexia AI something first.")
        }
        generate(cleanTurns, thinkMore, onProgress)
    }
}
