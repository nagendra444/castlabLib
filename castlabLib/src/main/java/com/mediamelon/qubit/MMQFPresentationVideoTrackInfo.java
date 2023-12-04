package com.mediamelon.qubit;
import com.mediamelon.qubit.MMQFPresentationVideoTrackSegmentInfo;
import com.mediamelon.smartstreaming.MMRepresentation;

import java.util.Vector;

abstract interface TemplatizedURLGeneratorInterface{
    public abstract String generateURL(int bitrate, long startTime);
    public abstract String getBaseURL(String url);
}

public class MMQFPresentationVideoTrackInfo implements Comparable<MMQFPresentationVideoTrackInfo>{
    public MMQFPresentationVideoTrackInfo(MMRepresentation representation) {
        startSequenceNum = 0;
        width = representation.getWidth();
        height = representation.getHeight();
        bitrate = representation.getBitrate();
        codecInfo = representation.codecId();
        timeScale = 1000;
        trackIndex = representation.getTrackId();
        hasClientSideAdInsertionInTrack = false;
        trackSegmentInfoVect = new Vector();
        for (int j = 0; j < representation.getChunkCount(); j++) {
            MMQFPresentationVideoTrackSegmentInfo segInfo = new MMQFPresentationVideoTrackSegmentInfo(representation.getChunkAtIdx(j));
            segInfo.segmentIndex = j;
            trackSegmentInfoVect.add(segInfo);
        }
        fps = -1.0;
    }

    public MMQFPresentationVideoTrackInfo() {
        startSequenceNum = 0;
        width = height = bitrate = -1;
        codecInfo = null;
        baseURL = null;
        generatorIntf = null;
        trackSegmentInfoVect = new Vector();
        timeScale = -1;
        fps = -1.0;
    }

    MMQFPresentationVideoTrackInfo(TemplatizedURLGeneratorInterface aGeneratorIntf) {
        startSequenceNum = 0;
        width = height = bitrate = -1;
        codecInfo = null;
        baseURL = null;
        generatorIntf = aGeneratorIntf;
        trackSegmentInfoVect = new Vector();
        timeScale = -1;
        fps = -1.0;
    }

    MMQFPresentationVideoTrackSegmentInfo getSegmentInfo(int index) {
        MMQFPresentationVideoTrackSegmentInfo retval = null;
        if(index < trackSegmentInfoVect.size() && (trackSegmentInfoVect.size() > 0))
        {
            retval = (MMQFPresentationVideoTrackSegmentInfo)(trackSegmentInfoVect.elementAt(index));
        }
        return retval;
    }

    public int startSequenceNum(){
        return startSequenceNum;
    }

    public int compareTo(MMQFPresentationVideoTrackInfo track) {

        int compareQuantity = track.bitrate;

        //ascending order
        return this.bitrate - compareQuantity;
    }

    MMQFPresentationVideoTrackSegmentInfo getSegmentInfoAtTime(long startTime) {
        if(trackSegmentInfoVect.size() > 0)
        {
            MMQFPresentationVideoTrackSegmentInfo seginfo = (MMQFPresentationVideoTrackSegmentInfo)trackSegmentInfoVect.elementAt(trackSegmentInfoVect.size() -1);
            if(seginfo.segmentStartTime >= startTime) {
                int lastSegmentIdx = trackSegmentInfoVect.size() - 1;
                int beginSegmentIdx = 0;
                int i = beginSegmentIdx;
                boolean found = false;
                while(!found && beginSegmentIdx <= lastSegmentIdx) {
                    int midSegmentIdx = (beginSegmentIdx + lastSegmentIdx)/2;
                    seginfo = (MMQFPresentationVideoTrackSegmentInfo)trackSegmentInfoVect.elementAt(midSegmentIdx);
                    if(seginfo.segmentStartTime == startTime)
                    {
                        found = true;
                    }
                    else
                    {
                        if(seginfo.segmentStartTime > startTime)
                        {
                            lastSegmentIdx = midSegmentIdx - 1;
                        }
                        else
                        {
                            beginSegmentIdx = midSegmentIdx + 1;
                        }
                    }
                }
                if(found) {
                    return seginfo;
                }
            }
        }
        return null;
    }

    String getSegmentURL(int index) {
        String retval = null;
        MMQFPresentationVideoTrackSegmentInfo segInfo = getSegmentInfo(index);
        if(segInfo!= null)
        {
            if(segInfo.segmentURL == null && generatorIntf != null){
                return generatorIntf.generateURL(bitrate, segInfo.segmentStartTime);
            }
            else
            {
                return segInfo.segmentURL;
            }
        }
        return null;
    }

    String getBaseURL(String inURL) {
        String retval = baseURL;
        if(generatorIntf != null)
        {
            retval = generatorIntf.getBaseURL(inURL);
        }
        return retval;
    }

    public boolean hasClientSideAdInsertionInTrack;
    int getSegmentCount(){
        if (trackSegmentInfoVect != null){
            return trackSegmentInfoVect.size();
        }
        return -1;
    }

    public int width;
    public int height;
    public int bitrate;
    public String codecInfo;
    public String baseURL;
    TemplatizedURLGeneratorInterface generatorIntf;
    public int startSequenceNum;
    Vector trackSegmentInfoVect;
    public long timeScale;
    public int trackIndex;
    public double fps;
}

