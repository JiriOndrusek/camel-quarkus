package org.apache.camel.quarkus.component.solr.it;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class SolrRouteBuilder {

    @ConfigProperty(name = "solr.cloud.url", defaultValue = "localhost")
    String solrUrl;

    //    @Override
    //    public void configure() throws Exception {
    //        from("direct:start").to(solrUrl);
    //    }
}
