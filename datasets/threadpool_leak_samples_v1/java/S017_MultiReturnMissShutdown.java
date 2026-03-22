package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S017_MultiReturnMissShutdown {
    public int handle(int code) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        if (code < 0) {
            return -1;
        }
        if (code == 0) {
            return 0;
        }
        executor.submit(() -> System.out.println("run"));
        executor.shutdown();
        return 1;
    }
}
