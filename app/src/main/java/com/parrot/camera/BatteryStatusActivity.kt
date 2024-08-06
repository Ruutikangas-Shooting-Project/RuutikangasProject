package com.parrot.camera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.facility.AutoConnection

class BatteryStatusActivity : AppCompatActivity() {
    private lateinit var droneBatteryInfo: TextView
    private lateinit var remoteBatteryInfo: TextView
    private lateinit var groundSdk: GroundSdk
    private lateinit var mediaManager: MediaManager
    private val mediaListView by lazy { findViewById<ListView>(R.id.media_list_view) }

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        setContentView(R.layout.activity_battery_status)

        groundSdk = ManagedGroundSdk.obtainSession(this)

        droneBatteryInfo = findViewById(R.id.droneBatteryTxt)
        remoteBatteryInfo = findViewById(R.id.remoteBatteryTxt)

        val flyViewBtn = findViewById<Button>(R.id.connectDroneBtn)
        flyViewBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        mediaManager = MediaManager(this, mediaListView)

        updateBatteryStatus()
    }

    private fun updateBatteryStatus() {
        val autoConnection = groundSdk.getFacility(AutoConnection::class.java)
        autoConnection?.let { facility ->
            facility.start()
            facility.drone?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
                batteryInfo?.let { info ->
                    droneBatteryInfo.text = "Drone Battery: ${info.charge}%"
                } ?: run {
                    droneBatteryInfo.text = "Drone Battery: --%"
                }
            }
            facility.remoteControl?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
                batteryInfo?.let { info ->
                    remoteBatteryInfo.text = "Remote Battery: ${info.charge}%"
                } ?: run {
                    remoteBatteryInfo.text = "Remote Battery: --%"
                }
            }

            facility.drone?.let {
                mediaManager.monitorMediaStore(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaManager.close()
    }
}




