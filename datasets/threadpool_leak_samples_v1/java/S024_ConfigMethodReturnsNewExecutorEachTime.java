package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S024_ConfigMethodReturnsNewExecutorEachTime {
    public ExecutorService appExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
