package com.mediamelon.smartstreaming.Utils.trackingEvents;

public class mmAdTrackerInfo {

  public boolean isAdImpressionSent;
  public boolean isAdStartSent;
  public boolean isFirstQuartileSent;
  public boolean isMidQuartileSent;
  public boolean isThirdQuartileSent;
  public boolean isAdCompleteSent;

  public void setDefaults(){
    isAdImpressionSent = false;
    isAdStartSent = false;
    isFirstQuartileSent = false;
    isMidQuartileSent = false;
    isThirdQuartileSent = false;
    isAdCompleteSent = false;
  }

  public void setAdComplete(){
    isAdImpressionSent = false;
    isAdStartSent = false;
    isFirstQuartileSent = false;
    isMidQuartileSent = false;
    isThirdQuartileSent = false;
    isAdCompleteSent = true;
  }
  public mmAdTrackerInfo(){
    setDefaults();
  }

}
