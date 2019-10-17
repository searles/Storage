package at.searles.storage.data

import android.content.Context
import java.lang.RuntimeException

/**
 * Save and load data based on a name.
 */
interface DataProvider<A> {
    fun save(context: Context, name: String, value: A): Boolean
    fun load(context: Context, name: String): A

    class DataProviderException(context: Context, msgId: Int, name: String): RuntimeException(context.getString(msgId, name))
}