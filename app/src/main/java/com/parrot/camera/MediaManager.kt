package com.parrot.camera

import android.content.Context
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus
import java.io.File
import android.view.LayoutInflater

class MediaManager(
    private val context: Context,
    private val mediaListView: ListView
) {
    var mediaList: List<MediaItem> = emptyList()
        private set
    private var mediaStoreRef: Ref<MediaStore>? = null

    fun monitorMediaStore(drone: Drone) {
        mediaStoreRef = drone.getPeripheral(MediaStore::class.java) { mediaStore ->
            mediaStore?.browse { list ->
                mediaList = list ?: emptyList()
                displayMediaList(mediaList)
            }
        }
    }

    /*private fun displayMediaList(mediaList: List<MediaItem>) {
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
    }*/

    /*private fun displayMediaList(mediaList: List<MediaItem>) {
        // For display purposes, append file extensions to media names
//        val mediaNames = mediaList.map { mediaItem ->
//            val extension = when (mediaItem.type) {
//                MediaItem.Type.VIDEO -> ".mp4"
//                MediaItem.Type.PHOTO -> ".jpg"
//                else -> ""
//            }
//            "${mediaItem.name}$extension"
//        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1) //mediaNames
        mediaListView.adapter = adapter

        // Handle item click for downloading media
        mediaListView.setOnItemClickListener { _, _, position, _ ->
            val selectedMedia = mediaList[position]
            val mediaResource = selectedMedia.resources.firstOrNull()
            mediaResource?.let {
                //downloadMediaResource(it, selectedMedia.name)  // Use original name for download
                showNamingDialog(it)
            }
        }
    }*/

    private fun displayMediaList(mediaList: List<MediaItem>) {
        val adapter = MediaListAdapter(context, mediaList, mediaStoreRef)
        mediaListView.adapter = adapter

        mediaListView.setOnItemClickListener { _, _, position, _ ->
            val selectedMedia = mediaList[position]
            val mediaResource = selectedMedia.resources.firstOrNull()
            mediaResource?.let {
                //downloadMediaResource(it, selectedMedia.name)
                showNamingDialog(it)
            }
        }
    }

    //8-20 test new
    /*private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String){
        val directoryType = when (resource.format) {
            MediaItem.Resource.Format.MP4-> Environment.DIRECTORY_MOVIES
            MediaItem.Resource.Format.JPG -> Environment.DIRECTORY_PICTURES
            else -> {
                Toast.makeText(context, "File format is not supported", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val  directory = File(context.getExternalFilesDir(directoryType), "DroneMedia")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        val mediaDestination = MediaDestination.path(file)

        mediaStoreRef?.get()?.let { mediaStore ->
            mediaStore.download(
                listOf(resource),
                MediaStore.DownloadType.FULL,
                mediaDestination
            ) { downloader ->
                downloader?.let { mediaDownloader ->
                    val timer = Timer()
                    timer.schedule(timerTask {
                        (context as Activity).runOnUiThread {
                            val progress = mediaDownloader.totalProgress
                            val status = mediaDownloader.status
                            if (status == MediaTaskStatus.COMPLETE) {
                                val downloadedFile = mediaDownloader.downloadedFile
                                Toast.makeText(context, "$fileName downloaded to ${downloadedFile?.absolutePath}", Toast.LENGTH_SHORT).show()
                                timer.cancel()
                            } else {
                                Toast.makeText(context, "Download status: $progress%", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, 0, 1000)

                }
            }
        }
    }*/

    //2592024
    /*private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String) {
        // Determine the appropriate directory based on file type
        val directory = when (resource.format) {
            MediaItem.Resource.Format.JPG -> {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DronePhotos")
            }
            MediaItem.Resource.Format.MP4 -> {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")
            }
            else -> {
                //Toast.makeText(this, "Unsupported file format", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        // Add the correct extension for saving purposes
        val extension = when (resource.format) {
            MediaItem.Resource.Format.JPG -> ".jpg"
            MediaItem.Resource.Format.MP4 -> ".mp4"
            else -> ""
        }

        // Save the file with the correct extension
        val file = File(directory, "$fileName$extension")
        val mediaDestination = MediaDestination.path(file)

        mediaStoreRef?.get()?.download(listOf(resource), MediaStore.DownloadType.FULL, mediaDestination) { downloader ->
            downloader?.let { mediaDownloader ->
                val fileProgress = mediaDownloader.currentFileProgress
                Toast.makeText(context, "Download status: $fileProgress%", Toast.LENGTH_SHORT).show()
            }
        }
    }*/

    private fun downloadMediaResource(resource: MediaItem.Resource, shooterId: String, sessionType: String) {
        // Get the current date and time for unique file naming
        val currentDateTime = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())

        // Sanitize Shooter ID to ensure it's valid in a file name
        val sanitizedShooterId = shooterId.replace(Regex("[^a-zA-Z0-9_]"), "")

        // Construct the file name using Shooter ID, date/time, and session type
        val fileName = "${sanitizedShooterId}_${currentDateTime}_$sessionType"

        // Set the fixed directory for the download (DroneVideos inside Movies)
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")

        // Create the DroneVideos folder if it doesn't exist
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // Add the correct extension for the file
        val extension = when (resource.format) {
            MediaItem.Resource.Format.JPG -> ".jpg"
            MediaItem.Resource.Format.MP4 -> ".mp4"
            else -> ""
        }

        // Save the file directly in the DroneVideos directory
        val file = File(directory, "$fileName$extension")

        // Specify the file download path as the media destination
        val mediaDestination = MediaDestination.path(file)

        // Start the download process to save the file to the specific folder
        mediaStoreRef?.get()?.download(listOf(resource), MediaStore.DownloadType.FULL, mediaDestination) { downloader ->
            downloader?.let { mediaDownloader ->
                val fileProgress = mediaDownloader.currentFileProgress
                // Show download status for user feedback
                //Toast.makeText(this, "Download status: $fileProgress%", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNamingDialog(resource: MediaItem.Resource) {
        // Use the activity context for creating the AlertDialog
        val dialogBuilder = AlertDialog.Builder(context)

        // Get the LayoutInflater from the context
        val inflater = LayoutInflater.from(context)

        // Inflate the custom dialog layout
        val dialogView = inflater.inflate(R.layout.dialog_naming, null)
        dialogBuilder.setView(dialogView)

        // Get references to the dialog input fields
        val shooterIdInput = dialogView.findViewById<EditText>(R.id.shooterIdInput)
        val sessionTypeSpinner = dialogView.findViewById<Spinner>(R.id.sessionTypeSpinner)

        // Setup session type spinner
        val adapter = ArrayAdapter.createFromResource(
            context,
            R.array.session_types, // Ensure that you have this array in your `res/values/strings.xml`
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sessionTypeSpinner.adapter = adapter

        dialogBuilder.setTitle("Set File Name and Session Type")

        // Set up dialog buttons
        dialogBuilder.setPositiveButton("Download") { _, _ ->
            val shooterId = shooterIdInput.text.toString()
            val sessionType = sessionTypeSpinner.selectedItem.toString()

            // Pass inputs to download the media with custom file name
            downloadMediaResource(resource, shooterId, sessionType)
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        // Show the dialog
        val dialog = dialogBuilder.create()
        dialog.show()
    }

//old code
    /*
    private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String) {
        val directory = when (resource.format) {
            MediaItem.Resource.Format.MP4 -> {
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "DroneVideos"
                )
            }
            MediaItem.Resource.Format.JPG -> {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DronePhotos")
            }
            else -> {
                Toast.makeText(context, "Unsupported file format", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)
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
                                (context as? MediaListActivity)?.runOnUiThread {
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
*/
    fun deleteMedia(resources: Collection<MediaItem.Resource>) {
        mediaStoreRef?.get()?.delete(resources) { deleter ->
            deleter?.let { mediaDeleter ->
                if (mediaDeleter.status == MediaTaskStatus.COMPLETE) {
                    Toast.makeText(context, "Deletion complete", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun close() {
        mediaStoreRef?.close()
        mediaStoreRef = null
    }
}

