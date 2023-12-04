package com.mediamelon.qubit;

/**
 * Created by satija on 7/11/16.
 */

/**
 * MMQFQubitRangedResouce : Purpose of object of this class is to identify the resource to be qubitized, and the qubitized resource in the method getQubitizedResource
 */

public class MMQFQubitRangedResouce {
    public MMQFQubitRangedResouce(String uri, long start, long length, int trackIdx, int seqIdx) {
        this.uri = uri;
        this.start = start;
        this.length = length;
        this.trackIdx = trackIdx;
        this.seqIdx = seqIdx;
    }

    public final long start;
    /**
     * The length of the range, or -1 to indicate that the range is unbounded.
     */
    public final long length;

    public final String uri;

    public final int trackIdx;

    public final int seqIdx; //hint for the input seq num
}
