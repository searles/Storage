package at.searles.android.storage.demo

import android.content.Context
import android.widget.ImageView
import at.searles.android.storage.data.FilesProvider
import at.searles.storage.R
import com.bumptech.glide.Glide

class DemoFilesProvider(context: Context) : FilesProvider(context.getDir(directoryName, 0)) {
    override fun setImageInView(name: String, imageView: ImageView) {
        Glide
            .with(imageView.context)
            .load(R.drawable.ic_launcher_foreground)
            .centerCrop()
            .into(imageView)
    }

    companion object {
        private const val directoryName = "demo"
    }
}