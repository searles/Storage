package at.searles.storage

import android.widget.ImageView
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import java.util.stream.Stream

class Data: ViewModel() {
    private val items = ArrayList<String>(100).also {
        (1..100).forEach { i -> it.add("$i") }
    }

    fun keys(): List<String> {
        return items
    }

    /**
     * The title should always at least contain the key as a substring because
     * the search filter operates on the key.
     */
    fun getTitle(key: String): String {
        return "title $key"
    }

    fun getSubtitle(key: String): String {
        return "subtitle $key"
    }

    fun getImageInView(key: String, imageView: ImageView) {
        Glide
            .with(imageView.context)
            .load(R.drawable.ic_launcher_foreground)
            .centerCrop()
            // TODO .placeholder(R.drawable.loading_spinner)
            .into(imageView)
    }

    fun remove(key: String) {
        items.remove(key)
    }
}