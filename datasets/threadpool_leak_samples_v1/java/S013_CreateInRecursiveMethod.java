package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S013_CreateInRecursiveMethod {
    public void walk(int depth) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> System.out.println("depth=" + depth));
        if (depth > 0) {
            walk(depth - 1);
        }
    }
}
