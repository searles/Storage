package at.searles.storage

import java.util.stream.Stream

class Data {

    private val items = ArrayList<String>(100).also {
        (1..100).forEach { i -> it.add("$i") }
    }

    fun keys(): Stream<String> {
        return items.stream()
    }

    /**
     * The title should always at least contain the key as a substring because
     * the search filter operates on the key.
     */
    fun getTitle(key: String): String {
        return "title " + key
    }

    fun getSubtitle(key: String): String {
        return "subtitle " + key
    }

    fun size(): Int {
        return items.size
    }

    fun remove(key: String) {
        items.remove(key)
    }
}