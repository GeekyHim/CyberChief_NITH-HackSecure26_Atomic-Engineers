package com.example.hacksecure

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class StartVpnActivity : AppCompatActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("VPN_ENGINE", "VPN permission granted by user")
                startLocalVpnServiceAndNavigate()
            } else {
                Log.d("VPN_ENGINE", "VPN permission denied by user")
                // You could show a Toast here explaining why the permission is needed
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_vpn)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val startButton = findViewById<Button>(R.id.startVpnButton)

        startButton.setOnClickListener {
            prepareAndStartVpn()
        }
    }

    private fun prepareAndStartVpn() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            // Permission needs to be granted
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            // Permission already granted
            Log.d("VPN_ENGINE", "VPN permission already granted")
            startLocalVpnServiceAndNavigate()
        }
    }

    private fun startLocalVpnServiceAndNavigate() {
        // 1. Start the Background VPN Engine
        val serviceIntent = Intent(this, LocalVpnService::class.java)
        startService(serviceIntent)

        // 2. Navigate to the Dashboard (Landing Page)
        val mainIntent = Intent(this, MainActivity::class.java)
        startActivity(mainIntent)
        
        // 3. Close this starting screen so the user doesn't return here on 'Back' press
        finish() 
    }
}