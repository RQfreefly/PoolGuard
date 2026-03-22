package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class S010_MethodLocalThreadPoolExecutorNoClose {
    public void process() {
        ExecutorService executor = new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        executor.submit(() -> System.out.println("process"));
    }
}
