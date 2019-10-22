package at.searles.android.storage

interface StorageDialogCallback {
    fun discardAndOpen(name: String)
    fun overrideAndSaveAs(name: String)
}