package org.apache.camel.quarkus.component.jt400.it;

import com.ibm.as400.access.AS400ConnectionPool;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;

public class Jt400Produce {

    @Produces
    @Singleton
    @Named("jt400ConnectionPool")
    public AS400ConnectionPool produceMinioClient() {
        AS400ConnectionPool retVal = new AS400ConnectionPool();
        //to avoid CPF2451 Message queue REPLYMSGQ is allocated to another job
        retVal.setCleanupInterval(0);
        //        retVal.setThreadUsed(false);
        //        retVal.setRunMaintenance(true);

        //        retVal.addConnectionPoolListener(new ConnectionPoolListener() {
        //            @Override
        //            public void connectionCreated(ConnectionPoolEvent connectionPoolEvent) {
        //                System.out.println("created");
        //            }
        //
        //            @Override
        //            public void connectionExpired(ConnectionPoolEvent connectionPoolEvent) {
        //                System.out.println("expired");
        //            }
        //
        //            @Override
        //            public void connectionPoolClosed(ConnectionPoolEvent connectionPoolEvent) {
        //                System.out.println("closed");
        //            }
        //
        //            @Override
        //            public void connectionReleased(ConnectionPoolEvent connectionPoolEvent) {
        //                ((AS400) connectionPoolEvent.getSource()).disconnectAllServices();
        //                System.out.println("release");
        //            }
        //
        //            @Override
        //            public void connectionReturned(ConnectionPoolEvent connectionPoolEvent) {
        //                ((AS400) connectionPoolEvent.getSource()).disconnectAllServices();
        //                System.out.println("returned");
        //            }
        //
        //            @Override
        //            public void maintenanceThreadRun(ConnectionPoolEvent connectionPoolEvent) {
        //                System.out.println("maintenance");
        //            }
        //        });
        return retVal;
    }

}
