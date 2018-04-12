package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.google.common.collect.SetMultimap

/*
 * This class is mainly for testing.
 */
class ConsoleSink : DataSink {
    val TAG = javaClass.canonicalName;
    override fun submit(data: SetMultimap<String, Object>) {
        Log.d(TAG, data.toString() )
    }
}