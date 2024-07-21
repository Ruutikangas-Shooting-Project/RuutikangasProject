package com.parrot.camera

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import java.time.Instant

class BatteryStatusActivity :AppCompatActivity() {
    private lateinit var droneBatteryInfo: TextView
    private lateinit var remoteBatteryInfo: TextView

    override fun onCreate(saveInstanceState: Bundle?)  {
        super.onCreate(saveInstanceState)
        setContentView(R.layout.activity_battery_status)
        droneBatteryInfo = findViewById(R.id.droneBatteryTxt)
        remoteBatteryInfo = findViewById(R.id.remoteBatteryTxt)

        updateBatteryStatus()

    }

    private fun updateBatteryStatus()  {
        val droneBatteryTxt = droneBatteryInfo
        val remoteBatteryTxt = remoteBatteryInfo
        droneBatteryTxt.text = "Drone Battery: 75%"
        remoteBatteryTxt.text = "Remote Battery: 65%"
    }


}