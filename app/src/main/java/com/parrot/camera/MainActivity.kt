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
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView
import com.parrot.drone.groundsdk.stream.GsdkStreamView.PADDING_FILL_BLUR_CROP

import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import java.io.File
import java.util.Timer
import java.util.TimerTask
import android.os.Environment
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.view.View
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus

class MainActivity : AppCompatActivity() {

    /** GroundSdk instance. */
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
    /** Reference to a current drone piloting interface. */
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null
    /** Reference to the current drone battery info instrument. */
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    /** Reference to the current drone media store. */
    private var mediaStoreRef: Ref<MediaStore>? = null
    /** List of media items on the drone. */
    private var mediaList: List<MediaItem> = emptyList()

    // Remote control:
    /** Current remote control instance. */
    private var rc: RemoteControl? = null
    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null
    /** Reference to the current remote control battery info instrument. */
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null
    private val mediaListView by lazy { findViewById<ListView>(R.id.media_list_view) }
    // User interface:
    /** Video stream view. */
    private val streamView by lazy { findViewById<GsdkStreamView>(R.id.stream_view) }
    /** Drone state and battery text view. */
    private val droneStatusTxt by lazy { findViewById<TextView>(R.id.droneStatusTxt) }
    /** Remote state and battery text view. */
    private val rcStatusTxt by lazy { findViewById<TextView>(R.id.rcStatusTxt) }
    /** Take off / land button. */
    private val takeOffLandBt by lazy { findViewById<Button>(R.id.takeOffLandBt) }

    // Delegates to manage camera user interface:
    /** Delegate to display camera active state. */
    private val activeState by lazy { ActiveState(findViewById(R.id.activeTxt)) }
    /** Delegate to display and change camera mode. */
    private val cameraMode by lazy { CameraMode(findViewById(R.id.photoMode), findViewById(R.id.recordingMode)) }
    /** Delegate to manage start and stop photo capture and video recording button. */
    private val startStop by lazy { StartStop(findViewById(R.id.startStopBtn)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //mine is activity_main_land
        setContentView(R.layout.activity_main_land)

        // Initialize user interface default values.
        droneStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")
        rcStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")


        streamView.paddingFill = PADDING_FILL_BLUR_CROP

        takeOffLandBt.setOnClickListener { onTakeOffLandClick() }
        //add battery view here
        //mine has conflicts with new code, need to check how to fix
        //battery view, to link to other view
        //val batteryStatusButton:Button = findViewById(R.id.batteryStatusButton)
        //batteryStatusButton.setOnClickListener {
        //    val intent = Intent(this@MainActivity, BatteryStatusActivity::class.java)
        //    startActivity(intent)
        //}
        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.
    }
    override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) {
            // Called when the auto connection facility is available and when it changes.

            it?.let {
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {
                    if (drone != null) {
                        // Stop monitoring the previous drone.
                        stopDroneMonitors()

                        // Reset user interface drone part.
                        resetDroneUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if (drone != null) {
                        startDroneMonitors()
                    }
                }

                // If the remote control has changed.
                if (rc?.uid != it.remoteControl?.uid) {
                    if (rc != null) {
                        // Stop monitoring the previous remote.
                        stopRcMonitors()

                        // Reset user interface Remote part.
                        resetRcUi()
                    }

                    // Monitor the new remote.
                    rc = it.remoteControl
                    if (rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    /**
     * Resets drone user interface part.
     */
    private fun resetDroneUi() {
        // Reset drone user interface views.
        droneStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")
        takeOffLandBt.isEnabled = false
        // Stop rendering the stream.
        streamView.setStream(null)
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()

        // Monitor drone battery charge level.
        monitorDroneBatteryChargeLevel()

        // Start video stream.
        startVideoStream()

        // Monitor piloting interface.
        monitorPilotingInterface()

        // Start monitoring by camera user interface delegates.
        drone?.let { drone ->
            activeState.startMonitoring(drone)
            cameraMode.startMonitoring(drone)
            startStop.startMonitoring(drone)
        }

        // Monitor media store.
        monitorMediaStore()
    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

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

        mediaStoreRef?.close()
        mediaStoreRef = null

        // Stop monitoring by camera user interface delegates.
        activeState.stopMonitoring()
        cameraMode.stopMonitoring()
        startStop.stopMonitoring()
    }

    /**
     * Monitor current drone state.
     */
    private fun monitorDroneState() {
        // Monitor current drone state.
        droneStateRef = drone?.getState {
            // Called at each drone state update.

            it?.let {
                // Update drone connection state and battery charge level view.
                droneStatusTxt.text = getString(R.string.status_format, it.connectionState.toString(), getDroneBatteryCharge())
            }
        }
    }

    /**
     * Monitors current drone battery charge level.
     */
    private fun monitorDroneBatteryChargeLevel() {
        // Monitor the battery info instrument.
        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                droneStatusTxt.text = getString(R.string.status_format, getDroneConnectionState(), getString(R.string.percentage, it.charge))
            }
        }
    }

    /**
     * Helper function to get the current drone connection state.
     */
    private fun getDroneConnectionState(): String {
        return droneStateRef?.get()?.connectionState.toString()
    }

    /**
     * Helper function to get the current drone battery charge level.
     */
    private fun getDroneBatteryCharge(): String {
        return droneBatteryInfoRef?.get()?.charge?.let { getString(R.string.percentage, it) } ?: ""
    }

    /**
     * Resets remote user interface part.
     */
    private fun resetRcUi() {
        // Reset remote control user interface views.
        rcStatusTxt.text = getString(R.string.status_format, DeviceState.ConnectionState.DISCONNECTED.toString(), "")
    }

    /**
     * Starts remote control monitors.
     */
    private fun startRcMonitors() {
        // Monitor remote state.
        monitorRcState()

        // Monitor remote battery charge level.
        monitorRcBatteryChargeLevel()
    }

    /**
     * Stops remote control monitors.
     */
    private fun stopRcMonitors() {
        // Close all references linked to the current remote to stop their monitoring.

        rcStateRef?.close()
        rcStateRef = null

        rcBatteryInfoRef?.close()
        rcBatteryInfoRef = null
    }

    /**
     * Monitor current remote control state.
     */
    private fun monitorRcState() {
        // Monitor current remote control state.
        rcStateRef = rc?.getState {
            // Called at each remote state update.

            it?.let {
                // Update remote connection state and battery charge level view.
                rcStatusTxt.text = getString(R.string.status_format, it.connectionState.toString(), getRcBatteryCharge())
            }
        }
    }

    /**
     * Monitors current remote control battery charge level.
     */
    private fun monitorRcBatteryChargeLevel() {
        // Monitor the battery info instrument.
        rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update remote battery charge level view.
                rcStatusTxt.text = getString(R.string.status_format, getRcConnectionState(), getString(R.string.percentage, it.charge))
            }
        }
    }

    /**
     * Helper function to get the current remote connection state.
     */
    private fun getRcConnectionState(): String {
        return rcStateRef?.get()?.connectionState.toString()
    }

    /**
     * Helper function to get the current remote battery charge level.
     */
    private fun getRcBatteryCharge(): String {
        return rcBatteryInfoRef?.get()?.charge?.let { getString(R.string.percentage, it) } ?: ""
    }

    /**
     * Monitors current drone piloting interface.
     */
    private fun monitorPilotingInterface() {
        // Monitor a piloting interface.
        pilotingItfRef = drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
            // Called when the manual copter piloting Interface is available
            // and when it changes.

            // Disable the button if the piloting interface is not available.
            if (it == null) {
                takeOffLandBt.isEnabled = false
            } else {
                managePilotingItfState(it)
            }
        }
    }

    /**
     * Manage piloting interface state.
     *
     * @param itf the piloting interface
     */
    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> {
                // Piloting interface is unavailable.
                takeOffLandBt.isEnabled = false
            }

            Activable.State.IDLE -> {
                // Piloting interface is idle.
                takeOffLandBt.isEnabled = false

                // Activate the interface.
                itf.activate()
            }

            Activable.State.ACTIVE -> {
                // Piloting interface is active.

                when {
                    itf.canTakeOff() -> {
                        // Drone can take off.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = getString(R.string.take_off)
                    }
                    itf.canLand() -> {
                        // Drone can land.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = getString(R.string.land)
                    }
                    else -> // Disable the button.
                        takeOffLandBt.isEnabled = false
                }
            }
        }
    }

    /**
     * Called on take off/land button click.
     */
    private fun onTakeOffLandClick() {
        // Get the piloting interface from its reference.
        pilotingItfRef?.get()?.let { itf ->
            // Do the action according to the interface capabilities.
            if (itf.canTakeOff()) {
                // Take off.
                itf.takeOff()
            } else if (itf.canLand()) {
                // Land.
                itf.land()
            }
        }
    }

    /**
     * Starts the video stream.
     */
    private fun startVideoStream() {
        // Monitor the stream server.
        streamServerRef = drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            // Called when the stream server is available and when it changes.

            streamServer?.run {
                // Enable Streaming.
                if (!streamingEnabled()) {
                    enableStreaming(true)
                }

                // Monitor the live stream.
                if (liveStreamRef == null) {
                    liveStreamRef = live { stream ->
                        // Called when the live stream is available and when it changes.

                        if (stream != null) {
                            if (liveStream == null) {
                                // It is a new live stream.

                                // Set the live stream as the stream to be rendered by the stream view.
                                streamView.setStream(stream)
                            }

                            // Play the live stream.
                            if (stream.playState() != CameraLive.PlayState.PLAYING) {
                                stream.play()
                            }
                        } else {
                            // Stop rendering the stream.
                            streamView.setStream(null)
                        }
                        // Keep the live stream to know if it is a new one or not.
                        liveStream = stream
                    }
                }
            } ?: run {
                // Stop monitoring the live stream.
                liveStreamRef?.close()
                liveStreamRef = null
                // Stop rendering the stream.
                streamView.setStream(null)
            }
        }
    }

    /**
     * Monitor the drone's media store.
     */
    private fun monitorMediaStore() {
        mediaStoreRef = drone?.getPeripheral(MediaStore::class.java) { mediaStore ->
            mediaStore?.browse { list ->
                mediaList = list ?: emptyList()
                displayMediaList(mediaList)
            }
        }
    }

    /**
     * Display the media list in the ListView.
     */
    private fun displayMediaList(mediaList: List<MediaItem>) {
        val mediaNames = mediaList.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mediaNames)
        mediaListView.adapter = adapter

        mediaListView.setOnItemClickListener { _, _, position, _ ->
            val selectedMedia = mediaList[position]
            val mediaResource = selectedMedia.resources.firstOrNull()
            mediaResource?.let {
                downloadMediaResource(it, selectedMedia.name)
            }
        }
    }


    /**
     * Download the selected media resource.
     */
    private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String) {
        val droneVideosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")
        if (!droneVideosDir.exists()) {
            droneVideosDir.mkdirs()
        }

        val file = File(droneVideosDir, fileName)
        val mediaDestination = MediaDestination.Companion.path(file)

        mediaStoreRef?.get()?.let { mediaStore ->
            mediaStore.download(
                listOf(resource),
                MediaStore.DownloadType.FULL,
                mediaDestination
            ) { downloader ->
                downloader?.let { mediaDownloader ->
                    // Check download status
                    if (mediaDownloader.status == MediaTaskStatus.RUNNING) {
                        // Periodically check the download progress
                        Timer().schedule(object : TimerTask() {
                            override fun run() {
                                runOnUiThread {
                                    val progress = mediaDownloader.totalProgress
                                    val status = mediaDownloader.status
                                    if (status == MediaTaskStatus.COMPLETE) {
                                        val downloadedFile = mediaDownloader.downloadedFile
                                        Toast.makeText(this@MainActivity, "$fileName downloaded to ${downloadedFile?.absolutePath}", Toast.LENGTH_SHORT).show()
                                        this.cancel()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Download progress: $progress%", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }, 0, 1000) // Check every second
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDroneMonitors()
        stopRcMonitors()
    }
}
