package dev.esoterik.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.esoterik.rift.codec.jackson.JacksonSerializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ExamplePojo implements JacksonSerializable {

    private UUID uniqueId;
    private Map<String, String> data;

    @JsonCreator
    private ExamplePojo() {
        // Default constructor for Jackson
    }

    public ExamplePojo(UUID uniqueId, Map<String, String> data) {
        this.uniqueId = uniqueId;
        this.data = data;
    }

    public ExamplePojo(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.data = new HashMap<>();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Map<String, String> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ExamplePojo that = (ExamplePojo) o;
        return Objects.equals(uniqueId, that.uniqueId) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, data);
    }

    @Override
    public String toString() {
        return "ExamplePojo{" + "uniqueId=" + uniqueId + ", data=" + data + '}';
    }
}
