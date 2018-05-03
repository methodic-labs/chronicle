package com.openlattice.chronicle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.openlattice.chronicle.sources.Datasource;

import java.io.IOException;

public class TypeRefs {
    public static TypeReference<Optional<Datasource>> optDS() {
        return new TypeReference<Optional<Datasource>>() {
        };
    }

    public static <T> T map(ObjectMapper mapper, String json, TypeReference<T> typeReference ) throws IOException {
        return  mapper.readValue(json, typeReference );
    }
}
