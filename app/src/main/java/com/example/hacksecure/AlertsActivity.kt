package com.example.hacksecure

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class AlertStatus {
    ACTIVE,
    RESOLVED
}

private enum class AlertSeverity {
    MALICIOUS,
    SUSPICIOUS,
    RESOLVED
}

private data class SecurityAlert(
    val id: Int,
    val appName: String,
    val alertType: String,
    val destinationIp: String,
    val timestamp: String,
    val status: AlertStatus,
    val severity: AlertSeverity,
    val protocol: String = "TCP",
    val safetyScore: String = "Suspicious"
)

private val mockAlerts: List<SecurityAlert> = listOf(
    SecurityAlert(
        id = 1,
        appName = "Chrome",
        alertType = "Suspicious IP Connection",
        destinationIp = "45.33.21.101",
        timestamp = "14:23",
        status = AlertStatus.ACTIVE,
        severity = AlertSeverity.SUSPICIOUS,
        protocol = "UDP",
        safetyScore = "Suspicious"
    ),
    SecurityAlert(
        id = 2,
        appName = "Instagram",
        alertType = "Unusual network traffic",
        destinationIp = "157.240.20.61",
        timestamp = "13:58",
        status = AlertStatus.ACTIVE,
        severity = AlertSeverity.MALICIOUS,
        protocol = "TCP",
        safetyScore = "High Risk"
    ),
    SecurityAlert(
        id = 3,
        appName = "WhatsApp",
        alertType = "Connection to blacklisted server",
        destinationIp = "52.58.63.252",
        timestamp = "12:40",
        status = AlertStatus.RESOLVED,
        severity = AlertSeverity.RESOLVED,
        protocol = "UDP",
        safetyScore = "Safe"
    ),
    SecurityAlert(
        id = 4,
        appName = "YouTube",
        alertType = "Malicious domain detected",
        destinationIp = "142.250.190.174",
        timestamp = "11:12",
        status = AlertStatus.RESOLVED,
        severity = AlertSeverity.SUSPICIOUS,
        protocol = "TCP",
        safetyScore = "Suspicious"
    )
)

class AlertsActivity : ComponentActivity() {

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
                                title = { Text(text = "Security Alerts") },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        bottomBar = {
                            DynamicBottomNavigation(
                                selectedTab = BottomTab.ALERTS,
                                onHomeSelected = {
                                    startActivity(Intent(this@AlertsActivity, MainActivity::class.java))
                                    finish()
                                },
                                onLogsSelected = {
                                    startActivity(Intent(this@AlertsActivity, AppWiseLogsActivity::class.java))
                                    finish()
                                },
                                onAlertsSelected = { /* already here */ },
                                onParentalSelected = {
                                    startActivity(Intent(this@AlertsActivity, ParentalModeActivity::class.java))
                                    finish()
                                },
                                onDeveloperSelected = {
                                    startActivity(Intent(this@AlertsActivity, DeveloperModeActivity::class.java))
                                    finish()
                                }
                            )
                        }
                    ) { innerPadding ->
                        AlertsScreen(
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
private fun AlertsScreen(modifier: Modifier = Modifier) {
    var expandedAlertId by remember { mutableStateOf<Int?>(null) }

    // Empty state if there are no alerts
    if (mockAlerts.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No security alerts detected",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Column(
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(mockAlerts) { alert ->
                AlertCard(
                    alert = alert,
                    isExpanded = expandedAlertId == alert.id,
                    onExpandClick = {
                        expandedAlertId = if (expandedAlertId == alert.id) null else alert.id
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: SecurityAlert,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    val (indicatorColor, statusTextColor) = when (alert.severity) {
        AlertSeverity.MALICIOUS -> Color.Red to Color.Red
        AlertSeverity.SUSPICIOUS -> Color(0xFFFF9800) to Color(0xFFFF9800) // orange
        AlertSeverity.RESOLVED -> Color(0xFF4CAF50) to Color(0xFF4CAF50) // green
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onExpandClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(4.dp)
                        .background(indicatorColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = alert.alertType,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Status: ${alert.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusTextColor
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Destination IP: ${alert.destinationIp}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Time: ${alert.timestamp}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Protocol: ${alert.protocol}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Safety Score: ${alert.safetyScore}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /* Kill Connection action */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Kill Connection",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Button(
                            onClick = { /* Block App action */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Block App",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
