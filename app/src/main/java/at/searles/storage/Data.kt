package at.searles.storage

import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide

class Data: ViewModel() {
    private val items = ArrayList<String>(100).also {
        (1..1000).forEach { i -> it.add("abc$i") }
    }

    private val names = MutableLiveData<List<String>>().apply { value = items }

    fun size(): Int = items.size

    fun getNames(): LiveData<List<String>> {
        return names
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

    fun delete(name: String) {
        items.remove(name)
        names.value = items
    }
}