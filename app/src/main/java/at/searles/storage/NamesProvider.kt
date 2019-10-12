package at.searles.storage

interface NamesProvider {
    fun size(): Int
    fun getNames(): List<String>
    fun delete(name: String)
    fun rename(oldName: String, newName: String)
}