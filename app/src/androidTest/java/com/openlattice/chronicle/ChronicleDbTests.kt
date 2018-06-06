package com.openlattice.chronicle

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.openlattice.chronicle.storage.ChronicleDb
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import android.arch.persistence.room.Room
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.storage.StorageQueue
import junit.framework.Assert


@RunWith(AndroidJUnit4::class)
class ChronicleDbTests {
    companion object ChronicleDbHolder {
        lateinit var chronicleDb: ChronicleDb
        lateinit var storageQueue: StorageQueue

        @BeforeClass
        @JvmStatic
        fun setupChronicleDb() {
            val appContext = InstrumentationRegistry.getTargetContext()
            chronicleDb = Room.databaseBuilder(appContext, ChronicleDb::class.java!!, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            chronicleDb.queueEntryData().deleteEntries( chronicleDb.queueEntryData().getNextEntries(1000) )
        }

    }

    @Test
    fun testchronicleeadWriteSingleQueueEntry() {
        val qe = QueueEntry(System.currentTimeMillis(), 1, ByteArray(8, { i -> (i * i).toByte() }))
        ChronicleDbHolder.storageQueue.insertEntry(qe);
        val actual = ChronicleDbHolder.storageQueue.getNextEntries(1)[0]
        Assert.assertEquals(qe, actual)

        ChronicleDbHolder.storageQueue.deleteEntry(qe)

        val qe1 = QueueEntry(System.currentTimeMillis(), 1, ByteArray(8, { i -> (i * i).toByte() }))
        Thread.sleep(100);
        val qe2 = QueueEntry(System.currentTimeMillis(), 1, ByteArray(8, { i -> (i * i).toByte() }))
        Thread.sleep(100);
        val qe3 = QueueEntry(System.currentTimeMillis(), 1, ByteArray(8, { i -> (i * i).toByte() }))
        Thread.sleep(100);
        val qe4 = QueueEntry(System.currentTimeMillis(), 1, ByteArray(8, { i -> (i * i).toByte() }))

        val qeList = ArrayList<QueueEntry>(4)
        qeList.add(qe4)
        qeList.add(qe2)
        qeList.add(qe1)
        qeList.add(qe3)

        ChronicleDbHolder.storageQueue.insertEntries(qeList);
        val actualArr = ChronicleDbHolder.storageQueue.getNextEntries(4)
        Assert.assertEquals(4, actualArr.size )
        Assert.assertEquals(qe1,actualArr[ 0 ])
        Assert.assertEquals(qe2,actualArr[ 1 ])
        Assert.assertEquals(qe3,actualArr[ 2 ])
        Assert.assertEquals(qe4,actualArr[ 3 ])
    }
}