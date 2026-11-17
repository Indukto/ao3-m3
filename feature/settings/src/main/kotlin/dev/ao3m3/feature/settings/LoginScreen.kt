package dev.ao3m3.feature.settings

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ao3m3.core.data.net.Ao3Urls
import dev.ao3m3.core.data.repo.SessionRepository
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loggedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(loggedIn = session.loggedIn.value))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /**
     * Called when the WebView has a session cookie and the page header shows a signed-in username.
     * AO3 hands a session cookie to anonymous visitors too, so the cookie alone is not proof of a
     * login; without a real username the user stays on the page and can retry.
     *
     * Only the cookie and the visible username are kept; the password field was submitted by the
     * WebView directly to AO3.
     */
    fun onSessionCookie(cookieHeader: String, rawUserHref: String?) {
        val userName = rawUserHref
            ?.trim('"')
            ?.substringAfter("/users/", "")
            ?.substringBefore('/')
            ?.substringBefore('?')
            ?.takeIf { it.isNotBlank() && !it.equals("login", ignoreCase = true) }
            ?: return
        viewModelScope.launch {
            session.completeLogin(cookieHeader, userName)
            _state.value = LoginUiState(loggedIn = session.loggedIn.value)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log in to AO3") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Ao3Icons.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            val cookies = CookieManager.getInstance()
                                .getCookie(Ao3Urls.BASE)
                                ?: return
                            if (!cookies.contains("_otwarchive_session")) return

                            view.evaluateJavascript(
                                "(function(){" +
                                    "var a=document.querySelector('#header .primary .dropdown a[href^=\"/users/\"]');" +
                                    "if(!a){var links=document.querySelectorAll('#header a[href^=\"/users/\"]');" +
                                    "for(var i=0;i<links.length;i++){" +
                                    "if((links[i].textContent||'').trim().indexOf('Hi,')===0){a=links[i];break;}}}" +
                                    "return a ? a.getAttribute('href') : '';})()",
                            ) { result ->
                                viewModel.onSessionCookie(cookies, result)
                            }
                        }
                    }
                    loadUrl("${Ao3Urls.BASE}/users/login")
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
        )
    }
}
