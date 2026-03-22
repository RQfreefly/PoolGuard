package sample.dataset;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class S004_CreateInRetryLoop {
    public void retryTask() {
        int retry = 0;
        while (retry < 3) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            boolean ok = doCall();
            if (ok) {
                break;
            }
            retry++;
        }
    }

    private boolean doCall() {
        return false;
    }
}
