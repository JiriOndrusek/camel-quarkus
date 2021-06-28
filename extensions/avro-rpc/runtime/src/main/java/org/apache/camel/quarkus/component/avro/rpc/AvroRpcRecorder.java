package org.apache.camel.quarkus.component.avro.rpc;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.avro.AvroComponent;

@Recorder
public class AvroRpcRecorder {

    public RuntimeValue<AvroComponent> configureAvroComponent() {
        return new RuntimeValue<>(new QuarkusAvroComponent());
    }

}
