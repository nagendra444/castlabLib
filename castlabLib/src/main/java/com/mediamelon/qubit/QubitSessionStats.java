package com.mediamelon.qubit;

import android.util.Log;

import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QubitSessionStats {
    public static QubitSessionStats instance(MMQFQubitStatisticsInterface statIntf){statInterface = statIntf;  return inst; }
    private static QubitSessionStats inst = new QubitSessionStats();
    private void setStatsWindowSz(int wndSz) {
        if(wndSz > 0) {
            statsWindowSz = wndSz;
        }
    }

    public int getWndSize()
    {
        return statsWindowSz;
    }

    public void purgeStaleEntries(int time)
    {
		final int segmentStatsSize = segmentStats.size();
        if(segmentStats != null && segmentStatsSize >=2) {
            for (int i = 0; i < segmentStatsSize - 1; i++) {
                QubitStats stats = segmentStats.elementAt(0);
                QubitStats nextStats = segmentStats.elementAt(1);
                int segStartTime = (int)((float)stats.segmentSizeInfo.segmentStartTime/stats.segmentSizeInfo.timescale);
                int nextSegmentTime = (int)((float)nextStats.segmentSizeInfo.segmentStartTime/nextStats.segmentSizeInfo.timescale);
                if ((segStartTime <time  && nextSegmentTime<time) && segmentStats.size() >= statsWindowSz)
                {
                    segmentStats.remove(0);
                }
                else {
                    break;
                }
            }
        }
    }

    public void saveStatsForUrl(String url, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo)
    {
        QubitStats stats = new QubitStats();
        if(stats != null) {
            lock.lock();
            stats.segmentSizeInfo = statInterface.getAverageSegmentSizeInfo(url, segmentInfo);
            stats.segmentQualityInfo = statInterface.getSegmentQualityInfo(url, segmentInfo);
            stats.segmentURL = url;
            stats.avlBandwidth = currentDownloadRate;
            stats.switchInfo = switchInfo;
            if (stats.segmentSizeInfo == null) {
                lock.unlock();
                return;
            }

            Log.e("XResolution", "Segment start time -- " + stats.segmentSizeInfo.segmentStartTime);
            switchInfo = null;
            //Segment stats need to maintain the unique entries, duplicate entry should overwrite the previous entry
            //Entries in the array should be sorted w.r.t. start time of the segment
            int segmentsCount = segmentStats.size();
            if(segmentsCount > 0){
                int begin = 0;
                int end = segmentsCount - 1;
                int mid = (begin + end) /2;
                while(begin<=end){
                    QubitStats avlStats = segmentStats.elementAt(mid);
                    if(avlStats.segmentSizeInfo.segmentStartTime < stats.segmentSizeInfo.segmentStartTime){
                        begin = mid + 1;
                    }
                    else if(avlStats.segmentSizeInfo.segmentStartTime == stats.segmentSizeInfo.segmentStartTime){
                        segmentStats.set(mid, stats);
                        MMLogger.e("QubitSessionStats.Graph", "Updating the element at starttime" + avlStats.segmentSizeInfo.segmentStartTime);
                        lock.unlock();
                        return;
                    }
                    else{
                        end = mid - 1;
                    }
                    mid = (begin + end)/2;
                }
            }

            if (stats != null && stats.segmentSizeInfo != null) {
                MMLogger.e("QubitSessionStats.Graph", " StartTime " + (int) ((float) stats.segmentSizeInfo.segmentStartTime / stats.segmentSizeInfo.timescale) + " BitsDiff " + (stats.segmentSizeInfo.requestedSegmentSz - stats.segmentSizeInfo.qubitizedSegmentSz) + " iMOS Info " + stats.segmentQualityInfo.requestedSegmentQuality + " -> " + stats.segmentQualityInfo.qubitizedSegmentQuality);
            }
            segmentStats.add(stats);
            lock.unlock();
        }
    }

    public int getMinBitrate()
    {
        return minBitrate;
    }

    public int getMaxBitrate()
    {
        return maxBitrate;
    }

    public int[] getBitrates()
    {
        return bitrateArray;
    }

    public void registerPresentationBitrates(MMQFPresentationInfo presentationInfo) {
        acquireLockedStats();
        maxBitrate = minBitrate = -1;
        segmentStats.clear();
        releaseLockedStats();
        if (presentationInfo != null) {
            int trackCount = presentationInfo.getVideoTracksCount();
            bitrateArray = new int[trackCount];
            for(int i =0; i< trackCount; i++)
            {
                MMQFPresentationVideoTrackInfo trackInfo = presentationInfo.getVideoTrack(i);
                bitrateArray[i] = trackInfo.bitrate;
            }
            for (int i = 0; i < bitrateArray.length; i++) {
                if (minBitrate > bitrateArray[i]) {
                    minBitrate = bitrateArray[i];
                }
                if (maxBitrate < bitrateArray[i]) {
                    maxBitrate = bitrateArray[i];
                }
            }
        }
    }

    public void acquireLockedStats(){
        lock.lock();
    }

    public void releaseLockedStats(){
        lock.unlock();
    }

    public Vector<QubitStats> getRegisteredPresentationBitrates()
    {
        return segmentStats;
    }

    public void discardStats()
    {
        acquireLockedStats();
        segmentStats.clear();
        currentDownloadRate = -1;
        releaseLockedStats();
    }

    public void onABRSwitchDecisionCompleted(long downloadRate, long currentBitrate, long suggestedBitrate, long qbrTargetBitrate){
        MMLogger.e("QubitStats", "onABRSwitchDecisionCompleted from " + currentBitrate + " to " + qbrTargetBitrate + " suggested was " + suggestedBitrate);
        switchInfo = new QubitABRSwitchInfo();
        switchInfo.fromBitrate = currentBitrate;
        switchInfo.toBitrate = qbrTargetBitrate;
        switchInfo.suggestedBitrate = suggestedBitrate;
        acquireLockedStats();
        currentDownloadRate = downloadRate;
        releaseLockedStats();
    }

    public void updateDownloadRate(long downloadRate){
        currentDownloadRate = downloadRate;
    }

    public class QubitABRSwitchInfo{
        public boolean isSwitchHappened(){
          return (toBitrate != fromBitrate)?true:false;
        }
        public long  fromBitrate;
        public long toBitrate;
        public long suggestedBitrate; //bitrate suggested by the player ...
    }

    public class QubitStats{
        public MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo segmentSizeInfo;
        public MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo segmentQualityInfo;
        public String segmentURL;
        public long avlBandwidth = 0; //in bitspersecond
        public QubitABRSwitchInfo switchInfo = null;
    }

    private QubitABRSwitchInfo switchInfo = null;
    private static MMQFQubitStatisticsInterface statInterface = null;
    private static int statsWindowSz = 10;
    private Vector<QubitStats> segmentStats = new Vector<QubitStats>();
    private int minBitrate;
    private int maxBitrate;
    private long currentDownloadRate = -1;
    private int [] bitrateArray = null;
    private static Lock lock = new ReentrantLock();
}
