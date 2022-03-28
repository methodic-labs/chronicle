package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.google.common.collect.SetMultimap
<<<<<<< HEAD
import com.openlattice.chronicle.android.ChronicleDataUpload
=======
import com.openlattice.chronicle.android.ChronicleUsageEvent
>>>>>>> e7c0ed09476a63cbd6b50ec78d74b7462710a223
import com.openlattice.chronicle.util.RetrofitBuilders
import retrofit2.Retrofit
import java.util.*

/*
 * This class is mainly for testing.
 */
class ConsoleSink : DataSink {
<<<<<<< HEAD
    override fun submit(data: List<ChronicleDataUpload>): Map<String, Boolean> {
=======
    override fun submit(data: List<ChronicleUsageEvent>): Map<String, Boolean> {
>>>>>>> e7c0ed09476a63cbd6b50ec78d74b7462710a223
        Log.d(javaClass.name, RetrofitBuilders.mapper.writeValueAsString(data))
        return mapOf(ConsoleSink::class.java.name to true )
    }
}