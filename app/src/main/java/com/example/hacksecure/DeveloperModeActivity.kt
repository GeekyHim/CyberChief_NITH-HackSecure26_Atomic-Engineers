package com.example.hacksecure

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hacksecure.debug.DebugDataGenerator
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class DeveloperLogEntry(
    val timestamp: String,
    val appName: String,
    val sourceIp: String,
    val destinationIp: String,
    val protocol: String,
    val safetyScore: String
)

// Simple mock dataset for export and debug info
private val developerMockLogs: List<DeveloperLogEntry> = listOf(
    DeveloperLogEntry(
        timestamp = "2025-03-10T10:15:00",
        appName = "Chrome",
        sourceIp = "192.168.1.12",
        destinationIp = "142.250.183.14",
        protocol = "TCP",
        safetyScore = "Safe"
    ),
    DeveloperLogEntry(
        timestamp = "2025-03-10T10:20:00",
        appName = "Instagram",
        sourceIp = "192.168.1.12",
        destinationIp = "157.240.20.60",
        protocol = "TCP",
        safetyScore = "Safe"
    ),
    DeveloperLogEntry(
        timestamp = "2025-03-10T10:25:30",
        appName = "WhatsApp",
        sourceIp = "192.168.1.12",
        destinationIp = "52.58.63.252",
        protocol = "UDP",
        safetyScore = "Suspicious"
    ),
    DeveloperLogEntry(
        timestamp = "2025-03-10T10:30:10",
        appName = "YouTube",
        sourceIp = "192.168.1.12",
        destinationIp = "45.33.21.101",
        protocol = "TCP",
        safetyScore = "Malicious"
    )
)

class DeveloperModeActivity : ComponentActivity() {

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
                                title = { Text(text = "Developer Mode") },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        bottomBar = {
                            DynamicBottomNavigation(
                                selectedTab = BottomTab.DEVELOPER,
                                onHomeSelected = {
                                    startActivity(Intent(this@DeveloperModeActivity, MainActivity::class.java))
                                    finish()
                                },
                                onLogsSelected = {
                                    startActivity(Intent(this@DeveloperModeActivity, AppWiseLogsActivity::class.java))
                                    finish()
                                },
                                onAlertsSelected = {
                                    startActivity(Intent(this@DeveloperModeActivity, AlertsActivity::class.java))
                                    finish()
                                },
                                onParentalSelected = {
                                    startActivity(Intent(this@DeveloperModeActivity, ParentalModeActivity::class.java))
                                    finish()
                                },
                                onDeveloperSelected = { /* already here */ }
                            )
                        }
                    ) { innerPadding ->
                        val totalConnections = developerMockLogs.size
                        val flaggedAlerts = developerMockLogs.count { it.safetyScore != "Safe" }
                        val monitoredApps = developerMockLogs.map { it.appName }.distinct().size

                        DeveloperModeScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            onExportLogs = { exportNetworkLogs() },
                            onGenerateDemoData = {
                                DebugDataGenerator.generateDemoData(this@DeveloperModeActivity)
                                Toast.makeText(
                                    this@DeveloperModeActivity,
                                    "Demo data generation started (IO thread).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            totalConnections = totalConnections,
                            flaggedAlerts = flaggedAlerts,
                            monitoredApps = monitoredApps
                        )
                    }
                }
            }
        }
    }

    private fun exportNetworkLogs() {
        val header = "Timestamp,AppName,SourceIP,DestinationIP,Protocol,SafetyScore"
        val rows = developerMockLogs.joinToString(separator = "\n") { entry ->
            listOf(
                entry.timestamp,
                entry.appName,
                entry.sourceIp,
                entry.destinationIp,
                entry.protocol,
                entry.safetyScore
            ).joinToString(separator = ",")
        }

        val csvContent = buildString {
            appendLine(header)
            append(rows)
        }

        val timeSuffix = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .format(LocalDateTime.now())
        val fileName = "network_logs_export_$timeSuffix.csv"

        val outputDir = getExternalFilesDir(null) ?: filesDir
        val outputFile = File(outputDir, fileName)
        outputFile.writeText(csvContent)

        Toast.makeText(
            this,
            "Logs exported successfully to ${outputFile.absolutePath}",
            Toast.LENGTH_LONG
        ).show()
    }
}

@Composable
private fun DeveloperModeScreen(
    modifier: Modifier = Modifier,
    onExportLogs: () -> Unit,
    onGenerateDemoData: () -> Unit,
    totalConnections: Int,
    flaggedAlerts: Int,
    monitoredApps: Int
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Developer Mode",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Export network traffic logs for research and debugging",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "This section allows exporting captured network metadata for external analysis. " +
                "The exported file contains connection information such as IP addresses, protocols, timestamps, and safety scores.",
            style = MaterialTheme.typography.bodySmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Export Network Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Generate a CSV file with captured network metadata for offline analysis.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onExportLogs,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(text = "Export Logs as CSV")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Demo Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Generate realistic demo traffic in the local database. " +
                        "Real captured traffic will continue to appear alongside this data.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onGenerateDemoData,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(text = "Generate Demo Data")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Debug Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total captured connections: $totalConnections",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Flagged alerts: $flaggedAlerts",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Monitored apps: $monitoredApps",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

