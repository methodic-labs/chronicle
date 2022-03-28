package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.android.ChronicleDataUpload
import com.openlattice.chronicle.util.RetrofitBuilders
import retrofit2.Retrofit
import java.util.*

/*
 * This class is mainly for testing.
 */
class ConsoleSink : DataSink {
    override fun submit(data: List<ChronicleDataUpload>): Map<String, Boolean> {
        Log.d(javaClass.name, RetrofitBuilders.mapper.writeValueAsString(data))
        return mapOf(ConsoleSink::class.java.name to true )
    }
}