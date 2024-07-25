package com.parrot.camera

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.parrot.drone.groundsdk.*
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus
import java.io.File
import java.util.*

class MediaListActivity : AppCompatActivity() {

    private lateinit var mediaListView: ListView
    private var mediaStoreRef: Ref<MediaStore>? = null
    private var mediaList: List<MediaItem> = emptyList()
    private lateinit var groundSdk: GroundSdk
    private var drone: Drone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_media_list)

        mediaListView = findViewById(R.id.media_list_view)


        groundSdk = ManagedGroundSdk.obtainSession(this)

        val droneUid = intent.getStringExtra("DRONE_UID")
        if (droneUid != null) {
            groundSdk.getDrone(droneUid)?.let { drone ->
                this.drone = drone
                mediaStoreRef = drone.getPeripheral(MediaStore::class.java) { mediaStore ->
                    mediaStore?.browse { list ->
                        mediaList = list ?: emptyList()
                        displayMediaList(mediaList)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Drone UID is missing", Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String) {
        val droneVideosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")
        if (!droneVideosDir.exists()) {
            droneVideosDir.mkdirs()
        }

        val file = File(droneVideosDir, fileName)
        val mediaDestination = MediaDestination.path(droneVideosDir)

        mediaStoreRef?.get()?.let { mediaStore ->
            mediaStore.download(
                listOf(resource),
                MediaStore.DownloadType.FULL,
                mediaDestination
            ) { downloader ->
                downloader?.let { mediaDownloader ->
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread {
                                val progress = mediaDownloader.totalProgress
                                val status = mediaDownloader.status
                                if (status == MediaTaskStatus.COMPLETE) {
                                    val downloadedFile = mediaDownloader.downloadedFile
                                    Toast.makeText(this@MediaListActivity, "$fileName downloaded to ${downloadedFile?.absolutePath}", Toast.LENGTH_SHORT).show()
                                    this.cancel()
                                } else {
                                    Toast.makeText(this@MediaListActivity, "Download progress: $progress%", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }, 0, 1000) // Check every second
                }
            }
        }
    }
}