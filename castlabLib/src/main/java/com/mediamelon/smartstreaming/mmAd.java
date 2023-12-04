package com.mediamelon.smartstreaming;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Time information of an event. */
public class mmAd {

    public String adId;
    public String adCreativeId;
    public String adTitle;
    public String adServer;
    public long adDuration;
    public long adCurrentPlaybackTimeInSec;
    public String position;
    public String streamType;
    public boolean isLinear;
    public int adIndex;
    public long startTimeInMs;
    public long startPos; //Added for A1 req
    public long endPos; //Added for A1 req
    public JSONObject adInfo;
    public int adSkipOffset;
    public int totalAds;
    public List<String> impressionTrackers;
    public List<String> startTrackers;
    public List<String> firstQuartileTrackers;
    public List<String> midpointTrackers;
    public List<String> thirdQuartileTrackers;
    public List<String> completeTrackers;
    public List<String> clickTrackingURLs;
    public List<String> clickThroughURLs;

    public mmAd()
    {
      adId = null;
      adCreativeId = null;
      adTitle = null;
      adDuration = 0;
      adCurrentPlaybackTimeInSec = 0;
      position = null;
      isLinear = true;
      adServer = null;
      adInfo = null;
      streamType = null;
      adIndex = 1;
      startTimeInMs = 0;
      startPos = 0; //default
      endPos = 0; //default
      adSkipOffset=-1;
      totalAds=1;
    impressionTrackers = new ArrayList<>();
    startTrackers = new ArrayList<>();
    firstQuartileTrackers = new ArrayList<>();
    midpointTrackers = new ArrayList<>();
    thirdQuartileTrackers = new ArrayList<>();
    completeTrackers = new ArrayList<>();
    clickTrackingURLs=new ArrayList<>();
    clickThroughURLs = new ArrayList<>();
    }


    public  long getStartPos(){return startPos;}

    public long getEndPos(){return endPos;}

    public long getAdCurrentPlaybackTimeInSec() {
      return adCurrentPlaybackTimeInSec;
    }

    public long getAdDuration() {
      return adDuration;
    }

    public String getAdCreativeId() {
      return adCreativeId;
    }

    public String getAdId() {
      return adId;
    }

    public int getAdIndex(){return adIndex;}

    public JSONObject getAdInfo() {
      return adInfo;
    }

    public String getAdServer() {
      return adServer;
    }

    public String getAdTitle() {
      return adTitle;
    }

    public boolean isLinear() {
      return isLinear;
    }

    public String getPosition() {
      return position;
    }

    public long getStartTimeInMs() {return startTimeInMs; }

    public String getStreamType() {
      return streamType;
    }

    public int getAdSkipOffset() {
        return adSkipOffset;
    }

    public List<String> getImpressionTrackers() {
        return impressionTrackers;
    }

    public List<String> getStartTrackers() {
        return startTrackers;
    }

    public List<String> getFirstQuartileTrackers() {
        return firstQuartileTrackers;
    }

    public List<String> getMidpointTrackers() {
        return midpointTrackers;
    }

    public List<String> getThirdQuartileTrackers() {
        return thirdQuartileTrackers;
    }

    public List<String> getCompleteTrackers() {
        return completeTrackers;
    }

    public List<String> getClickTrackingURLs() {
        return clickTrackingURLs;
    }

    public List<String> getClickThroughURLs() {
        return clickThroughURLs;
    }

    public int getTotalAds() {
        return totalAds;
    }
}
  
