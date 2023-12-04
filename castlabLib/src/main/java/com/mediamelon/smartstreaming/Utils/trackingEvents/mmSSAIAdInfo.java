package com.mediamelon.smartstreaming.Utils.trackingEvents;

import com.mediamelon.smartstreaming.MMVastParser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class mmSSAIAdInfo {

  public String adId;
  public String adCreativeId;
  public String adTitle;
  public String adServer;
  public int adIndex;
  public long adDuration;
  public long startTime;
  public long endTime;
  public long startPos; //related programTime added for A1 req
  public long endPos; //Added for A1 req
  public long firstQuartile;
  public long midPoint;
  public long thirdQuartile;
  public long adCurrentPlaybackTimeInSec;
  public String position;
  public boolean active;
  public String adState;
  public String streamType;
  public boolean isLinear;
  public mmAdTrackerInfo adTrackerInfo;
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


  //TODO - ad skip offset -DONE

  public MMVastParser trackerObj;

  public mmSSAIAdInfo()
  {
    adId = null;
    adCreativeId = null;
    adTitle = null;
    adDuration = 0;
    startTime = 0;
    endTime = 0 ;
    startPos = 0; //default
    endPos = 0; //default
    firstQuartile = 0;
    midPoint = 0;
    thirdQuartile = 0;
    adIndex =1;
    adCurrentPlaybackTimeInSec = 0;
    position = null;
    active = false;
    adState = null;
    isLinear = true;
    adServer = null;
    adInfo = null;
    streamType = null;
    adTrackerInfo = new mmAdTrackerInfo();
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

  public long getStartTime() {
    return startTime;
  }

  public String getAdCreativeId() {
    return adCreativeId;
  }

  public String getAdId() {
    return adId;
  }

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

  public String getStreamType() {
    return streamType;
  }

  public int getAdIndex() {return adIndex; }

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
