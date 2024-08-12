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
    //private lateinit var connectStatusTxt:TextView
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
        //connectStatusTxt = findViewById(R.id.connectStatusTxt)
        flyViewBtn = findViewById(R.id.connectDroneBtn)
        //connectDroneBtn = findViewById(R.id.connectDroneBtn)

        // link to main fly model (id change to flyviewBtn)
        val flyViewBtn = findViewById<Button>(R.id.connectDroneBtn)
        flyViewBtn.setOnClickListener {
            val intent = Intent (this, MainActivity::class.java)
            startActivity(intent)
        }
        val galleryBtn = findViewById<Button>(R.id.galleryBtn)
        galleryBtn.setOnClickListener {
            val intent = Intent(this, MediaListActivity::class.java)
            startActivity(intent)
        }
        updateBatteryStatus()
    }
    private fun updateBatteryStatus(){
        groundSdk.getFacility(AutoConnection::class.java) {autoConnection ->
            autoConnection?.let {
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }
                it.drone?.let {drone->
                    drone.getInstrument(BatteryInfo::class.java) {batteryInfo->
                        batteryInfo?.let {info->
                            droneBatteryInfo.text = "Drone Battery: ${info.charge}%"
                        }
                it.remoteControl?.let {remoteControl ->
                    remoteControl.getInstrument(BatteryInfo::class.java) {batteryInfo->
                        batteryInfo?.let {info ->
                            remoteBatteryInfo.text = "Remote Battery: ${info.charge}%"
                        }

                    }
                }

                    }


                }

        }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }



}
