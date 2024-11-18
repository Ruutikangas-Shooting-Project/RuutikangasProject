package com.parrot.camera

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MediaManager(
    private val context: Context,
    //private val filesDir: File,
    private val mediaListView: ListView,
    private val droneVideosListView: ListView
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


    private fun displayMediaList(mediaList: List<MediaItem>) {
        val adapter = MediaListAdapter(context, mediaList, mediaStoreRef)
        mediaListView.adapter = adapter

        mediaListView.setOnItemClickListener { _, _, position, _ ->
            val selectedMedia = mediaList[position]
            val mediaResource = selectedMedia.resources.firstOrNull()
            mediaResource?.let {
                downloadMediaResource(it, selectedMedia.name)
                //showNamingDialog(it)
            }
        }
    }

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

    /*private fun downloadMediaResource(resource: MediaItem.Resource, shooterId: String, sessionType: String) {
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
    }*/

    private fun downloadMediaResource(resource: MediaItem.Resource, fileName: String) {
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
        val mediaDestination = MediaDestination.path(directory)

        mediaStoreRef?.get()?.download(listOf(resource), MediaStore.DownloadType.FULL, mediaDestination) { downloader ->
            downloader?.let { mediaDownloader ->
                val fileProgress = mediaDownloader.currentFileProgress
                Toast.makeText(context, "Download status: $fileProgress%", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun uploadToFirebaseStorage(file: File, customFileName: String) {
        if (!file.exists() || !file.isFile) {
            Log.e("FirebaseUploadError", "Invalid file selected for upload: ${file.absolutePath}")
            Toast.makeText(context, "Invalid file selected for upload", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val firebaseFileName = "${customFileName}_$timeStamp.${file.extension}"

        val storage = FirebaseStorage.getInstance()
        val storageRef: StorageReference = storage.reference
        val fileRef = storageRef.child("drone_media/$firebaseFileName")

        val fileUri = Uri.fromFile(file)
        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                Toast.makeText(context, "File uploaded successfully to Firebase as $firebaseFileName!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                val errorMessage = exception.localizedMessage ?: "Unknown error"
                Log.e("FirebaseUploadError", "Failed to upload file: $errorMessage")
                Toast.makeText(context, "Failed to upload file: $errorMessage", Toast.LENGTH_LONG).show()
            }
    }

    fun browseExternalStorageForVideos() {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DroneVideos")
        if (!directory.exists() || !directory.isDirectory) {
            Toast.makeText(context, "No videos found", Toast.LENGTH_SHORT).show()
            return
        }

        val videoFiles = directory.listFiles { file ->
            file.isFile && file.extension.equals("mp4", ignoreCase = true)
        } ?: emptyArray()

        if (videoFiles.isEmpty()) {
            Toast.makeText(context, "No videos found", Toast.LENGTH_SHORT).show()
            return
        }

        val videoNames = videoFiles.map { it.name }
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, videoNames)
        droneVideosListView.adapter = adapter

        droneVideosListView.setOnItemClickListener { _, _, position, _ ->
            val selectedVideo = videoFiles[position]
            showNamingDialog(selectedVideo)
        }
    }


    /*private fun showNamingDialog(resource: MediaItem.Resource) {
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
    }*/

    private fun showNamingDialog(file: File) {
        fetchUsersFromFirestore { userList ->
            if (userList.isNotEmpty()) {
                val userNames = userList.map { "${it.first} ${it.second}" }
                val dialogBuilder = AlertDialog.Builder(context)
                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_naming, null)
                dialogBuilder.setView(dialogView)

                val userNameSpinner = dialogView.findViewById<Spinner>(R.id.userNameSpinner)
                val sessionTypeSpinner = dialogView.findViewById<Spinner>(R.id.sessionTypeSpinner)

                val userAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, userNames)
                userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                userNameSpinner.adapter = userAdapter

                val sessionAdapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_item,
                    context.resources.getStringArray(R.array.session_types) // Load from strings.xml

                )
                sessionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                sessionTypeSpinner.adapter = sessionAdapter

                dialogBuilder.setPositiveButton("Save") { _, _ ->
                    val selectedUserPosition = userNameSpinner.selectedItemPosition
                    val selectedUser = userList[selectedUserPosition]
                    val customFileName = "${selectedUser.first}_${selectedUser.second}"
                    val sessionType = sessionTypeSpinner.selectedItem.toString()
                    uploadToFirebaseStorage(file, "$customFileName ($sessionType)")
                }

                dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

                dialogBuilder.create().show()
            } else {
                Toast.makeText(context, "No users found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*private fun fetchUsersFromFirestore(callback: (List<Pair<String, String>>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").get()
            .addOnSuccessListener { result ->
                val userList = result.documents.mapNotNull {
                    val firstName = it.getString("fm")
                    val lastName = it.getString("lm")
                    if (firstName != null && lastName != null) Pair(firstName, lastName) else null
                }
                callback(userList)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch users", Toast.LENGTH_SHORT).show()
                callback(emptyList())
            }
    }*/

    private fun fetchUsersFromFirestore(callback: (List<Pair<String, String>>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("users")

        usersRef.get().addOnSuccessListener { documents ->
            val userList = mutableListOf<Pair<String, String>>()
            for (document in documents) {
                val firstName = document.getString("fm") ?: ""
                val lastName = document.getString("lm") ?: ""
                userList.add(Pair(firstName, lastName))
            }
            callback(userList) // Pass the list of user names to the callback
        }.addOnFailureListener { exception ->
            Log.e("FirestoreError", "Error fetching users: ", exception)
            Toast.makeText(context, "Failed to fetch users", Toast.LENGTH_SHORT).show()
        }
    }



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

