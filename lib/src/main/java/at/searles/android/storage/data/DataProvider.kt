package at.searles.android.storage.data

/**
 * Save and load data based on a name.
 */
interface DataProvider<A>: InformationProvider {
    fun load(name: String, contentHolder: (A) -> Unit)
    fun save(name: String, value: () -> A, allowOverride: Boolean): Boolean
}