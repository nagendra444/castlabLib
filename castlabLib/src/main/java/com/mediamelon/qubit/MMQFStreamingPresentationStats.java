package com.mediamelon.qubit;

import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.mediamelon.qubit.MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo;
import static com.mediamelon.qubit.MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo;

/**
 * Created by Rupesh on 12-10-2015.
 */
public class MMQFStreamingPresentationStats {

    public static MMQFStreamingPresentationStats instance(){
        if(inst == null){
            inst = new MMQFStreamingPresentationStats();
        }
        return inst;
    }

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

    public void saveStatsForUrl(String url, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segInfo) //segInfo is optional
    {
        lock.lock();
        if(presentationInfo == null){
            lock.unlock();
            return;
        }
        QubitStats stats = new QubitStats();
        if(stats != null) {
            if (segInfo == null) {
                segInfo = presentationInfoRetriever.getSegmentInfoForURL(url);
            }
            if (segInfo != null) {
                MMQFSegmentSizeInfo segmentSizeInfo = new MMQFSegmentSizeInfo();
                segmentSizeInfo.segmentStartTime = segInfo.videoTrackInfo.getSegmentInfo(segInfo.segmentIndex).segmentStartTime;
                segmentSizeInfo.segmentDuration = segInfo.videoTrackInfo.getSegmentInfo(segInfo.segmentIndex).duration;
                segmentSizeInfo.timescale = segInfo.videoTrackInfo.timeScale;
                segmentSizeInfo.requestedSegmentSz = segInfo.videoTrackInfo.getSegmentInfo(segInfo.segmentIndex).segmentSz;
                stats.segmentSizeInfo = segmentSizeInfo;
                stats.segmentQualityInfo = null;
                stats.segmentURL = url;
                stats.avlBandwidth = currentDownloadRate;
                stats.switchInfo = switchInfo;
            }
            if (stats.segmentSizeInfo == null) {
                //MMLogger.e("QubitSessionStats", "LockAcquired - skip saving stats " + url);
                lock.unlock();
                return;
            }
            switchInfo = null;
            //Segment stats need to maintain the unique entries, duplicate entry should overwrite the previous entry
            //Entries in the array should be sorted w.r.t. start time of the segment
            int segmentsCount = segmentStats.size();
            if (segmentsCount > 0) {
                int begin = 0;
                int end = segmentsCount - 1;
                int mid = (begin + end) / 2;
                while (begin <= end) {
                    QubitStats avlStats = segmentStats.elementAt(mid);
                    if (avlStats.segmentSizeInfo.segmentStartTime < stats.segmentSizeInfo.segmentStartTime) {
                        begin = mid + 1;
                    } else if (avlStats.segmentSizeInfo.segmentStartTime == stats.segmentSizeInfo.segmentStartTime) {
                        segmentStats.set(mid, stats);
                        MMLogger.e("QubitSessionStats.Graph", "Updating the element at starttime" + avlStats.segmentSizeInfo.segmentStartTime);
                        lock.unlock();
                        return;
                    } else {
                        end = mid - 1;
                    }
                    mid = (begin + end) / 2;
                }
            }

            if (stats != null && stats.segmentSizeInfo != null) {
                MMLogger.e("QubitSessionStats.Graph", " StartTime " + (int) ((float) stats.segmentSizeInfo.segmentStartTime / stats.segmentSizeInfo.timescale) + " BitsDiff " + (stats.segmentSizeInfo.requestedSegmentSz - stats.segmentSizeInfo.qubitizedSegmentSz));
            }

            segmentStats.add(stats);
        }
        lock.unlock();
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

    public void registerPresentationBitrates() {
        acquireLockedStats();
        inst.presentationInfoRetriever =  MMQFStreamingPresentationInfoRetriever.getInstance();
        if(inst.presentationInfoRetriever != null) {
            inst.presentationInfo = inst.presentationInfoRetriever.presentationInfo;
        }
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
        switchInfo = new QubitABRSwitchInfo();
        switchInfo.fromBitrate = currentBitrate;
        switchInfo.toBitrate = qbrTargetBitrate;
        switchInfo.suggestedBitrate = suggestedBitrate;
        acquireLockedStats();
        currentDownloadRate = downloadRate;
        releaseLockedStats();
    }

    public class QubitABRSwitchInfo{
        public boolean isSwitchHappened(){
          return (toBitrate != fromBitrate)?true:false;
        }
        public long fromBitrate;
        public long toBitrate;
        public long suggestedBitrate; //bitrate suggested by the player ...
    }

    public class QubitStats{
        public MMQFSegmentSizeInfo segmentSizeInfo;
        public MMQFSegmentQualityInfo segmentQualityInfo;
        public String segmentURL;
        public long avlBandwidth = 0; //in bitspersecond
        public QubitABRSwitchInfo switchInfo = null;
    }


    private static int statsWindowSz = 10;
    private Vector<QubitStats> segmentStats = new Vector<QubitStats>();
    private int minBitrate;
    private int maxBitrate;
    private long currentDownloadRate = -1;
    private int [] bitrateArray = null;
    private QubitABRSwitchInfo switchInfo = null;
    private static Lock lock = new ReentrantLock();
    private MMQFPresentationInfo presentationInfo;
    private MMQFStreamingPresentationInfoRetriever presentationInfoRetriever;
    private static MMQFStreamingPresentationStats inst;
}
