package com.parrot.camera

import android.Manifest
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem

class MediaListActivity : AppCompatActivity() {

    private lateinit var groundSdk: ManagedGroundSdk
    private lateinit var mediaListView: ListView
    private lateinit var mediaManager: MediaManager
    private var drone: Drone? = null
    private var mediaStoreRef: Ref<MediaStore>? = null
    private var selectedMediaItem: MediaItem? = null
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_list) // Use the correct activity layout

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)

        groundSdk = ManagedGroundSdk.obtainSession(this)
        mediaListView = findViewById(R.id.media_list_view)

        mediaManager = MediaManager(this, mediaListView)
        monitorAutoConnection()

        registerForContextMenu(mediaListView)
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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.media_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        selectedMediaItem = mediaManager.mediaList[info.position]

        return when (item.itemId) {
            R.id.delete -> {
                deleteSelectedMedia()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun deleteSelectedMedia() {
        selectedMediaItem?.let { mediaItem ->
            val resources = mediaItem.resources
            mediaManager.deleteMedia(resources)
        }
    }
}








