package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S005_FieldExecutorNoShutdownHook {
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public void runAsync() {
        executor.submit(() -> System.out.println("field pool"));
    }
}
