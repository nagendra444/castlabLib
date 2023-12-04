package com.mediamelon.qubit;

import com.mediamelon.smartstreaming.MMChunkInformation;

/**
 * Created by Rupesh on 30-03-2015.
 */
public class MMQFPresentationVideoTrackSegmentInfo {
    MMQFPresentationVideoTrackSegmentInfo(){

    }
    MMQFPresentationVideoTrackSegmentInfo(MMChunkInformation information){
        duration = information.duration;
        long segSz = 0;
        if (information.endByte != -1 && information.startByte != -1){
            segSz = information.endByte - information.startByte;
        }else if(duration!= 0 && information.bitrate >0){
            segSz = (long)((duration * 1.0)/1000 * information.bitrate);
        }
        segmentSz = (int) segSz;
        segmentStartTime = information.startTime;
    }
    public String segmentURL;
    public int segmentIndex = -1;
    //TODO: Add range from the main content for the segment
    public long duration; //in timescale as mentioned in track info
    public int segmentSz; //in bits
    public long segmentStartTime; //in timescale as mentioned in track info
}
