package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.util.RetrofitBuilders
import retrofit2.Retrofit
import java.util.*

/*
 * This class is mainly for testing.
 */
class ConsoleSink : DataSink {
    override fun submit(data: List<SetMultimap<UUID, Any>>): Map<String, Boolean> {
        Log.d(javaClass.name, RetrofitBuilders.mapper.writeValueAsString(data))
        return mapOf(ConsoleSink::javaClass.name to true )
    }
}