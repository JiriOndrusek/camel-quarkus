package org.apache.camel.quarkus.component.avro.rpc;

import java.util.HashMap;
import java.util.Map;

import org.apache.avro.ipc.Responder;
import org.apache.camel.component.avro.AvroComponent;
import org.apache.camel.component.avro.AvroConsumer;
import org.apache.camel.spi.annotations.Component;

@Component("avro")
public class QuarkusAvroComponent extends AvroComponent {

    private Responder avroSpecificResponder;
    private Responder avroReflectiveResponder;

    private Map<String, String> listenerMapping = new HashMap<>();

    @Override
    public void register(String uri, String messageName, AvroConsumer consumer) throws Exception {
        String uriWithType = uri + "/" + consumer.getEndpoint().getConfiguration().isReflectionProtocol();
        super.register(uriWithType, messageName, consumer);
        listenerMapping.put(uri + "/" + messageName, uriWithType);
    }

    @Override
    public void unregister(String uri, String messageName) {
        super.unregister(listenerMapping.get(uri + "/" + messageName), messageName);
        listenerMapping.remove(uri + "/" + messageName);
    }

    public Responder getAvroSpecificResponder() {
        return avroSpecificResponder;
    }

    public void setAvroSpecificResponder(Responder avroSpecificResponder) {
        this.avroSpecificResponder = avroSpecificResponder;
    }

    public Responder getAvroReflectiveResponder() {
        return avroReflectiveResponder;
    }

    public void setAvroReflectiveResponder(Responder avroReflectiveResponder) {
        this.avroReflectiveResponder = avroReflectiveResponder;
    }
}
