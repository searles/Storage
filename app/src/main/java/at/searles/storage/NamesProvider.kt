package at.searles.storage

import androidx.lifecycle.LiveData

interface NamesProvider {
    fun size(): Int
    fun getNames(): LiveData<List<String>>
    fun delete(name: String)
}