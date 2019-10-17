package at.searles.android.storage

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.searles.android.storage.data.DemoProvider
import at.searles.android.storage.data.InformationProvider

class DemoActivity : StorageActivity() {

    override fun initInformationProvider(): InformationProvider.Mutable {
        return ViewModelProvider(this,
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.newInstance()
            }
        )[DemoProvider::class.java]
    }

    override fun confirm(name: String) {
        Toast.makeText(this, "Selected $name", Toast.LENGTH_SHORT).show()
    }
}
