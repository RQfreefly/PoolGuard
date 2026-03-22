package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class S020_UnboundedQueueThreadPool {
    public void submit() {
        ExecutorService executor = new ThreadPoolExecutor(
                4,
                8,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        executor.submit(() -> System.out.println("queue"));
        executor.shutdown();
    }
}
