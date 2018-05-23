package com.openlattice.chronicle.serialization;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.openlattice.chronicle.util.RetrofitBuilders;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JsonSerializer {
    public static final ObjectMapper mapper = RetrofitBuilders.getMapper();

    public static final byte[] serializeQueueEntry(List<SetMultimap<? extends UUID, ? extends Object>> queueData) {
        try {
            return mapper.writeValueAsBytes(queueData);
        } catch (JsonProcessingException e) {
            Log.e(JsonSerializer.class.getName(), "Unable to serialize queue entry. " + queueData.toString());
            return new byte[0];
        }
    }

    public static List<SetMultimap<UUID, Object>> deserializeQueueEntry(byte[] bytes) {
        try {
            return mapper.readValue(bytes, new TypeReference<List<SetMultimap<UUID, Object>>>() {
            });
        } catch (IOException e) {
            Log.e(JsonSerializer.class.getName(), "Unable to deserialize queue entry " + new String(bytes));
            return ImmutableList.of();
        }
    }

    public static String serializePropertyTypeIds(Map<String, UUID> propertyTypeIds) {
        try {
            return mapper.writeValueAsString(propertyTypeIds);
        } catch (JsonProcessingException e) {
            Log.e(JsonSerializer.class.getName(), "Unable to serialize property type ids.");
            return "";
        }
    }

    public static Map<String, UUID> deserializePropertyTypeIds(String json) {
        try {
            return mapper.readValue(json, new TypeReference<Map<String, UUID>>() {
            });
        } catch (IOException e) {
            Log.e(JsonSerializer.class.getName(), "Unable tode serialize property type ids.");
            return ImmutableMap.of();
        }
    }
}
