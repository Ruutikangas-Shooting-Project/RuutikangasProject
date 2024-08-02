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
    //cancel progress to let battery show text first
    //private lateinit var droneBatteryProgress: ProgressBar
    private lateinit var remoteBatteryInfo: TextView
    //follow the main to add groundsdk to get sdk
    private lateinit var groundSdk: GroundSdk


    override fun onCreate(saveInstanceState: Bundle?)  {
        super.onCreate(saveInstanceState)
        setContentView(R.layout.activity_battery_status)
        // Initialize GroundSdk
        groundSdk = ManagedGroundSdk.obtainSession(this)

        droneBatteryInfo = findViewById(R.id.droneBatteryTxt)
        remoteBatteryInfo = findViewById(R.id.remoteBatteryTxt)
        //cancel progress to let battery show text first
        //droneBatteryProgress = findViewById(R.id.droneBatteryProgress)
        //remoteBatteryProgress = findViewById(R.id.remoteBatteryProgress)
        // link to main fly model (id is connect to drone)
        val flyViewBtn = findViewById<Button>(R.id.connectDroneBtn)
        flyViewBtn.setOnClickListener {
            val intent = Intent (this, MainActivity::class.java)
            startActivity(intent)
        }

        updateBatteryStatus()

    }

    private fun updateBatteryStatus()  {
        groundSdk = ManagedGroundSdk.obtainSession(this)

        //auto connect
        val autoConnection = groundSdk.getFacility(AutoConnection::class.java)
        autoConnection?.let {facility->
            facility.start()
            // Subscribe to drone's battery information
            facility.drone?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
                batteryInfo?.let { info ->
                    // Display drone battery level
                    droneBatteryInfo.text = "Drone Battery: ${info.charge}%"
                } ?: run {
                    // Display default message if battery info is not available
                    droneBatteryInfo.text = "Drone Battery: --%"
                }
            }
           facility.remoteControl?.let {
               Log.d("BatteryStatus", "Remote control is connected")
               it.getInstrument(BatteryInfo::class.java) {batteryInfo ->
                   batteryInfo?.let { info ->
                       remoteBatteryInfo.text ="Remote Battery: ${info.charge}%"
                   }?:run {
                       remoteBatteryInfo.text = "Remote Battery: --%"
                   }

               }
           }

        }

    }
}

//welcome page code
/*
 facility.remoteControl?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
                batteryInfo?.let { info ->
                    remoteBatteryInfo.text = "Remote Battery:$(info.charge)%"
                }?:run {
                    remoteBatteryInfo.text="Remote Battery: --%"
                }
            }
* */
//old code, keep it here
/*

            facility.remoteControl?.let {
                Log.d("BatteryStatus", "Remote control is connected")
                it.getInstrument(BatteryInfo::class.java){batteryInfo ->
                    batteryInfo?.let { info->
                        remoteBatteryInfo.text = "Remote Battery: ${info.charge}%"
                    }?:run {
                        remoteBatteryInfo.text = "Remote Battery: --%"
                        Log.d("BatteryStatus", "Failed to retrieve battery information for remote control.")
                    }
                }
            } ?: Log.d("BatteryStatus", "Remote Control is NOT connected.")
*/

/*
  groundSdk.getFacility(AutoConnection::class.java)  {
      it?.let {autoConnection ->
          autoConnection.start()
          //monitor drone battery
          autoConnection.drone?.getInstrument(BatteryInfo::class.java)
          {batteryInfo ->
              droneBatteryInfo?.let { droneBatteryInfo->
                  val droneBatteryInfo =info.droneBatterylevel
                  droneBatteryInfo.text = "Drone Battery: $droneBatterylevel%"
              }
          }
          // Monitor remote control battery info
          autoConnection.remoteControl?.getInstrument(BatteryInfo::class.java) { batteryInfo ->
              batteryInfo?.let { info ->
                  val batteryLevel = info.batteryLevel
                  remoteBatteryInfo.text = "Remote Battery: $batteryLevel%"
              }
          }
      }
  }*/