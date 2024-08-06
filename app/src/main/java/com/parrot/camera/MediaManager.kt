package com.parrot.camera

import android.content.Context
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus
import java.io.File
import java.util.Timer
import java.util.TimerTask

class MediaManager(
    private val context: Context,
    private val mediaListView: ListView
) {
    private var mediaStoreRef: Ref<MediaStore>? = null
    private var mediaList: List<MediaItem> = emptyList()

    fun monitorMediaStore(drone: Drone) {
        mediaStoreRef = drone.getPeripheral(MediaStore::class.java) { mediaStore ->
            mediaStore?.browse { list ->
                mediaList = list ?: emptyList()
                displayMediaList(mediaList)
            }
        }
    }

    private fun displayMediaList(mediaList: List<MediaItem>) {
        val mediaNames = mediaList.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, mediaNames)
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
                                (context as? MainActivity)?.runOnUiThread {
                                    val progress = mediaDownloader.totalProgress
                                    val status = mediaDownloader.status
                                    if (status == MediaTaskStatus.COMPLETE) {
                                        val downloadedFile = mediaDownloader.downloadedFile
                                        Toast.makeText(context, "$fileName downloaded to ${downloadedFile?.absolutePath}", Toast.LENGTH_SHORT).show()
                                        this.cancel()
                                    } else {
                                        Toast.makeText(context, "Download progress: $progress%", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }, 0, 1000) // Check every second
                    }
                }
            }
        }
    }

    fun close() {
        mediaStoreRef?.close()
        mediaStoreRef = null
    }
}