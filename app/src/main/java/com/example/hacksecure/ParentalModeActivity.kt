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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class ParentalModeActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = "Parental Mode") },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        bottomBar = {
                            DynamicBottomNavigation(
                                selectedTab = BottomTab.PARENTAL,
                                onHomeSelected = {
                                    startActivity(Intent(this@ParentalModeActivity, MainActivity::class.java))
                                    finish()
                                },
                                onLogsSelected = {
                                    startActivity(Intent(this@ParentalModeActivity, AppWiseLogsActivity::class.java))
                                    finish()
                                },
                                onAlertsSelected = {
                                    startActivity(Intent(this@ParentalModeActivity, AlertsActivity::class.java))
                                    finish()
                                },
                                onParentalSelected = { /* already here */ },
                                onDeveloperSelected = {
                                    startActivity(Intent(this@ParentalModeActivity, DeveloperModeActivity::class.java))
                                    finish()
                                }
                            )
                        }
                    ) { innerPadding ->
                        ParentalModeScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentalModeScreen(modifier: Modifier = Modifier) {
    var isInternetBlocked by rememberSaveable { mutableStateOf(false) }
    var isSocialMediaBlocked by rememberSaveable { mutableStateOf(false) }
    var isGamingBlocked by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Parental Control",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Restrict network access and app categories",
            style = MaterialTheme.typography.bodyMedium
        )

        ParentalToggleCard(
            title = "Stop Internet Access",
            description = "Disable internet connectivity for all applications monitored by the analyzer.",
            checked = isInternetBlocked,
            onCheckedChange = { isInternetBlocked = it }
        )

        ParentalToggleCard(
            title = "Block Social Media",
            description = "Prevent social media apps like Instagram, Facebook, Snapchat, and Twitter from accessing the internet.",
            checked = isSocialMediaBlocked,
            onCheckedChange = { isSocialMediaBlocked = it }
        )

        ParentalToggleCard(
            title = "Block Gaming Apps",
            description = "Prevent gaming apps like PUBG, Call of Duty Mobile, and Free Fire from accessing the internet.",
            checked = isGamingBlocked,
            onCheckedChange = { isGamingBlocked = it }
        )

        if (isInternetBlocked || isSocialMediaBlocked || isGamingBlocked) {
            Text(
                text = "Parental restrictions are currently active",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "No parental restrictions are currently active",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ParentalToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
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

