package com.parrot.camera

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.internal.device.peripheral.DtedStoreCore
import java.io.File
//import java.util.jar.Manifest
import android.Manifest
import androidx.core.content.ContextCompat


class MediaListActivity : AppCompatActivity() {

    private lateinit var groundSdk: ManagedGroundSdk
    private lateinit var mediaListView: ListView
    private lateinit var mediaManager: MediaManager
    private var drone: Drone? = null
    private var mediaStoreRef: Ref<MediaStore>? = null
    private var selectedMediaItem: MediaItem? = null
    companion object {
        private const val REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_list)
        //checkPermission()
        //ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
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

     
    //0813 can see file name but cant download, try new one
    private fun monitorMediaStore() {
        drone?.let { drone->
            mediaStoreRef = drone.getPeripheral(MediaStore::class.java) {mediaStore ->
                mediaStore?.let {store ->
                    store.browse{list ->
                        list?.let {
                            displayMediaList(it)
                        }

                    }
                }

            }
        }
    }
    //0812 try new code to save files
    /*
    private fun monitorMediaStore()  {
        drone?.let {drone ->
            mediaStoreRef = drone.getPeripheral(MediaStore::class.java)  {mediaStore->
                mediaStore?.let { store ->
                    store.browse {list ->
                        if (list !=null) {
                            displayMediaList(list)
                        }

                    }
                }

            }

        }

    }*/
    private fun displayMediaList(mediaList: List<MediaItem>) {
        val mediaNames = mediaList.map {mediaItem->
            val extension = when(mediaItem.type) {
                MediaItem.Type.VIDEO -> ".mp4"
                MediaItem.Type.PHOTO -> ".jpg"
                else ->""
            }
            "${mediaItem.name}$extension"
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mediaNames)
        mediaListView.adapter = adapter

        //0813 new code
        mediaListView.setOnItemClickListener{_, _, position, _->
            val selectMedia = mediaList[position]
            selectMedia.resources.firstOrNull().let {
                downloadMediaResource(it!!, selectMedia.name)
            }

        }
    }
    //0814 new code
    private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String){
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")
        val extension = when(resource.format){
            MediaItem.Resource.Format.JPG -> ".jpg"
            MediaItem.Resource.Format.MP4 -> ".mp4"
            else -> ""
        }
        val droneVideosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")
        if (!droneVideosDir.exists()) {
            droneVideosDir.mkdirs()
        }
        val file = File(droneVideosDir, "$fileName$extension")
        val mediaDestination = MediaDestination.path(file)
        mediaStoreRef?.get()?.download(listOf(resource), MediaStore.DownloadType.FULL, mediaDestination) { downloader ->
            downloader?.let { mediaDownloader ->
                var fileProgress= mediaDownloader.currentFileProgress
                Toast.makeText(this@MediaListActivity, "Downloading $fileName, progress $fileProgress %", Toast.LENGTH_SHORT).show()
            }
        }

    }


    //old code
    /*
    private fun monitorMediaStore() {
        drone?.let { drone ->
            mediaStoreRef = drone.getPeripheral(MediaStore::class.java) { mediaStore ->
                mediaStore?.let { store ->
                    mediaManager.monitorMediaStore(drone)
                }
            }
        }
    }
*/
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

//0820 new code
private  fun deleteSelectedMedia() {
    selectedMediaItem?.let { mediaItem ->
        if (mediaItem.resources.isEmpty()) {
            Toast.makeText(this, "No file available to delete", Toast.LENGTH_SHORT).show()
        }else{
            mediaManager.deleteMedia(mediaItem.resources)
        }
}?: Toast.makeText(this, "no media item selected", Toast.LENGTH_SHORT).show()
}

//0812 old code, change due to new code
    /*
private fun deleteSelectedMedia() {
    selectedMediaItem?.let { mediaItem ->
        val resources = mediaItem.resources
        mediaManager.deleteMedia(resources)
    }
}*/
}


