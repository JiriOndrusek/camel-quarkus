package org.apache.camel.quarkus.component.google.storage;

import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.retrying.DirectRetryingExecutor;
import com.google.api.gax.retrying.RetryingFuture;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = DirectRetryingExecutor.class)
public final class DirectRetryingExecutorSubstitution {

    @Substitute
    public ApiFuture submit(RetryingFuture retryingFuture) {
        System.out.println("5555555555555 submit 555555555555555555555");
        int i = 0;
        while (!retryingFuture.isDone()) {
            System.out.println("555555555555555: " + i++);
            try {
                Thread.sleep(retryingFuture.getAttemptSettings().getRandomizedRetryDelay().toMillis());
                Object response = retryingFuture.getCallable().call();
                System.out.println("555555555 - Response: " + response);
                retryingFuture.setAttemptFuture(ApiFutures.immediateFuture(response));
            } catch (InterruptedIOException | ClosedByInterruptException | InterruptedException var3) {
                System.out.println("555555555 - Exception1: " + var3);
                var3.printStackTrace();
                Thread.currentThread().interrupt();
                retryingFuture.setAttemptFuture(ApiFutures.immediateFailedFuture(var3));
            } catch (Exception var4) {
                System.out.println("555555555 - Exception2: " + var4);
                var4.printStackTrace();
                retryingFuture.setAttemptFuture(ApiFutures.immediateFailedFuture(var4));
            }
        }
        try {
            System.out.println("555555555555 done: " + retryingFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return retryingFuture;
    }
}
