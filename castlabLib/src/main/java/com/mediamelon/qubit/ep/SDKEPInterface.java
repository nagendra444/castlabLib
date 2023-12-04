package com.mediamelon.qubit.ep;

import android.content.Context;

import com.mediamelon.qubit.MMQFQubitStatusCode;
import com.mediamelon.smartstreaming.MMAdState;
import com.mediamelon.smartstreaming.MMAdType;
import com.mediamelon.smartstreaming.MMCellInfo;
import com.mediamelon.smartstreaming.MMConnectionInfo;
import com.mediamelon.smartstreaming.MMOverridableMetric;
import com.mediamelon.smartstreaming.MMPlayerState;

import org.json.JSONObject;


/**
 * Created by shri on 14/10/15.
 */
public interface SDKEPInterface {

    boolean isSDKuserInfoRegistered();
    Integer getTelephonyMetricsUpdateInterval();
    void reportCellularInformation(MMCellInfo info);
    void reportLocation(Double latitude, Double longitude);
    boolean registerSDKUserInfo(String playerName, String customerID, String componentName, String subscriberID, String domainName,boolean hashSubscriberId);
    boolean registerSDKUserInfo(String playerName, String customerID, String componentName, String subscriberID, String domainName, String subscriberType,boolean hashSubscriberId);
    boolean registerSDKUserInfo(String playerName, String customerID, String componentName, String subscriberID, String domainName, String subscriberType, String subscriberTag,boolean hashSubscriberId);

    void trackSegment(long startTimeMillisec, long size, int bitrate, int height, int width, float fps, String codecs);

    void reportCustomMetadata(String key, String value);
    //ag
    void setInternalSubscriberID(Context appCtx,boolean hashSubscriberId);

    void reportABRSwitch(Integer prevBitrate, Integer newBitrate);
    void reportPresentationSize(Integer width, Integer height);
    void initializeEPSDK(String manifestUrl, String mode, RegisterAPI.onRegistrationCompleteObserver observer);
    void reportPlayerSeeked(Long seekPos);
    void notifyMMSmartStreamingSDKInitialized(MMQFQubitStatusCode code);
    void notifyPresentationInformationUpdated();
    void initizationFailed();
    void initializationCancelled();

    void reportWifiDataRate(Integer dataRate);
    void reportWifiSignalStrengthPercentage(Double strength);
    void reportWifiSSID(String ssid);
    void reportMetricValue(MMOverridableMetric metric, String value);
    void onPlayerStateChanged(MMPlayerState state);

    /**
     * Notification from player when buffering begins.
     */
    void onBufferingBegin();

    /**
     * Notification from player when buffering ends and player goes to play state.
     */
    void onBufferingEnd();

    void onAdBufferingBegin();
    void onAdBufferingEnd();
    void saveBandwidthSample(Double value);
    void onPlayerError(String exception, Long playbackPosMillisec);

    void setPBStats(String attribute, Double value);
    void setPBStats(String attribute, Integer value);


    void reportAdState(MMAdState adState);
    //ag
   // void reportAdInfo(String adClient, String adId, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer,String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime);
    void reportAdInfo(String adClient, String adId, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer,String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime, String adCreativeId, String adUrl, String adTitle, int adBitrate);
    void reportAdPlaybackTime(Long playbackPosition);
    void reportAdError(String error, Long playbackPosMilliSec);

    /**
     * Sets playback duration
     */
    void setDuration(Long duration);

    /**
     * Sets presentation live attribute
     */
    void setPresentationLive(boolean live);

    /**
     * Sets location if it is available
     * @param longitude  longitude of the location
     * @param latitude   latitude of the location
     */
    void setLocation(Double longitude, Double latitude);

    /**
     * Set the current playback time
     * <p>
     *     For example in exo player this can be set periodically using a timer event that fires every few seconds
     *     EPInterfaceFactory.createEPInterface().setPbTime(Double pbTime)
     *
     *         timer.scheduleAtFixedRate(new TimerTask() {
     *      synchronized public void run() {
     *      EPInterfaceFactory.createEPInterface().setPbTime(player.getCurrentPosition() / 1000.0);
     *      }
     *      }, 2000, 2000);
     * </p>
     * @param pbTime
     */
    void setPbTime(Double pbTime);

    /**
     * call this method to set subscriber id.
     * This will help to diagnose session based streaming.
     * call this method before calling registerEPSDK.
     * @param subscriberId
     */
    void setSubscriberId(String subscriberId);

    void setSubscriber(String subscriberId, String subsType);
    //Bitsave
    //Quality
    //CostSave
    //Disabled
    void setPlayerMode(String mode);

    /**
     * call this method to set asset name explicitly
     * @param assetId
     */
    void setAssetInformation(String assetId, String assetName);
    /**
     * call this method to set asset name explicitly
     * @param assetId  content group id
     * @param assetName content name
     * @param videoId content id
     */

    void setAssetInformation(String assetId, String assetName, String videoId);
    //ag
    void setContentMetadata(JSONObject contentMetadata);
    /**
     * Sets the device information - brand, devicemodel, os, os version, telecom operator, screen width and height
     */
    public void setDeviceInformation(String brand, String deviceModel, String os, String osVersion, String telecomOperator, Integer screenWidth, Integer screenHeight);

    /**
     * Sets the Network type. Should be updated as network changes
     */
    public void setNetworkType(MMConnectionInfo networkType);

    public void onPresentationInformationReceivedExternal(Long durationMillisec, boolean isLive, String aStreamFormat,  int aProfileCount, String aMinRes, String aMaxRes, Double aMinFPS, Double aMaxFPS);
}
