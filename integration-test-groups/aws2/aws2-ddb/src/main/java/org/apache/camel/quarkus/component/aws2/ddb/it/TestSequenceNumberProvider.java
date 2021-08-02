package org.apache.camel.quarkus.component.aws2.ddb.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.camel.component.aws2.ddbstream.SequenceNumberProvider;

@ApplicationScoped
@Named("aws2DdbStreamSequenceNumberProvider")
public class TestSequenceNumberProvider implements SequenceNumberProvider {

    private String lastSn = "0";

    @Override
    public String getSequenceNumber() {
        return lastSn;
    }
    //
    //    public int getLastSequenceNumber() {
    //        return lastSn.addAndGet(1);
    //    }

    public void setLastSequenceNumber(String newSn) {
        lastSn = newSn;
    }
}
