package com.mediamelon.smartstreaming;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.castlabs.android.adverts.Ad;
import com.castlabs.android.adverts.AdInterface;
import com.castlabs.android.player.PeriodChangedListener;
import com.castlabs.android.player.TimelineChangedListener;
import com.castlabs.android.player.models.AudioTrack;
import com.castlabs.android.player.models.SubtitleTrack;
import com.castlabs.android.player.models.Timeline;
import com.castlabs.android.player.models.VideoTrack;
import com.castlabs.android.player.models.Timeline.Window;
import com.mediamelon.qubit.ep.SDKExperienceProbe;
import com.mediamelon.smartstreaming.MMAdState;
import com.mediamelon.smartstreaming.MMAdInfo;
import com.mediamelon.smartstreaming.MMAdType;
import com.mediamelon.smartstreaming.MMChunkInformation;
import com.mediamelon.smartstreaming.MMOverridableMetric;
import com.mediamelon.smartstreaming.MMPlayerState;
import com.mediamelon.smartstreaming.MMQBRMode;
import com.mediamelon.smartstreaming.MMSmartStreaming;
import com.mediamelon.smartstreaming.MMSmartStreamingInitializationStatus;
import com.mediamelon.smartstreaming.MMSmartStreamingObserver;
import com.mediamelon.smartstreaming.MMSmartStreamingNowtilusSSAIPlugin;
import com.mediamelon.smartstreaming.MMSSAIEventsCollector;


import com.castlabs.android.player.DisplayInfo;
import com.castlabs.android.player.PlayerController;
import com.castlabs.android.player.PlayerControllerListener;
import com.castlabs.android.player.PlayerListener;
import com.castlabs.android.player.PlayerView;
import com.castlabs.android.player.TrackSelectionListener;
import com.castlabs.android.player.exceptions.CastlabsPlayerException;
import com.castlabs.android.player.models.VideoTrackQuality;





import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import  org.json.JSONObject;
import  org.json.JSONException;

public class MMSmartStreamingPrestoPlayAdaptor implements
MMSmartStreamingObserver {

    private static final String TAG = "MMSmartStreamingPrestoPlayAdaptor";
    private static final String ComponentName = "ANDROIDSDK";
    Context context;
    MMSmartStreamingObserver obs = null;
    private int cumulativeFramesDropped = 0;
    private static boolean logStackTrace = false;
    private boolean isSSAIAdPlaying = false;
    private boolean isAdRequestSent = false;
    private boolean isOnloadSent = false;
    private boolean isBuffering = false;
    private MMPlayerState mmPreviousPlayerState = null;
    private MMAdState mmPreviousAdState = null;
    private boolean sendBufferingCompletionOnReady = false;
    private boolean initializationDone = false;
    private boolean isErrorSent = false;
    private static boolean ad_playing = false;
    private static boolean isStartAfterAdSent = false;
    private static long ad_duration;
    private Context ctx;
    //PlayerView playerView;
    PlayerController pc; //changed here

    private static String StackTraceLogTag = "MMSmartStreamingIntgr";
    private static String prev_state = "";
    private boolean isPresentationInfoSet = false;
    private static MMSmartStreamingPrestoPlayAdaptor myObj;

    private MMSmartStreamingNowtilusSSAIPlugin mmSmartStreamingNowtilusSSAIPlugin = null;


    //change made here
    public MMSmartStreamingPrestoPlayAdaptor(Context context,PlayerController pc)
    {
        this.context = context;
        this.pc = pc;
    }


    public void sessionInitializationCompleted(Integer initCmdId, MMSmartStreamingInitializationStatus status, String description) {
        if (obs != null) {
            obs.sessionInitializationCompleted(initCmdId, status, description);
        }

        if (status == MMSmartStreamingInitializationStatus.Success) {
            Integer interval = MMSmartStreaming.getInstance().getLocationUpdateInterval();
            MMNetworkInformationRetriever.instance().startRetriever(ctx, interval);
        }
    }

    public void closeMediaMelonSDK(){
        MMSmartStreaming.getInstance().closeMediaMelonSDK();
        if(mmSmartStreamingNowtilusSSAIPlugin!=null) mmSmartStreamingNowtilusSSAIPlugin.closeSSAIAdManager();

    }
    /**
     * Gets the SDK version
     * @return SDK version (major.minor.patch)
     */
    public static String getVersion(){
        return MMSmartStreaming.getVersion();
    }

    /**
     * Gets the registration status (done via registerMMSmartStreaming)
     * @return true if the SDK has successfully registered with the registerMMSmartStreaming method;
     * otherwise returns false.
     * @see registerMMSmartStreaming
     */
    public static boolean getRegistrationStatus(){
        return MMSmartStreaming.getRegistrationStatus();
    }

    /**
     * Sets the activity context
     * @param aCtx Player context
     */
    public void setContext(Context aCtx){
        if(logStackTrace){
            Log.v(StackTraceLogTag, "setContext" + aCtx);
        }

        ctx = aCtx;

        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        Integer height = dm.heightPixels;
        Integer width = dm.widthPixels;

        TelephonyManager tm = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
        MMSmartStreaming.reportDeviceInfo(Build.BRAND, Build.MODEL, "Android", Build.VERSION.RELEASE, (tm!=null? (tm.getNetworkOperatorName()):null), width, height);

        MMNetworkInformationRetriever.instance().initializeRetriever(ctx);
    }


    /**
     * Sets the activity context
     * @param Player context
     */
    public void setContext(){
        if(logStackTrace){
            Log.v(StackTraceLogTag, "setContext" + context);
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        Integer height = dm.heightPixels;
        Integer width = dm.widthPixels;

        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        MMSmartStreaming.reportDeviceInfo(Build.BRAND, Build.MODEL, "Android", Build.VERSION.RELEASE, (tm!=null? (tm.getNetworkOperatorName()):null), width, height);

        MMNetworkInformationRetriever.instance().initializeRetriever(context);
    }




    /**
     * Registers the QBR SmartStreaming engine and performs a license verification. This API should
     * be called once when player starts. The QBR SmartStreaming engine must be successfully
     * registered before initialization.
     * This is a synchronous call. Registration status can be checked at any time using the
     * getRegistrationStatus method.
     *
     * @param playerName Name of the player
     * @param customerID MediaMelon assigned customer ID
     * @param [subscriberID] Viewer's subscriber ID
     * @param [domainName] Content-owner domain name.
     *                   Some business organizations may would like to do analytics segmented
     *                   by group. For example, a Media House may have many divisions, and will like
     *                   to categorize their analysis based on division. Or a content owner has
     *                   distributed content to various resellers and would like to know the reseller
     *                   from whom the user is playing the content. In this case every reseller will
     *                   have separate application, and will configure the domain name.
     *
     * @note Please be aware that this API will be deprecated in the version 4.x.x. Integrators are
     * advised to use another overload of this API that accepts subscriberType as parameter as well
     * @see registerMMSmartStreaming
     * @see getRegistrationStatus
     * @see updateSubscriberID
     */
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName);
    }

    /**
     * Registers the QBR SmartStreaming engine and performs a license verification. This API should
     * be called once when player starts. The QBR SmartStreaming engine must be successfully
     * registered before initialization.
     * This is a synchronous call. Registration status can be checked at any time using the
     * getRegistrationStatus method.
     *
     * @param playerName Name of the player
     * @param customerID MediaMelon assigned customer ID
     * @param [subscriberID] Viewer's subscriber ID
     * @param [domainName] Content-owner domain name.
     *                   Some business organizations may would like to do analytics segmented
     *                   by group. For example, a Media House may have many divisions, and will like
     *                   to categorize their analysis based on division. Or a content owner has
     *                   distributed content to various resellers and would like to know the reseller
     *                   from whom the user is playing the content. In this case every reseller will
     *                   have separate application, and will configure the domain name.
     * @param [subscriberType] Viewer's subscriber Type
     *
     * @see getRegistrationStatus
     * @see updateSubscriberID
     */
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName, String subscriberType){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName, subscriberType);
    }

    /**
     * Registers the QBR SmartStreaming engine and performs a license verification. This API should
     * be called once when player starts. The QBR SmartStreaming engine must be successfully
     * registered before initialization.
     * This is a synchronous call. Registration status can be checked at any time using the
     * getRegistrationStatus method.
     *
     * @param playerName Name of the player
     * @param customerID MediaMelon assigned customer ID
     * @param [subscriberID] Viewer's subscriber ID
     * @param [domainName] Content-owner domain name.
     *                   Some business organizations may would like to do analytics segmented
     *                   by group. For example, a Media House may have many divisions, and will like
     *                   to categorize their analysis based on division. Or a content owner has
     *                   distributed content to various resellers and would like to know the reseller
     *                   from whom the user is playing the content. In this case every reseller will
     *                   have separate application, and will configure the domain name.
     * @param [subscriberType] Viewer's subscriber type such as "Free", "Basic" or "Premium" as
     *                         configured by the customer for the end user of the player.
     * @param [subscriberTag] Viewer's tag using which one can track their pattern

     *
     * @see getRegistrationStatus
     * @see updateSubscriberID
     */
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName, String subscriberType, String subscriberTag){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName, subscriberType,subscriberTag,true);
    }


    //Added hashSubscriberID
    public static void registerMMSmartStreaming(String playerName, String customerID, String subscriberID, String domainName, String subscriberType, String subscriberTag,boolean hashSubscriberId){
        MMSmartStreaming.registerMMSmartStreaming(playerName, customerID, "ANDROIDSDK", subscriberID, domainName, subscriberType,subscriberTag,hashSubscriberId);
    }

    public ArrayList<String> getMissingPermissions(Context context){
        return MMNetworkInformationRetriever.getMissingPermissions(context);
    }

    /**
     * Disables the fetching of manifests by the SDK to determine the presentation information of the content.
     * SDK will rely completely on presentation information provided as part of setPresentationInformation.
     * @param [disable] Disables/Enables the manifest fetch by the SDK
     * @see setPresentationInformation
     */
    public static void disableManifestsFetch(boolean disable){
        try{
            MMSmartStreaming.disableManifestsFetch(disable);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * After the registration, user may will like to update the subscriber ID,
     * for example - user logged off from the Video service website, and logs in again with different
     * user.
     * @note This API will be deprecated for updateSubscriber in the version 4.x.x of the SDK
     *
     * @param subscriberID New Subscriber ID
     * @see registerMMSmartStreaming
     * @see updateSubscriber
     *
     */
    public static void updateSubscriberID(String subscriberID){
        MMSmartStreaming.updateSubscriberID(subscriberID);
    }

    /**
     * After the registration, user may will like to update the subscriber ID,
     * for example - user logged off from the Video service website, and logs in again with different
     * user.
     * @param subscriberID New Subscriber ID
     * @param subscriberID New Subscriber Type
     * @see registerMMSmartStreaming
     *
     */
    public static void updateSubscriber(String subscriberID, String subscriberType){
        MMSmartStreaming.updateSubscriber(subscriberID, subscriberType);
    }

    /**
     * Reports the media player characteristics to analytics.
     * Use a NULL pointer if the value is unknown or inapplicable.
     *
     * @param [brand] Brand of the player. For example - Brand could be Organisation Name.
     * @param [model] Model of the player. For example - This could be a variant of player.
     *              Say name of third party player used by organisation. Or any human readable name of
     *              the player.
     * @param [version] Version of the player.
     */
    public static void reportPlayerInfo(String brand, String model, String version){
        MMSmartStreaming.reportPlayerInfo(brand, model, version);
    }


    public void initialize(String stream_url,JSONObject mmVideoAssetInfo){

        System.out.println("Integrating with the MMSmartStreaming- Player Integration - " + MMSmartStreaming.getVersion());

        reset();
        resetNewSession();
        setContext();
        SDKExperienceProbe.getInstance().startMediaMelonSDK();
        String AssetName = "";
        String AssetId = "";
        String VideoId = "";
        JSONObject customTagObj = null;
        try {
            if (mmVideoAssetInfo != null) {
                if (mmVideoAssetInfo.get("assetName") != null) {
                    AssetName = mmVideoAssetInfo.get("assetName").toString();
                }
                if (mmVideoAssetInfo.get("assetId") != null) {
                    AssetId = mmVideoAssetInfo.get("assetId").toString();
                }
                if (mmVideoAssetInfo.get("videoId") != null) {
                    VideoId = mmVideoAssetInfo.get("videoId").toString();
                }
                if (mmVideoAssetInfo.get("customTags") != null) {
                    customTagObj = (JSONObject) mmVideoAssetInfo.get("customTags");
                }
            }
        }catch (JSONException e) {
            //some exception handler code.
        }

        if(AssetName.equals("")) {
            AssetName = null;
        }
        if(AssetId.equals("")){
            AssetId = null;
        }
        if(VideoId.equals("")){
            VideoId = null;
        }

        if(this.pc != null) {
            registerPlayerEvents(this.pc, this.mmSmartStreamingNowtilusSSAIPlugin );
        }


        MMSmartStreaming.getInstance().initializeSession(MMQBRMode.QBRModeDisabled, stream_url, null, mmVideoAssetInfo, this);

        if(customTagObj != null){
            Iterator <String> itt = customTagObj.keys();
            while(itt.hasNext()) {
                String key = itt.next();
                try {
                    MMSmartStreaming.getInstance().reportCustomMetadata(key, customTagObj.get(key).toString());
                }catch (JSONException e) {
                    //some exception handler code.
                }

            }
        }

    }

    public void initialize(String stream_url,JSONObject mmVideoAssetInfo,boolean isLive){

        System.out.println("Integrating with the MMSmartStreaming- Player Integration - " + MMSmartStreaming.getVersion());

        reset();
        resetNewSession();
        setContext();
        SDKExperienceProbe.getInstance().startMediaMelonSDK();
        String AssetName = "";
        String AssetId = "";
        String VideoId = "";
        JSONObject customTagObj = null;
        try {
            if (mmVideoAssetInfo != null) {
                if (mmVideoAssetInfo.get("assetName") != null) {
                    AssetName = mmVideoAssetInfo.get("assetName").toString();
                }
                if (mmVideoAssetInfo.get("assetId") != null) {
                    AssetId = mmVideoAssetInfo.get("assetId").toString();
                }
                if (mmVideoAssetInfo.get("videoId") != null) {
                    VideoId = mmVideoAssetInfo.get("videoId").toString();
                }
                if (mmVideoAssetInfo.get("customTags") != null) {
                    customTagObj = (JSONObject) mmVideoAssetInfo.get("customTags");
                }
            }
        }catch (JSONException e) {
            //some exception handler code.
        }

        if(AssetName.equals("")) {
            AssetName = null;
        }
        if(AssetId.equals("")){
            AssetId = null;
        }
        if(VideoId.equals("")){
            VideoId = null;
        }

        if(this.pc != null) {
            registerPlayerEvents(this.pc, this.mmSmartStreamingNowtilusSSAIPlugin );
        }


        MMSmartStreaming.getInstance().initializeSession(MMQBRMode.QBRModeDisabled, stream_url, null, mmVideoAssetInfo, this,isLive);

        if(customTagObj != null){
            Iterator <String> itt = customTagObj.keys();
            while(itt.hasNext()) {
                String key = itt.next();
                try {
                    MMSmartStreaming.getInstance().reportCustomMetadata(key, customTagObj.get(key).toString());
                 }catch (JSONException e) {
                //some exception handler code.
            }

        }
        }

    }

    public void initialize(String stream_url,String ad_url,JSONObject mmVideoAssetInfo,boolean isLive){

        System.out.println("Integrating with the MMSmartStreaming- Player Integration - " + MMSmartStreaming.getVersion());

        reset();
        SDKExperienceProbe.getInstance().startMediaMelonSDK();

        String AssetName = "";
        String AssetId = "";
        String VideoId = "";
        JSONObject customTagObj = null;
    try {
        if(mmVideoAssetInfo != null){
            if(mmVideoAssetInfo.get("assetName") != null) {
                AssetName = mmVideoAssetInfo.get("assetName").toString();
            }
            if(mmVideoAssetInfo.get("assetId") != null) {
                AssetId = mmVideoAssetInfo.get("assetId").toString();
            }
            if(mmVideoAssetInfo.get("videoId") != null) {
                VideoId = mmVideoAssetInfo.get("videoId").toString();
            }
            if(mmVideoAssetInfo.get("customTags") != null) {
                customTagObj = (JSONObject) mmVideoAssetInfo.get("customTags");
            }
        }
    }catch (JSONException e) {
        //some exception handler code.
    }


        if(AssetName.equals("")) {
            AssetName = null;
        }
        if(AssetId.equals("")){
            AssetId = null;
        }
        if(VideoId.equals("")){
            VideoId = null;
        }

        if(this.pc != null) {
            registerPlayerEvents(this.pc, this.mmSmartStreamingNowtilusSSAIPlugin);

            registerAdEvents(this.pc, ad_url);
        }

        MMSmartStreaming.getInstance().initializeSession(MMQBRMode.QBRModeDisabled, stream_url, null, AssetId, AssetName, VideoId, this,isLive);

        if(customTagObj != null){
            Iterator <String> itt = customTagObj.keys();
            while(itt.hasNext())
            {
                String key = itt.next();
                try {
                    MMSmartStreaming.getInstance().reportCustomMetadata(key,customTagObj.get(key).toString());
                }catch (JSONException e) {
                    //some exception handler code.
            }


            }

        }

    }

    public void registerPlayerEvents(PlayerController pc, MMSmartStreamingNowtilusSSAIPlugin mmSmartStreamingNowtilusSSAIPlugin )

    {

            pc.addPlayerListener(new PlayerListener() {
            @Override
            public void onFatalErrorOccurred(@NonNull CastlabsPlayerException e) {

                if(isOnloadSent == false)
                {
                    MMSmartStreaming.getInstance().reportUserInitiatedPlayback();
                    isOnloadSent = true;
                }
                if(isErrorSent == false) {
                    MMSmartStreaming.getInstance().reportError(e.getCauseMessage(), pc.getPosition());
                    isErrorSent = true;
                }

            }

            @Override
            public void onError(@NonNull CastlabsPlayerException e) {

                if(isOnloadSent == false)
                {
                    MMSmartStreaming.getInstance().reportUserInitiatedPlayback();
                    isOnloadSent = true;
                }
                if(isErrorSent == false) {
                    MMSmartStreaming.getInstance().reportError(e.getCauseMessage(), pc.getPosition());
                    isErrorSent = true;
                }

            }

            @Override
            public void onStateChanged(@NonNull PlayerController.State state) {
                if(logStackTrace){
                    Log.v(StackTraceLogTag, "reportPlayerState - < "  + state + " >");
                    //Log.i("A1","reportPlayerState - < "  + state + " >");
                }

                switch (state){
                    case Idle:

                    case Preparing:

                        if (ad_playing == true) return;

                        break;

                    case Buffering:{

                        if (ad_playing == true) return;

                        MMSmartStreaming.getInstance().reportBufferingStarted();
                        sendBufferingCompletionOnReady = true;
                    }
                    break;

                    case Playing:{
                        //Log.i("A1","PLAYING-----");

                        if (ad_playing == true) return;

                        if(isPresentationInfoSet == false)
                        {
                            updatePresentationInfo(pc);
                        }

                        if(isOnloadSent == false)
                        {
                            MMSmartStreaming.getInstance().reportUserInitiatedPlayback();
                            isOnloadSent = true;
                        }

                        if(sendBufferingCompletionOnReady)
                        {
                            MMSmartStreaming.getInstance().reportBufferingCompleted();
                            sendBufferingCompletionOnReady = false;
                        }

                        if(!prev_state.equals("PLAYING")) {
                            MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.PLAYING);
                            prev_state = "PLAYING";
                        }

                    }
                    break;

                    case Finished:{

                        if (ad_playing == true) return;

                        MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.STOPPED);
                        prev_state = "STOPPED";
                    }
                    break;

                    case Pausing:{

                        if (ad_playing == true) return;

                        MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.PAUSED);
                        prev_state = "PAUSED";
                    }
                    break;
                }

            }

            @Override
            public void onSeekCompleted()
            {

            }

            @Override
            public void onTrackKeyStatusChanged()
            {

            }


            @Override
            public void  onSpeedChanged(float speed)
            {

            }

            @Override
            public void onSeekTo(long l) {

                if (ad_playing == true) return;

                System.out.println("MMSS Seek time "+l);
                MMSmartStreaming.getInstance().reportPlayerSeekCompleted(l/1000);

            }

            @Override
            public void onVideoSizeChanged(int i, int i1, float v) {

            }

            @Override
            public void onSeekRangeChanged(long l, long l1) {

            }

            @Override
            public void onPlaybackPositionChanged(long l) {
                //Log.d("onInformationLog","LiveStartTime    " + pc.getLiveStartTime() + "       playbackPosition      "+l);

                long pb = pc.getLiveStartTime();


                if (ad_playing == true) return;


                if(isOnloadSent == true && isStartAfterAdSent == false)
                {
                    MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.PLAYING);
                    isStartAfterAdSent = true;
                    prev_state = "PLAYING";
                }

                MMSmartStreaming.getInstance().reportPlaybackPosition((pb/1000)+l);
                if(mmSmartStreamingNowtilusSSAIPlugin != null){
                    //Log.d("onInformationLog", "Playback time " +  l );
                    mmSmartStreamingNowtilusSSAIPlugin.reportPlaybackPosition((pb/1000)+l);
                }


            }

            @Override
            public void onDisplayChanged(DisplayInfo displayInfo, boolean b) {

            }

            @Override
            public void onDurationChanged(long l) {

            }

            @Override
            public void onPlayerModelChanged() {

            }

            @Override
            public void onFullyBuffered() {

            }
        });

        pc.addTrackSelectionListener(new TrackSelectionListener() {
            @Override
            public void onVideoQualitySelectionChanged(@NonNull VideoTrackQuality videoTrackQuality, int i, @Nullable String s, long l, long l1) {

                MMChunkInformation chunk = new MMChunkInformation();

                chunk.bitrate = videoTrackQuality.getBitrate();
                chunk.trackIdx = videoTrackQuality.getTrackIndex();

                MMSmartStreaming.getInstance().reportDownloadRate(l1); //due to this

                MMSmartStreaming.getInstance().reportChunkRequest(chunk); //due to this

            }

            @Override
            public void onSubtitleTrackChanged(@Nullable SubtitleTrack subtitleTrack) {

            }

            @Override
            public void onAudioTrackChanged(@Nullable AudioTrack audioTrack) {

            }

            @Override
            public void onVideoTrackChanged(@Nullable VideoTrack videoTrack) {

            }
        });


        pc.addPlayerControllerListener(new PlayerControllerListener() {
            @Override
            public void onRelease(PlayerController playerController) {
                if(!prev_state.equals("STOPPED")) {
                   if(mmSmartStreamingNowtilusSSAIPlugin!=null) mmSmartStreamingNowtilusSSAIPlugin.closeSSAIAdManager();
                    MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.STOPPED);
                    prev_state = "STOPPED";
                }
            }

            @Override
            public void onDestroy(PlayerController playerController) {
                if(!prev_state.equals("STOPPED")) {
                    MMSmartStreaming.getInstance().reportPlayerState(MMPlayerState.STOPPED);
                    prev_state = "STOPPED";
                }
            }
        });


    }

    public void registerAdEvents(PlayerController pc,String ad_url)

    {
        pc.getAdInterface().addAdListener(new AdInterface.Listener() {
            @Override
            public void onAdStarted(@NonNull Ad ad) {

                if (ad_playing == true) return;

                if (!(prev_state.equals("PLAYING"))) {

                    if (!isOnloadSent) {

                        if(isPresentationInfoSet == false)
                        {
                            updatePresentationInfo(pc);
                        }

                        MMSmartStreaming.getInstance().reportUserInitiatedPlayback();
                        isOnloadSent = true;
                    }
                }

                if(isAdRequestSent == false){
                    MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_REQUEST);
                    isAdRequestSent = true;
                }

                MMAdInfo mmAdInfo = new MMAdInfo();

                mmAdInfo.adID = ad.id;
                mmAdInfo.adDuration = ad.durationMs;
                ad_duration = ad.durationMs;

                MMSmartStreaming.getInstance().reportAdInfo(mmAdInfo);
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_IMPRESSION);

                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_STARTED);

                ad_playing = true;

            }

            @Override
            public void onAdCompleted() {

                ad_playing = false;
                MMSmartStreaming.getInstance().reportAdPlaybackTime(ad_duration);
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_COMPLETED);

            }

            @Override
            public void onAdPlaybackPositionChanged(long l) {

                MMSmartStreaming.getInstance().reportAdPlaybackTime(l);

            }

            @Override
            public void onAdSkipped() {

            }

            @Override
            public void onAdError(CastlabsPlayerException e) {

            }
        });

    }

    boolean isLive = false;
    void updatePresentationInfo(PlayerController pc){

        if (!isPresentationInfoSet) {

            if (pc.getVideoQualities() != null) {
                int len = pc.getVideoQualities().size();
                MMPresentationInfo presentationInfo = new MMPresentationInfo();
                for (int i = 0; i < len; i++) {
                    VideoTrackQuality videoTrackQuality = pc.getVideoQualities().get(i);
                    if (videoTrackQuality.getTrackIndex() != -1 && videoTrackQuality.getBitrate() > 0) {
                        MMRepresentation representation = new MMRepresentation(videoTrackQuality.getTrackIndex(), videoTrackQuality.getBitrate(),videoTrackQuality.getWidth(),videoTrackQuality.getHeight(),videoTrackQuality.getCodecs());
                        presentationInfo.representations.add(representation);
                    }
                }

                //Whether the stream is Live or not
                if(pc.isLive() == true)
                {
                    isLive = true;
                    presentationInfo.isLive = true;
                    presentationInfo.duration = -1L;
                }
                else
                {
                    isLive = false;
                    presentationInfo.isLive = false;
                    presentationInfo.duration = (pc.getDuration())/1000;
                }

                MMSmartStreaming.getInstance().setPresentationInformation(presentationInfo);
                isPresentationInfoSet = true;
            }
        }

    }

    void reset(){
        obs = null;
        sendBufferingCompletionOnReady = false;
        initializationDone = false;
        isOnloadSent = false;
        isPresentationInfoSet = false;
        prev_state = "";
        ad_playing = false;
        isErrorSent = false;
    }

    /**
     * Reports that user initiated the playback session.
     * This should be called at different instants depending on the mode of operation of player.
     * In Auto Play Mode, should be the called when payer is fed with the manifest URL for playback
     * In non-Auto Play Mode, should be called when the user presses the play button on the
     * player
     */
    public void reportUserInitiatedPlayback(){
        MMSmartStreaming.getInstance().reportUserInitiatedPlayback();
    }

    /**
     * Returns the bandwidth required for the QBR representation that delivers constant quality across
     * the session.
     *
     * @param representationTrackIdx Track Index of the representation whose corresponding
     *                               quality bitrate is to be evaluated.
     * @param defaultBitrate Bitrate of the CBR representation as advertised in the manifest (in
     *                         bits per second).
     * @param bufferLength Amount of media buffered in player ahead of current playback position (in
     *                    milliseconds).
     * @param playbackPosition Current playback position (in milliseconds).
     * @return Bandwidth of QBR representation (in bits per second).
     */
    public Integer getQBRBandwidth(Integer representationTrackIdx, Integer defaultBitrate, Long bufferLength, Long playbackPosition){
        return MMSmartStreaming.getInstance().getQBRBandwidth(representationTrackIdx, defaultBitrate, bufferLength, playbackPosition);
    }

    /**
     * During the playback session, player is expected to query the constant quality chunk that it
     * should request from server for the chunk selected based on ABR algorithm.
     * This API is used only if Qubitisation of content is to be achieved.
     * @param cbrChunk MMChunkInformation object identifying the chunk selected by ABR algorithm.
     * For referencing the chunk there are two option:
     * (a) Caller of API may specify resourceURL
     * (b) Caller of API may specify combination of sequence id and track id.
     * Using option b) may result in improved CPU performace of this API and is recommended.
     * @return The chunk selected by the QBR algorithm.
     * @see MMChunkInformation
     */
    public MMChunkInformation getQBRChunk(MMChunkInformation cbrChunk){
        return MMSmartStreaming.getInstance().getQBRChunk(cbrChunk);
    }

    /**
     * Reports the chunk request to analytics. This method is not used when QBR optimization is
     * enabled.
     * @param chunkInfo Chunk selected by the player.
     * @see MMChunkInformation
     */
    public void reportChunkRequest(MMChunkInformation chunkInfo){
        MMSmartStreaming.getInstance().reportChunkRequest(chunkInfo);
    }

    /**
     * Reports current download rate (rate at which chunk is downloaded) to analytics. This should be
     * reported for every chunk download (if possible). If this value is not available on every
     * chunk download, then last updated value with player should be reported every 2 seconds.
     *
     * @param downloadRate Download rate as measured by the player (in bits per second)
     */
    public void reportDownloadRate(Long downloadRate){
        MMSmartStreaming.getInstance().reportDownloadRate(downloadRate);
    }

    /**
     * Reports custom metadata, in the form of a key-value pair, to analytics.
     *
     * @param key Custom metadata key.
     * @param value Custom metadata value.
     */
    public void reportCustomMetadata(String key, String value){
        MMSmartStreaming.getInstance().reportCustomMetadata(key, value);
    }

    /**
     * Reports current playback position in media to analytics. This should be reported every two
     * seconds if possible.
     *
     * @param playbackPos Current playback position (in milliseconds).
     */
    public void reportPlaybackPosition(Long playbackPos){
        MMSmartStreaming.getInstance().reportPlaybackPosition(playbackPos);
    }

    /**
     * Override the SmartSight-calculated metric with a specific value.
     *
     * @param metric : Metric to be overridden.
     * @param value : New metric value. Even if the value of
     *   metric is numeric, int (for example in case of latency), user
     *   is expected to provide its string representation:
     * - For Latency, the latency in seconds, with with millisecond resolution (e.g., "1.236")
     * - For ServerAddress, the name of the cdn (e.g., "PrivateCDN")
     * - For DurationWatched, the duration watched in seconds, with millisecond resolution (e.g., "137.935")
     * @see MMOverridableMetric
     */
    public void reportMetricValue(MMOverridableMetric metric, String value){
        MMSmartStreaming.getInstance().reportMetricValue(metric, value);
    }

    /**
     * Reports an error encountered during playback.
     * @param error Error encountered during playback session.
     * @param playbackPosMilliSec Playback position in millisec when error occurred.
     */
    public void reportError(String error, Long playbackPosMilliSec){
        MMSmartStreaming.getInstance().reportError(error, playbackPosMilliSec);
    }

    /**
     * Reports that a seek event is complete, with the new playback starting position.
     * @param seekEndPos Playback position(in milliseconds) when seek completed. This is point from
     *                   which playback will start after the seek.
     */
    public void reportPlayerSeekCompleted(Long seekEndPos){
        MMSmartStreaming.getInstance().reportPlayerSeekCompleted(seekEndPos);
    }

    /**
     * Reports the WiFi Service Set Identifier (SSID).
     * @param ssid WiFi Service Set Identifier (SSID).
     */
    public void reportWifiSSID(String ssid){
        MMSmartStreaming.getInstance().reportWifiSSID(ssid);
    }

    /**
     * Reports the WiFi signal strength. This may be useful, if someone is analyzing a
     * back playback session using smartsight's microscope feature, and wants to know if Wifi signal
     * strength is the cause fo poor performance of that session. This API is relevant if Wifi is used
     * for the playback session.
     *
     * @param strength Strength of Wifi signal in %
     */
    public void reportWifiSignalStrengthPercentage(Double strength){
        MMSmartStreaming.getInstance().reportWifiSignalStrengthPercentage(strength);
    }

    /**
     * Reports the WiFi maximum data rate.
     * @param dataRate WiFi data rate (in kbps)
     */
    public void reportWifiDataRate(Integer dataRate){
        MMSmartStreaming.getInstance().reportWifiDataRate(dataRate);
    }

    /**
     * Reports advertisement playback state
     * @param adState State of the advertisement
     * @see MMAdState
     */
    public void reportAdState(MMAdState adState){
        MMSmartStreaming.getInstance().reportAdState(adState);
    }

    /**
     * Reports current advertisement playback position
     * @param playbackPosition Current playback position in the Ad (in milliseconds)
     */
    public void reportAdPlaybackTime(Long playbackPosition){
        MMSmartStreaming.getInstance().reportAdPlaybackTime(playbackPosition);
    }

    /**
     * Reports error encountered during the advertisement playback
     * @param error Error encountered during advertisement playback
     * @param pos Playback position (in milliseconds) where error occurred
     */
    public void reportAdError(String error, Long playbackPosMilliSec){
        MMSmartStreaming.getInstance().reportAdError(error, playbackPosMilliSec);
    }

    /**
     * Enables/Disables console logs for the SDK methods. This is to help in debugging and testing
     * of the player to SDK integration.
     * @param logStTrace True to enable console logs; false to disable console logs.
     */
    public static void enableLogTrace(boolean logStTrace){
        logStackTrace = logStTrace;
        MMSmartStreaming.enableLogTrace(logStTrace);
    }



    private void resetNewSession(){
        isSSAIAdPlaying = false;
        isAdRequestSent = false;
        isOnloadSent = false;
        isBuffering = false;
        cumulativeFramesDropped = 0;
    }



      public MMSmartStreamingNowtilusSSAIPlugin getSSAIAdManager(){
        if(this.mmSmartStreamingNowtilusSSAIPlugin == null) {
            this.mmSmartStreamingNowtilusSSAIPlugin = new MMSmartStreamingNowtilusSSAIPlugin();
        }
        return this.mmSmartStreamingNowtilusSSAIPlugin;
    }

    public void setupNowtilusAdManager(String mediaUrl, String trackingUrl,JSONArray clips, boolean isLive, boolean enablePolling,boolean clientSideTracking){
        setNowtilusAdClient();
        if(this.mmSmartStreamingNowtilusSSAIPlugin != null){
            this.mmSmartStreamingNowtilusSSAIPlugin.mmSSAIClientInit(mediaUrl,trackingUrl,isLive,enablePolling,clips,clientSideTracking);
        }
    }

    public void setupNowtilusAdManager(String mediaUrl, String trackingUrl,JSONArray clips, boolean isLive, boolean enablePolling){
        setNowtilusAdClient();
        if(this.mmSmartStreamingNowtilusSSAIPlugin != null){
            this.mmSmartStreamingNowtilusSSAIPlugin.mmSSAIClientInit(mediaUrl,trackingUrl,isLive,enablePolling,clips,true);
        }
    }

    private void setNowtilusAdClient(){
        if(this.mmSmartStreamingNowtilusSSAIPlugin == null) {
            this.mmSmartStreamingNowtilusSSAIPlugin = new MMSmartStreamingNowtilusSSAIPlugin();
        }
        // Set the SSAI Ad event listener
        MMSSAIEventsListeners mmssaiEventsListeners = new MMSSAIEventsListeners() {

            void reportSSAIAdInfo(mmAd ssaiAdInfo){
                if(ssaiAdInfo != null) {
                    MMAdInfo mmAdInfo = new MMAdInfo();

                    mmAdInfo.adClient = "mmAds-Nowtilus";
                    mmAdInfo.adType =  MMAdType.AD_LINEAR;
                    mmAdInfo.adID = ssaiAdInfo.getAdId();
                    mmAdInfo.adDuration = (long) (ssaiAdInfo.getAdDuration() * 1000);
                    mmAdInfo.adCreativeType = ssaiAdInfo.getStreamType();
                    mmAdInfo.adServer = ssaiAdInfo.getAdServer();
                    mmAdInfo.setSSAI(true);

                    String adPosition = ssaiAdInfo.getPosition();
                    if(adPosition.equals("PRE")){
                        mmAdInfo.adPosition = "pre";
                    }
                    else if (adPosition.equals("POST")){
                        mmAdInfo.adPosition = "post";
                    }
                    else {
                        mmAdInfo.adPosition = "mid";
                    }
                    MMSmartStreaming.getInstance().reportAdInfo(mmAdInfo);
                }
            }

            @Override
            public void onAdImpression(mmAd ssaiAdInfo) {

                if(ssaiAdInfo != null) {
                    if (isAdRequestSent == false && isOnloadSent == true) {
                        MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_REQUEST);
                        isAdRequestSent = true;
                    }
                }

                reportSSAIAdInfo(ssaiAdInfo);
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_IMPRESSION);
            }

            @Override
            public void onAdStarted(mmAd ssaiAdInfo) {
                if(mmPreviousPlayerState != MMPlayerState.PAUSED) {
                    mmPreviousPlayerState = MMPlayerState.PAUSED;
                    MMSmartStreaming.getInstance().reportPlayerState(mmPreviousPlayerState);
                }
                mmPreviousAdState = MMAdState.AD_PLAYING;
                isSSAIAdPlaying = true;
                reportSSAIAdInfo(ssaiAdInfo);
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_STARTED);
            }

            @Override
            public void onAdFirstQuartile(mmAd ssaiAdInfo) {
                isSSAIAdPlaying = true;
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_FIRST_QUARTILE);
            }

            @Override
            public void onAdMidpoint(mmAd ssaiAdInfo) {
                isSSAIAdPlaying = true;
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_MIDPOINT);
            }

            @Override
            public void onAdThirdQuartile(mmAd ssaiAdInfo) {
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_THIRD_QUARTILE);
            }

            @Override
            public void onAdComplete(mmAd ssaiAdInfo) {
                if(ssaiAdInfo != null && (ssaiAdInfo.adDuration > 0)) {
                    MMSmartStreaming.getInstance().reportAdPlaybackTime(ssaiAdInfo.adDuration * 1000);
                }
                MMSmartStreaming.getInstance().reportAdState(MMAdState.AD_COMPLETED);
                isSSAIAdPlaying = false;
                mmPreviousAdState = MMAdState.AD_COMPLETED;
                if(mmPreviousPlayerState == MMPlayerState.PAUSED) {
                    mmPreviousPlayerState = MMPlayerState.PLAYING;
                    MMSmartStreaming.getInstance().reportPlayerState(mmPreviousPlayerState);
                }
            }

            @Override
            public void onAdProgress(mmAd ssaiAdInfo) {
                if(ssaiAdInfo != null) {
                    long posInSec = ssaiAdInfo.getAdCurrentPlaybackTimeInSec();
                    if (posInSec >= 0) {
                        MMSmartStreaming.getInstance().reportAdPlaybackTime(posInSec * 1000);
                    }
                }
            }
        };

        if(this.mmSmartStreamingNowtilusSSAIPlugin != null){
            // ADD SSAI event Listeners
            this.mmSmartStreamingNowtilusSSAIPlugin.addListener(mmssaiEventsListeners);
        }

    }

}
