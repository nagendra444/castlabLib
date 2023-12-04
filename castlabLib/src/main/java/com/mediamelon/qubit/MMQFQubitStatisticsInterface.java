package com.mediamelon.qubit;
public interface MMQFQubitStatisticsInterface {
    
    public class MMQFSegmentInfo{
        public int trackBitrate = -1;
        public long segSize = -1; //In Bits
        public int profileIdx = -1; //Index in hintfile
        public double mosScore = -1;
        public long duration = -1;//in millisec
        public double frameRate = -1;
        public int representationWidth = -1;
        public int representationHeight = -1;
        public String codecInformation = "";
        public double segmentStartTime = -1.0;//In Milliseconds
        public int segmentIndex = -1;
    }
    
    public class MMQFSegmentSizeInfo{
        MMQFSegmentSizeInfo()
        {
            segmentStartTime = segmentDuration = -1L;
            requestedSegmentSz = qubitizedSegmentSz = -1;
            cbrBitrate = qbrBitrate = -1L;
        }

        public long segmentStartTime;
        public long segmentDuration;
        public long timescale;
        public int requestedSegmentSz;
        public int qubitizedSegmentSz;

        public long cbrBitrate;
        public long qbrBitrate;

        public int getSizeReduction()
        {
            return requestedSegmentSz - qubitizedSegmentSz;
        }
    }

    public class MMQFSegmentQualityInfo{
        public double requestedSegmentQuality;
        public double qubitizedSegmentQuality;
        public int profileId;
        public int height;
        public int width;
        public int qubitizedSegmentOffsetFromAverageQuality()
        {
            return 0;
        }
    }

    public class MMQFABRSwitchInfo{
        public int sourceProfileId;
        public int destProfileID;
        public int timeOfSwitch;
    }

    public class ResolutionObject {
        public Integer minHeight;
        public Integer maxHeight;
        public Integer minWidth;
        public Integer maxWidth;
        ResolutionObject(){
            minHeight = Integer.MAX_VALUE;
            maxHeight = Integer.MIN_VALUE;
            minWidth = Integer.MAX_VALUE;
            maxWidth = Integer.MIN_VALUE;
        }
    }


    public abstract long getCBRTotalBitsTransferred();
    public abstract long getCQTotalBitsTransferred();
    public abstract int getPercentageBitSavings();
    public abstract int getiMOSImprovementOccurences();
    public abstract double getiMOSImprovementPercentage();

    public abstract int getMinImosImprovementInPerc();
    public abstract int getMaxiMosImprovementInPerc();
    public abstract double getMaxImprovediMOS();

    public abstract double getMaxImprovediMOSOriginaliMOS();
    public abstract int getQubitPlaybackDuration();
    public abstract double getiMOSVarianceCQ();
    public abstract double getiMOSStdDeviationCQ();
    public abstract double getiMOSVarianceCBR();
    public abstract double getiMOSStdDeviationCBR();

    public abstract String getAudioCodecInfo();
    public abstract String getVideoCodecInfo();
    public abstract Long getSDKBootTime();

    public abstract ResolutionObject getStreamResolution();
    public abstract Boolean isLiveStreaming();
    public abstract int getNumberOfProfile();
    public abstract int getTotalDuration();
    public abstract Double getFrameRate();
    public abstract MMQFSegmentQualityInfo getSegmentQualityInfo(String inUrl, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo);
    public abstract MMQFSegmentSizeInfo getSegmentSizeInfo(String inUrl, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo);
    public abstract MMQFSegmentSizeInfo getAverageSegmentSizeInfo(String inUrl, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo);

    public abstract MMQFSegmentInfo getSegmentInfoForSegment(int trackIdx, int sequenceNum);        
    public abstract MMQFSegmentInfo getQBRSegmentInfoForSegment(int trackIndex, int sequenceIndex);        
    public abstract Integer getTrackIndex(Integer bitrate);        
    public abstract Integer getSequenceIndex(int bitrate,long startTime);        

    public abstract void resetStatistics();
    //public abstract int getABRSwitchCount();
    //public abstract Vector<MMQFABRSwitchInfo> getABRSwitchInfo();
    public abstract long getPotentialStorageSavings();
}
