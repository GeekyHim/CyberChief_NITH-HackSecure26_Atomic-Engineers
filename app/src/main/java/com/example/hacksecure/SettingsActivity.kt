package com.example.hacksecure

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var parentalEnabled by remember { mutableStateOf(ModeSettings.isParentalModeEnabled) }
                var developerEnabled by remember { mutableStateOf(ModeSettings.isDeveloperModeEnabled) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = "Settings") },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        bottomBar = {
                            DynamicBottomNavigation(
                                selectedTab = BottomTab.SETTINGS,
                                onHomeSelected = {
                                    startActivity(Intent(this@SettingsActivity, MainActivity::class.java))
                                    finish()
                                },
                                onLogsSelected = {
                                    startActivity(Intent(this@SettingsActivity, AppWiseLogsActivity::class.java))
                                    finish()
                                },
                                onAlertsSelected = {
                                    startActivity(Intent(this@SettingsActivity, AlertsActivity::class.java))
                                    finish()
                                },
                                onParentalSelected = {
                                    startActivity(Intent(this@SettingsActivity, ParentalModeActivity::class.java))
                                    finish()
                                },
                                onDeveloperSelected = {
                                    startActivity(Intent(this@SettingsActivity, DeveloperModeActivity::class.java))
                                    finish()
                                },
                                isParentalEnabled = parentalEnabled,
                                isDeveloperEnabled = developerEnabled
                            )
                        }
                    ) { innerPadding ->
                        SettingsScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            parentalEnabled = parentalEnabled,
                            developerEnabled = developerEnabled,
                            onParentalToggle = { checked ->
                                parentalEnabled = checked
                                ModeSettings.isParentalModeEnabled = checked
                            },
                            onDeveloperToggle = { checked ->
                                developerEnabled = checked
                                ModeSettings.isDeveloperModeEnabled = checked
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    parentalEnabled: Boolean,
    developerEnabled: Boolean,
    onParentalToggle: (Boolean) -> Unit,
    onDeveloperToggle: (Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModeToggleRow(
            title = "Parental Mode",
            description = "Restrict internet usage and block certain app categories",
            checked = parentalEnabled,
            onCheckedChange = onParentalToggle
        )

        ModeToggleRow(
            title = "Developer Mode",
            description = "Export network logs and access debugging tools",
            checked = developerEnabled,
            onCheckedChange = onDeveloperToggle
        )
    }
}

@Composable
private fun ModeToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

enum class BottomTab {
    HOME, LOGS, ALERTS, PARENTAL, DEVELOPER, SETTINGS
}

@Composable
fun DynamicBottomNavigation(
    selectedTab: BottomTab,
    onHomeSelected: () -> Unit,
    onLogsSelected: () -> Unit,
    onAlertsSelected: () -> Unit,
    onParentalSelected: () -> Unit,
    onDeveloperSelected: () -> Unit,
    isParentalEnabled: Boolean = ModeSettings.isParentalModeEnabled,
    isDeveloperEnabled: Boolean = ModeSettings.isDeveloperModeEnabled
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick = onHomeSelected,
            icon = { Icon(painterResource(id = android.R.drawable.ic_menu_view), contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.LOGS,
            onClick = onLogsSelected,
            icon = { Icon(painterResource(id = android.R.drawable.ic_menu_info_details), contentDescription = "Logs") },
            label = { Text("Logs") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.ALERTS,
            onClick = onAlertsSelected,
            icon = { Icon(painterResource(id = android.R.drawable.ic_dialog_alert), contentDescription = "Alerts") },
            label = { Text("Alerts") }
        )

        if (isParentalEnabled) {
            NavigationBarItem(
                selected = selectedTab == BottomTab.PARENTAL,
                onClick = onParentalSelected,
                icon = { Icon(painterResource(id = android.R.drawable.ic_lock_idle_lock), contentDescription = "Parental") },
                label = { Text("Parental") }
            )
        }

        if (isDeveloperEnabled) {
            NavigationBarItem(
                selected = selectedTab == BottomTab.DEVELOPER,
                onClick = onDeveloperSelected,
                icon = { Icon(painterResource(id = android.R.drawable.ic_menu_manage), contentDescription = "Developer") },
                label = { Text("Developer") }
            )
        }
    }
}

