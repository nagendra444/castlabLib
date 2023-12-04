package com.mediamelon.qubit;

import java.util.Collections;
import java.util.Vector;

public class MMQFPresentationInfo {
    MMQFPresentationInfo(String format, int aLookAheadSegmentCnt, int aDvrWndLength, Boolean aIsLive, long aDuration, int aTimescale)
    {
        videoPresentationTracksSets = new Vector();
        lookAheadSegmentCnt = aLookAheadSegmentCnt;
        dvrWndLength = aDvrWndLength;
        isLive = aIsLive;
        duration = aDuration;
        timescale = aTimescale;
        maxResHeight = -1;
        maxResWidth = -1;
        minResHeight = -1;
        minResWidth = -1;
        maxRes = -1;
        minRes = -1;
        maxFPS = -1.0;
        minFPS = -1.0;
        streamingFormat = format;
    }
    public Boolean isLivePresentation()
    {
        return isLive;
    }

    public int getDuration()
    {
        double factor = (double)1000/timescale;
        int durationInMilli = (int)(duration * factor);
        return durationInMilli;
    }//in millisec

    public int getDvrWndLength() {return dvrWndLength;}

    public int getLookAheadSegmentCnt() {return lookAheadSegmentCnt;}

    public Boolean canQubitBeApplied()
    {
        if (videoPresentationTracksSets.size()>1) {
            MMQFPresentationVideoTrackInfo trackInfo = (MMQFPresentationVideoTrackInfo) videoPresentationTracksSets.elementAt(0);
            if(trackInfo != null) {
                int segCount = trackInfo.getSegmentCount();
                if (segCount > 0){
                    return true;
                }
            }
        }
        return false;
    }

    public void addVideoPresentationTrack(int width, int height, String codecInfo, MMQFPresentationVideoTrackInfo trackInfo)
    {
        videoPresentationTracksSets.add(trackInfo);
        if(trackInfo.hasClientSideAdInsertionInTrack){
            hasClientSideAdInsertion = true;
        }
        if(width >0 && height > 0){
            if(width*height > maxRes) {
                maxResHeight = height;
                maxResWidth = width;
                maxRes = maxResHeight * maxResWidth;
            }

            if((minRes<0) || (width*height < minRes)) {
                minResHeight = height;
                minResWidth = width;
                minRes = minResHeight * minResWidth;
            }
        }

        if (trackInfo.fps > 0){
            if(trackInfo.fps > maxFPS){
                maxFPS = trackInfo.fps;
            }
            if(trackInfo.fps < minFPS || (minFPS < 0)){
                minFPS = trackInfo.fps;
            }
        }
    }

    public int getVideoTracksCount()
    {
        return videoPresentationTracksSets.size();
    }

    public MMQFPresentationVideoTrackInfo getVideoTrack(int index)
    {
        if(index < videoPresentationTracksSets.size()) {
            MMQFPresentationVideoTrackInfo videoTrackInfo = (MMQFPresentationVideoTrackInfo) videoPresentationTracksSets.elementAt(index);
            return videoTrackInfo;
        }
        return null;
    }

    public void AlignTrackInfoAsPerMetaFileExpectations(){
        Collections.sort(videoPresentationTracksSets);
        for (int i = 0; i< videoPresentationTracksSets.size(); i++){
            MMQFPresentationVideoTrackInfo info = videoPresentationTracksSets.get(i);
            info.trackIndex = i;
            videoPresentationTracksSets.set(i, info);
        }
    }

    public String getMaxRes(){
        return "" + maxResWidth + "x" + maxResHeight;
    }

    public String getMinRes(){
        return "" + minResWidth + "x" + minResHeight;
    }

    public Double getMaxFPS(){
        return maxFPS;
    }

    public Double getMinFPS(){
        return minFPS;
    }

    public String getStreamingFormat(){
        return streamingFormat;
    }

    boolean hasClientSideAdInsertion(){
        return hasClientSideAdInsertion;
    }

    public boolean hasClientSideAdInsertion = false;

    private String streamingFormat;
    private int maxResHeight;
    private int maxResWidth;
    private int minResHeight;
    private int minResWidth;
    private int maxRes;
    private int minRes;

    private double maxFPS;
    private double minFPS;

    private Vector<MMQFPresentationVideoTrackInfo> videoPresentationTracksSets;
    private int lookAheadSegmentCnt;
    private int dvrWndLength;
    private Boolean isLive;
    private long duration;
    private int timescale;
}
