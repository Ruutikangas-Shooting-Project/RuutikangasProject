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

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView
import com.parrot.drone.groundsdk.stream.GsdkStreamView.PADDING_FILL_BLUR_CROP

class MainActivity : AppCompatActivity() {

    private lateinit var groundSdk: GroundSdk
    private var drone: Drone? = null
    private var droneStateRef: Ref<DeviceState>? = null
    private var streamServerRef: Ref<StreamServer>? = null
    private var liveStreamRef: Ref<CameraLive>? = null
    private var liveStream: CameraLive? = null
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    private var rc: RemoteControl? = null
    private var rcStateRef: Ref<DeviceState>? = null
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null
    private val streamView by lazy { findViewById<GsdkStreamView>(R.id.stream_view) }
    private val droneStatusTxt by lazy { findViewById<TextView>(R.id.droneStatusTxt) }
    private val rcStatusTxt by lazy { findViewById<TextView>(R.id.rcStatusTxt) }
    private val takeOffLandBt by lazy { findViewById<Button>(R.id.takeOffLandBt) }
    private val activeState by lazy { ActiveState(findViewById(R.id.activeTxt)) }
    private val cameraMode by lazy { CameraMode(findViewById(R.id.photoMode), findViewById(R.id.recordingMode)) }
    private val startStop by lazy { StartStop(findViewById(R.id.startStopBtn)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_land)

        droneStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")
        rcStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")

        streamView.paddingFill = PADDING_FILL_BLUR_CROP

        takeOffLandBt.setOnClickListener { onTakeOffLandClick() }

        groundSdk = ManagedGroundSdk.obtainSession(this)
    }

    override fun onStart() {
        super.onStart()

        groundSdk.getFacility(AutoConnection::class.java) {
            it?.let {
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                if (drone?.uid != it.drone?.uid) {
                    if (drone != null) {
                        stopDroneMonitors()
                        resetDroneUi()
                    }

                    drone = it.drone
                    if (drone != null) {
                        startDroneMonitors()
                    }
                }

                if (rc?.uid != it.remoteControl?.uid) {
                    if (rc != null) {
                        stopRcMonitors()
                        resetRcUi()
                    }

                    rc = it.remoteControl
                    if (rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    private fun resetDroneUi() {
        droneStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")
        takeOffLandBt.isEnabled = false
        streamView.setStream(null)
    }

    private fun startDroneMonitors() {
        monitorDroneState()
        monitorDroneBatteryChargeLevel()
        startVideoStream()
        monitorPilotingInterface()

        drone?.let { drone ->
            activeState.startMonitoring(drone)
            cameraMode.startMonitoring(drone)
            startStop.startMonitoring(drone)
        }
    }

    private fun stopDroneMonitors() {
        droneStateRef?.close()
        droneStateRef = null
        droneBatteryInfoRef?.close()
        droneBatteryInfoRef = null
        liveStreamRef?.close()
        liveStreamRef = null
        streamServerRef?.close()
        streamServerRef = null
        liveStream = null
        pilotingItfRef?.close()
        pilotingItfRef = null

        activeState.stopMonitoring()
        cameraMode.stopMonitoring()
        startStop.stopMonitoring()
    }

    private fun monitorDroneState() {
        droneStateRef = drone?.getState {
            it?.let {
                droneStatusTxt.text = getString(R.string.status_format, it.connectionState.toString(), getDroneBatteryCharge())
            }
        }
    }

    private fun monitorDroneBatteryChargeLevel() {
        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            it?.let {
                droneStatusTxt.text = getString(R.string.status_format, getDroneConnectionState(), getString(R.string.percentage, it.charge))
            }
        }
    }

    private fun getDroneConnectionState(): String {
        return droneStateRef?.get()?.connectionState.toString()
    }

    private fun getDroneBatteryCharge(): String {
        return droneBatteryInfoRef?.get()?.charge?.let { getString(R.string.percentage, it) } ?: ""
    }

    private fun resetRcUi() {
        rcStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")
    }

    private fun startRcMonitors() {
        monitorRcState()
        monitorRcBatteryChargeLevel()
    }

    private fun stopRcMonitors() {
        rcStateRef?.close()
        rcStateRef = null
        rcBatteryInfoRef?.close()
        rcBatteryInfoRef = null
    }

    private fun monitorRcState() {
        rcStateRef = rc?.getState {
            it?.let {
                rcStatusTxt.text = getString(R.string.status_format, it.connectionState.toString(), getRcBatteryCharge())
            }
        }
    }

    private fun monitorRcBatteryChargeLevel() {
        rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
            it?.let {
                rcStatusTxt.text = getString(R.string.status_format, getRcConnectionState(), getString(R.string.percentage, it.charge))
            }
        }
    }

    private fun getRcConnectionState(): String {
        return rcStateRef?.get()?.connectionState.toString()
    }

    private fun getRcBatteryCharge(): String {
        return rcBatteryInfoRef?.get()?.charge?.let { getString(R.string.percentage, it) } ?: ""
    }

    private fun monitorPilotingInterface() {
        pilotingItfRef = drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
            if (it == null) {
                takeOffLandBt.isEnabled = false
            } else {
                managePilotingItfState(it)
            }
        }
    }

    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> takeOffLandBt.isEnabled = false
            Activable.State.IDLE -> {
                takeOffLandBt.isEnabled = false
                itf.activate()
            }
            Activable.State.ACTIVE -> {
                when {
                    itf.canTakeOff() -> {
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = getString(R.string.take_off)
                    }
                    itf.canLand() -> {
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = getString(R.string.land)
                    }
                    else -> takeOffLandBt.isEnabled = false
                }
            }
        }
    }

    private fun onTakeOffLandClick() {
        pilotingItfRef?.get()?.let { itf ->
            if (itf.canTakeOff()) {
                itf.takeOff()
            } else if (itf.canLand()) {
                itf.land()
            }
        }
    }

    private fun startVideoStream() {
        streamServerRef = drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            streamServer?.run {
                if (!streamingEnabled()) {
                    enableStreaming(true)
                }

                if (liveStreamRef == null) {
                    liveStreamRef = live { stream ->
                        if (stream != null) {
                            if (liveStream == null) {
                                streamView.setStream(stream)
                            }

                            if (stream.playState() != CameraLive.PlayState.PLAYING) {
                                stream.play()
                            }
                        } else {
                            streamView.setStream(null)
                        }
                        liveStream = stream
                    }
                }
            } ?: run {
                liveStreamRef?.close()
                liveStreamRef = null
                streamView.setStream(null)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDroneMonitors()
        stopRcMonitors()
    }
}


