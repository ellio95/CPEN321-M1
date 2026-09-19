package com.example.cpen321application

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                MainApp(apiBaseUrl = BuildConfig.API_BASE_URL)
            }
        }
    }
}

@Composable
fun MainApp(apiBaseUrl: String) {
    var currentScreen by remember { mutableStateOf("Home") }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                "Home" -> HomeScreen(

                    onNavigateToStatus = { currentScreen = "Status" },
                    onNavigateToProfile = { currentScreen = "Profile" },
                    onNavigateToSettings = { currentScreen = "Settings" },
                    modifier = Modifier.padding(innerPadding)
                )

                "Status" -> StatusScreen(
                    userEmail = userEmail,
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = "Home" },
                    modifier = Modifier.padding(innerPadding),
                    onLogin = { email ->
                        userEmail = email
                    },
                    setLoggingIn = { isLoggingIn = it },
                )

                "Profile" -> DummyScreen(
                    title = "Profile Screen",
                    onBack = { currentScreen = "Home" },
                    modifier = Modifier.padding(innerPadding)
                )

                "Settings" -> DummyScreen(
                    title = "Settings Screen",
                    onBack = { currentScreen = "Home" },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            if (isLoggingIn) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateToStatus: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Main Menu")
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onNavigateToStatus) {
            Text("Server Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToProfile) {
            Text("Live Pixel Art")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToSettings) {
            Text("Timer")
        }
    }
}

@Composable
fun DummyScreen(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}

@Composable
fun StatusScreen(
    userEmail: String?,
    apiBaseUrl: String,
    onBack: () -> Unit,
    onLogin: (String) -> Unit,
    setLoggingIn: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mainText by remember(userEmail) { mutableStateOf(if (userEmail != null) "Signed in as $userEmail" else "Logging in...") }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LaunchedEffect(apiBaseUrl) {
            if (userEmail == null) {
                setLoggingIn(true)
                try {
                    val idToken = signInWithGoogle(context, BuildConfig.GOOGLE_CLIENT_ID)
                    if (idToken != null) {
                        val email = loginWithBackend(apiBaseUrl, idToken)
                        if (email != null) {
                            onLogin(email)
                            mainText = "Signed in as $email"
                        } else {
                            Toast.makeText(context, "Backend login failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Login error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoggingIn(false)
                }

            }
        }
        Text(text = mainText)
        Spacer(modifier = Modifier.height(16.dp))
        Greeting(apiBaseUrl = apiBaseUrl)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}

@Composable
fun Greeting(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var statusText by remember { mutableStateOf("Checking backend at $apiBaseUrl/health...") }

    LaunchedEffect(apiBaseUrl) {
        statusText = fetchHealthStatus(apiBaseUrl)
    }

    Text(
        text = statusText,
        modifier = modifier
    )
}

private suspend fun signInWithGoogle(context: Context, serverClientId: String): String? {
    if (serverClientId.isBlank()) {
        Log.e("Auth", "GOOGLE_CLIENT_ID is empty! Check local.properties")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Error: GOOGLE_CLIENT_ID is missing", Toast.LENGTH_LONG).show()
        }
        return null
    }

    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            googleIdTokenCredential.idToken
        } else {
            Log.e("Auth", "Unexpected credential type: ${credential.type}")
            null
        }
    } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed: ${e.message}", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        null
    }
}

private suspend fun loginWithBackend(apiBaseUrl: String, idToken: String): String? = withContext(Dispatchers.IO) {
    val loginUrl = "${apiBaseUrl.trimEnd('/')}/login"
    try {
        val connection = (URL(loginUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 5_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val jsonBody = JSONObject().apply {
            put("idToken", idToken)
        }.toString()

        connection.outputStream.use { it.write(jsonBody.toByteArray()) }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                try {
                    JSONObject(body).optString("email", "Signed In")
                } catch (e: Exception) {
                    "Signed In"
                }
            }
            else -> {
                Log.e("Auth", "Backend login failed: HTTP $code")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("Auth", "Backend unreachable", e)
        null
    }
}

private suspend fun fetchHealthStatus(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
    try {
        val connection = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "Backend healthy ($healthUrl): $body"
            }
            else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($healthUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($healthUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}