/*
 *     Copyright (C) 2021 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView
import com.parrot.drone.groundsdk.stream.GsdkStreamView.PADDING_FILL_BLUR_CROP
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.device.pilotingitf.Activable

import android.widget.Button
import android.widget.TextView

/**
 * GroundSdk sample code for camera API usage.
 *
 * This sample app show how to use `MainCamera` and `camera2.MainCamera` peripherals to display and
 * change some camera configuration parameters and to trigger photo capture or video recording.
 *
 * `MainCamera` is the peripheral to control the main camera of drones supporting Camera1 API.
 * `camera2.MainCamera` is the peripheral to control the main camera of drones supporting Camera2 API.
 *
 * This activity manages the connection to the drone, the streaming view and uses delegates to
 * control the camera user interface.
 *
 * The sample code showing how to use the camera API is available is the following delegates:
 * - ActiveState: sample code to display the active state of a camera
 * - CameraMode: sample code to display and change the camera mode (photo capture or video recording)
 * - StartStop: sample code to manage a button to start/stop photo capture and video recording
 * - WhiteBalanceTemperature: sample code to display and change custom white balance temperature
 */
class MainActivity : AppCompatActivity() {

    /*/** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    // Drone:
    /** Current drone instance. */
    private var drone: Drone? = null
    /** Reference to the current drone state. */
    private var droneStateRef: Ref<DeviceState>? = null
    /** Reference to the current drone stream server Peripheral. */
    private var streamServerRef: Ref<StreamServer>? = null
    /** Reference to the current drone live stream. */
    private var liveStreamRef: Ref<CameraLive>? = null
    /** Current drone live stream. */
    private var liveStream: CameraLive? = null

    // Remote control:
    /** Current remote control instance. */
    private var rc: RemoteControl? = null
    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null

    // User interface:
    /** Video stream view. */
    private val streamView by lazy { findViewById<GsdkStreamView>(R.id.stream_view) }
    /** Drone state text view. */
    private val droneStateTxt by lazy { findViewById<TextView>(R.id.droneStateTxt) }
    /** Remote state text view. */
    private val rcStateTxt by lazy { findViewById<TextView>(R.id.rcStateTxt) }

    // Delegates to manage camera user interface:
    /** Delegate to display camera active state. */
    private val activeState by lazy { ActiveState(findViewById(R.id.activeTxt)) }
    /** Delegate to display and change camera mode. */
    private val cameraMode by lazy { CameraMode(findViewById(R.id.photoMode), findViewById(R.id.recordingMode)) }
    /** Delegate to manage start and stop photo capture and video recording button. */
    private val startStop by lazy { StartStop(findViewById(R.id.startStopBtn)) }
    /** Delegate to display and change custom white balance temperature. */
    private val whiteBalanceTemperature by lazy { WhiteBalanceTemperature(findViewById(R.id.whiteBalanceSpinner)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize user interface default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()

        streamView.paddingFill = PADDING_FILL_BLUR_CROP

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.
    }*/



    /*override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) {
            // Called when the auto connection facility is available and when it changes.

            it?.let{
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {
                    if(drone != null) {
                        // Stop monitoring the previous drone.
                        stopDroneMonitors()

                        // Reset user interface drone part.
                        resetDroneUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if(drone != null) {
                        startDroneMonitors()
                    }
                }

                // If the remote control has changed.
                if (rc?.uid  != it.remoteControl?.uid) {
                    if(rc != null) {
                        // Stop monitoring the previous remote.
                        stopRcMonitors()

                        // Reset user interface Remote part.
                        resetRcUi()
                    }

                    // Monitor the new remote.
                    rc = it.remoteControl
                    if(rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }*/



 */
    /** GroundSdk instance. */

  private lateinit var groundSdk: GroundSdk
    private var drone: Drone? = null
    private var rc: RemoteControl? = null

 */

  // Drone:
    //started the code from here Anu

    private lateinit var droneBatteryText: TextView
    private lateinit var remoteBatteryText: TextView
    private lateinit var droneConnectionText: TextView
    private lateinit var remoteConnectionText: TextView
    private lateinit var droneStatusText: TextView
    private lateinit var flyViewButton: Button
    private lateinit var galleryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        droneBatteryText = findViewById(R.id.droneBatteryText)
        remoteBatteryText = findViewById(R.id.remoteBatteryText)
        droneConnectionText = findViewById(R.id.droneConnectionText)
        remoteConnectionText = findViewById(R.id.remoteConnectionText)
        droneStatusText = findViewById(R.id.droneStatusText)
        flyViewButton = findViewById(R.id.flyViewButton)
        galleryButton = findViewById(R.id.galleryButton)

        flyViewButton.setOnClickListener {
            val intent = Intent(this, FlyViewActivity::class.java)
            startActivity(intent)
        }

        galleryButton.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        groundSdk = ManagedGroundSdk.obtainSession(this)

        startMonitoring()
    }

    private fun startMonitoring() {
        groundSdk.getFacility(AutoConnection::class.java)?.let { autoConnection ->
            autoConnection.start()

            autoConnection.monitorDrone { newDrone ->
                if (drone?.uid != newDrone?.uid) {
                    drone?.let { stopMonitoringDrone(it) }
                    newDrone?.let { startMonitoringDrone(it) }
                    drone = newDrone
                }
            }

            autoConnection.monitorRemoteControl { newRc ->
                if (rc?.uid != newRc?.uid) {
                    rc?.let { stopMonitoringRemoteControl(it) }
                    newRc?.let { startMonitoringRemoteControl(it) }
                    rc = newRc
                }
            }
        }
    }

    private fun startMonitoringDrone(drone: Drone) {
        drone.getPeripheral(BatteryInfo::class.java)?.let { batteryInfo ->
            batteryInfo.monitorBatteryLevel { level ->
                droneBatteryText.text = "Drone battery: $level%"
            }
        }

        drone.monitorConnectionState { state ->
            droneConnectionText.text = "Drone: ${state.connectionState}"
            droneStatusText.text = "Drone Active: ${state.isActive}"
        }

        drone.getPeripheral(MainCamera::class.java)?.let { mainCamera ->
            mainCamera.monitorActiveState { active ->
                droneStatusText.text = "Drone Active: $active"
            }
        }
    }

    private fun stopMonitoringDrone(drone: Drone) {
        drone.getPeripheral(BatteryInfo::class.java)?.stopMonitoringBatteryLevel()
        drone.stopMonitoringConnectionState()
    }

    private fun startMonitoringRemoteControl(rc: RemoteControl) {
        rc.getPeripheral(BatteryInfo::class.java)?.let { batteryInfo ->
            batteryInfo.monitorBatteryLevel { level ->
                remoteBatteryText.text = "Remote battery: $level%"
            }
        }

        rc.monitorConnectionState { state ->
            remoteConnectionText.text = "Remote: ${state.connectionState}"
        }
    }

    ```kotlin
    private fun stopMonitoringRemoteControl(rc: RemoteControl) {
        rc.getPeripheral(BatteryInfo::class.java)?.stopMonitoringBatteryLevel()
        rc.stopMonitoringConnectionState()
    }

    override fun onDestroy() {
        drone?.let { stopMonitoringDrone(it) }
        rc?.let { stopMonitoringRemoteControl(it) }
        ManagedGroundSdk.releaseSession(this)
        super.onDestroy()
    }
}
