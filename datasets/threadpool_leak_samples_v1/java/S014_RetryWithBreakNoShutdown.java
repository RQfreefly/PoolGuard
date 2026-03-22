package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S014_RetryWithBreakNoShutdown {
    public void executeWithRetry() {
        for (int attempt = 0; attempt < 3; attempt++) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            if (callRemote()) {
                break;
            }
        }
    }

    private boolean callRemote() {
        return false;
    }
}
