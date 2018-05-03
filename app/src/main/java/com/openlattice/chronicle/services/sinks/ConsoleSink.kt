package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.google.common.collect.SetMultimap
import java.util.*

/*
 * This class is mainly for testing.
 */
class ConsoleSink : DataSink {
    val TAG = javaClass.canonicalName;
    override fun submit(data: List<SetMultimap<UUID, Any>>) {
        Log.d(TAG, data.toString() )
    }
}