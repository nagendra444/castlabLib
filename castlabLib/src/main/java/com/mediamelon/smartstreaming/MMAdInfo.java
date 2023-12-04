package com.mediamelon.smartstreaming;

/**
 * Identifies the Ad Information.
 */
public class MMAdInfo{

    /**
     * @return The MMAdInfo object
     */
    public MMAdInfo(){
       // adID = adClient = adPosition = adCreativeType = adResolution = adServer = null;
        //ag
        adID = adClient = adPosition = adCreativeType = adResolution = adServer = adCreativeId = adUrl = adTitle = null;
        adScheduledTime = -1.0;
        isBumper = false;
        adPodLength = 0;
        adDuration = 0L;
        adPositionInPod = 1;
        //ag
        adPodIndex = -2;
        adBitrate = 0;

    }

    /**
     * Sets the AD ID
     * @param inAdId 
     */
    void setAdId(String inAdId){
      if(inAdId != null) {
        adID = new String(inAdId);
      }
    }
    
    /**
     * Sets the adClient
     * @param inAdClient 
     */
    void setAdClient(String inAdClient){
      if(inAdClient != null) {
        adClient = new String(inAdClient);
      }
    }

    /**
     * Sets the adPosition
     * @param inAdPosition 
     */
    void setAdPosition(String inAdPosition){
      if(inAdPosition != null) {
        adPosition = new String(inAdPosition);
      }
    }
    
    /**
     * Sets the adDuration
     * @param inAdDuration
     */
    void setAdDuration(Long inAdDuration){
      if(inAdDuration >= 0) {
        adDuration = inAdDuration;
      }
    }

    /**
     * Sets the MMAdType
     * @param inAdType 
     */
    void setAdType(MMAdType inAdType){
      adType = inAdType;
    }

    /**
     * Sets the AD Mime type
     * @param inAdCreativeType 
     */
    void setAdCreativeType(String inAdCreativeType){
      if(inAdCreativeType != null) {
        adCreativeType = new String(inAdCreativeType);
      }
    }

    /**
     * Sets the Ad Server
     * @param inAdServer 
     */
    void setAdServer(String inAdServer){
      if(inAdServer != null) {
        adServer = new String(inAdServer);
      }
    }

    /**
     * Sets the AD Video Resolution
     * @param inAdResolution 
     */
    void setAdResolution(String inAdResolution){
      if(inAdResolution != null) {
        adResolution = new String(inAdResolution);
      }
    }

    /**
     * Sets the AD PodIndex
     * @param inAdPodIndex
     */
    void setAdPodIndex(Integer inAdPodIndex){
      if(inAdPodIndex >= 0) {
        adPodIndex = inAdPodIndex;
      }
    }

    /**
     * Sets the AD Position In Pod
     * @param inAdPositionInPod 
     */
    void setAdPositionInPod(Integer inAdPositionInPod){
      if(inAdPositionInPod >= 0) {
        adPositionInPod = inAdPositionInPod;
      }
    }

    /**
     * Sets the Total Ads in Pod
     * @param inAdPodLength 
     */
    void setAdPodLength(Integer inAdPodLength){
      if(inAdPodLength >= 0) {
        adPodLength = inAdPodLength;
      }
    }

    /**
     * Sets the isBumper for Bumpper Ads
     * @param inIsBumper 
     */
    void setAdIsBumper(boolean inIsBumper){
        isBumper = inIsBumper;
    }

    /**
     * Sets the AD ScheduledTime 
     * @param inAdScheduledTime 
     */
    void setAdScheduledTime(double inAdScheduledTime){
        adScheduledTime = inAdScheduledTime;
    }
    /**
     * Sets the isSSAI for SSAI Ads
     * @param inIsSSAI 
     */
    void setSSAI(boolean inIsSSAI){
        isSSAI = inIsSSAI;
    }
    
    /**
     * adClient Client used to play the ad, eg: VAST, IMA etc.
     */     
    public String adClient;
    /**
     * adID Tag represented by the ad (AD ID) 
     */ 
    public String adID;
    /**
     * adDuration Length of the video ad (in milliseconds).
     */ 
    public Long adDuration;
    /**
     * adPosition Position of the ad in the video  playback; one of "pre", "post" or "mid"
     *            that represents that the ad played before, after or during playback respectively. 
     */ 
    public String adPosition;
    /**
     * adType Type of the ad (linear, non-linear etc). 
     */ 
    public MMAdType adType;
    /**
     * adCreativeType  Ad MIME type.
     */ 
    public String adCreativeType;
    /**
     * adServer Ad server (ex. DoubleClick, YuMe, AdTech, Brightroll, etc.)
     */ 
    public String adServer;
    /**
     * adResolution Advertisement video resolution
     */ 
    public String adResolution;
    /**
     * adPodIndex Position of the ad within the ad group 
     * 0 for pre-roll 
     * 1-N for mid-roll
     * -1 for post-roll
     */ 
    public Integer adPodIndex;
    /**
     * adPositionInPod Position of the ad within the pod (starts from 1)
     */ 
    public Integer adPositionInPod;
    /**
     * adPodLength Total number of ads in the ad group
     */ 
    public Integer adPodLength;
    
    /**
     * isBumper True if bumper Ad else false
     */ 
    public boolean isBumper;
    /**
     * isSSAI True if SSAI Ad else false
     */ 
    public boolean isSSAI;
    /**
     * adScheduledTime The content time offset at which the current ad pod was scheduled (-1 if unknown)
     */ 
    public double adScheduledTime;
    //ag
    public String adCreativeId;
    public String adUrl;
    public String adTitle;
    public Integer adBitrate;

    
}
