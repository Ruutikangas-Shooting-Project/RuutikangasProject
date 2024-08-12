package com.parrot.camera

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class GalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val videoListView: ListView = findViewById(R.id.video_list)

        // Assuming videos are stored in "DroneVideos" directory
        val videoDir = File(getExternalFilesDir(null), "DroneVideos")
        val videoFiles = videoDir.listFiles()?.filter { it.isFile && it.extension == "mp4" } ?: emptyList()

        // Display video names in the list
        val videoNames = videoFiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, videoNames)
        videoListView.adapter = adapter

        // Set item click listener to play video or open in a player
        videoListView.setOnItemClickListener { _, _, position, _ ->
            val selectedVideo = videoFiles[position]
            // Here you can implement logic to open or play the selected video
            // For example, using an intent to open the video in a media player
        }
    }
}
