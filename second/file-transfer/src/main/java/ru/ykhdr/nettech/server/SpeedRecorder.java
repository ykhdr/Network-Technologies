package ru.ykhdr.nettech.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class SpeedRecorder implements Runnable {
    private final long dataSize;
    private final Supplier<Long> getCurBytesRead;
    private final Supplier<Boolean> isReceivingStopped;
    private final String clientName;

    @Override
    public void run() {
        long bytesReadBefore = 0;
        long bytesReadCurrent = 0;
        try {
            Thread.sleep(3000);

            while (dataSize != bytesReadCurrent && !isReceivingStopped.get()) {
                bytesReadCurrent = getCurBytesRead.get();
                long dif = bytesReadCurrent - bytesReadBefore;

                log.info("Data retrieval speed of file " + clientName + " is\t\t" + countMbPerSecond(dif));

                bytesReadBefore = bytesReadCurrent;
                Thread.sleep(3000);
            }
        } catch (InterruptedException e) {
            log.error("Speed Recorder thread sleep error", e);
        }
    }

    private String countMbPerSecond(long bytesPerThreeSeconds) {
        return String.format("%.1f Mb/s", (double) bytesPerThreeSeconds / 3 / 1024 / 1024);
    }

    public String getAverageSpeed(long seconds) {
        return String.format("%.1f Mb/s", (double) dataSize / seconds / 1024 / 1024);
    }
}
