package test;

import java.util.concurrent.ExecutorService;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.debezium.engine.DebeziumRecorder;
import io.quarkus.runtime.ShutdownContext;

@TargetClass(DebeziumRecorder.class)
final class DebeziumRecorderSubst {

    @Substitute
    public void startEngine(ExecutorService executorService, ShutdownContext context, BeanContainer container) {
        //do nothing
    }
}
