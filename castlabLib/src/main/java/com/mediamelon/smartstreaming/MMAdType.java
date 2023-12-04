package com.mediamelon.smartstreaming;

/**
 * Advertisement playback state
 */
public enum MMAdType{
  AD_LINEAR(1),

  AD_UNKNOWN(2);

  // Constructor
  private MMAdType(final Integer advertisementType) {
    this.adType = advertisementType;
  }

  // Internal state
  private int adType;

  public int getAdType() {
    return adType;
  }

  public boolean isLinearAd(){
    return (adType == 1);
  }
}
