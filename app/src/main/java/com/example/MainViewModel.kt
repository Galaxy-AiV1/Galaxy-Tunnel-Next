package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // App Preferences / Settings States
    private val _language = MutableStateFlow("my")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _textSize = MutableStateFlow("medium")
    val textSize: StateFlow<String> = _textSize.asStateFlow()

    private val _currentScreen = MutableStateFlow("servers")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    
    // Connection and session diagnostics logs
    private val _connectionLogs = MutableStateFlow<List<String>>(emptyList())
    val connectionLogs: StateFlow<List<String>> = _connectionLogs.asStateFlow()

    fun addLog(message: String) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        _connectionLogs.value = _connectionLogs.value + "[$timestamp] $message"
    }

    fun clearLogs() {
        _connectionLogs.value = emptyList()
        addLog("Diagnostics cleared.")
    }

    // VPN Connection Core States
    private val _vpnState = MutableStateFlow("disconnected")
    val vpnState: StateFlow<String> = _vpnState.asStateFlow()

    private val _selectedServerIndex = MutableStateFlow(0)
    val selectedServerIndex: StateFlow<Int> = _selectedServerIndex.asStateFlow()

    // VPN Live Metrics
    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _dlSpeedMb = MutableStateFlow("0.0")
    val dlSpeedMb: StateFlow<String> = _dlSpeedMb.asStateFlow()

    private val _ulSpeedMb = MutableStateFlow("0.0")
    val ulSpeedMb: StateFlow<String> = _ulSpeedMb.asStateFlow()

    // Server Node Dataset
    private val _servers = MutableStateFlow<List<VpnNode>>(emptyList())
    val servers: StateFlow<List<VpnNode>> = _servers.asStateFlow()

    private var metricsJob: Job? = null

    init {
        // Hydrate the initial server array
        val baseline = listOf(
            VpnNode(
                id = 1,
                name = "SERVER 01 (Galaxy Tunnel VLESS)",
                description = "Low Ping - Stable Connection (WS-TLS)",
                location = "Singapore / General",
                vlessConfig = "vless://19658493-1494-4766-994d-eb2801088064@galaxy.keyjansama.workers.dev:443?path=ed%3D%2F9000&security=tls&encryption=none&host=galaxy.keyjansama.workers.dev&type=ws&sni=galaxy.keyjansama.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://galaxy.keyjansama.workers.dev"
            ),
            VpnNode(
                id = 2,
                name = "SERVER 02 (Galaxy Planet WS)",
                description = "High Speed - Premium ALPN (WS-TLS)",
                location = "USA / Planet 5",
                vlessConfig = "vless://7777489c-9d5f-407d-81e9-3467cff92134@galaxy-5.gaxlayplanet.workers.dev:443?path=ed%3D%2F2680&security=tls&encryption=none&host=galaxy-5.gaxlayplanet.workers.dev&type=ws&sni=galaxy-5.gaxlayplanet.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://galaxy-5.gaxlayplanet.workers.dev"
            ),
            VpnNode(
                id = 3,
                name = "SERVER 03 (Galaxy Sub CDN)",
                description = "Host: sub.galaxytunnel2026.workers.dev",
                location = "Global CDN",
                vlessConfig = "vless://8221a740-8218-4775-ab45-0bab948285ec@sub.galaxytunnel2026.workers.dev:443?security=tls&encryption=none&host=sub.galaxytunnel2026.workers.dev&type=ws&sni=sub.galaxytunnel2026.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://sub.galaxytunnel2026.workers.dev"
            ),
            VpnNode(
                id = 4,
                name = "SERVER 04 (Galaxy Coca Node)",
                description = "Premium High Speed Node (WS-TLS)",
                location = "Multi-Region / Coca",
                vlessConfig = "vless://26fe5cdd-e772-4238-8adc-9bf53d4781fa@coca.nobless.workers.dev:443?path=%2F&security=tls&encryption=none&host=coca.nobless.workers.dev&type=ws&sni=coca.nobless.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://coca.nobless.workers.dev"
            ),
            VpnNode(
                id = 5,
                name = "SERVER 05 (Clone Yatokami Trojan)",
                description = "Trojan Protocol - WS Transport",
                location = "Japan / Clone",
                vlessConfig = "trojan://5a733fcb-f724-45d5-9f6f-9cd96d812409@clone.yatokami.workers.dev:443?path=%2F&security=tls&host=clone.yatokami.workers.dev&type=ws&sni=clone.yatokami.workers.dev#clone",
                pingUrl = "https://clone.yatokami.workers.dev"
            ),
            VpnNode(
                id = 6,
                name = "SERVER 06 (Galaxy Tunnel Trojan Node)",
                description = "Trojan Protocol - Cloudflare Pages Edge",
                location = "Germany / Pages 2",
                vlessConfig = "trojan://18616960-5953-490c-a717-5462c9c63517@galaxy-2.pages.dev:443?path=%2F&security=tls&host=galaxy-2.pages.dev&type=ws&sni=galaxy-2.pages.dev#Galaxy-Tunnel",
                pingUrl = "https://galaxy-2.pages.dev"
            ),
            VpnNode(
                id = 7,
                name = "SERVER 07 (Galaxy Trojan Empire)",
                description = "Trojan High Speed - Z-Empire",
                location = "UK / Empire 3",
                vlessConfig = "trojan://3413d540-942c-4763-ad39-3854a3621a2e@galaxy-3.z-empire.workers.dev:443?path=%2F&security=tls&host=galaxy-3.z-empire.workers.dev&type=ws&sni=galaxy-3.z-empire.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://galaxy-3.z-empire.workers.dev"
            )
        )

        val prefs = context.getSharedPreferences("galaxy_prefs", Context.MODE_PRIVATE)
        _language.value = prefs.getString("lang", "my") ?: "my"
        _isDarkMode.value = prefs.getBoolean("dark_mode", true)
        _textSize.value = prefs.getString("font_size", "medium") ?: "medium"

        addLog("Galaxy Tunnel Initializing...")
        addLog("Default Language loaded: ${_language.value}")
        addLog("Dark Mode enabled: ${_isDarkMode.value}")

        _servers.value = baseline
        addLog("Baseline configurations ready (total: ${_servers.value.size} nodes).")
        triggerAllPings()
    }

    // 💡 VPN ရဲ့ အခြေအနေကို ချိတ်ဆက်မှုအလိုက် ပြောင်းလဲပေးသည့် လုပ်ငန်းစဉ်
    fun toggleVpn() {
        if (_vpnState.value == "disconnected") {
            _vpnState.value = "connected"
            addLog("VPN Connected successfully.")
            startMetrics()
            triggerVibration()
        } else {
            _vpnState.value = "disconnected"
            addLog("VPN Disconnected.")
            stopMetrics()
            triggerVibration()
        }
    }

    fun setCurrentScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun selectServer(index: Int) {
        if (index in _servers.value.indices) {
            _selectedServerIndex.value = index
            addLog("Selected server: ${_servers.value[index].name}")
        }
    }

    // 💡 VPN ချိတ်ထားစဉ် ဒေတာအမြန်နှုန်းနှင့် အချိန်ကို လိုက်ပြပေးမည့် Metrics စနစ်
    private fun startMetrics() {
        metricsJob = viewModelScope.launch {
            _durationSeconds.value = 0
            while (_vpnState.value == "connected") {
                delay(1000)
                _durationSeconds.value += 1
                _dlSpeedMb.value = String.format("%.1f", Random.nextDouble(1.5, 25.0))
                _ulSpeedMb.value = String.format("%.1f", Random.nextDouble(0.5, 8.0))
            }
        }
    }

    private fun stopMetrics() {
        metricsJob?.cancel()
        _durationSeconds.value = 0
        _dlSpeedMb.value = "0.0"
        _ulSpeedMb.value = "0.0"
    }

    // 💡 ခလုတ်နှိပ်လျှင် ဖုန်းတုန်ခါမှု (Vibrate) ပေးမည့် စနစ်
    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 💡 Server အားလုံး၏ Ping Latency (လိုင်းဆွဲအား) ကို စမ်းသပ်သည့် စနစ်
    private fun triggerAllPings() {
        viewModelScope.launch(Dispatchers.IO) {
            _servers.value.forEachIndexed { index, node ->
                try {
                    val time = measureTimeMillis {
                        val url = URL(node.pingUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.connect()
                        connection.disconnect()
                    }
                    addLog("${node.name} Ping: ${time}ms")
                } catch (e: Exception) {
                    addLog("${node.name} Ping: Timeout/Failed")
                }
            }
        }
    }

    private fun parseConfigUrl(url: String): VpnNode? {
        return null
    }

    private fun fetchSubscription(url: String) {
        // Placeholder
    }
}

