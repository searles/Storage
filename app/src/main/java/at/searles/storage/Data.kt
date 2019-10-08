package at.searles.storage

import android.widget.ImageView
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide

class Data: ViewModel() {
    private val items = ArrayList<String>(100).also {
        (1..1000).forEach { i -> it.add("abc$i") }
    }

    fun names(): List<String> {
        return items
    }

    fun getDescription(name: String): String {
        return "subtitle $name"
    }

    fun getImageInView(name: String, imageView: ImageView) {
        Glide
            .with(imageView.context)
            .load(R.drawable.ic_launcher_foreground)
            .centerCrop()
            // TODO .placeholder(R.drawable.loading_spinner)
            .into(imageView)
    }

    fun remove(name: String) {
        items.remove(name)
    }
}