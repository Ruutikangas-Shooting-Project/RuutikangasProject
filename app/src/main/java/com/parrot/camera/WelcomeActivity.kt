package com.parrot.camera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.facility.AutoConnection

class WelcomeActivity : AppCompatActivity() {

    // GroundSdk instance
    private lateinit var groundSdk: GroundSdk
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null

    // UI Components
    private lateinit var droneBatteryStatusTextView: TextView
    private lateinit var remoteBatteryStatusTextView: TextView
    private lateinit var droneConnectionStatusTextView: TextView
    private lateinit var remoteConnectionStatusTextView: TextView
    private lateinit var droneActiveStatusTextView: TextView
    private lateinit var droneImageView: ImageView
    private lateinit var flyViewButton: Button
    private lateinit var galleryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Initialize GroundSdk
        groundSdk = ManagedGroundSdk.obtainSession(this)

        // Initialize UI components with updated IDs
        droneBatteryStatusTextView = findViewById(R.id.tv_drone_battery_status)
        remoteBatteryStatusTextView = findViewById(R.id.tv_remote_battery_status)
        droneConnectionStatusTextView = findViewById(R.id.tv_drone_connection_status)
        remoteConnectionStatusTextView = findViewById(R.id.tv_remote_connection_status)
        droneActiveStatusTextView = findViewById(R.id.tv_drone_active_status)
        droneImageView = findViewById(R.id.iv_drone_image)
        flyViewButton = findViewById(R.id.btn_fly_view)
        galleryButton = findViewById(R.id.btn_gallery)

        // Set up button listeners
        flyViewButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        galleryButton.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        // Monitor battery levels
        monitorBatteryLevels()

        // Update UI elements dynamically
        updateDroneAndRemoteStatus()
    }

    private fun monitorBatteryLevels() {
        groundSdk.getFacility(AutoConnection::class.java) { autoConnection ->
            val drone = autoConnection?.drone
            val rc = autoConnection?.remoteControl

            // Monitor drone battery level
            droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
                it?.let {
                    droneBatteryStatusTextView.text = getString(R.string.drone_battery_text, it.charge)
                }
            }

            // Monitor remote control battery level
            rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
                it?.let {
                    remoteBatteryStatusTextView.text = getString(R.string.remote_battery_text, it.charge)
                }
            }
        }
    }

    private fun updateDroneAndRemoteStatus() {
        // Logic to update the UI with actual data (dummy data here for illustration)
        droneConnectionStatusTextView.text = "Drone: Connected"
        remoteConnectionStatusTextView.text = "Remote: Connected"
        droneBatteryStatusTextView.text = "Drone battery: 75%"
        remoteBatteryStatusTextView.text = "Remote battery: 80%"
        droneActiveStatusTextView.text = "Drone Active: True"
    }

    override fun onDestroy() {
        super.onDestroy()
        droneBatteryInfoRef?.close()
        rcBatteryInfoRef?.close()
    }
}
