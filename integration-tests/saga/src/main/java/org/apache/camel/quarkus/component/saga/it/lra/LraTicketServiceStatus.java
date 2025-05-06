package org.apache.camel.quarkus.component.saga.it.lra;

public enum LraTicketServiceStatus {
    nothing, //credit is enough
    refunded, //the other ticket failed, therefore current ticket was refunded
    error, //purchase of ticket failed due to insufficient credit
    reserved;
}
