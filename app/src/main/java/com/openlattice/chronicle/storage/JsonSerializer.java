package com.openlattice.chronicle.storage;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import java.io.IOException;
import java.util.UUID;

public class JsonSerializer {
    public static final ObjectMapper mapper = new ObjectMapper();

    static {
//        mapper.registerModule( new GuavaModule() );
//        mapper.registerModule( new JodaModule() );
//        mapper.registerModule( new AfterburnerModule() );
    }

    public static final byte[] serializeQueueEntry(QueueEntry queueEntry) {
        try {
            return mapper.writeValueAsBytes(queueEntry);
        } catch (JsonProcessingException e) {
            Log.e(JsonSerializer.class.getCanonicalName(), "Unable to serialize queue entry. " + queueEntry.toString());
            return new byte[0];
        }
    }

    public static SetMultimap<UUID, Object> deserializeQueueEntry(byte[] bytes) {
        try {
            return mapper.readValue(bytes, new TypeReference<SetMultimap<UUID, Object>>() {
            });
        } catch (IOException e) {
            Log.e(JsonSerializer.class.getCanonicalName(), "Unable to deserialize queue entry " + new String(bytes));
            return ImmutableSetMultimap.of();
        }
    }
}
