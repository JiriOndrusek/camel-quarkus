package org.apache.camel.quarkus.component.kamelet.deployment;

import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.PluginHelper;

class DefaultKameletResolver implements KameletResolver {

    static String idToLocation(String id) {
        return "/kamelets/" + id + ".kamelet.yaml";
    }

    @Override
    public Optional<Resource> resolve(String id, CamelContext context) throws Exception {
        return Optional.ofNullable(
                PluginHelper.getResourceLoader(context).resolveResource(idToLocation(id)));
    }
}
