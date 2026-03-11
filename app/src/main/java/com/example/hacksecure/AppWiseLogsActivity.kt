package com.example.hacksecure

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hacksecure.ui.models.AppLog
import com.example.hacksecure.ui.models.ConnectionLog
import com.example.hacksecure.ui.viewmodel.AppWiseLogsViewModel

private enum class AppLogFilter {
    MOST_DATA,
    RECENTLY_ACTIVE
}

class AppWiseLogsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val viewModel: AppWiseLogsViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "app_list") {
                        composable("app_list") {
                            val appLogs by viewModel.observeAppWiseLogs().observeAsState(emptyList())
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text(text = "App-wise Network Logs") },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                },
                                bottomBar = {
                                    DynamicBottomNavigation(
                                        selectedTab = BottomTab.LOGS,
                                        onHomeSelected = {
                                            startActivity(Intent(this@AppWiseLogsActivity, MainActivity::class.java))
                                            finish()
                                        },
                                        onLogsSelected = { /* already here */ },
                                        onAlertsSelected = {
                                            startActivity(Intent(this@AppWiseLogsActivity, AlertsActivity::class.java))
                                            finish()
                                        },
                                        onParentalSelected = {
                                            startActivity(Intent(this@AppWiseLogsActivity, ParentalModeActivity::class.java))
                                            finish()
                                        },
                                        onDeveloperSelected = {
                                            startActivity(Intent(this@AppWiseLogsActivity, DeveloperModeActivity::class.java))
                                            finish()
                                        }
                                    )
                                }
                            ) { innerPadding ->
                                AppWiseLogsScreen(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    appLogs = appLogs,
                                    onAppClick = { appLog ->
                                        navController.navigate("app_detail/${appLog.appId}")
                                    }
                                )
                            }
                        }
                        composable(
                            "app_detail/{appId}",
                            arguments = listOf(navArgument("appId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val appId = backStackEntry.arguments?.getInt("appId") ?: -1
                            val appLog by viewModel.observeLogsForApp(appId).observeAsState(null)
                            val appName = appLog?.appName ?: ""
                            
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text(text = "$appName Network Logs") },
                                        navigationIcon = {
                                            IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back",
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            ) { innerPadding ->
                                AppDetailScreen(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    appLog = appLog
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppWiseLogsScreen(
    modifier: Modifier = Modifier,
    appLogs: List<AppLog>,
    onAppClick: (AppLog) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(AppLogFilter.MOST_DATA) }

    val sortedApps = when (selectedFilter) {
        AppLogFilter.MOST_DATA ->
            appLogs.sortedByDescending { it.totalDataMb }

        AppLogFilter.RECENTLY_ACTIVE ->
            appLogs.sortedByDescending { it.lastActiveTimestamp }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        FilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedApps) { appLog ->
                AppLogCard(appLog = appLog, onClick = { onAppClick(appLog) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selectedFilter: AppLogFilter,
    onFilterSelected: (AppLogFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == AppLogFilter.MOST_DATA,
            onClick = { onFilterSelected(AppLogFilter.MOST_DATA) },
            label = { Text("Most Data Used") }
        )

        FilterChip(
            selected = selectedFilter == AppLogFilter.RECENTLY_ACTIVE,
            onClick = { onFilterSelected(AppLogFilter.RECENTLY_ACTIVE) },
            label = { Text("Recently Active Apps") }
        )
    }
}

@Composable
private fun AppLogCard(appLog: AppLog, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Text(
                        text = appLog.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${appLog.connectionCount} connections",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "${appLog.totalDataMb} MB",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppDetailScreen(
    modifier: Modifier = Modifier,
    appLog: AppLog?
) {
    if (appLog == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "Logs not found for this app")
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(appLog.connections) { log ->
            ConnectionLogItem(log)
        }
    }
}

@Composable
private fun ConnectionLogItem(log: ConnectionLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Source IP: ${log.sourceIp}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Destination IP: ${log.destinationIp}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Protocol: ${log.protocol}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Safety Score: ${log.safetyScore}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Timestamp: ${log.timestamp}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
