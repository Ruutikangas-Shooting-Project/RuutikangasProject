package com.parrot.camera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.facility.AutoConnection
import java.time.Instant

class BatteryStatusActivity :AppCompatActivity() {
    private lateinit var droneBatteryInfo: TextView
    private lateinit var remoteBatteryInfo: TextView
    private lateinit var connectStatusTxt:TextView
    //follow the main to add groundsdk to get sdk
    private lateinit var groundSdk: GroundSdk
    private lateinit var connectDroneBtn :Button
    private lateinit var flyViewBtn : Button


    override fun onCreate(saveInstanceState: Bundle?)  {
        super.onCreate(saveInstanceState)
        setContentView(R.layout.activity_battery_status)
        // Initialize GroundSdk
        groundSdk = ManagedGroundSdk.obtainSession(this)

        droneBatteryInfo = findViewById(R.id.droneBatteryTxt)
        remoteBatteryInfo = findViewById(R.id.remoteBatteryTxt)
        connectStatusTxt = findViewById(R.id.connectStatusTxt)
        flyViewBtn = findViewById(R.id.flyViewBtn)
        connectDroneBtn = findViewById(R.id.connectDroneBtn)

        connectDroneBtn.setOnClickListener { connectToBatteryInfo() }

        // link to main fly model (id change to flyviewBtn)
        val flyViewBtn = findViewById<Button>(R.id.flyViewBtn)
        flyViewBtn.setOnClickListener {
            val intent = Intent (this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun connectToBatteryInfo(){
        val autoConnection = groundSdk.getFacility(AutoConnection::class.java)
        autoConnection?.start()
        autoConnection?.let {facility ->
            updateConnectStatus(facility.drone !=null && facility.remoteControl != null)
            facility.drone?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
                updateBatteryInfo(batteryInfo, droneBatteryInfo, "Drone Battery")
            }
            facility.remoteControl?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
                updateBatteryInfo(batteryInfo, droneBatteryInfo, "Remote Battery")
            }

        }
    }
    private fun updateConnectStatus(isConnected: Boolean) {
        connectStatusTxt.text = if (isConnected) "Connected" else "Disconnected"

    }

        private fun updateBatteryInfo(batteryInfo: BatteryInfo?, batteryTextView: TextView, label: String){
        batteryTextView.text = batteryInfo?.let {info ->
            "$label: ${info.charge}%"
        }?:"$label: --%"
    }

}
