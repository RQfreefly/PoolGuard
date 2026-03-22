package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S016_TryCatchNoFinally {
    public void execute(boolean fail) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            if (fail) {
                throw new RuntimeException("x");
            }
            executor.submit(() -> System.out.println("ok"));
            executor.shutdown();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
