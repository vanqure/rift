package dev.esoterik.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.esoterik.rift.codec.jackson.AbstractJacksonPacket;

public class ExamplePacket extends AbstractJacksonPacket {

    private String exampleField;

    @JsonCreator
    private ExamplePacket() {}

    public ExamplePacket(String exampleField) {
        this.exampleField = exampleField;
    }

    public String getExampleField() {
        return exampleField;
    }

    @Override
    public String toString() {
        return "ExamplePacket{" + "exampleField='" + exampleField + '\'' + '}';
    }
}
