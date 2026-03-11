package com.example.hacksecure

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var vpnStatusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("VPN_ENGINE", "VPN permission granted by user")
                startLocalVpnService()
            } else {
                Log.d("VPN_ENGINE", "VPN permission denied by user")
                // Revert optimistic UI if permission denied
                LocalVpnService.isRunning = false
                updateVpnUi(vpnStatusText, toggleButton)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Disable default title as we have it in XML for better control or just use toolbar title
        supportActionBar?.setDisplayShowTitleEnabled(false)

        vpnStatusText = findViewById<TextView>(R.id.vpnStatusText)
        toggleButton = findViewById<Button>(R.id.toggleVpnButton)
        bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val btnMore = findViewById<ImageButton>(R.id.btnMore)

        btnMore.setOnClickListener { view ->
            showSettingsDropdown(view)
        }

        updateVpnUi(vpnStatusText, toggleButton)

        toggleButton.setOnClickListener {
            if (LocalVpnService.isRunning) {
                Log.d("VPN_ENGINE", "Stop VPN button tapped")
                LocalVpnService.isRunning = false
                updateVpnUi(vpnStatusText, toggleButton)
                stopService(Intent(this, LocalVpnService::class.java))
            } else {
                Log.d("VPN_ENGINE", "Start VPN button tapped")
                LocalVpnService.isRunning = true
                updateVpnUi(vpnStatusText, toggleButton)
                prepareAndStartVpn()
            }
        }

        updateBottomNavVisibility(bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_logs -> {
                    startActivity(Intent(this, AppWiseLogsActivity::class.java))
                    true
                }
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    true
                }
                R.id.nav_parental -> {
                    startActivity(Intent(this, ParentalModeActivity::class.java))
                    true
                }
                R.id.nav_developer -> {
                    startActivity(Intent(this, DeveloperModeActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun showSettingsDropdown(anchorView: View) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.layout_settings_dropdown, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Find views in popup
        val itemParental = popupView.findViewById<View>(R.id.item_parental)
        val switchParental = popupView.findViewById<SwitchCompat>(R.id.switch_parental_dropdown)
        val itemDeveloper = popupView.findViewById<View>(R.id.item_developer)
        val switchDeveloper = popupView.findViewById<SwitchCompat>(R.id.switch_developer_dropdown)
        val itemAutoStart = popupView.findViewById<View>(R.id.item_autostart)
        val switchAutoStart = popupView.findViewById<SwitchCompat>(R.id.switch_autostart_dropdown)

        // Set initial states
        switchParental.isChecked = ModeSettings.isParentalModeEnabled
        switchDeveloper.isChecked = ModeSettings.isDeveloperModeEnabled
        switchAutoStart.isChecked = ModeSettings.isAutoStartEnabled

        // Set click listeners for the whole rows
        itemParental.setOnClickListener {
            val newState = !switchParental.isChecked
            switchParental.isChecked = newState
            ModeSettings.isParentalModeEnabled = newState
            updateBottomNavVisibility(bottomNavigation)
        }

        itemDeveloper.setOnClickListener {
            val newState = !switchDeveloper.isChecked
            switchDeveloper.isChecked = newState
            ModeSettings.isDeveloperModeEnabled = newState
            updateBottomNavVisibility(bottomNavigation)
        }

        itemAutoStart.setOnClickListener {
            val newState = !switchAutoStart.isChecked
            switchAutoStart.isChecked = newState
            ModeSettings.isAutoStartEnabled = newState
        }

        // Show the popup
        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }

    private fun prepareAndStartVpn() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent as android.content.Intent)
        } else {
            Log.d("VPN_ENGINE", "VPN permission already granted")
            startLocalVpnService()
        }
    }

    private fun startLocalVpnService() {
        val intent = Intent(this, LocalVpnService::class.java)
        startService(intent)
    }

    private fun updateVpnUi(vpnStatusText: TextView, toggleButton: Button) {
        if (LocalVpnService.isRunning) {
            vpnStatusText.text = "VPN engine is running"
            toggleButton.text = "Stop Analyzing"
        } else {
            vpnStatusText.text = "VPN engine is not running"
            toggleButton.text = "Start Analyzing"
        }
    }

    private fun updateBottomNavVisibility(bottomNavigation: BottomNavigationView) {
        val menu = bottomNavigation.menu
        menu.findItem(R.id.nav_parental)?.isVisible = ModeSettings.isParentalModeEnabled
        menu.findItem(R.id.nav_developer)?.isVisible = ModeSettings.isDeveloperModeEnabled
    }
}
