package at.searles.storage

import android.widget.ImageView

interface InformationProvider {
    fun getDescription(name: String): String
    fun setImageInView(name: String, imageView: ImageView)
}