package com.mediamelon.smartstreaming;


public class DownloadRateAggregator {
    /** Default initial bitrate estimate in bits per second. */
    public static final long DEFAULT_INITIAL_BITRATE_ESTIMATE = 1_000_000;

    /** Default maximum weight for the sliding window. */
    public static final int DEFAULT_SLIDING_WINDOW_MAX_WEIGHT = 2000;

    private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
    private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 512 * 1024;

    private int slidingWindowMaxWeight;

    DownloadRateAggregator(){
        slidingWindowMaxWeight = DEFAULT_SLIDING_WINDOW_MAX_WEIGHT;
        this.slidingPercentile = new SlidingPercentile(slidingWindowMaxWeight);
        bitrateEstimate = DEFAULT_INITIAL_BITRATE_ESTIMATE;
    }

    /**
     * Sets the maximum weight for the sliding window.
     *
     * @param slidingWindowMaxWeight The maximum weight for the sliding window.
     * @return This builder.
     */
    public void setSlidingWindowMaxWeight(int slidingWindowMaxWeight) {
        this.slidingWindowMaxWeight = slidingWindowMaxWeight;
    }

    public synchronized long getBitrateEstimate() {
        return bitrateEstimate;
    }

    public synchronized void reportDownloadSample(long sampleBytesTransferred, int sampleElapsedTimeMs) {
        long nowMs = android.os.SystemClock.elapsedRealtime();
        totalElapsedTimeMs += sampleElapsedTimeMs;
        totalBytesTransferred += sampleBytesTransferred;
        if (sampleElapsedTimeMs > 0) {
            float bitsPerSecond = (sampleBytesTransferred * 8000) / sampleElapsedTimeMs;
            long downloadRate = (long) bitsPerSecond;
            MMSmartStreaming.getInstance().reportDownloadRate(downloadRate);
            slidingPercentile.addSample((int) Math.sqrt(sampleBytesTransferred), bitsPerSecond);
            if (totalElapsedTimeMs >= ELAPSED_MILLIS_FOR_ESTIMATE
                    || totalBytesTransferred >= BYTES_TRANSFERRED_FOR_ESTIMATE) {
                bitrateEstimate = (long) slidingPercentile.getPercentile(0.5f);
            }
        }
    }

    private final SlidingPercentile slidingPercentile;
    private long totalElapsedTimeMs;
    private long totalBytesTransferred;
    private long bitrateEstimate;
}

