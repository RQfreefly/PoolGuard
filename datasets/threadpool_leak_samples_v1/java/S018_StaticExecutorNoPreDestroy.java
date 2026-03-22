package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S018_StaticExecutorNoPreDestroy {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    public void submit() {
        EXECUTOR.submit(() -> System.out.println("static"));
    }
}
