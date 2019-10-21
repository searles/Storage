package at.searles.android.storage.demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import at.searles.android.storage.StorageActivity
import at.searles.storage.R

class DemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo)
    }

    fun startStorage(view: View) {
        Intent(this, StorageActivity::class.java).also {
            it.putExtra(StorageActivity.providerClassNameKey, DemoProvider::class.java.canonicalName)
            startActivityForResult(it, callback)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val callback = 101
    }
}
