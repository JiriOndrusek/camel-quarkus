package org.apache.camel.quarkus.component.zendesk.runtime.graal;

import java.util.concurrent.ThreadFactory;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import org.asynchttpclient.netty.channel.IoUringIncubatorTransportFactory;

final class ZendeskSubstitutions {
}
//
//@TargetClass(DefaultAsyncHttpClientConfig.class)
//final class DefaultAsyncHttpClientConfigSubstitutions {
//
//    @Substitute
//    public boolean isUseNativeTransport() {
//        return false;
//    }
//}

@TargetClass(IoUringIncubatorTransportFactory.class)
final class DIoUringIncubatorTransportFactorySubstitutions {

    @Substitute
    public IOUringSocketChannel newChannel() {
        throw new RuntimeException("not supported!");
    }

    @Substitute
    public IOUringEventLoopGroup newEventLoopGroup(int ioThreadsCount, ThreadFactory threadFactory) {
        throw new RuntimeException("not supported!");
    }
}
