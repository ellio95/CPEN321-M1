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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class ServerInfo(
    val ip: String,
    val localTime: String,
    val author: String,
    val status: String
)

data class User(
    val ip: String,
    val firstName: String,
    val lastName: String?,
)

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
    var user by remember { mutableStateOf<User?>(null) }
    var server by remember { mutableStateOf<ServerInfo?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                "Home" -> HomeScreen(

                    onNavigateToStatus = { currentScreen = "Status" },
                    onNavigateToProfile = { currentScreen = "LiveCanvas" },
                    onNavigateToSettings = { currentScreen = "Settings" },
                    modifier = Modifier.padding(innerPadding)
                )

                "Status" -> StatusScreen(
                    user = user,
                    server = server,
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = "Home" },
                    modifier = Modifier.padding(innerPadding),
                    onLogin = { user = it },
                    onFetch = { server = it },
                    setLoggingIn = { isLoggingIn = it },
                )

                "LiveCanvas" -> LiveCanvasScreen(
                    onBack = { currentScreen = "Home" },
                    apiBaseUrl = apiBaseUrl,
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
fun StatusScreen(
    user: User?,
    server: ServerInfo?,
    apiBaseUrl: String,
    onBack: () -> Unit,
    onLogin: (User) -> Unit,
    onFetch: (ServerInfo?) -> Unit,
    setLoggingIn: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val signedInMsg = "Status Screen"
    val loggingInMsg = "Logging in..."
    val errorMsg = "Error: Failed to log in"

    val context = LocalContext.current

    var mainText by remember(user) { mutableStateOf(if (user != null) signedInMsg else loggingInMsg) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LaunchedEffect(apiBaseUrl) {
            onFetch(fetchServerInfo(apiBaseUrl))
            if (user == null) {
                setLoggingIn(true)
                try {
                    val idToken = signInWithGoogle(context, BuildConfig.GOOGLE_CLIENT_ID)
                    if (idToken != null) {
                        val loggedInUser = loginWithBackend(apiBaseUrl, idToken)
                        if (loggedInUser != null) {
                            onLogin(loggedInUser)
                            mainText = signedInMsg
                        } else {
                            Toast.makeText(context, "Backend login failed", Toast.LENGTH_LONG).show()
                            mainText = errorMsg
                        }
                    } else {
                        Toast.makeText(context, "Google Sign-In failed", Toast.LENGTH_LONG).show()
                        mainText = errorMsg
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Login error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    mainText = errorMsg
                } finally {
                    setLoggingIn(false)
                }

            }
        }
        Text(text = mainText)
        Spacer(modifier = Modifier.height(16.dp))
        StatusInfo(server, user)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}

@Composable
fun LiveCanvasScreen(
    onBack: () -> Unit,
    apiBaseUrl: String,
    modifier: Modifier = Modifier
) {
    // 1. Maintain a 16x16 grid structure grid using a 2D snapshot list layout state tracking
    // Initialize everything to a neutral light-gray canvas background
    val canvasState = remember {
        mutableStateListOf<SnapshotStateList<Color>>().apply {
            for (i in 0 until 16) {
                val row = mutableStateListOf<Color>().apply {
                    for (j in 0 until 16) {
                        add(androidx.compose.ui.graphics.Color(0xFFE0E0E0))
                    }
                }
                add(row)
            }
        }
    }

    // A test loop simulation that drops randomized colors on random indices every 300 milliseconds 
    // to verify that layout updates and color parsing function perfectly until WebSockets are linked.
    LaunchedEffect(Unit) {
        val hexColors = listOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#000000", "#FFFFFF")
        while (true) {
            delay(300L)
            val randomX = (0 until 16).random()
            val randomY = (0 until 16).random()
            val randomHex = hexColors.random()
            
            // This safely mimics how your websocket handler will read json and update state:
            try {
                val colorObj = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(randomHex))
                canvasState[randomY][randomX] = colorObj
            } catch (e: Exception) {
                // Fail-safe color parse skip
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Live Pixel Art!",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Render the 16x16 canvas using an aspect-ratio bounded Box wrapper
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
                .background(Color.DarkGray)
                .padding(2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (y in 0 until 16) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (x in 0 until 16) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(0.5.dp) // Creates pixel border outlines grid grid spacing separation
                                    .background(canvasState[y][x])
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Back to Home")
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
fun StatusInfo(server: ServerInfo?, user: User?, modifier: Modifier = Modifier) {
    var tickCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(user, server) {
        if (user != null && server != null) {
            while (true) {
                kotlinx.coroutines.delay(1000L.milliseconds)
                tickCount++
            }
        }
    }

    val statusText = if (user != null && server != null) {
        val currentTick = tickCount // Triggers recomposition every second

        // 1. Calculate running client local time in requested format (hh:mm:ss GMT+hh:mm)
        val zonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = zonedDateTime.format(timeFormatter)
        val offsetId = zonedDateTime.offset.id
        val formattedOffset = if (offsetId == "Z") "+00:00" else offsetId
        val clientLocalTimeStr = "$formattedTime GMT$formattedOffset"

        // 2. Parse initial server local time string snapshot and add passed seconds to make it roll live
        val rollingServerTimeStr = try {
            // server.localTime looks like "hh:mm:ss GMT+hh:mm"
            val parts = server.localTime.split(" ")
            val timeParts = parts[0].split(":")
            val hours = timeParts[0].toInt()
            val minutes = timeParts[1].toInt()
            val seconds = timeParts[2].toInt()
            
            // Total seconds passed since the server response snapshot baseline
            val totalSeconds = hours * 3600 + minutes * 60 + seconds + currentTick
            
            val runningSeconds = totalSeconds % 60
            val runningMinutes = (totalSeconds / 60) % 60
            val runningHours = (totalSeconds / 3600) % 24
            
            val formattedServerTime = String.format(Locale.US, "%02d:%02d:%02d", runningHours, runningMinutes, runningSeconds)
            "$formattedServerTime ${parts[1]}" // Re-append the "GMT+hh:mm" zone component
        } catch (e: Exception) {
            server.localTime
        }

        "Server IP: ${server.ip}\n" +
        "User IP: ${user.ip}\n" +
        "Server Local Time: $rollingServerTimeStr\n" +
        "Client Local Time: $clientLocalTimeStr\n" +
        "Author: ${server.author}\n" +
        "User Name: ${user.firstName} ${user.lastName}\n"
    } else if (user != null) {
        "An error occurred when fetching server info, please try again later."
    } else {
        "Log in to view status."
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

private suspend fun loginWithBackend(apiBaseUrl: String, idToken: String): User? = withContext(Dispatchers.IO) {
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
                    val json = JSONObject(body)
                    User(
                        ip = json.optString("clientIP", ""),
                        firstName = json.optString("userFirstName", ""),
                        lastName = json.optString("userLastName", ""),
                    )
                } catch (e: Exception) {
                    null
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

private suspend fun fetchServerInfo(apiBaseUrl: String): ServerInfo? = withContext(Dispatchers.IO) {
    val baseUrl = apiBaseUrl.trimEnd('/')
    
    // Helper function to handle individual text-based GET requests safely
    fun performGetRequest(endpointUrl: String): String? {
        return try {
            val connection = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_000
                readTimeout = 3_000
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else  {
                Log.e("Fetch", "Error fetching $endpointUrl: HTTP ${connection.responseCode}")
                null
            }

        } catch (e: Exception) {
            Log.e("Fetch", "Error fetching $endpointUrl: ${e.message ?: e.javaClass.simpleName}")
            null
        }
    }

    // Fire endpoints concurrently using async coroutine primitives within CoroutineScope
    val scope = CoroutineScope(Dispatchers.IO)
    val ipDeferred = scope.async { performGetRequest("$baseUrl/ip") }
    val timeDeferred = scope.async { performGetRequest("$baseUrl/time") }
    val authorDeferred = scope.async { performGetRequest("$baseUrl/author") }

    try {
        val ipBody = ipDeferred.await() ?: return@withContext null
        val timeBody = timeDeferred.await() ?: return@withContext null
        val authorBody = authorDeferred.await() ?: return@withContext null

        val ipJson = JSONObject(ipBody)
        val timeJson = JSONObject(timeBody)
        val authorJson = JSONObject(authorBody)

        ServerInfo(
            ip = ipJson.optString("ip", ""),
            localTime = timeJson.optString("time", ""),
            author = authorJson.optString("author", ""),
            status = "ok"
        )
    } catch (e: Exception) {
        Log.e("Fetch", "Error parsing server info: ${e.message ?: e.javaClass.simpleName}")
        null
    }
}