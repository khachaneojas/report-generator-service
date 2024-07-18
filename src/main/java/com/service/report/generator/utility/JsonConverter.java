package com.service.report.generator.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.report.generator.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonConverter {

    private final ObjectMapper objectMapper;
    private static final String ERROR_GENERIC_MESSAGE = "Oops! Something went wrong.";


    public <K, V> Map<K, V> convertToMap(String json, Class<K> keyClass, Class<V> valueClass) throws IOException {
        TypeReference<Map<K, V>> typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            }
        };

        return objectMapper.readValue(json, typeReference);
    }

    public <K, V> String convertMapToJsonString(Map<K, V> map) throws JsonProcessingException {
        return objectMapper.writeValueAsString(map);
    }

    public <K, V> Map<K, V> getMapFromJsonString(String jsonString, Class<K> keyClass, Class<V> valueClass) {
        Map<K, V> map;
        try {
            map = convertToMap(jsonString, keyClass, valueClass);
        } catch (IOException e) {
            throw new InvalidDataException(ERROR_GENERIC_MESSAGE);
        }

        if (null == map) return Collections.emptyMap();
        else return map;
    }

}
