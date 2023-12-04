package com.mediamelon.smartstreaming;

/**
 * There may be times when the player needs to override metric values that are computed by the SDK.
 * This object lists the metrics that can be overridden.
 */
public enum MMOverridableMetric {
    /**
     * Time between when user requests the start of the playback session and playback starts.
     */
    Latency,

    /**
     * IP address of manifest server
     */
    ServerAddress,

    /**
     * Duration of time that the player was in the PLAYING state, excluding advertisement play time.
     */
    DurationWatched
}
