package com.parrot.camera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.facility.AutoConnection

class BatteryStatusActivity :AppCompatActivity() {
    //follow the main to add groundsdk to get sdk
    private lateinit var groundSdk: GroundSdk

    private lateinit var droneBatteryInfo: TextView
    private lateinit var remoteBatteryInfo: TextView
    //private lateinit var connectStatusTxt:TextView

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

        // link to main fly model (id change to flyviewBtn)
        val flyViewBtn = findViewById<Button>(R.id.connectDroneBtn)
        val flyviewBtnOverlay = findViewById<View>(R.id.flyviewBtnOverlay)
        flyviewBtnOverlay.setOnClickListener{
            if(!flyViewBtn.isEnabled) {
                Toast.makeText(this, "Connect to drone to get fly mode", Toast.LENGTH_SHORT).show()
            }
        }
        flyViewBtn.setOnClickListener {
            val intent = Intent (this, MainActivity::class.java)
            startActivity(intent)
        }
        val galleryBtn = findViewById<Button>(R.id.galleryBtn)
        val galleryBtnOverlay = findViewById<View>(R.id.galleryBtnOverlay)
        galleryBtnOverlay.setOnClickListener {
            if (!galleryBtn.isEnabled) {
                Toast.makeText(this, "Please connect to drone", Toast.LENGTH_SHORT).show()
            }
        }
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

                //0821 new code check if connect to drone
                val droneConnected = it.drone?.state?.connectionState == DeviceState.ConnectionState.CONNECTED
                updateBtnState(droneConnected)

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
    private fun updateBtnState(droneConnected: Boolean) {
       //0821 new code check if connect to drone
        val galleryButton: Button = findViewById(R.id.galleryBtn)
        val galleryBtnOverlay : View = findViewById(R.id.galleryBtnOverlay)
        val flyViewBtn: Button = findViewById(R.id.connectDroneBtn)
        val flyviewBtnOverlay: View = findViewById(R.id.flyviewBtnOverlay)

        galleryButton.isEnabled = droneConnected
        galleryBtnOverlay.visibility = if (droneConnected) View.GONE else View.VISIBLE
        //for fly view btn
        flyViewBtn.isEnabled = droneConnected
        flyviewBtnOverlay.visibility = if (droneConnected) View.GONE else View.VISIBLE

        val backgroundResource = if (droneConnected){
            R.drawable.btn_connected
        }else {
            R.drawable.btn_disconnted
        }
        galleryButton.background = ContextCompat.getDrawable(this, backgroundResource)
        flyViewBtn.background = ContextCompat.getDrawable(this, backgroundResource)

    }
    //problem, this override is no function
    override fun onDestroy() {
        super.onDestroy()
    }

}
