package com.parrot.camera

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.facility.AutoConnection

class MediaListActivity : AppCompatActivity() {

    private lateinit var groundSdk: ManagedGroundSdk
    private lateinit var mediaListView: ListView
    private lateinit var mediaManager: MediaManager
    private var drone: Drone? = null
    private var mediaStoreRef: Ref<MediaStore>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_list)

        groundSdk = ManagedGroundSdk.obtainSession(this)
        mediaListView = findViewById(R.id.media_list_view)

        mediaManager = MediaManager(this, mediaListView)
        monitorAutoConnection()
    }

    private fun monitorAutoConnection() {
        groundSdk.getFacility(AutoConnection::class.java) { autoConnection ->
            autoConnection?.let {
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                if (drone?.uid != it.drone?.uid) {
                    drone = it.drone
                    monitorMediaStore()
                }
            }
        }
    }

    private fun monitorMediaStore() {
        drone?.let { drone ->
            mediaStoreRef = drone.getPeripheral(MediaStore::class.java) { mediaStore ->
                mediaStore?.let { store ->
                    mediaManager.monitorMediaStore(drone)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaManager.close()
        mediaStoreRef?.close()
    }
}
