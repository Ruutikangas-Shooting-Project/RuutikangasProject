package com.parrot.camera

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.MediaStore

// MediaListAdapter is a custom adapter class that extends BaseAdapter to display media items with thumbnails in a ListView.
class MediaListAdapter(
    private val context: Context,            // The context in which the adapter is used.
    private val mediaItems: List<MediaItem>, // List of media items to display.
    private val mediaStoreRef: Ref<MediaStore>? // Reference to MediaStore for fetching thumbnails.
) : BaseAdapter() {

    // Returns the number of items in the mediaItems list.
    override fun getCount(): Int = mediaItems.size

    // Returns the media item at the specified position.
    override fun getItem(position: Int): Any = mediaItems[position]

    // Returns the ID of the item at the specified position.
    override fun getItemId(position: Int): Long = position.toLong()

    // getView method to provide a view for an adapter view (ListView) with a data item at a specific position.
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.media_list_item, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        // Get the media item at the current position.
        val mediaItem = mediaItems[position]

        // Set the media name in the TextView.
        viewHolder.mediaNameTextView.text = mediaItem.name

        // Clear the ImageView to avoid displaying incorrect images due to view recycling.
        //viewHolder.thumbnailImageView.setImageResource(R.drawable.default_thumbnail)

        // Fetch the thumbnail using the MediaStore reference.
        /*mediaStoreRef?.get()?.fetchThumbnailOf(mediaItem, Ref.Observer { bitmap ->
            // Ensure the current position matches before setting the bitmap to avoid incorrect images.
            if (bitmap != null && viewHolder.position == position) {
                viewHolder.thumbnailImageView.setImageBitmap(bitmap)
            }
        })*/

        // Store the current position in the ViewHolder for reference during thumbnail loading.
        viewHolder.position = position

        return view
    }

    // ViewHolder class to hold references to the views for each item in the list.
    private class ViewHolder(view: View) {
        val mediaNameTextView: TextView = view.findViewById(R.id.mediaNameTextView) // TextView for the media name.
        //val thumbnailImageView: ImageView = view.findViewById(R.id.thumbnailImageView) // ImageView for the thumbnail.
        var position: Int = -1 // Variable to hold the current position for matching with the correct item.
    }
}