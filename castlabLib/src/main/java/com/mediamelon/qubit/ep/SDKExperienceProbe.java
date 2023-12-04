package com.mediamelon.qubit.ep;

//import android.util.Log;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.mediamelon.qubit.MMLogger;
import com.mediamelon.qubit.MMQFQubitEngine;
import com.mediamelon.qubit.MMQFQubitPresentationInfoRetriever;
import com.mediamelon.qubit.MMQFQubitStatisticsInterface;
import com.mediamelon.qubit.MMQFQubitStatusCode;
import com.mediamelon.qubit.MMSmartStreamingRegistrationObservor;
import com.mediamelon.qubit.QBRXResInfo;
import com.mediamelon.smartstreaming.MMAdState;
import com.mediamelon.smartstreaming.MMAdType;
import com.mediamelon.smartstreaming.MMCellInfo;
import com.mediamelon.smartstreaming.MMConnectionInfo;
import com.mediamelon.smartstreaming.MMOverridableMetric;
import com.mediamelon.smartstreaming.MMPlayerState;
import com.mediamelon.smartstreaming.MMSmartStreaming;
//import com.mediamelon.smartstreaming.MMSmartStreamingExo2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

//ag
//ag

public class SDKExperienceProbe extends Application implements SDKEPInterface,
        MMSmartStreamingRegistrationObservor
{
    public void onRegistrationWithSmartSightCompleted(){

    }
    public void closeMediaMelonSDK(){
        Postman.instance().closeMediaMelonSDK();
    }
    public void startMediaMelonSDK(){ Postman.instance().startMediaMelonSDK();}
    public void onPresentationInformationReceived(Long durationMillisec, boolean isLive, String aStreamFormat,  int aProfileCount, String aMinRes, String aMaxRes, Double aMinFPS, Double aMaxFPS){
        internalPresInfo_DurationMS = durationMillisec;
        internalPresInfo_isLive = isLive;

        streamFormat = aStreamFormat;
        maxFPS = aMaxFPS;
        minFPS = aMinFPS;
        minResStr = aMinRes;
        maxResStr = aMaxRes;
        profileCnt = aProfileCount;

        Postman.instance().SetSessionInfo((externalPresInfo_DurationMS!= null)?externalPresInfo_DurationMS:durationMillisec, (externalPresInfo_isLive!=null)?externalPresInfo_isLive:isLive);
    }

    public void onPresentationInformationReceivedExternal(Long durationMillisec, boolean isLive, String aStreamFormat,  int aProfileCount, String aMinRes, String aMaxRes, Double aMinFPS, Double aMaxFPS){
        //Caution : We pick duration and isLive via explicit APIs
        //Caution : StreamFormat, if not set externally, is set internally using the manifest URL, provided streamFormat is nullor Unknown
        if(streamFormat == null || ((streamFormat.toLowerCase().contains("unknown") == true) && (aStreamFormat == null || aStreamFormat.toLowerCase().contains("unknown") == true))){
            //Try fetching from the manifest URL
            if(eventStats.qubitData.get(recIndex).streamID.streamURL != null){
                String manifestURL = eventStats.qubitData.get(recIndex).streamID.streamURL;
                if(manifestURL.contains(".m3u8")){
                    aStreamFormat = "HLS";
                }else if(manifestURL.contains(".mpd")){
                    aStreamFormat = "DASH";
                }else if(manifestURL.endsWith("/Manifest")){
                    aStreamFormat = "MSS";
                }else if(manifestURL.endsWith(".mp4")){
                    aStreamFormat = "MP4";
                }
            }
        }

        if(aMaxFPS > 0) {
            external_maxFPS = aMaxFPS;
        }

        if(aMinFPS > 0) {
            external_minFPS = aMinFPS;
        }

        if(aMaxRes != null && (aMaxRes.toLowerCase().contains("unknown") == false)){
            external_maxResStr = aMaxRes;
        }

        if(aMinRes != null && (aMinRes.toLowerCase().contains("unknown") == false)){
            external_minResStr = aMinRes;
        }

        if(aStreamFormat != null && (aStreamFormat.toLowerCase().contains("unknown") == false)){
            external_streamFormat = aStreamFormat;
        }

        if(aProfileCount > 0){
            external_profileCnt = aProfileCount;
        }
        Postman.instance().SetStreamInfo(getStreamFormat(), getProfileCnt(), getMinResStr(), getMaxResStr(), getMinFPS(), getMaxFPS());
    }

    public void onQBRModeFinalised(String qbrMode){
        qbrModeFinalized = qbrMode;
        Postman.instance().SetQBRMode(qbrModeFinalized);
        Postman.instance().SetStreamInfo(getStreamFormat(), getProfileCnt(), getMinResStr(), getMaxResStr(), getMinFPS(), getMaxFPS());
        Postman.instance().Start();
    }


    Long internalPresInfo_DurationMS = null;
    Boolean internalPresInfo_isLive = null;

    Double maxFPS = 0.0;
    Double minFPS = 0.0;
    String maxResStr = "Unknown";
    String minResStr = "Unknown";
    String streamFormat = "Unknown";
    Integer profileCnt = -1;

    Double external_maxFPS = 0.0;
    Double external_minFPS = 0.0;
    String external_maxResStr = "Unknown";
    String external_minResStr = "Unknown";
    String external_streamFormat = "Unknown";
    Integer external_profileCnt = -1;

    Double getMaxFPS(){
        if(external_maxFPS != null && external_maxFPS > 0){
            return external_maxFPS;
        }
        return maxFPS;
    }

    Double getMinFPS(){
        if(external_minFPS != null && external_minFPS > 0){
            return external_minFPS;
        }
        return minFPS;
    }

    String getMaxResStr(){
        if(external_maxResStr != null && (external_maxResStr.toLowerCase().contains("unknown") == false)){
            return external_maxResStr;
        }
        return maxResStr;
    }

    String getMinResStr(){
        if(external_minResStr != null && (external_minResStr.toLowerCase().contains("unknown") == false)){
            return external_minResStr;
        }
        return minResStr;
    }

    Integer getProfileCnt(){
        if(external_profileCnt != null && (external_profileCnt != -1)){
            return external_profileCnt;
        }
        return profileCnt;
    }

    String getStreamFormat(){
        if(external_streamFormat != null && (external_streamFormat.toLowerCase().contains("unknown") == false)){
            return external_streamFormat;
        }
        return streamFormat;
    }

    Long externalPresInfo_DurationMS = null;
    Boolean externalPresInfo_isLive = null;
    String qbrModeFinalized = null;

    private enum MMAdStateInternal{
        AD_UNKNOWN(0),
        /**
         * Ad is requested [initial state]]
         */
        AD_REQUEST(1),

        /**
         * Ad started playing or is unpaused
         */
        AD_PLAYING(2),

        /**
         * Ad is paused
         */
        AD_PAUSED(3),

        /**
         * Ad is skipped [terminal state]
         */
        AD_SKIPPED(4),

        /**
         * Ad completed play [terminal state]
         */
        AD_COMPLETED(5),

        /**
         * Error prevented Ad play [terminal state]
         */
        AD_ERROR(6),

        /**
         * Ad is blocked [terminal state]
         */
        AD_BLOCKED(7),

        /**
         * Based on the IAB definition of an ad impression
         */
        AD_IMPRESSION(8),

        /**
         * VPAID script signaled that it is starting
         */
        AD_STARTED(9),

        /**
         * User clicks an ad to be redirected to its landing page
         */
        AD_CLICKED(10),

        /**
         * Ad playback session resumed
         */
        AD_RESUMED(11),

        AD_PLAY(12),

        AD_BUFFERING(13),

        AD_MIDPOINT(14),

        AD_FIRST_QUARTILE(15),

        AD_THIRD_QUARTILE(16),

        AD_ENDED(17);

        // Constructor
        MMAdStateInternal(final Integer state) {
            this.state = state;
        }

        // Internal state
        private int state;

        public int getAdState() {
            return state;
        }
    }

    boolean _adBeforePlayback  = false;
    class MMAdvertisentInformation{
        private void Reset(){
            _adInformation._adClient = null;
            _adInformation._adId = null;
            _adInformation._adPosition = "";
            _adInformation._adDuration = -1;
            _adInformation._adResolution = null;
            _adInformation._adType = MMAdType.AD_UNKNOWN;
           // _adInformation._adCreatureType = null;
            //ag
            _adInformation._adCreativeType = null;
            _adInformation._adServer = null;
            _adInformation._prevAdState = null;
            _adInformation._adPlaybackTime = -1;
            _adInformation._playSent = false;
            _adInformation._adPlaybackStartWallClockTime = -1;
            _adInformation._adLastPlayingEventTimeStamp = -1;
            _adInformation._adLastAdEventTimeStamp = -1;
            _adInformation._isBumper = false;
            //ag
            _adInformation._adCreativeId = null;
            _adInformation._adUrl = null;
            _adInformation._adTitle = null;
            _adInformation._adBitrate = 0;

            _adInformation._adPodIndex = -2;
            _adInformation._adPodLength = -1;
            _adInformation._adPositionInPod = -1;
            _adInformation._adScheduledTime = -1.0;
            _adInformation._adPlayDur = 0;
        }

        private void ResetForNewAd(){
            _adInformation._prevAdState = null;
            _adInformation._adPlaybackTime = -1;
            _adInformation._playSent = false;
            _adInformation._adPlaybackStartWallClockTime = -1;
            _adInformation._adLastPlayingEventTimeStamp = -1;
            _adInformation._adLastAdEventTimeStamp = -1;
            _adInformation._adPlayDur = 0;
        }

        String _adClient;
        String _adId;
        String _adPosition;
        long _adDuration;
        int _adPodIndex;
        int _adPodLength;
        int _adPositionInPod;
        double _adScheduledTime;
        boolean _isBumper;
        //ag
        String _adCreativeId;
        String _adUrl;
        String _adTitle;
        int _adBitrate;
        String _adResolution;
        MMAdType _adType;
       // String _adCreatureType;
        //ag
        String _adCreativeType;
        String _adServer;
        MMAdState _prevAdState;
        boolean _playSent = false;
        double _adPlayDur =0;
        long _adPlaybackTime = -1;
        long _adPlaybackStartWallClockTime = -1;
        long _adLastPlayingEventTimeStamp = -1;
        long _adLastAdEventTimeStamp = -1;
    };

    private MMAdvertisentInformation _adInformation;
    private double BITSTOKILOBITS = 1024.0;
    private boolean isStartAfterAd = false;
    private double pbTimeBeforeAdStart =0L;
    MMAdStateInternal getInternalAdState(MMAdState adState){
        MMAdStateInternal state;
        switch (adState){
            case AD_ERROR:{
                state = MMAdStateInternal.AD_ERROR;
            }
            break;
            case AD_BLOCKED:{
                state = MMAdStateInternal.AD_BLOCKED;
            }
            break;
            case AD_CLICKED:{
                state = MMAdStateInternal.AD_CLICKED;
            }
            break;
            case AD_FIRST_QUARTILE: {
                state = MMAdStateInternal.AD_FIRST_QUARTILE;
            }
            break;
            case AD_MIDPOINT: {
                state = MMAdStateInternal.AD_MIDPOINT;
            }
            break;
            case AD_THIRD_QUARTILE: {
                state = MMAdStateInternal.AD_THIRD_QUARTILE;
            }
            break;
            case AD_ENDED:{
                state = MMAdStateInternal.AD_ENDED;
            }
            break;
            case AD_COMPLETED:{
                state = MMAdStateInternal.AD_COMPLETED;
            }
            break;
            case AD_IMPRESSION:{
                state = MMAdStateInternal.AD_IMPRESSION;
            }
            break;
            case AD_PAUSED:{
                state = MMAdStateInternal.AD_PAUSED;
            }
            break;
            case AD_SKIPPED:{
                state = MMAdStateInternal.AD_SKIPPED;
            }
            break;
            case AD_PLAYING:{
                state = MMAdStateInternal.AD_PLAYING;
            }
            break;
            case AD_REQUEST:{
                state = MMAdStateInternal.AD_REQUEST;
            }
            break;
            case AD_RESUMED:{
                state = MMAdStateInternal.AD_RESUMED;
            }
            break;
            case AD_PLAY:
            case AD_STARTED:{
                state = MMAdStateInternal.AD_STARTED;
                adState = MMAdState.AD_STARTED;
            }
            break;
            default:{
                state =  MMAdStateInternal.AD_UNKNOWN;
            }

        }
        return state;
    }

    private static Long custId;
    private static String domainName = "";
    private static String componentName = "";
    private static boolean isRegistered = false;

    private static String playerName = "MMTestPlayer";
    private static String subscriberId = null;
    private static String subscriberType = null;
    private static String subscriberTag = null;


    private static String playerBrand;
    private static String playerModel;
    private static String playerVersion;

    private static String deviceBrand;
    private static String deviceModel;
    private static String deviceOS;
    private static String deviceOSVersion;
    private static String telecomOperator;
    private static Integer screenWidth;
    private static Integer screenHeight;

    private static String connectionType;
    private static SegmentInfo prevSegInfo;
    private String wifiSSID;
    private String wifiDataRate;
    private String wifiSignalStrength;
    private String cdn;
    private String externalSetLatency;
    private boolean sessionInitizationFailed = false;
    private boolean qbrSegInfoIntegrated = false;
    private String playbackMode = "";
    Integer interval;
    String producerURL;

    private boolean doneWithSession = false;



    private String abrURL; //Manifest URL
    private static RegisterAPI registerationAPIReq = null;
    private QBRXResInfo xResInfo_; //XRes info as had from the Registration response
    private long srvClkLclClkDiff; //Time diff between server clock and the player clock at initialisation time

    //For computation of latency
    private long loadStartTime; //Time when user pressed the play button on the player for the first time
    private long adLoadStartTime;
    long loadTime; //Duration of time since user pressed the play button and playback started [User experienced latency]
    long adLoadTime;
    Boolean onloadSent; //On load event is sent
    //For Stats Monitor TimeStamp
    long epocTime;
    long statsMonitorStartedAtTS;
    long lastStatsPostedAtTS;

    long startPayloadSentTime = 0;
    long startOffsetForStats = 0;

    //State transition handling
    private MMPlayerState prevState; //prev state
    private MMPlayerState prev2PrevState; //prev 2 prev state
    private double playbackTimeForLastComplete = -1.0; //Playback time when last STOPPED was notified
    private boolean needToSendComplete = true; //Mapping STOPPED ->{ENDED/COMPLETE}
    Boolean startSent; //START (playback rendering for the first time) sent to backend

    //Pause and buffering duration
    private Long timeOnPlayingStateTransition; //time at which player transitioned to PLAYING state
    Long pauseStartTime; //Pause start time
    Long pauseDuration; //Duration for which user kept player in PAUSED state
    Long pauseDurationToOffsetFromInterval = -1L;
    Long adPauseDurationToOffsetFromInterval = -1L;
    Long adPauseStartTime;
    Long adPauseDuration;
    private long buffStarTime; //Buffering start time
    private long buffWait; //Duration of buffering
    private long adBuffWait; // Duration of buffering for Ads
    private long adBuffWaitForInterval = 0L;

    //Average bandwidth used by player
    private Double sumBandwidth;
    private int bwCount;

    //ABR switch computation
    private int prevBitrate = 0;
    private Long prevBitrateL = 0l; //in long for QBR
    private int upShift = 0;
    private int downShift = 0;

    private double totalDur; //total duration in seconds
    private double pbTime; //playback position in milliseconds

    private String sessionId; //Internal session Identifier


    private Integer activeStreamPresentationWidth;
    private Integer activeStreamPresentationHeight;

    //Download rate [at which chunk is downloaded (bits per second)]
    Double downloadRate;
    Long bufferLength;
    //Asset Information
    String assetId;
    String assetName;
    String videoId;
    //ag
    JSONObject cmData;
    String assetNameCM;
    String videoIdCM;
    String assetIdCM;
    String contentType;
    String drmProtection;
    String episodeNumber;
    String genre;
    String season;
    String seriesTitle;
    String videoType;
   // public boolean isLiveStream = false;

    //Enable the Sending of stats by EP => Initialization succeeded, and currently not in PAUSE state
    boolean canSendProbes;
    boolean integrationEnabled; //init API was triggered for a session

    private Integer telephonyFetchInterval;

    private MMCellInfo cellInfo;
    private Double latitude;
    private Double longitude;
    //ag
    Context appContext;
    boolean isSubscriberIDSet = false;

    //Custom KVPs
    Map<String, String> customKvps;

    //Eventing
    RecordStructure qbrStats;   //Periodic QoE data
    RecordStructure eventStats; //Event Stats
    StatsSender statsSender;
    boolean statsSendingPrepared = false;
    boolean postmanDriverNeedsStart = false; //to clear up prev event sender stats
    Thread postmanThread;
    Timer statsTimer; //Periodic timer to send the stats
    int recIndex; //ALWAYS 0 :-(

    public boolean registerSDKUserInfo(String playerName, String customerID, String componentName, String subscriberID,
                                       String domainName, String subsType, String subsTag,boolean hashSubscriberId){
        boolean status = registerSDKUserInfo(playerName, customerID, componentName, subscriberID, domainName,hashSubscriberId);
        if(status){
            if(subsType != null && !subsType.isEmpty()) subscriberType = subsType;
            if(subsTag != null && !subsTag.isEmpty()) subscriberTag = subsTag;
        }
        return status;
    }
    public boolean registerSDKUserInfo(String playerName, String customerID, String componentName, String subscriberID,
                                       String domainName, String subsType,boolean hashSubscriberId){
        return registerSDKUserInfo(playerName,customerID,componentName,subscriberID,domainName,subsType,null,true);
    }

    public boolean registerSDKUserInfo(String pName, String cID, String cName, String sID, String dName,boolean hashSubscriberId){
        playerName = pName;
        try {
            custId = Long.parseLong(cID);
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
        componentName = cName;


        if (sID != null && sID.length() > 0) {
            subscriberId = getHashed(sID);
            //ag
            isSubscriberIDSet = true;
        }
        else if (sID==null || sID.length()==0)
        {
            //setInternalSubscriberID(MMSmartStreamingExo2.getInstance().applicationContext);
            isSubscriberIDSet = true;

        }
        else
        {
            isSubscriberIDSet = false;
        }
        domainName = dName;

        if (playerName == null || playerName.length() == 0 || cID == null || cID.length() == 0 || custId < 0 || componentName == null || componentName.length() == 0){
            return false;
        }
        isRegistered = true;
        return true;
    }

    public boolean isSDKuserInfoRegistered(){
        return isRegistered;
    }

    //ag
        public void setInternalSubscriberID(Context appCtx,boolean hashSubscriberId){

                 if(isSubscriberIDSet != true)
                 {

                        appContext = appCtx;

                        String internalSubscriberUUID = UUID.randomUUID().toString().replace("-", "");

                        String internalStorageFile = "subscriberID";

                        String internalStorageContent = "";

                        Boolean err_file = false;

                        FileInputStream fis = null;

                        try {
                        fis = appContext.openFileInput(internalStorageFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        err_file = true;
                    }

                        if(err_file != true) {
                    InputStreamReader inputStreamReader = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
                        }
                    StringBuilder stringBuilder = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                                String line = reader.readLine();
                                while (line != null) {
                                        stringBuilder.append(line).append('\n');
                                        line = reader.readLine();
                                    }
                            } catch (IOException e) {
                                // Error occurred when opening raw file for reading.
                                        e.printStackTrace();
                            } finally {
                                internalStorageContent = stringBuilder.toString();

                                    }
                    }


                                if(internalStorageContent != null && !internalStorageContent.equals(""))
                    {
                                Log.v(logTag, "MM Internal subscriberID already there: "+internalStorageContent);
                        if(hashSubscriberId) subscriberId = getHashed(internalStorageContent);
                        else subscriberId = internalStorageContent;
                    addSubscriberId();
                }
                else
                {
                            String fileContents = internalSubscriberUUID;
                    try (FileOutputStream fos = appContext.openFileOutput(internalStorageFile, Context.MODE_PRIVATE)) {
                            fos.write(fileContents.getBytes());
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                            try {
                            fis = appContext.openFileInput(internalStorageFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                            InputStreamReader inputStreamReader_int = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            inputStreamReader_int = new InputStreamReader(fis, StandardCharsets.UTF_8);
                        }

                            StringBuilder stringBuilder_int = new StringBuilder();

                            try (BufferedReader reader = new BufferedReader(inputStreamReader_int)) {
                            String line = reader.readLine();
                            while (line != null) {
                                    stringBuilder_int.append(line).append('\n');
                                    line = reader.readLine();
                                }
                        } catch (IOException e) {
                            // Error occurred when opening raw file for reading.
                                    e.printStackTrace();
                        } finally {
                            internalStorageContent = stringBuilder_int.toString();

                                }

                            Log.v(logTag, "MM Internal subscriberID created now: "+internalStorageContent);
                    subscriberId = getHashed(internalStorageContent);
                    addSubscriberId();
                 }

                      }

                }



    public void reportPresentationSize(Integer width, Integer height){
        activeStreamPresentationHeight = height;
        activeStreamPresentationWidth = height;
    }

    public void setDeviceInformation(String dBrand, String dModel, String dOS, String dOSVersion, String dTelecomOperator, Integer dScreenWidth, Integer dScreenHeight){
        deviceBrand = dBrand;
        deviceModel = dModel;
        deviceOS = dOS;
        deviceOSVersion = dOSVersion;
        telecomOperator = dTelecomOperator;
        screenHeight = dScreenHeight;
        screenWidth = dScreenWidth;
    }

    private void updateCDN(String server){
        if (server != null) {
            setClientInfo(EPAttributes.CDN, server);
        }
    }

    private void addDeviceInformation(){
        setClientInfo(EPAttributes.BRAND, deviceBrand);
        setClientInfo(EPAttributes.MODEL, deviceModel);
        setClientInfo(EPAttributes.DEVVERSION, deviceOSVersion);
        setClientInfo(EPAttributes.PLATFORM, deviceOS);
        setClientInfo(EPAttributes.OPERATOR, telecomOperator);
        setClientInfo(EPAttributes.SCREENRES, screenWidth.toString() + "x" + screenHeight.toString());
    }

    public void setPlayerInformation(String brand, String model, String version){
        playerBrand = brand;
        playerModel = model;
        playerVersion = version;
    }

    public void reportPlayerSeeked(Long seekPos){
	if (startSent && prevState != MMPlayerState.STOPPED){
            setPbTime( (seekPos * 1.0)/1000);
            notifyEvent("SEEKED","Playback Seeked","SEEKED",(seekPos * 1.0)/1000);
        }else {
            Log.i(logTag, "Playback starting from non zeo position - [" + seekPos + "]");
        }
    }

    public void reportCustomMetadata(String key, String value){
        customKvps.put(key, value);
        qbrStats.qubitData.get(recIndex).customTags.clear();
        eventStats.qubitData.get(recIndex).customTags.clear();

        for (Map.Entry<String, String> entry : customKvps.entrySet()) {
            qbrStats.qubitData.get(recIndex).customTags.put(entry.getKey(), entry.getValue());
            eventStats.qubitData.get(recIndex).customTags.put(entry.getKey(), entry.getValue());
        }
    }

    private void createSessionId() {

	String uuid = UUID.randomUUID().toString().replace("-", "");

	String time_now = String.valueOf(System.currentTimeMillis());

	sessionId = uuid + "-" + time_now;

        if (playerName != null){
            sessionId += "_" + playerName;
        }

        if (playerBrand != null){
            sessionId += "_" + playerBrand;
        }

        if (playerModel != null){
            sessionId += "_" + playerModel;
        }

        if (playerVersion != null){
            sessionId += "_" + playerVersion;
        }
        sessionId = sessionId.replaceAll("/", "_");
        sessionId = sessionId.replaceAll(" ", "_");

        Log.i(logTag, "Creating new session." + sessionId);
    }

    private static class SDKExperienceProbeHolder {
        public static SDKExperienceProbe mmqfExperienceProbe = new SDKExperienceProbe();
    }

    public static SDKExperienceProbe getInstance() {
        return SDKExperienceProbeHolder.mmqfExperienceProbe;
    }

    SDKExperienceProbe(){
        resetSessionParams();

        xResInfo_ = new QBRXResInfo();
        customKvps = new HashMap<String, String>();
        qbrStats = new RecordStructure();
        qbrStats.qubitData = new CopyOnWriteArrayList<QBRMetric>();
        qbrStats.timestamp = timeNow();
        qbrStats.version = EPDataFormat.version;
        QBRMetric qbrMetric = new QBRMetric();
        qbrMetric.pbInfo = new CopyOnWriteArrayList<PlaybackInfo>();

        qbrMetric.clientInfo = new ClientInfo();
        qbrMetric.streamID = new StreamID();
        qbrMetric.contentMetadata = new ContentMetadata();
        qbrMetric.segInfo = new CopyOnWriteArrayList<SegmentInfo>();
        qbrMetric.streamInfo = new StreamInfo();
        qbrMetric.sdkInfo = new SDKInfo();
        qbrMetric.diagnostics = new Diagnostics();
        recIndex = 0;
        pbTime = 0.0;
        qbrStats.qubitData.add(recIndex, qbrMetric);

        eventStats = new RecordStructure();
        eventStats.qubitData = new CopyOnWriteArrayList<QBRMetric>();
        eventStats.timestamp = timeNow();
        eventStats.version = EPDataFormat.version;

        QBRMetric eventQbrMetric = new QBRMetric();
        eventQbrMetric.clientInfo = new ClientInfo();
        eventQbrMetric.sdkInfo = new SDKInfo();
        eventQbrMetric.diagnostics = new Diagnostics();
        eventQbrMetric.pbEventInfo = new PBEventInfo();
        eventQbrMetric.streamID = new StreamID();
        eventQbrMetric.contentMetadata = new ContentMetadata();
        eventQbrMetric.streamInfo = new StreamInfo();
        eventQbrMetric.pbInfo = new CopyOnWriteArrayList<PlaybackInfo>();
        eventStats.qubitData.add(recIndex,eventQbrMetric);
    }


    private void resetSessionParamsRetainRegistrationAndAssetInfo(){
        Log.i(logTag, "resetSessionParamsRetainRegistrationAndAssetInfo.");
        loadStartTime = -1;
        adLoadStartTime = -1;
        loadTime = -1;
        adLoadTime = -1;
        onloadSent = false;

        prevState = prev2PrevState = null;

        playbackTimeForLastComplete = -1.0;
        needToSendComplete = true;
        startSent = false;

        if(statsTimer != null){
            statsTimer.cancel();
            statsTimer.purge();
        }

        statsTimer = null;

        //Pause and buffering duration
        timeOnPlayingStateTransition = -1L;
        pauseStartTime = -1L;
        pauseDuration = -1L;
        adPauseStartTime = -1L;
        adPauseDuration = -1L;
        buffStarTime = -1L;
        buffWait = -1L;
        adBuffWait = -1L;
        adBuffWaitForInterval = -1L;
        pbTimeBeforeAdStart =0L;

        //MonitorInterval
        epocTime = 0L;
        lastStatsPostedAtTS =0L;
        statsMonitorStartedAtTS =0L;
        startPayloadSentTime = 0L;
        startOffsetForStats = 0;

        //Average bandwidth used by player
        sumBandwidth = -1.0;
        bwCount = -1;

        //ABR switch computation
        prevBitrate = 0;
        prevBitrateL = 0L; //in long for QBR
        upShift = 0;
        downShift = 0;

        totalDur = -1.0; //total duration in seconds
        pbTime = 0.0; //playback position in milliseconds

        sessionId = ""; //Internal session Identifier

        //Download rate [at which chunk is downloaded (bits per second)]
        downloadRate = 0.0;
        bufferLength = 0L;

        activeStreamPresentationWidth = 0;
        activeStreamPresentationHeight = 0;
    }

    private void resetSessionParams(){
        Log.i(logTag, "Resetting the session.");
        _adBeforePlayback = false;
        interval = -1;
        producerURL = "";
        abrURL = "";

        srvClkLclClkDiff = -1;

        loadStartTime = -1;
        adLoadStartTime = -1;
        loadTime = -1;
        adLoadTime = -1;
        onloadSent = false;

        prevState = prev2PrevState = null;

        pbTimeBeforeAdStart =0L;
        playbackTimeForLastComplete = -1.0;
        needToSendComplete = true;
        startSent = false;

        if(statsTimer != null){
            statsTimer.cancel();
            statsTimer.purge();
        }
        statsSender = null;
        statsTimer = null;
        //Pause and buffering duration
        timeOnPlayingStateTransition = -1L;
        pauseStartTime = -1L;
        pauseDuration = -1L;
        adPauseStartTime = -1L;
        adPauseDuration = -1L;
        buffStarTime = -1L;
        buffWait = -1L;
        adBuffWait = -1L;
        adBuffWaitForInterval = -1L;
        isStartAfterAd = false;
        //MonitorInterval
        epocTime = 0L;
        lastStatsPostedAtTS =0L;
        statsMonitorStartedAtTS =0L;
        startPayloadSentTime = 0L;
        startOffsetForStats = 0L;

        //Average bandwidth used by player
        sumBandwidth = -1.0;
        bwCount = -1;

        //ABR switch computation
        prevBitrate = 0;
        prevBitrateL = 0L; //in long for QBR
        upShift = 0;
        downShift = 0;

        totalDur = -1.0; //total duration in seconds
        pbTime = 0.0; //playback position in milliseconds

        sessionId = ""; //Internal session Identifier

        //Download rate [at which chunk is downloaded (bits per second)]
        downloadRate = 0.0;
        bufferLength = 0L;

        //Asset Information
        assetId = null;
        assetName = null;
        videoId = null;

        //Enable the Sending of stats by EP => Initialization succeeded, and currently not in PAUSE state
        canSendProbes = false;
        integrationEnabled = false; //init API was triggered for a session

        telephonyFetchInterval = -1;

        cellInfo = null;
        latitude = null;
        longitude = null;

        activeStreamPresentationWidth = 0;
        activeStreamPresentationHeight = 0;
        playbackMode = "";
        if(customKvps != null) {
            customKvps.clear();
        }

        // Clear Custom Tags
        if(eventStats != null && eventStats.qubitData != null) {
            eventStats.qubitData.get(recIndex).customTags.clear();
        }
        if(qbrStats != null && qbrStats.qubitData != null) {
            qbrStats.qubitData.get(recIndex).customTags.clear();
        }

        if(eventStats != null && eventStats.qubitData != null) {
            eventStats.qubitData.get(recIndex).clientInfo.cdn = null;
        }
        if(qbrStats != null && qbrStats.qubitData != null) {
            qbrStats.qubitData.get(recIndex).clientInfo.cdn = null;
        }
	
        statsSendingPrepared = false;
        qbrSegInfoIntegrated = false;
        pauseDurationToOffsetFromInterval = -1L;
        adPauseDurationToOffsetFromInterval = -1L;

        externalPresInfo_DurationMS = null;
        externalPresInfo_isLive = null;
        qbrModeFinalized = null;

        internalPresInfo_DurationMS = null;
        internalPresInfo_isLive = null;

        maxFPS = 0.0;
        minFPS = 0.0;
        maxResStr = "Unknown";
        minResStr = "Unknown";
        streamFormat = "Unknown";
        profileCnt = -1;

        external_maxFPS = 0.0;
        external_minFPS = 0.0;
        external_maxResStr = "Unknown";
        external_minResStr = "Unknown";
        external_streamFormat = "Unknown";
        external_profileCnt = -1;
    }

    public void initializeEPSDK(String manifestUrl, String mode, RegisterAPI.onRegistrationCompleteObserver observer) {
        xResInfo_.Reset();

        doneWithSession = false;
        if (registerationAPIReq != null){
            registerationAPIReq.cancel(true);
        }

        registerationAPIReq = new RegisterAPI(manifestUrl, componentName, deviceOS, MMSmartStreaming.getVersion(), mode);
        registerationAPIReq.observer = observer;

        String screenResolution = screenWidth.toString() + "x" + screenHeight.toString();
        registerationAPIReq.setDeviceInformation(deviceBrand, deviceModel, deviceOSVersion, telecomOperator, screenResolution);

        sessionInitizationFailed = false;
        totalDur = 0;
        eventStats.playDur = 0;
        qbrStats.playDur = 0;

        boolean isLive = false; //Initialize with false
        addStreamID(manifestUrl, mode, isLive);
        addClientInformation();
        setStreamInfo();
        setSDKInfo();
        registerationAPIReq.execute();
	
	if(eventStats.qubitData.get(recIndex).clientInfo.cdn == null && qbrStats.qubitData.get(recIndex).clientInfo.cdn == null){
            setClientInfo(EPAttributes.CDN,"");
	}
	String domainName = extractDomainName(manifestUrl);
        updateHostIP(domainName);
    }

    public Integer getTelephonyMetricsUpdateInterval(){
        return telephonyFetchInterval;
    }

    public void reportCellularInformation(MMCellInfo info){
        cellInfo = info;
    }

    public void reportLocation(Double lat, Double longi){
        latitude = lat;
        longitude = longi;
    }

    public void notifyMMSmartStreamingSDKInitialized(MMQFQubitStatusCode status){

        addDiagnostics();
        addStreamInfo();
        updateLiveStreamingAttribute();
    }

    public void notifyPresentationInformationUpdated(){
        addStreamInfo();
    }

    private void addStreamInfo(){
        setStreamInfo();
    }

    private void addDiagnostics(){
        setDiagnostics();
    }

    public void addStreamID(String manifestUrl, String mode, boolean isLive) {
        eventStats.qubitData.get(recIndex).streamID.streamURL = manifestUrl;
        qbrStats.qubitData.get(recIndex).streamID.streamURL = manifestUrl;

        addAssetInformation();
        setStreamIDInfo(EPAttributes.CUSTID, custId);
        addSubscriberId();

        eventStats.qubitData.get(recIndex).streamID.sessionId = sessionId;
        qbrStats.qubitData.get(recIndex).streamID.sessionId = sessionId;

        addPlayerMode(mode); //it should be reset once the Registration response comes back

        qbrStats.qubitData.get(recIndex).streamID.isLive = isLive;
        eventStats.qubitData.get(recIndex).streamID.isLive = isLive;

        eventStats.qubitData.get(recIndex).streamID.dataSrc = "Player";
        qbrStats.qubitData.get(recIndex).streamID.dataSrc = "Player";

        addPlayerName();
        addDomainName();
    }

    public void cleanupEventStructures(String manifestUrl, Long custID){
        resetSessionParams();
        createSessionId();

        abrURL = manifestUrl;
        qbrStats.qubitData.get(recIndex).pbInfo.clear();
        qbrStats.qubitData.get(recIndex).segInfo.clear();
        eventStats.qubitData.get(recIndex).pbInfo.clear();
        integrationEnabled = false;

        custId = custID;

        if(statsTimer != null){
            statsTimer.purge();
            statsTimer.cancel();
            statsTimer = null;
        }

        if(postmanThread == null){
            postmanThread = new Thread(Postman.instance());
            postmanDriverNeedsStart = true;
        }

        if(producerURL != null){
            Postman.instance().Flush();
        }else{
            Postman.instance().Clear();
        }
        Postman.instance().Stop();
    }


    public void initialize(Long custId, Integer interval, String pUrl, String manifestUrl, Long timestamp, String mode, Double cfVal, Integer tFetchInterval, Integer maxStepsUp, Integer maxStepsDown) {
        MMLogger.v(logTag, "SDKEP initialize is called: custId=" + custId + " interval=" + interval + " producerUrl=" + pUrl);
        canSendProbes = true;

        telephonyFetchInterval = tFetchInterval;

        xResInfo_.mode = mode;
        xResInfo_.cfVal = cfVal;
        xResInfo_.maxStepsUp = maxStepsUp;
        xResInfo_.maxStepsDown = maxStepsDown;

        this.interval = interval;
        producerURL = pUrl;

        qbrStats.interval = interval;
        eventStats.interval = interval;

        if(timestamp > 0) {
            srvClkLclClkDiff = timestamp - timeNow();
        }else{
            srvClkLclClkDiff = 0;
        }
        MMLogger.v(logTag, "ServerClockDifference= " + srvClkLclClkDiff + " timeNow= " + timeNow());
        try {
            Postman.instance().SetQBRMode(mode);
            Postman.instance().SetProducerUrl(new URL(pUrl));
            Postman.instance().SetInterval(interval);
            Postman.instance().Prepare();
            if (postmanDriverNeedsStart) {
                postmanThread.start();
                postmanDriverNeedsStart = false;
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }

        setStreamIDInfo(EPAttributes.PLAYERMODE, mode);
        if(statsSendingPrepared == true && doneWithSession == false){
            if(prevState == MMPlayerState.PLAYING) {
                Log.d(logTag, "Starting the stats Sender");
                long currTime  = timeNow();
                startOffsetForStats = currTime - startPayloadSentTime;
                if(startOffsetForStats < 0){
                    startOffsetForStats = 0;
                    MMLogger.d("ExceptionCondition", "Time Now, Reg completion Time is " + currTime + " START sent at " + startPayloadSentTime);
                }
                startStatsSender();
            }else{
                sendStatsNow();
            }
        }
    }

    public void setSubscriberId(String subId) {
        subscriberId = getHashed(subId);
        subscriberType = null;
        addSubscriberId();
    }

    public void setSubscriber(String subId, String subsType) {
        setSubscriber(subId,subsType,null);
    }
    public void setSubscriber(String subId, String subsType, String subsTag) {
        if(subId != null && !subId.isEmpty())subscriberId = getHashed(subId);
        if(subsType!= null && !subsType.isEmpty()) subscriberType = subsType;
        if(subsTag!= null && !subsTag.isEmpty()) subscriberTag = subsTag;

        addSubscriberId();
    }

    public String getHashed(String subId){
        String retval = null;
        try {
            if(subId != null && subId.length()>0){
                try {
                    MessageDigest digester = MessageDigest.getInstance("MD5");
                    digester.update(subId.getBytes());
                    // Converting the digest from byte format to hex string.
                    retval = new BigInteger(1, digester.digest()).toString(16);
                    int len = 0;
                    len = retval.length();
                    String prefixStr = "";
                    while (32 - len > 0){
                        prefixStr = prefixStr + "0";
                        len++;
                    }
                    String paddedStr = prefixStr + retval;
                    retval = paddedStr;
                }catch(Exception e){
                    MMLogger.e(logTag," Could not hash subs id="+subId);
                }
            }
        } catch (Exception e) {
            MMLogger.e(logTag," Could not hash subs id="+subId);
        }
        return retval;
    }

    private void addSubscriberId(){
        setStreamIDInfo(EPAttributes.SUBSCRIBERID, subscriberId);
        setStreamIDInfo(EPAttributes.SUBSCRIBERTYPE, subscriberType);
        setStreamIDInfo(EPAttributes.SUBSCRIBERTAG, subscriberTag);
    }


    @Override
    public void setAssetInformation(String aId, String aName) {
        setAssetInformation(aId,aName,null);
    }
    public void setAssetInformation(String aId, String aName, String vId) {
        assetId = aId;
        assetName = aName;
        videoId = vId;
        addAssetInformation();
    }

    private void addAssetInformation(){
        if (assetId != null) {
            qbrStats.qubitData.get(recIndex).streamID.assetId = assetId;
            eventStats.qubitData.get(recIndex).streamID.assetId = assetId;
        }

        if (assetName != null){
            qbrStats.qubitData.get(recIndex).streamID.assetName = assetName;
            eventStats.qubitData.get(recIndex).streamID.assetName = assetName;
        }

        if (videoId != null){
            qbrStats.qubitData.get(recIndex).streamID.videoId = videoId;
            eventStats.qubitData.get(recIndex).streamID.videoId = videoId;
        }
    }
    //ag
    public void setContentMetadata(JSONObject contentMetadata) {


                	if(contentMetadata != null){

                    	cmData = contentMetadata;

                            try {

                        	     if(contentMetadata.has("assetName")){
                                     assetNameCM = contentMetadata.get("assetName").toString();
                    	     }

                        	     if(contentMetadata.has("videoId")){
                                 videoIdCM = contentMetadata.get("videoId").toString();
                    	     }

                        	     if(contentMetadata.has("assetId")){
                                 	assetIdCM = contentMetadata.get("assetId").toString();
                    	     }

                        	     if(contentMetadata.has("contentType")){
                                 	contentType = contentMetadata.get("contentType").toString();
                    	     }

                        	     if(contentMetadata.has("drmProtection")){
                                 	drmProtection = contentMetadata.get("drmProtection").toString();
                    	     }

                        	     if(contentMetadata.has("episodeNumber")){
                                 	episodeNumber = contentMetadata.get("episodeNumber").toString();
                    	     }

                        	     if(contentMetadata.has("genre")){
                                 	genre = contentMetadata.get("genre").toString();
                    	     }

                        	     if(contentMetadata.has("season")){
                                 	season = contentMetadata.get("season").toString();
                    	     }

                        	     if(contentMetadata.has("seriesTitle")){
                                     seriesTitle = contentMetadata.get("seriesTitle").toString();
                    	     }
                                if(contentMetadata.has("videoType")){

                                    videoType = contentMetadata.get("videoType").toString();

                                }

                                } catch (JSONException e) {
                        e.printStackTrace();
                        }

                                    if (assetIdCM != null) {
                            qbrStats.qubitData.get(recIndex).contentMetadata.assetId = assetIdCM;
                            eventStats.qubitData.get(recIndex).contentMetadata.assetId = assetIdCM;
                        }

                            if (assetNameCM != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.assetName = assetNameCM;
                            eventStats.qubitData.get(recIndex).contentMetadata.assetName = assetNameCM;
                        }

                            if (videoIdCM != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.videoId = videoIdCM;
                            eventStats.qubitData.get(recIndex).contentMetadata.videoId = videoIdCM;
                        }

                            if (contentType != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.contentType = contentType;
                            eventStats.qubitData.get(recIndex).contentMetadata.contentType = contentType;
                        }

                            if (drmProtection != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.drmProtection = drmProtection;
                            eventStats.qubitData.get(recIndex).contentMetadata.drmProtection = drmProtection;
                        }

                            if (episodeNumber != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.episodeNumber = episodeNumber;
                            eventStats.qubitData.get(recIndex).contentMetadata.episodeNumber = episodeNumber;
                        }

                            if (genre != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.genre = genre;
                            eventStats.qubitData.get(recIndex).contentMetadata.genre = genre;
                        }

                            if (season != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.season = season;
                            eventStats.qubitData.get(recIndex).contentMetadata.season = season;
                        }

                            if (seriesTitle != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.seriesTitle = seriesTitle;
                            eventStats.qubitData.get(recIndex).contentMetadata.seriesTitle = seriesTitle;
                        }
                        if (videoType != null){
                            qbrStats.qubitData.get(recIndex).contentMetadata.videoType = videoType;
                            eventStats.qubitData.get(recIndex).contentMetadata.videoType = videoType;
                        }

                    	}
            }


    private void addPlayerName(){
        try {
            String playerNameToReport = playerName;
            if (playerVersion != null){
                playerNameToReport += "_";
                playerNameToReport += playerVersion;
            }
            setStreamIDInfo(EPAttributes.PLAYERNAME, playerName);
        } catch (Exception e) {
            MMLogger.e("EPIntegration", " Could not addPlayerName" + e.getMessage());
        }
    }

    private void addDomainName(){
        if (domainName != null){
            setStreamIDInfo(EPAttributes.DOMAINNAME, domainName);
        }
    }

    public String getPlayerName(){
        return playerName;
    }

    public void setPlayerMode(String mode){
        playbackMode = mode;
        addPlayerMode(mode);
    }

    public void addPlayerMode(String mode){
        setStreamIDInfo(EPAttributes.PLAYERMODE, playbackMode);
    }

    public void setDuration(Long duration) {
        if(duration != null) {
            if (duration == -1) {
                qbrStats.qubitData.get(recIndex).streamID.isLive = true;
                eventStats.qubitData.get(recIndex).streamID.isLive = true;
                totalDur = -1;
            } else {
                totalDur = duration / 1000.0;
            }
            externalPresInfo_DurationMS = duration;
            externalPresInfo_isLive = (duration > 0)?false:true;
            Postman.instance().SetSessionInfo(externalPresInfo_DurationMS, externalPresInfo_isLive);
        }
        qbrStats.qubitData.get(recIndex).streamInfo.totalDuration = totalDur;
        eventStats.qubitData.get(recIndex).streamInfo.totalDuration = totalDur;
    }

    public void setPresentationLive(boolean live){
        if(live) {
            setDuration(-1L);
        }
    }

    public long serverlocalClockDiff(){
        return srvClkLclClkDiff;
    }

    private void setServerLocalClockDiff(long diff){
        srvClkLclClkDiff = diff;
    }

    public QBRXResInfo qbrXResInfo(){
        return xResInfo_;
    }

    public void setNetworkType(MMConnectionInfo networkType){
        String connType = networkType.toString();
        if (connType != null && connType.length() > 0) {
            connectionType = connType;
        }
        addNetworkType();
        if (networkType != MMConnectionInfo.Wifi){
            qbrStats.qubitData.get(recIndex).clientInfo.wifidatarate=null;
            eventStats.qubitData.get(recIndex).clientInfo.wifidatarate=null;
            qbrStats.qubitData.get(recIndex).clientInfo.signalstrength = null;
            eventStats.qubitData.get(recIndex).clientInfo.signalstrength = null;
            qbrStats.qubitData.get(recIndex).clientInfo.wifissid = null;
            eventStats.qubitData.get(recIndex).clientInfo.wifissid = null;
        }
    }

    private void addNetworkType(){
        if(connectionType != null) {

            if(connectionType.equals("NotReachable")){
                setClientInfo(EPAttributes.NETWORKTYPE, "");
             }
            else
            {
                setClientInfo(EPAttributes.NETWORKTYPE, connectionType);
            }
        }
    }
    
    private void stopStatsSender(){
        if(statsTimer != null) {
            try {
                statsSender.cancel();
                statsSender = null;

                statsTimer.purge();
                statsTimer.cancel();
                statsTimer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        statsMonitorStartedAtTS = 0L;
//        lastStatsPostedAtTS =0L;
    }

    private void startStatsSender() {
        if(statsTimer == null && (interval > 0)){
            try{
                statsTimer = new Timer();
                if(statsSender == null){
                    statsSender = new StatsSender(producerURL);
                }
                statsTimer.schedule(statsSender, interval * 1000, interval * 1000);
                statsMonitorStartedAtTS = timeNow();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
//        else{
//            lastStatsPostedAtTS = timeNow();
//        }
    }

    private void sendStatsNow() {
        try{
            MMLogger.v("EPIntegration", " Stats SendingNow");
            StatsSenderThread StatsSenderTh = new StatsSenderThread();
            StatsSenderTh.loadQubitStatsData(producerURL);
            StatsSenderTh.start();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLastQBRStatsSentTs(long timeStampInMs){
        lastStatsPostedAtTS = timeStampInMs;
    }

    public RecordStructure getQBRStats() {
        qbrStats.timestamp = timeNow();
        long timeDiffInterval = 0;
        if(lastStatsPostedAtTS <= 0){ //First stat
            timeDiffInterval = (statsMonitorStartedAtTS > 0)?((qbrStats.timestamp - statsMonitorStartedAtTS) + startOffsetForStats):0;
        }else{
            timeDiffInterval = (qbrStats.timestamp - lastStatsPostedAtTS);
        }
        if (pauseDurationToOffsetFromInterval > 0){
            if(timeDiffInterval - pauseDurationToOffsetFromInterval > 0){
                timeDiffInterval -= pauseDurationToOffsetFromInterval;
            }else{
                timeDiffInterval = 0;
            }
            pauseDurationToOffsetFromInterval = 0L;
        }
        qbrStats.interval = (int)((timeDiffInterval > 0)?(timeDiffInterval/1000):0);
        MMLogger.v("Postman", " Periodic || onDemand QOE Stats" + qbrStats.interval + " Sz:" + qbrStats.qubitData.size() + "lastStatsPostedAtTS" + lastStatsPostedAtTS + "TimeNow" + qbrStats.timestamp);
        if(qbrStats.interval < 1) {
            return null;
        }

        if(pbTime>0)
            setPBStats(EPAttributes.PBTIME,pbTime);
        //Set aggregated metrics so far like upshiftcount, downshiftcount etc.
        if(upShift>0)
            setPBStats(EPAttributes.UPSHIFTCOUNT,upShift);
        if(downShift>0)
            setPBStats(EPAttributes.DOWNSHIFTCOUNT,downShift);
        if(bwCount>0)
            setPBStats(EPAttributes.BANDWIDTH,(sumBandwidth * 1.0/BITSTOKILOBITS)/bwCount);
        if(qbrStats.qubitData.get(recIndex).segInfo.size() == 0 && prevSegInfo != null){
            prevSegInfo.timestamp = timeNow();
            if(downloadRate > 0){
                prevSegInfo.downloadRate = downloadRate;
            }
            if(bufferLength > 0){
                prevSegInfo.bufferLength = bufferLength;
            }
            qbrStats.qubitData.get(recIndex).segInfo.add(prevSegInfo);
        }
        return qbrStats;
    }

    public  RecordStructure getEventStats() {
        eventStats.timestamp = timeNow();
        return eventStats;
    }

    public void setPbTime(Double pbT) {
        if(pbT < 0) return;
        pbTime = pbT;
        if (needToSendComplete == false && pbT != playbackTimeForLastComplete){
            needToSendComplete = true;
        }
    }

    public void notifyError(String err) {
    }

    public void notifyAdStart(String ad) {
    }

    public void notifyAdEnd(String ad) {
    }

    private void addPbInfoToEventStats (PlaybackInfo pbInfo) {
        MMLogger.v(logTag,"Adding pbinfo to Eventstats with latency=" + pbInfo.latency)     ;
        eventStats.qubitData.get(recIndex).pbInfo.add(pbInfo);
        MMLogger.v(logTag,"Latency set ="+   eventStats.qubitData.get(recIndex).pbInfo.get(0).latency);
    }

    private void addPbInfo(PlaybackInfo pbInfo) {
        qbrStats.pbTime = pbTime;
        qbrStats.qubitData.get(recIndex).pbInfo.add(pbInfo);
    }

    public synchronized void purgePbInfo() {
        qbrStats.qubitData.get(recIndex).pbInfo.clear();
        upShift = downShift = 0;
        bwCount = 0;
        sumBandwidth = 0.0;
    }
    public synchronized void purgeEventPbInfo() {
        eventStats.qubitData.get(recIndex).pbInfo.clear();
    }

    public synchronized int getPlayDur() {
        long now = timeNow();
        if(pauseStartTime != null && pauseStartTime > 0){
            //We are in paused state, let us assume current time to be same when playback was PAUSED
            now = pauseStartTime;
        }
        if(epocTime != 0) {
            if(now > epocTime) {
                return (int)((now - epocTime)/1000);
            }
        }
        return 0;
    }

    public synchronized void increasePlayDur() {
        if(canSendProbes) {
            eventStats.playDur = qbrStats.playDur = getPlayDur();
        }
    }

    private void addSegInfo(SegmentInfo segmentInfo) {
        if(segmentInfo != null) {
            segmentInfo.downloadRate = downloadRate;
            segmentInfo.bufferLength = bufferLength;
            segmentInfo.timestamp = timeNow();
        }
        if(qbrStats != null && qbrStats.qubitData != null && qbrStats.qubitData.size() > 0) {
            qbrStats.qubitData.get(recIndex).segInfo.add(segmentInfo);
            prevSegInfo = segmentInfo;
        }
    }

    public void trackSegment(long startTimeMilliSec, long size, int bitrate, int height, int width, float fps, String codecs) {
        if (qbrSegInfoIntegrated == true){
            return;
        }
        if(prevBitrate != 0 && prevBitrate > bitrate) {
            downShift++;
        }
        if(prevBitrate != 0 && prevBitrate < bitrate) {
            upShift++;
        }
        prevBitrate = bitrate;
        SegmentInfo segmentInfo = new SegmentInfo();
        segmentInfo.cbrBitrate = segmentInfo.qbrBitrate = (long)bitrate;
        if(width > 0 && height > 0){
            segmentInfo.res = width + "x" + height;
        }

        segmentInfo.fps = (double)fps;
        segmentInfo.cbrSize = segmentInfo.qbrSize = (int)size;
        segmentInfo.vCodec = codecs;
        segmentInfo.startTime = (startTimeMilliSec>=0)?(startTimeMilliSec * 1.0)/1000:startTimeMilliSec;
        segmentInfo.dur = (segmentInfo.dur == null)?-1:segmentInfo.dur;
        Integer trackIdx = MMQFQubitEngine.getInstance().getStatisticsInterface().getTrackIndex(bitrate);
        if (trackIdx != null && trackIdx != -1) {
            Integer segmentIndex = MMQFQubitEngine.getInstance().getStatisticsInterface().getSequenceIndex(bitrate, startTimeMilliSec);
            if(segmentIndex == null || segmentIndex == -1) {
                MMLogger.v("MMSmartStreamingIntgr.Exception", "SegIdx " + segmentIndex + " Bitrate " + bitrate);
            }

            if (segmentIndex != null && segmentIndex != -1) {
                MMQFQubitStatisticsInterface.MMQFSegmentInfo cbrSegInfo = MMQFQubitEngine.getInstance().getStatisticsInterface().getSegmentInfoForSegment(trackIdx, segmentIndex);
                MMQFQubitStatisticsInterface.MMQFSegmentInfo qbrSegInfo = MMQFQubitEngine.getInstance().getStatisticsInterface().getQBRSegmentInfoForSegment(trackIdx, segmentIndex);

                if (cbrSegInfo != null && qbrSegInfo != null) {
                    segmentInfo.qbrBitrate = new Long(qbrSegInfo.trackBitrate);
                    segmentInfo.dur = (double)qbrSegInfo.duration/1000;
                    segmentInfo.qbrQual = qbrSegInfo.mosScore / 10.0;
                    segmentInfo.cbrQual = cbrSegInfo.mosScore / 10.0;
                    segmentInfo.qbrSize = (int) (qbrSegInfo.segSize /8); // To Bytes
                    segmentInfo.cbrSize = (int) (cbrSegInfo.segSize /8); // To Bytes
                    segmentInfo.downloadRate = downloadRate;
                    segmentInfo.startTime = (startTimeMilliSec * 1.0) / 1000;
                    segmentInfo.profileNum = qbrSegInfo.profileIdx;
                    segmentInfo.cbrProfileNum = cbrSegInfo.profileIdx;
                    segmentInfo.seqNum = (cbrSegInfo.segmentIndex != -1)?cbrSegInfo.segmentIndex:segmentIndex;
                    segmentInfo.qbrRes = qbrSegInfo.representationWidth + "x" + qbrSegInfo.representationHeight;
                    segmentInfo.res = cbrSegInfo.representationWidth + "x" + cbrSegInfo.representationHeight;

                } else {
                    MMLogger.v("MMSmartStreamingIntgr.Exception", " ----------- ");
                }
            }
        }

        prevSegInfo = segmentInfo;
        addSegInfo(segmentInfo);
    }

    public synchronized void purgeSegInfo() {
        qbrStats.qubitData.get(recIndex).segInfo.clear();
    }

    private void setStreamIDInfo(String property, Long value) {
        if(property.equals(EPAttributes.CUSTID)) {
            qbrStats.qubitData.get(recIndex).streamID.custId = value;
            eventStats.qubitData.get(recIndex).streamID.custId = value;
        }
    }

    private void setStreamIDInfo(String property, String value) {
        if(property.equals(EPAttributes.SUBSCRIBERID)) {
            qbrStats.qubitData.get(recIndex).streamID.subscriberId = value;
            eventStats.qubitData.get(recIndex).streamID.subscriberId = value;
        } else if(property.equals(EPAttributes.SUBSCRIBERTYPE)) {
            qbrStats.qubitData.get(recIndex).streamID.subscriberType = value;
            eventStats.qubitData.get(recIndex).streamID.subscriberType = value;
        } else if(property.equals(EPAttributes.SUBSCRIBERTAG)) {
            qbrStats.qubitData.get(recIndex).streamID.subscriberTag = value;
            eventStats.qubitData.get(recIndex).streamID.subscriberTag = value;
        } else if(property.equals(EPAttributes.PLAYERNAME)) {
            qbrStats.qubitData.get(recIndex).streamID.playerName = value;
            eventStats.qubitData.get(recIndex).streamID.playerName = value;
        } else if(property.equals(EPAttributes.PLAYERMODE)) {
            qbrStats.qubitData.get(recIndex).streamID.mode = value;
            eventStats.qubitData.get(recIndex).streamID.mode = value;
            xResInfo_.mode = value;
        } else if(property.equals(EPAttributes.ASSETID)) {
            qbrStats.qubitData.get(recIndex).streamID.assetId = value;
            eventStats.qubitData.get(recIndex).streamID.assetId = value;
        } else if(property.equals(EPAttributes.DOMAINNAME)) {
            qbrStats.qubitData.get(recIndex).streamID.domainName = value;
            eventStats.qubitData.get(recIndex).streamID.domainName = value;
        }
    }

    public void setLocation(Double longtd, Double lattd) {
        qbrStats.qubitData.get(recIndex).clientInfo.location = Double.toString(longtd) + "'" + Double.toString(lattd);
    }

    private void addClientInformation(){
        addDeviceInformation();
        addNetworkType();

        qbrStats.qubitData.get(recIndex).clientInfo.latitude = latitude;
        eventStats.qubitData.get(recIndex).clientInfo.latitude = latitude;

        if (latitude != null && longitude != null) {
            qbrStats.qubitData.get(recIndex).clientInfo.longitude = longitude;
            eventStats.qubitData.get(recIndex).clientInfo.longitude = longitude;
        }

        if (cellInfo != null) {
            qbrStats.qubitData.get(recIndex).clientInfo.cellInfo = cellInfo;
            eventStats.qubitData.get(recIndex).clientInfo.cellInfo = cellInfo;
        }
    }

    private void setClientInfo(String property, String value) {
        if(property.equals(EPAttributes.PLATFORM)) {
            qbrStats.qubitData.get(recIndex).clientInfo.platform=value;
            eventStats.qubitData.get(recIndex).clientInfo.platform=value;
        } else if(property.equals(EPAttributes.BRAND)) {
            qbrStats.qubitData.get(recIndex).clientInfo.brand=value;
            eventStats.qubitData.get(recIndex).clientInfo.brand=value;
        } else if(property.equals(EPAttributes.DEVVERSION)) {
            qbrStats.qubitData.get(recIndex).clientInfo.version=value;
            eventStats.qubitData.get(recIndex).clientInfo.version=value;
        } else if(property.equals(EPAttributes.MODEL)) {
            qbrStats.qubitData.get(recIndex).clientInfo.model=value;
            eventStats.qubitData.get(recIndex).clientInfo.model=value;
        } else if(property.equals(EPAttributes.NETWORKTYPE)) {
            qbrStats.qubitData.get(recIndex).clientInfo.nwType=value;
            eventStats.qubitData.get(recIndex).clientInfo.nwType=value;
        } else if(property.equals(EPAttributes.OPERATOR)) {
            qbrStats.qubitData.get(recIndex).clientInfo.operator=value;
            eventStats.qubitData.get(recIndex).clientInfo.operator=value;
        } else if(property.equals(EPAttributes.SCREENRES)) {
            qbrStats.qubitData.get(recIndex).clientInfo.scrnRes=value;
            eventStats.qubitData.get(recIndex).clientInfo.scrnRes=value;
        } else if (property.equals(EPAttributes.WIFIDATARATE)) {
            qbrStats.qubitData.get(recIndex).clientInfo.wifidatarate=value;
            eventStats.qubitData.get(recIndex).clientInfo.wifidatarate=value;
        } else if(property.equals(EPAttributes.WIFISIGNALSTRENGTH)){
            qbrStats.qubitData.get(recIndex).clientInfo.signalstrength = value;
            eventStats.qubitData.get(recIndex).clientInfo.signalstrength = value;
        } else if(property.equals(EPAttributes.WIFISSID)){
            qbrStats.qubitData.get(recIndex).clientInfo.wifissid = value;
            eventStats.qubitData.get(recIndex).clientInfo.wifissid = value;
        } else if(property.equals(EPAttributes.CDN)){
            qbrStats.qubitData.get(recIndex).clientInfo.cdn = value;
            eventStats.qubitData.get(recIndex).clientInfo.cdn = value;
        }
    }

    public void saveBandwidthSample(Double value){
        downloadRate = value; //bps
        sumBandwidth += value;
        bwCount = value == 0.0?bwCount:bwCount+1;
    }

    public void reportBufferLength(Long value){
        if(value >= 0) {
            bufferLength = value;
        }
    }

    public  void setEventPBStats(String property, Double value) {
        MMLogger.v(logTag, "setPBStats canSendProbes? " + canSendProbes);
        if(canSendProbes) {
            PlaybackInfo pbInfo = new PlaybackInfo();
            pbInfo.timestamp = timeNow();
            pbInfo.pbTime = pbTime;
            if (property.equals(EPAttributes.BUFFERING)) {
                pbInfo.buffWait = value;
                MMLogger.v(logTag,"Event || Evt - Metric => BufWait " + value);
            } if (property.equals(EPAttributes.SUMBUFFERING)) {
                pbInfo.sumBuffWait = value;
                MMLogger.v(logTag,"Event || Evt - Metric => Bandwidth " + value);
            }
            addPbInfoToEventStats(pbInfo);
        }
    }

    public  void setPBStats(String property, Double value) {
        MMLogger.v(logTag, "setPBStats canSendProbes? " + canSendProbes);
        if(canSendProbes) {
            PlaybackInfo pbInfo = new PlaybackInfo();
            pbInfo.timestamp = timeNow();
            pbInfo.pbTime = pbTime;
            if (property.equals(EPAttributes.BUFFERING)) {
                pbInfo.buffWait = value;
                MMLogger.v(logTag,"Stats || Evt - Metric => BufWait " + value);
            } if (property.equals(EPAttributes.BANDWIDTH)) {
                pbInfo.bwInUse = value;
                MMLogger.v(logTag,"Stats || Evt - Metric => Bandwidth " + value);
            }
            addPbInfo(pbInfo);
        }
    }
    
    public void setPBStats(String property, Integer value) {
        if(property.equals(EPAttributes.FRAMELOSS)) {
            PlaybackInfo pbInfo = new PlaybackInfo();
            pbInfo.frameloss = value;
            pbInfo.pbTime = pbTime;
            pbInfo.timestamp = timeNow();
            addPbInfo(pbInfo);
        } else if(property.equals(EPAttributes.UPSHIFTCOUNT)) {
            PlaybackInfo pbInfo = new PlaybackInfo();
            pbInfo.upShiftCount = value;
            pbInfo.pbTime = pbTime;
            pbInfo.timestamp = timeNow();
            addPbInfo(pbInfo);
        } else if(property.equals(EPAttributes.DOWNSHIFTCOUNT)) {
            PlaybackInfo pbInfo = new PlaybackInfo();
            pbInfo.downShiftCount = value;
            pbInfo.pbTime = pbTime;
            pbInfo.timestamp = timeNow();
            addPbInfo(pbInfo);
        }
    }
    
    public synchronized void stopMonitoring() {
        canSendProbes = false;
        stopStatsSender();
    }
    
    public synchronized void startMonitoring() {
        canSendProbes = true;
        startStatsSender();
    }

    private void notifyEvent(String event, String desc, String id, Double latency, Integer playbackTime) {
        PlaybackInfo pbInfo = new PlaybackInfo();
        pbInfo.timestamp = timeNow();
        if(latency >= 0){       // zero is valid value
            pbInfo.latency = latency;
        }
        if (!(event.startsWith("AD_"))) { // do not send adInfo with non-ad events
            if(buffWait >= 0) pbInfo.sumBuffWait = buffWait * 1.0;
            eventStats.qubitData.get(recIndex).adInfo = null;
        }
        else{
            eventStats.qubitData.get(recIndex).streamID.mode = null; // Skip Mode for Ads
            if(_adInformation._adResolution != null){
                eventStats.qubitData.get(recIndex).clientInfo.scrnRes = _adInformation._adResolution;
            }
            if((event.equals("AD_RESUMED") && adPauseDuration > 0)){
                pbInfo.pauseDuration = adPauseDuration;
            }
            if(adBuffWait >= 0) pbInfo.sumBuffWait = adBuffWait * 1.0;
        }

        pbInfo.pbTime = (playbackTime!= null)?playbackTime: pbTime;
        if(pbInfo.pbTime < 0){
            pbInfo.pbTime = 0.0;
        }
        CopyOnWriteArrayList<PlaybackInfo> pbInfoList = new CopyOnWriteArrayList<PlaybackInfo>();
        pbInfoList.add(pbInfo);
        sendEvent(event,desc,id,pbInfoList, pbInfo.pbTime);
    }
    
    private void notifyEvent(String event, String desc, String id, Double playbackTime) {
        String prevScrnResValue = eventStats.qubitData.get(recIndex).clientInfo.scrnRes;
        PlaybackInfo pbInfo = new PlaybackInfo();
        pbInfo.timestamp = timeNow();

        if (!(event.startsWith("AD_"))) { // do not send adInfo with non-ad events
            if(buffWait >= 0) pbInfo.sumBuffWait = buffWait * 1.0;
            eventStats.qubitData.get(recIndex).adInfo = null;
        }else{
            eventStats.qubitData.get(recIndex).streamID.mode = null; // Skip Mode for Ads
            if(_adInformation._adResolution != null){
                eventStats.qubitData.get(recIndex).clientInfo.scrnRes = _adInformation._adResolution;
            }
            if((event.equals("AD_RESUMED") && adPauseDuration > 0)){
                pbInfo.pauseDuration = adPauseDuration;
            }
            if(adBuffWait >= 0) pbInfo.sumBuffWait = adBuffWait * 1.0;
        }

        pbInfo.pbTime = (playbackTime!= null)?playbackTime: pbTime;
        if(pbInfo.pbTime < 0){
            pbInfo.pbTime = 0.0;
        }
        if((event.equals("RESUME") || event.equals("START")) && pauseDuration > 0 ) {
            pbInfo.pauseDuration = pauseDuration;
            pauseDurationToOffsetFromInterval = pauseDuration;
            MMLogger.v(logTag, "notifyEvent - Resume pauseDurationToOffsetFromInterval " + pauseDurationToOffsetFromInterval);
        }

        CopyOnWriteArrayList<PlaybackInfo> pbInfoList = new CopyOnWriteArrayList<PlaybackInfo>();
        pbInfoList.add(pbInfo);
        sendEvent(event,desc,id,pbInfoList, pbInfo.pbTime);
        eventStats.qubitData.get(recIndex).clientInfo.scrnRes = prevScrnResValue;
    }

    private void sendEvent(String event, String desc, String id, CopyOnWriteArrayList<PlaybackInfo> pbInfo, Double playbackTime) {
        RecordStructure eventStatsToSend = new RecordStructure();
        eventStatsToSend.qubitData = new CopyOnWriteArrayList<QBRMetric>();
        eventStatsToSend.timestamp = timeNow();
        eventStatsToSend.version = EPDataFormat.version;
        eventStatsToSend.pbTime = (playbackTime!= null)?playbackTime: pbTime;
        
        QBRMetric eventQbrMetric = new QBRMetric();
        eventQbrMetric.sdkInfo = new SDKInfo();
        eventQbrMetric.pbEventInfo = new PBEventInfo();
        eventQbrMetric.streamID = new StreamID();
        eventQbrMetric.streamInfo = new StreamInfo();
        
        eventQbrMetric.pbInfo = pbInfo;
        eventStatsToSend.qubitData.add(recIndex,eventQbrMetric);
        
        eventStatsToSend.playDur = getPlayDur();
        if ((event.startsWith("AD_"))) { // do not send adInfo with non-ad events
            eventStatsToSend.playDur = (int)(_adInformation._adPlayDur);
        }

        eventStatsToSend.interval = interval;

        setStreamInfo();
        //ag
        setContentMetadata(cmData);

        eventStatsToSend.qubitData.get(recIndex).adInfo = eventStats.qubitData.get(recIndex).adInfo;
        eventStatsToSend.qubitData.get(recIndex).streamID = eventStats.qubitData.get(recIndex).streamID;
        //ag
        eventStatsToSend.qubitData.get(recIndex).contentMetadata = eventStats.qubitData.get(recIndex).contentMetadata;
        eventStatsToSend.qubitData.get(recIndex).sdkInfo = eventStats.qubitData.get(recIndex).sdkInfo;
        eventStatsToSend.qubitData.get(recIndex).streamInfo = eventStats.qubitData.get(recIndex).streamInfo;
        eventStatsToSend.qubitData.get(recIndex).clientInfo = eventStats.qubitData.get(recIndex).clientInfo;
        eventStatsToSend.qubitData.get(recIndex).customTags = eventStats.qubitData.get(recIndex).customTags;

        eventStatsToSend.qubitData.get(recIndex).pbEventInfo.desc = desc;
        eventStatsToSend.qubitData.get(recIndex).pbEventInfo.event = event;
        eventStatsToSend.qubitData.get(recIndex).pbEventInfo.id = id;
        eventStatsToSend.qubitData.get(recIndex).pbEventInfo.pbTime = (playbackTime!= null)?playbackTime: pbTime;

        MMLogger.v(logTag, " Came to notifyEvent and it is sending event data... for event=" + eventStatsToSend.qubitData.get(recIndex).pbEventInfo.event);

        Postman.instance().Queue(eventStatsToSend);
    }

    public void setAssetId(String asset) {
        setStreamIDInfo(EPAttributes.ASSETID,asset);
    }

    public void onPlayerError(String exception, Long playbackTime) {
        MMLogger.v(logTag, "onPlayerError exception=" + exception);
        notifyEvent("ERROR", exception, "ERROR", (playbackTime != null )?(playbackTime * 1.0/1000):null);
    }
    
    public void updateLiveStreamingAttribute() {
        boolean isLive = false;
        if (totalDur <0 || MMQFQubitEngine.getInstance().isLiveStreaming() == true){
            isLive = true;
        }
        qbrStats.qubitData.get(recIndex).streamID.isLive = isLive;
        eventStats.qubitData.get(recIndex).streamID.isLive = isLive;
    }

    public void setDiagnostics() {
        qbrStats.qubitData.get(recIndex).diagnostics.sdkBootuptime = MMQFQubitEngine.getInstance().getSDKBootTime();
        eventStats.qubitData.get(recIndex).diagnostics.sdkBootuptime = MMQFQubitEngine.getInstance().getSDKBootTime();
    }

    public void setSDKInfo() {
        qbrStats.qubitData.get(recIndex).sdkInfo.sdkVersion = MMQFQubitEngine.getInstance().getSDKVersion();
        qbrStats.qubitData.get(recIndex).sdkInfo.hFileVersion = MMQFQubitEngine.getInstance().getMetaDataVersion();

        eventStats.qubitData.get(recIndex).sdkInfo.sdkVersion = MMQFQubitEngine.getInstance().getSDKVersion();
        eventStats.qubitData.get(recIndex).sdkInfo.hFileVersion = MMQFQubitEngine.getInstance().getMetaDataVersion();
    }

    private void setStreamInfo() {
        qbrStats.qubitData.get(recIndex).streamInfo.maxFps = getMaxFPS();
        qbrStats.qubitData.get(recIndex).streamInfo.minFps = getMinFPS();
        qbrStats.qubitData.get(recIndex).streamInfo.maxRes = getMaxResStr();
        qbrStats.qubitData.get(recIndex).streamInfo.minRes = getMinResStr();
        
        qbrStats.qubitData.get(recIndex).streamInfo.numOfProfile = getProfileCnt();

        eventStats.qubitData.get(recIndex).streamInfo.maxFps = getMaxFPS();
        eventStats.qubitData.get(recIndex).streamInfo.minFps = getMinFPS();
        eventStats.qubitData.get(recIndex).streamInfo.maxRes = getMaxResStr();
        eventStats.qubitData.get(recIndex).streamInfo.minRes = getMinResStr();

        eventStats.qubitData.get(recIndex).streamInfo.numOfProfile = getProfileCnt();

        qbrStats.qubitData.get(recIndex).streamInfo.streamFormat = getStreamFormat();
        eventStats.qubitData.get(recIndex).streamInfo.streamFormat = getStreamFormat();

        //live, and total dur set in the postman itself.
    }
    
    public void onSegRequest(String url, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segInfo) {
        if(segInfo != null && segInfo.segmentIndex > 0){
            MMLogger.v("QOE", "Made request for Seg " + segInfo.segmentIndex);
        }
        MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo segmentSizeInfo = MMQFQubitEngine.getInstance().getStatisticsInterface().getSegmentSizeInfo(url, segInfo);
        MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo segmentQualityInfo = MMQFQubitEngine.getInstance().getStatisticsInterface().getSegmentQualityInfo(url, segInfo);
        if(segmentQualityInfo != null && segmentSizeInfo != null) {
            MMLogger.v("EP", "Came to onSegRequest and url=" + url + " isQubitInitialized=" + MMQFQubitEngine.getInstance().isQubitInitialized() + " segmentDurtion=" + segmentSizeInfo.segmentDuration);
        }
        
        if(segmentQualityInfo != null && segmentSizeInfo != null && segmentSizeInfo.segmentDuration > 0) {
            setStreamInfo();
            
            SegmentInfo segmentInfo = new SegmentInfo();
            segmentInfo.timestamp = timeNow();
            
            segmentInfo.profileNum = segInfo.qbrTrackIndex;
            segmentInfo.cbrProfileNum = segInfo.cbrTrackIndex;
            MMLogger.v(logTag,"profileId i.e profileNum for the played segment="+segmentQualityInfo.profileId);
            segmentInfo.qbrQual = segmentQualityInfo.qubitizedSegmentQuality/10.0;
            if(segmentInfo.qbrQual <= 0){
                segmentInfo.qbrQual = 0.0;
            }

            segmentInfo.cbrQual = segmentQualityInfo.requestedSegmentQuality/10.0;
            if(segmentInfo.cbrQual <= 0){
                segmentInfo.cbrQual = 0.0;
            }

            segmentInfo.cbrSize = segmentSizeInfo.requestedSegmentSz / 8; // To Bytes
            if(segmentInfo.cbrSize <= 0){
                segmentInfo.cbrSize = null;
            }

            segmentInfo.qbrSize = segmentSizeInfo.qubitizedSegmentSz / 8; // To Bytes
            if(segmentInfo.qbrSize <= 0){
                segmentInfo.qbrSize = null;
            }

            segmentInfo.aCodec = MMQFQubitEngine.getInstance().getStatisticsInterface().getAudioCodecInfo();
            segmentInfo.vCodec = MMQFQubitEngine.getInstance().getStatisticsInterface().getVideoCodecInfo();

            Long scaledDuration = (segmentSizeInfo.segmentDuration/segmentSizeInfo.timescale);

            if(scaledDuration == 0) {
                MMLogger.v("EP","onSegRequest QubitURL: "+url);
                scaledDuration = 1L;
            }
            
            segmentInfo.dur = scaledDuration*1.0;
            segmentInfo.startTime = segmentSizeInfo.segmentStartTime/(segmentSizeInfo.timescale*1.0);
            if (segmentInfo.startTime < 0){
                segmentInfo.startTime = null;
            }
            segmentInfo.seqNum = segInfo.segmentIndex;
            segmentInfo.fps = MMQFQubitEngine.getInstance().getFrameRate();
            segmentInfo.cbrBitrate = segmentSizeInfo.cbrBitrate; //segmentSizeInfo.requestedSegmentSz/scaledDuration;
            segmentInfo.qbrBitrate = segmentSizeInfo.qbrBitrate; //segmentSizeInfo.qubitizedSegmentSz/scaledDuration;
            if (segmentInfo.qbrBitrate < 0){
                segmentInfo.qbrBitrate = null;
            }

            if(segmentQualityInfo.width > 0 && segmentQualityInfo.height > 0){
                segmentInfo.res = Integer.valueOf(segmentQualityInfo.width).toString()+"x"+Integer.valueOf(segmentQualityInfo.height).toString();
            }

            if(segInfo.qbrVideoTrackInfo!= null && segInfo.qbrVideoTrackInfo.width >0 && segInfo.qbrVideoTrackInfo.height >0){
                segmentInfo.qbrRes = segInfo.qbrVideoTrackInfo.width + "x" + segInfo.qbrVideoTrackInfo.height;
            }

            addSegInfo(segmentInfo);
            qbrSegInfoIntegrated = true;
            //upshift and downshift
            if(prevBitrateL != 0 && prevBitrateL > segmentInfo.qbrBitrate) {
                downShift++;
            }
            if(prevBitrateL != 0 && prevBitrateL < segmentInfo.qbrBitrate) {
                upShift++;
            }
            prevBitrateL = segmentInfo.qbrBitrate;
        }
        else if (segInfo != null){
            // QBRDisabled Case
            setStreamInfo();
            SegmentInfo segmentInfo = new SegmentInfo();
            segmentInfo.timestamp = timeNow();

            segmentInfo.cbrProfileNum = segmentInfo.profileNum = segInfo.videoTrackInfo.trackIndex;
            segmentInfo.qbrQual = segmentInfo.cbrQual = 0.0;

            segmentInfo.cbrSize = -1;
            if(segmentInfo.cbrSize <= 0){
                segmentInfo.cbrSize = null;
            }

            segmentInfo.qbrSize = -1;
            if(segmentInfo.qbrSize <= 0){
                segmentInfo.qbrSize = null;
            }

            String[] codecs= segInfo.videoTrackInfo.codecInfo.split(",");
            if(codecs.length > 1) {
                segmentInfo.vCodec = (codecs[0].contains("avc"))?codecs[0]:codecs[1];
            }else if(codecs.length == 1){
                segmentInfo.vCodec = (codecs[0].contains("avc"))?codecs[0]:null;
            }
            segmentInfo.aCodec = null;
            segmentInfo.dur = -1.0;
            segmentInfo.startTime = null;
            segmentInfo.seqNum = segInfo.segmentIndex;
            segmentInfo.fps = segInfo.videoTrackInfo.fps;
            segmentInfo.cbrBitrate = Long.valueOf(segInfo.videoTrackInfo.bitrate);
            segmentInfo.qbrBitrate = Long.valueOf(segInfo.videoTrackInfo.bitrate);
            if (segmentInfo.qbrBitrate < 0){
                segmentInfo.qbrBitrate = null;
            }

            if(segInfo.videoTrackInfo.width > 0 && segInfo.videoTrackInfo.height > 0){
                segmentInfo.res = Integer.valueOf(segInfo.videoTrackInfo.width).toString()+"x"+Integer.valueOf(segInfo.videoTrackInfo.height).toString();
            }

            if(segInfo.qbrVideoTrackInfo!= null && segInfo.qbrVideoTrackInfo.width >0 && segInfo.qbrVideoTrackInfo.height >0){
                segmentInfo.qbrRes = segInfo.qbrVideoTrackInfo.width + "x" + segInfo.qbrVideoTrackInfo.height;
            }

            addSegInfo(segmentInfo);
            qbrSegInfoIntegrated = true;
            //upshift and downshift
            if(prevBitrateL != 0 && prevBitrateL > segmentInfo.qbrBitrate) {
                downShift++;
            }
            if(prevBitrateL != 0 && prevBitrateL < segmentInfo.qbrBitrate) {
                upShift++;
            }
            prevBitrateL = segmentInfo.qbrBitrate;

        }
        if(!integrationEnabled)
            startMonitoring();
    }

    public void reportUserInitiatedPlayback(){
        loadStartTime = timeNow();
//        eventStats.qubitData.get(recIndex).streamID.isLive = false;
       // notifyEvent("ONLOAD", "Player loading", "ONLOAD", null);
       // onloadSent = true;
        if(!onloadSent){
                        notifyEvent("ONLOAD", "Player loading", "ONLOAD", null);
                        onloadSent = true;
                        startSent = false;
            	}

        if (prevState != null){
            Log.e(logTag, "Unexpected State Transition - reportUserInitiatedPlayback initiated when the previous state is -" + prevState.toString() + "Start Sent ? " + startSent);
        }
       // startSent = false;
    }

    public void reportMetricValue(MMOverridableMetric metric, String value){
        switch (metric){
            case Latency:{
                if (startSent){
                    Log.e(logTag, "reportMetricValue for Latency in unsupported state, latency value already communicated to backend.");
                }
                externalSetLatency = value;
            }
            break;
            case ServerAddress:{
                updateCDN(value);
            }
            break;
            case DurationWatched:{
                Log.e(logTag, "reportMetricValue - Unsupported Overridable Metric for Java SDK.");
            }
            break;
        }
    }


    void updateHostIP(String domain)
    {

        if(domain != null)
        {
            new Thread(new Runnable(){
            @Override
            public void run() {
            try {
                InetAddress address = InetAddress.getByName(domain);
                String hostAddress = address.getHostAddress();
                if(eventStats.qubitData.get(recIndex).clientInfo.cdn == "" && qbrStats.qubitData.get(recIndex).clientInfo.cdn == ""){
		    updateCDN(hostAddress);
		}    
            } catch (UnknownHostException e) {
              e.printStackTrace();
            }
          }
        }).start();

        }
    }

    static String extractDomainName(String url)
    {
        String domainName = null;

        if(url != null)
        {
            int start = url.indexOf("://");
            if (start < 0)
            {
                start = 0;
            }
            else
            {
                start += 3;
            }

            int end = url.indexOf('/', start);
            if (end < 0)
            {
                end = url.length();
            }

            domainName = url.substring(start, end);

            int port = domainName.indexOf(':');
            if (port >= 0)
            {
                domainName = domainName.substring(0, port);
            }
        }
        return domainName;
     }


    public void onPlayerStateChanged(MMPlayerState state) {
        MMLogger.v(logTag, "onPlayerStateChanged - " + state + " ts= " + timeNow());
        if (sessionInitizationFailed){
            Log.e(logTag, "onPlayerStateChanged noop. Session Initialzation Failed.");
            return;
        }
        boolean updateValidState = true;
        try {
            integrationEnabled = true;
            switch (state){
                case PLAYING:{
                    doneWithSession = false;
                    if (onloadSent && !startSent){
                        loadTime = timeNow() - loadStartTime;



                        if (loadStartTime == -1){
                            Log.e(logTag, "loadSTartTime is -1, please ensure transition to PLAYING should occur after the user requests for playback");
                        }
                        if (externalSetLatency != null){
                            try {
                                Long latencyExternallySet = Long.parseLong(externalSetLatency);
                                Log.i(logTag, "Overriding Latency of " + loadTime + " with externally set latency value of " + externalSetLatency);
                                loadTime = latencyExternallySet;
                            }catch(Exception e){
                                Log.i(logTag, "Ignoring externally set Latency of " + externalSetLatency + " It is expected to be the string representation of latency(in long) in milliseconds");
                            }
                        }                       
                        
                        statsSendingPrepared = true;
                        if (canSendProbes == true && producerURL != null  && producerURL.length() > 0) { // We are initialized
                            //startStatsSender();
                        }else{
                            Log.d(logTag, "Stat Sender tasks not initiated, as init not completed yet.");
                        }
                        startSent = true;
                        epocTime = timeNow();
                        startPayloadSentTime = epocTime;
                        if(prevState == MMPlayerState.PAUSED && pauseDuration > 0){
                            timeOnPlayingStateTransition = timeNow();
                            if (pauseStartTime > 0) {
                                pauseDuration = timeNow() - pauseStartTime;
                                MMLogger.v(logTag, "notifyEvent - PLAYING PauseStartTime " + pauseStartTime + " pauseDuration " + pauseDuration);
                                pauseStartTime = -1L;
                            }
                            epocTime += pauseDuration;
                            loadTime -= pauseDuration;
                            if(loadTime < 0){
                                MMLogger.w(logTag, "latency was negative, setting it to 0 " + "pauseDuration  " + pauseDuration + " loadStartTime  " + loadStartTime + " now " + timeNow());
                                loadTime = 0;
                            }
                        }

                        notifyEvent("START", "Playback attempted to start", "START",
                                loadTime * 1.0, null);
                        if(isStartAfterAd) {
                            notifyEvent("START_AFTER_AD", "Playback attempted to start",
                                "START_AFTER_AD",loadTime * 1.0, null);
                        }

                        startMonitoring();
                        pbTimeBeforeAdStart = pbTime;
                    }else if(!startSent && !onloadSent){
                        Log.e(logTag, "onPlayerStateChanged - Invalid State Transition, in PLAYING state, without user requested for initialisation of session.");
                    }else if(prevState == MMPlayerState.STOPPED){
                        Log.i(logTag, "Automatic initialisation of new session");

                        //Copy session params
                        Long copyCustomerID = custId;
                        Integer copyInterval = interval;
                        boolean postmanWasStarted = Postman.instance().isStarted();
                        String copyProducerURL = producerURL;
                        String copyABRURL = abrURL;
                        String copyMode = xResInfo_.mode;
                        Double copyCFVal = xResInfo_.cfVal;
                        Double copyTotalDuration = totalDur;
                        Integer copyTelephonyFetchInterval = telephonyFetchInterval;
                        MMCellInfo copyCellInfo = cellInfo;

                        Double copyLatitude = latitude;
                        Double copyLongitude = longitude;

                        Integer copyMaxStepUp = xResInfo_.maxStepsUp;
                        Integer copyMaxStepsDown = xResInfo_.maxStepsDown;
                        boolean isLive = qbrStats.qubitData.get(recIndex).streamID.isLive;

                        //Asset Information
                        String copyAssetID = assetId;
                        String copyAssetName = assetName;

                        Map<String, String> copyCustomKvps = customKvps;

                        Long copy_externalPresInfo_DurationMS = externalPresInfo_DurationMS;
                        Boolean copy_externalPresInfo_isLive = externalPresInfo_isLive;
                        String copy_qbrModeFinalized = qbrModeFinalized;

                        Long copy_internalPresInfo_DurationMS = internalPresInfo_DurationMS;
                        Boolean copy_internalPresInfo_isLive = internalPresInfo_isLive;

                        Double copymaxFPS = maxFPS;
                        Double copyminFPS = minFPS;
                        String copymaxResStr = maxResStr;
                        String copyminResStr = minResStr;
                        String copyStreamFormat = streamFormat;
                        Integer copyprofileCnt = profileCnt;

                        Double copyexternal_maxFPS = external_maxFPS;
                        Double copyexternal_minFPS = external_minFPS;
                        String copyexternal_maxResStr = external_maxResStr;
                        String copyexternal_minResStr = external_minResStr;
                        String copyexternal_streamFormat = external_streamFormat;
                        Integer copyexternal_profileCnt = external_profileCnt;

                        long clockDiff = serverlocalClockDiff();


                        //Perform cleanup for session
                        cleanupEventStructures(abrURL, custId);
                        assetId = copyAssetID;
                        assetName = copyAssetName;
                        customKvps = copyCustomKvps;

                        externalPresInfo_DurationMS = copy_externalPresInfo_DurationMS;
                        externalPresInfo_isLive = copy_externalPresInfo_isLive;
                        internalPresInfo_DurationMS =  copy_internalPresInfo_DurationMS;
                        internalPresInfo_isLive = copy_internalPresInfo_isLive;
                        qbrModeFinalized = copy_qbrModeFinalized;

                        maxFPS = copymaxFPS;
                        minFPS = copyminFPS;
                        maxResStr = copymaxResStr;
                        minResStr = copyminResStr;
                        streamFormat = copyStreamFormat;
                        profileCnt = copyprofileCnt;

                        external_maxFPS = copyexternal_maxFPS;
                        external_minFPS = copyexternal_minFPS;
                        external_maxResStr = copyexternal_maxResStr;
                        external_minResStr = copyexternal_minResStr;
                        external_streamFormat = copyexternal_streamFormat;
                        external_profileCnt = copyexternal_profileCnt;

                        Postman.instance().SetSessionInfo((externalPresInfo_DurationMS!= null)?externalPresInfo_DurationMS:internalPresInfo_DurationMS, (externalPresInfo_isLive!=null)?externalPresInfo_isLive:internalPresInfo_isLive);

                        xResInfo_.Reset();
                        totalDur = 0;
                        eventStats.playDur = 0;
                        qbrStats.playDur = 0;
                        totalDur = copyTotalDuration;
                        sessionInitizationFailed = false;
                        addStreamID(abrURL, copyMode, isLive);
                        addClientInformation();
                        setStreamInfo();
                        setSDKInfo();

                        reportUserInitiatedPlayback();
                        if(copyProducerURL != null) {
                            initialize(copyCustomerID, copyInterval, copyProducerURL, copyABRURL, -1L, copyMode, copyCFVal, copyTelephonyFetchInterval, copyMaxStepUp, copyMaxStepsDown);
                            setServerLocalClockDiff(clockDiff);
                            if(postmanWasStarted) {
                                Postman.instance().SetQBRMode(qbrModeFinalized);
                                Postman.instance().SetStreamInfo(getStreamFormat(), getProfileCnt(), getMinResStr(), getMaxResStr(), getMinFPS(), getMaxFPS());
                                Postman.instance().Start();
                            }
                        }
                        latitude = copyLatitude;
                        longitude = copyLongitude;
                        cellInfo = copyCellInfo;



                        onPlayerStateChanged(MMPlayerState.PLAYING);
                        startMonitoring();
                        pbTimeBeforeAdStart = pbTime;
                    }
                    else if(prevState == MMPlayerState.PAUSED){
                        startMonitoring();
                        timeOnPlayingStateTransition = timeNow();
                        if (pauseStartTime > 0) {
                            pauseDuration = timeNow() - pauseStartTime;
                            pauseStartTime = -1L;
                        }
                        epocTime += pauseDuration;
                        notifyEvent("RESUME", "Playback resumed", "RESUME", null);
                        pbTimeBeforeAdStart = pbTime;
                    }
                }
                break;
                case PAUSED:{
                    pbTimeBeforeAdStart = pbTime;
                    if (startSent && prevState == MMPlayerState.PLAYING) {
                        pauseStartTime = timeNow();
                        sendStatsNow();
                        notifyEvent("PAUSE", "Playback Paused", "PAUSE", null);
                        stopMonitoring();
                    }
                    else {
                        updateValidState = false;
                    }            
                }
                break;
                case STOPPED:{
                    if(prevState != null && prevState != MMPlayerState.STOPPED){
                        if(startSent) {
                            MMLogger.d("MMSmartStreamingIntgr.Events", "STOPPED");
                            if(buffWait < 0) buffWait = 0;
                            buffWait += (loadTime * 1.0); //sumbuffwait include with latency
                            if (needToSendComplete) {
                                if (prevState == MMPlayerState.PAUSED){
                                    if (pauseStartTime > 0) {
                                        pauseDuration = timeNow() - pauseStartTime;
                                        pauseDurationToOffsetFromInterval = pauseDuration;
                                        MMLogger.v(logTag, "notifyEvent - Stopped PauseStartTime " + pauseStartTime + " pauseDurationToOffsetFromInterval " + pauseDurationToOffsetFromInterval);
                                        //pauseStartTime = -1L;
                                    }
                                }
                                sendStatsNow();
                                if (totalDur > 0 && ((pbTime - (95 * totalDur / 100) >= 0) || (Math.abs(pbTime - totalDur) < 2)))    //if 95% of video has been watched the
                                {
                                    notifyEvent("COMPLETE", "Playback ended", "COMPLETE", null);
                                    playbackTimeForLastComplete = pbTime;
                                    needToSendComplete = false;
                                } else {
                                    notifyEvent("ENDED", "Playback ended", "ENDED", null);
                                }
                            } else {
                                MMLogger.e("MMSmartStreamingIntgr.Events", "STOPPED skipped Unknown State ?=>" + needToSendComplete + "pbTime " + pbTime);
                                return;
                            }
                            //TBD, create new session, for seek back and reset metrics
                            pauseStartTime = -1L;
                            stopMonitoring();
                            doneWithSession = true;
                            isStartAfterAd = false;
                            //resetSessionParams();
                        }else{
                            stopMonitoring();
                            doneWithSession = true;
                            onloadSent = false;
                            isStartAfterAd = false;
                            MMLogger.d("MMSmartStreamingIntgr.Events","Start Not Sent, Ignore sending STOPPED/COMPLETED");
                        }
                    }
                    else {
                        if(prevState != null) {//STOP on STOP
                            updateValidState = false;
                        }
                        stopMonitoring();
                        doneWithSession = true;
                        onloadSent = false;
                        isStartAfterAd = false;
                        MMLogger.d("MMSmartStreamingIntgr.Events","Already in STOPPED State");
                    } 
                }
                break;
                default: MMLogger.e("EP","Unknown State"); return;
            }
            if(updateValidState){
                prev2PrevState = prevState;
                prevState = state;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onBufferingBegin() {
        if (!startSent || (prevState != MMPlayerState.PLAYING)){
            return;
        }
        buffStarTime = timeNow();
        notifyEvent("BUFFERING","Buffering started","BUFFERING", null);
    }

    public void onBufferingEnd() {
        if (!startSent || (prevState != MMPlayerState.PLAYING)){
            return;
        }
        
        if (buffStarTime == -1){
            //Pick last set play time as the buffering start time
            buffStarTime = timeOnPlayingStateTransition;
        }

        if (buffStarTime != -1) {
            Long buffWt = timeNow() - buffStarTime;
            buffWait += buffWt;
            setPBStats(EPAttributes.BUFFERING, buffWt * 1.0);
            setPBStats(EPAttributes.SUMBUFFERING, buffWait * 1.0);
            buffStarTime = -1;
            MMLogger.v(logTag, "on-BufferingEnd - .Buffering for " + buffWt + "millisec of total buffering " + buffWait);
        }
    }

    public void reportABRSwitch(Integer prevBitrate, Integer newBitrate){

    }

    public void reportWifiDataRate(Integer dataRate){
        wifiDataRate = dataRate.toString();
        addWiFiDataRate();
    }

    public void reportWifiSignalStrengthPercentage(Double strength){
        wifiSignalStrength = strength.toString();
        addWiFiSignalStrength();
    }

    public void reportWifiSSID(String ssid){
        wifiSSID = ssid.toString();
        addWiFiSSID();
    }

    private void addWiFiSSID(){
        if (wifiSSID != null){
           setClientInfo(EPAttributes.WIFISSID, wifiSSID);
        }
    }

    private void addWiFiDataRate(){
        if (wifiDataRate != null){
            setClientInfo(EPAttributes.WIFIDATARATE, wifiDataRate);
        }
    }

    private void addWiFiSignalStrength(){
        if (wifiSignalStrength != null){
            setClientInfo(EPAttributes.WIFISIGNALSTRENGTH, wifiSignalStrength);
        }
    }

    private Double millisecToSec(long millisec){
        return ((millisec * 1.0)/1000);
    }

    private void resetAdInfoIfInitialState(MMAdState newState){
        if(_adInformation == null){
            _adInformation = new MMAdvertisentInformation();
        }
        if(newState == MMAdState.AD_REQUEST || _adInformation._prevAdState == null){//VAST only
            _adInformation.Reset();
            adLoadStartTime = adPauseStartTime = adPauseDuration = adLoadTime = 0L;
            adBuffWaitForInterval = adBuffWait = 0;
        }else{
            /*
            if(newState == MMAdState.AD_STARTED && _adInformation._prevAdState != MMAdState.AD_REQUEST){ //VPAID Only
                _adInformation.Reset();
            }
            */
            if(newState == MMAdState.AD_IMPRESSION && (_adInformation._prevAdState != MMAdState.AD_STARTED &&
                    _adInformation._prevAdState != MMAdState.AD_REQUEST &&
                    _adInformation._prevAdState != MMAdState.AD_IMPRESSION)){

                _adInformation.ResetForNewAd();
                adLoadStartTime = adPauseStartTime = adPauseDuration = adLoadTime = 0L;
                adBuffWaitForInterval = adBuffWait = 0;
            }
        }
        double playBackPos = Math.ceil(pbTime + 3); // +3 why ??
        if(_adInformation._adPosition == null || _adInformation._adPosition.isEmpty()) {
            if (startSent == false || pbTime <= 1) {
                _adInformation._adPosition = "pre";
            } else if ( (prevState != null && prevState != MMPlayerState.STOPPED && (pbTime > 0 &&  playBackPos < totalDur)) || totalDur==-1) {
                _adInformation._adPosition = "mid";
            } else if (prevState != null && prevState == MMPlayerState.STOPPED || (pbTime > 0
                && playBackPos >= totalDur)) {
                _adInformation._adPosition = "post";
            }
        }

        _adInformation._prevAdState = newState;
    }

    private void populateAdRelatedInfo(long timeNow){
        if(this.eventStats.qubitData.get(recIndex).adInfo == null) {
            this.eventStats.qubitData.get(recIndex).adInfo = new AdInfo();
        }

        if(_adInformation == null) return;

        if(_adInformation._adClient != null){
            this.eventStats.qubitData.get(recIndex).adInfo.adClient = _adInformation._adClient;
        }

        if(_adInformation._adId != null && !_adInformation._adId.isEmpty()){
            this.eventStats.qubitData.get(recIndex).adInfo.adId = _adInformation._adId;
        }

        if(_adInformation._adDuration != -1){
            this.eventStats.qubitData.get(recIndex).adInfo.adDuration = millisecToSec(_adInformation._adDuration);
        }

        if(_adInformation._adPosition != null && !_adInformation._adPosition.isEmpty()){
            this.eventStats.qubitData.get(recIndex).adInfo.adPosition = _adInformation._adPosition;
        }

        if(_adInformation._adType == MMAdType.AD_LINEAR){
            this.eventStats.qubitData.get(recIndex).adInfo.adLinear = "linear";
        }else{
            this.eventStats.qubitData.get(recIndex).adInfo.adLinear = null;
        }

//        if(_adInformation._adCreatureType != null){
//            this.eventStats.qubitData.get(recIndex).adInfo.adCreative = _adInformation._adCreatureType;
//        }
        if(_adInformation._adCreativeType != null){
                        this.eventStats.qubitData.get(recIndex).adInfo.adCreative = _adInformation._adCreativeType;
        }

        if(_adInformation._adServer != null){
            this.eventStats.qubitData.get(recIndex).adInfo.adSystem = _adInformation._adServer;
        }
        //ag
        if(_adInformation._adResolution != null){
                        this.eventStats.qubitData.get(recIndex).adInfo.adResolution = _adInformation._adResolution;
                    }

                        if(_adInformation._adCreativeId != null){
                        this.eventStats.qubitData.get(recIndex).adInfo.adCreativeId = _adInformation._adCreativeId;
                    }

                        if(_adInformation._adUrl != null){
                        this.eventStats.qubitData.get(recIndex).adInfo.adUrl = _adInformation._adUrl;
                    }

                        if(_adInformation._adTitle != null){
                       this.eventStats.qubitData.get(recIndex).adInfo.adTitle = _adInformation._adTitle;
                    }

        if(_adInformation._adPodLength > 0){
            this.eventStats.qubitData.get(recIndex).adInfo.adPodLength = _adInformation._adPodLength;
        }
        //ag
        if(_adInformation._adBitrate > 0){
                       this.eventStats.qubitData.get(recIndex).adInfo.adBitrate = _adInformation._adBitrate;
                    }

        if(_adInformation._adPodIndex >= -1){
            this.eventStats.qubitData.get(recIndex).adInfo.adPodIndex = _adInformation._adPodIndex;
        }

        if(_adInformation._adPositionInPod > 0){
            this.eventStats.qubitData.get(recIndex).adInfo.adPodPosition = _adInformation._adPositionInPod;
        }

        this.eventStats.qubitData.get(recIndex).adInfo.isBumper = _adInformation._isBumper;

        //if(_adInformation._adLastAdEventTimeStamp != -1){
            this.eventStats.qubitData.get(recIndex).adInfo.adInterval = getAdInterval(timeNow);
        //}
    }

    private Double getAdInterval(Long timeNow){
      Double adIntervalInMS = 0.0;
      if(_adInformation != null && _adInformation._adLastAdEventTimeStamp > 0) {
        if(adPauseStartTime != null && adPauseStartTime > 0){
            //We are in paused state, let us assume current time to be same when playback was PAUSED
            timeNow = adPauseStartTime;
        }
        adIntervalInMS = millisecToSec(timeNow - _adInformation._adLastAdEventTimeStamp);
      }
      if(adPauseDurationToOffsetFromInterval  > 0) {
        adIntervalInMS = adIntervalInMS - (adPauseDurationToOffsetFromInterval/1000);
        adPauseDurationToOffsetFromInterval = 0L;
      }
      if(adBuffWaitForInterval > 0){
        adIntervalInMS = adIntervalInMS - (adBuffWaitForInterval / 1000);
          _adInformation._adPlayDur += (adBuffWaitForInterval / 1000);
          adBuffWaitForInterval = 0L;
      }

      if(adIntervalInMS < 0){
        adIntervalInMS = 0.0;
      }
      _adInformation._adPlayDur += adIntervalInMS;
      return adIntervalInMS;
    }

    private void sendAdPlayingEvent(long timeNow, boolean force){
        if(_adInformation != null && _adInformation._playSent && _adInformation._prevAdState != MMAdState.AD_PAUSED){
            long elapsed = (timeNow - this._adInformation._adPlaybackStartWallClockTime)/1000;
            if(force || ((elapsed > 0) && (elapsed % (_KAdKeepAliveInterval/1000)) == 0)){
                this._adInformation._adPlaybackStartWallClockTime = timeNow;
                populateAdRelatedInfo(timeNow);
                this.notifyEvent("AD_PLAYING", "AD KEEP ALIVE", "AD_PLAYING", millisecToSec(this._adInformation._adPlaybackTime ));
                this._adInformation._adLastPlayingEventTimeStamp = timeNow;
                this._adInformation._adLastAdEventTimeStamp = timeNow();
            }
        }
    }

    public void reportAdState(MMAdState adState){

        MMAdStateInternal internalState = getInternalAdState(adState);
        long now = timeNow();

        if(_adInformation == null){
            _adInformation = new MMAdvertisentInformation();
        }

        if(_adInformation._prevAdState == MMAdState.AD_COMPLETED || _adInformation._prevAdState == MMAdState.AD_ENDED){
            if(adState != MMAdState.AD_REQUEST && adState != MMAdState.AD_IMPRESSION){
                return;     // INVALID STATE  (Expected Request or Impression)
            }
        }

        resetAdInfoIfInitialState(adState);
        populateAdRelatedInfo(now);

        // Stream Latency Calculation from now to start
        if (onloadSent && !startSent && (internalState != MMAdStateInternal.AD_REQUEST && internalState != MMAdStateInternal.AD_UNKNOWN)) {
            loadStartTime = now;
        }

        switch(internalState){
            case AD_BLOCKED: {
                notifyEvent("AD_BLOCK", "Ad blocked", "AD_BLOCK", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = timeNow();
                break;
            }
            case AD_IMPRESSION: {
                isStartAfterAd = true;
                adLoadStartTime = now;
                adPauseStartTime = -1L;
                _adInformation._adPlaybackTime = 0L;
                notifyEvent("AD_IMPRESSION", "Ad impression has been made", "AD_IMPRESSION", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = -1;
                break;
            }
            case AD_PLAYING: {
                sendAdPlayingEvent(now,false);
                break;
            }
            case AD_CLICKED: {
                notifyEvent("AD_CLICK", "Ad has been clicked", "AD_CLICK", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_PAUSED: {
                adPauseStartTime = now;
                //sendAdPlayingEvent(now,true);
                notifyEvent("AD_PAUSED", "Ad has been paused", "AD_PAUSED", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_RESUMED: {
                if(adPauseStartTime > 0){
                    adPauseDuration = now - adPauseStartTime;
                    if(adPauseDuration < 0) {
                        adPauseDuration = 0L;
                    }
                    adPauseDurationToOffsetFromInterval = adPauseDuration;
                }
                populateAdRelatedInfo(now);
                notifyEvent("AD_RESUMED", "Ad has been resumed", "AD_RESUMED", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                adPauseStartTime = -1L;
                break;
            }
            case AD_SKIPPED: {
                _adBeforePlayback = true;
                //sendAdPlayingEvent(now,true);
                if(adBuffWait < 0) adBuffWait = 0;
                adBuffWait += (adLoadTime * 1.0); //sumbuffwait include with latency
                //setEventPBStats(EPAttributes.SUMBUFFERING, adBuffWait * 1.0);
                notifyEvent("AD_SKIPPED", "Ad has been skipped", "AD_SKIPPED", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._playSent = false;
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_COMPLETED: {
                _adBeforePlayback = true;
                if(adBuffWait < 0) adBuffWait = 0;
                adBuffWait += (adLoadTime * 1.0); //sumbuffwait include with latency
                //setEventPBStats(EPAttributes.SUMBUFFERING, adBuffWait * 1.0);
                //sendAdPlayingEvent(now,true);
                if (_adInformation._adDuration > 0 &&
                    ((_adInformation._adPlaybackTime - (80 * _adInformation._adDuration / 100) >= 0) ||
                        (Math.abs(_adInformation._adPlaybackTime - _adInformation._adDuration) < 2)))    //if 95% of video has been watched the
                {
                    notifyEvent("AD_COMPLETE", "Ad completed playing", "" +
                                    "",
                        millisecToSec(_adInformation._adPlaybackTime));
                }else{
                    notifyEvent("AD_ENDED", "Ad ended", "AD_ENDED", // remove this - TODO
                        millisecToSec(_adInformation._adPlaybackTime));
                    if(pbTimeBeforeAdStart > 0){
                        pbTime = pbTimeBeforeAdStart;
                    }
                }
                _adInformation._playSent = false;
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_ENDED:{
                _adBeforePlayback = true;
                if(adBuffWait < 0) adBuffWait = 0;
                adBuffWait += (adLoadTime * 1.0); //sumbuffwait include with latency
                //setEventPBStats(EPAttributes.SUMBUFFERING, adBuffWait * 1.0);
                //sendAdPlayingEvent(now,true);
                notifyEvent("AD_ENDED", "Ad ended", "AD_ENDED",
                        millisecToSec(_adInformation._adPlaybackTime));

                _adInformation._playSent = false;
                _adInformation._adLastAdEventTimeStamp = now;
                if(pbTimeBeforeAdStart > 0){
                    pbTime = pbTimeBeforeAdStart;
                }
                break;
            }
            case AD_ERROR: {
                notifyEvent("AD_ERROR", "AD Unknown Error ", "AD_ERROR", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_REQUEST: {
                _adInformation._adPlaybackTime = 0L;
                notifyEvent("AD_REQUEST", "Ad has been requested", "AD_REQUEST", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = -1;
                break;
            }
            case AD_FIRST_QUARTILE:{
                notifyEvent("AD_PLAYED_FIRST_QUARTILE", "Ad reached first quartile", "AD_PLAYED_FIRST_QUARTILE", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_MIDPOINT:{
                notifyEvent("AD_PLAYED_SECOND_QUARTILE", "Ad reached midpoint", "AD_PLAYED_SECOND_QUARTILE", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_THIRD_QUARTILE:{
                notifyEvent("AD_PLAYED_THIRD_QUARTILE", "Ad reached third quartile", "AD_PLAYED_THIRD_QUARTILE", millisecToSec(_adInformation._adPlaybackTime));
                _adInformation._adLastAdEventTimeStamp = now;
                break;
            }
            case AD_STARTED:
            case AD_PLAY: {
                if(_adInformation._playSent == false){
                    _adInformation._playSent = true;
                    if(adLoadStartTime > 0){
                        if(loadStartTime > 0 && adLoadStartTime > loadStartTime){
                            //adLoadStartTime = loadStartTime;
                        }
                        adLoadTime = now - adLoadStartTime;
                        if(adPauseDuration > 0){
                            adLoadTime -= adPauseDuration;
                        }
                        if(adLoadTime < 0){
                            adLoadTime = 0;
                        }
                        _adInformation._adPlayDur += Math.ceil(adLoadTime/1000);
                    }
                    // Update Ad interval after Ad latency
                    populateAdRelatedInfo(now);

                    notifyEvent("AD_PLAY", "Ad Playback started", "AD_PLAY",
                        adLoadTime * 1.0, 0);

                    adLoadStartTime = 0;
                    _adInformation._adLastPlayingEventTimeStamp = _adInformation._adPlaybackStartWallClockTime = timeNow();
                    _adInformation._adLastAdEventTimeStamp = _adInformation._adLastPlayingEventTimeStamp;
                }
                break;
            }
            default:{
            }
        }
    }

    public void onAdBufferingBegin() {
        if (!_adInformation._playSent || (_adInformation._prevAdState != MMAdState.AD_PLAYING)){
            return;
        }
        long now = timeNow();
        populateAdRelatedInfo(now);
        buffStarTime = now;
        notifyEvent("AD_BUFFERING","Ad Buffering started","AD_BUFFERING", null);
        _adInformation._adLastAdEventTimeStamp = now;
    }

    public void onAdBufferingEnd() {
        if (!_adInformation._playSent || (_adInformation._prevAdState != MMAdState.AD_PLAYING)){
            return;
        }

        if (buffStarTime == -1){
            //Pick last set play time as the buffering start time
            buffStarTime = timeOnPlayingStateTransition;
        }

        if (buffStarTime != -1) {
            Long buffWt = timeNow() - buffStarTime;
            adBuffWaitForInterval = buffWt;
            adBuffWait += buffWt;
            setEventPBStats(EPAttributes.BUFFERING, buffWt * 1.0);
            //setEventPBStats(EPAttributes.SUMBUFFERING, adBuffWait * 1.0);
            buffStarTime = -1;
            MMLogger.v(logTag, "onAdBufferingEnd - .Buffering for " + buffWt + "millisec of total buffering " + adBuffWait);
        }
    }

   // public void reportAdInfo(String adClient, String adURL, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer,String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime){
        public void reportAdInfo(String adClient, String adId, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer,String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime, String adCreativeId, String adUrl, String adTitle, int adBitrate){
        if(_adInformation == null){
            _adInformation = new MMAdvertisentInformation();
        }
        if(adClient != null){
            this._adInformation._adClient = adClient;
        }

        if(adId != null){
            this._adInformation._adId = adId;
        }

        if(adDuration != null){
            this._adInformation._adDuration = adDuration;
        }

        if(adPosition != null){
            this._adInformation._adPosition = adPosition;
        }

        this._adInformation._adType = adType;

        if(adCreativeType != null){
            this._adInformation._adCreativeType = adCreativeType;
        }

        if(adServer != null){
            this._adInformation._adServer = adServer;
        }
            if(adCreativeId != null){
                            this._adInformation._adCreativeId = adCreativeId;
                        }

                    	if(adUrl != null){
                          this._adInformation._adUrl = adUrl;
                        }

                    	if(adTitle != null){
                            this._adInformation._adTitle = adTitle;
                        }

                            if(adBitrate > 0) {
                          _adInformation._adBitrate = adBitrate;
                        }

        if(adResolution != null){
            this._adInformation._adResolution = adResolution;
        }

        this._adInformation._isBumper = isBumper;

        if(adPodIndex >= -1){
          _adInformation._adPodIndex = adPodIndex;
        }

        if(adPodLength > 0) {
          _adInformation._adPodLength = adPodLength;
        }

        if(adPositionInPod > 0){
            _adInformation._adPositionInPod = adPositionInPod;
        }

        if(adScheduledTime >=0){
            _adInformation._adScheduledTime = adScheduledTime;
        }
    }

    public void reportAdPlaybackTime(Long adPlaybackTime){
        if(adPlaybackTime >=0 && _adInformation != null) {
            _adInformation._adPlaybackTime = adPlaybackTime;
            sendAdPlayingEvent(timeNow(),false);
        }
    }

    public void reportAdError(String error, Long adPlaybackPosMilliSec){
        if(this._adInformation == null){
            reportAdState(MMAdState.AD_REQUEST);
        }

        if(adPlaybackPosMilliSec == null && this._adInformation != null)
            adPlaybackPosMilliSec = this._adInformation._adPlaybackTime;

        if(adPlaybackPosMilliSec == null || adPlaybackPosMilliSec == -1)
            adPlaybackPosMilliSec = 0L;

        //sendAdPlayingEvent(timeNow(),true);
        this.populateAdRelatedInfo(timeNow());
        this.notifyEvent("AD_ERROR", error, "AD_ERROR", millisecToSec(adPlaybackPosMilliSec));
        _adInformation._adLastAdEventTimeStamp = timeNow();
    }

    public void initizationFailed(){
        sessionInitizationFailed = true;
    }

    public void initializationCancelled(){

    }

    long timeNow(){
        return System.currentTimeMillis();
    }

    static final private Integer _KAdKeepAliveInterval = 5 * 1000; // 5 seconds
    String logTag = "MMSmartStreamingEP";
}
