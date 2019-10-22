package at.searles.android.storage.data

import android.content.Context
import java.lang.RuntimeException

/**
 * Save and load data based on a name.
 */
interface DataProvider<A> {
    fun exists(context: Context, name: String): Boolean
    fun load(context: Context, name: String): A

    fun save(context: Context, name: String, value: A, allowOverride: Boolean): Boolean

    class DataProviderException(context: Context, msgId: Int, name: String): RuntimeException(context.getString(msgId, name))
}