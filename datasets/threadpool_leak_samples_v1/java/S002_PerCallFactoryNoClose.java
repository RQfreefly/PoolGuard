package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S002_PerCallFactoryNoClose {
    public void sendMessage(String msg) {
        ExecutorService executor = createExecutor();
        executor.submit(() -> System.out.println(msg));
    }

    private ExecutorService createExecutor() {
        return Executors.newCachedThreadPool();
    }
}
