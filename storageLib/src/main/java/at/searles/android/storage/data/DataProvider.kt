package at.searles.android.storage.data

/**
 * Save and load data based on a name.
 */
interface DataProvider<A>: InformationProvider {
    /**
     * Loads content with key 'name'
     */
    fun load(name: String, contentHolder: (A) -> Unit)

    /**
     * Saves content under the name 'name'. If such an entry already exists
     * and allowOverride is false, then false is returned.
     */
    fun save(name: String, value: () -> A, allowOverride: Boolean): Boolean
}