package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.result.contract.ActivityResultContracts
import android.net.VpnService
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import com.example.V2RayService 

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // 💡 VPN ခွင့်ပြုချက် (Pop-up) တောင်းခံပြီး အောင်မြင်လျှင် V2Ray နှိုးမည့် နေရာ
    val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startV2RayService()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            
            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) Color(0xFF14171B) else Color(0xFFF9F9FB)
                ) {
                    GalaxyTunnelApp(viewModel, ::checkVpnPermissionAndStart)
                }
            }
        }
    }

    // 💡 ဖုန်း System ရဲ့ VPN ခွင့်ပြုချက် ရှိမရှိ ကြိုတင်စစ်ဆေးပေးသည့် လုပ်ငန်းစဉ်
    private fun checkVpnPermissionAndStart() {
        val currentVpnState = viewModel.vpnState.value
        if (currentVpnState == "connected") {
            // ချိတ်ထားပြီးသားဆိုလျှင် ပိတ်ပစ်ရန် တိုက်ရိုက်ခေါ်ဆိုခြင်း
            stopV2RayService()
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnPrepareLauncher.launch(vpnIntent)
            } else {
                startV2RayService()
            }
        }
    }

    // 💡 တကယ့် V2Ray Core ဆီသို့ ရွေးချယ်ထားသော ဆာဗာ Config လှမ်းပို့ပြီး VPN စတင်နှိုးသည့် လုပ်ငန်းစဉ်
    private fun startV2RayService() {
        // ViewModel ထဲက အသုံးပြုသူ ရွေးချယ်ထားတဲ့ ဆာဗာရဲ့ vless/trojan config ကို လှမ်းယူခြင်း
        val selectedIndex = viewModel.selectedServerIndex.value
        val serverList = viewModel.servers.value
        val activeConfig = serverList.getOrNull(selectedIndex)?.vlessConfig ?: ""

        if (activeConfig.isNotEmpty()) {
            viewModel.addLog("Starting V2Ray Core Service...")
            val intent = Intent(this, V2RayService::class.java).apply {
                action = "com.v2ray.ang.action.START"
                putExtra("MAIN_COMMAND", activeConfig)
            }
            startService(intent)
            viewModel.toggleVpn() // UI ခလုတ်အား Connected အဖြစ် ပြောင်းလဲခြင်း
        } else {
            Toast.makeText(this, "No valid server configuration selected", Toast.LENGTH_SHORT).show()
        }
    }

    // 💡 VPN ပြန်လည်ပိတ်သိမ်းသည့် လုပ်ငန်းစဉ်
    private fun stopV2RayService() {
        viewModel.addLog("Stopping V2Ray Core Service...")
        val intent = Intent(this, V2RayService::class.java).apply {
            action = "com.v2ray.ang.action.STOP"
        }
        startService(intent)
        viewModel.toggleVpn() // UI ခလုတ်အား Disconnected အဖြစ် ပြောင်းလဲခြင်း
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxyTunnelApp(viewModel: MainViewModel, onConnectClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()

    val fontScale = when (textSize) {
        "small" -> 0.85f
        "medium" -> 1.0f
        "large" -> 1.2f
        else -> 1.0f
    }

    val bgColor = if (isDark) Color(0xFF14171B) else Color(0xFFF9F9FB)
    val cardColor = if (isDark) Color(0xFF1A1D22) else Color(0xFFF0F1F5)
    val borderColor = if (isDark) Color(0x0AFFFFFF) else Color(0x11000000)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                color = cardColor,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .border(1.dp, borderColor, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardColor)
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Galaxy Tunnel", color = textPrimary, fontSize = (18f * fontScale).sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = borderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text("Servers") },
                        selected = currentScreen == "servers",
                        onClick = { viewModel.setCurrentScreen("servers"); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = currentScreen == "settings",
                        onClick = { viewModel.setCurrentScreen("settings"); scope.launch { drawerState.close() } }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor, titleContentColor = textPrimary),
                    modifier = Modifier.border(1.dp, borderColor),
                    title = { Text("Galaxy Tunnel", fontSize = (16f * fontScale).sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = textPrimary)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // VPN ရဲ့ လက်ရှိအခြေအနေကို ပြသပေးသော စာသား
                    Text(
                        text = "Status: ${vpnState.uppercase()}",
                        color = if (vpnState == "connected") Color.Green else textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // VPN ချိတ်ဆက်မှု ခလုတ်
                    Button(
                        onClick = { onConnectClick() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (vpnState == "connected") Color.Red else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (vpnState == "connected") "STOP VPN" else "START VPN")
                    }
                }
            }
        }
    }
}
