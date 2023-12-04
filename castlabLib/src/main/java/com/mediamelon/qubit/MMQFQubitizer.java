package com.mediamelon.qubit;

import android.content.Context;
import android.util.Log;
import com.mediamelon.qubit.ep.RegisterAPI;
import com.mediamelon.qubit.ep.RegisterResponse;
import com.mediamelon.qubit.ep.SDKExperienceProbe;
import com.mediamelon.smartstreaming.MMPresentationInfo;
import com.mediamelon.smartstreaming.MMRepresentation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mediamelon on 08/07/16.
 */
public class MMQFQubitizer implements  MMQFQubitEngineInterface.OnInitializationCompleteListener,
        MMQFQubitEngineInterface.OnQubitEngineErrorEventListener,
        MMQFQubitEngineInterface.OnQubitEngineInfoEventListener{

    public enum QubitizationMode{
        QubitizationMode_CostSave,
        QubitizationMode_SaveBits,
        QubitizationMode_ImproveQuality,
        QubitizationMode_Disabled
    }

    private static MMQFQubitizer ourInstance = new MMQFQubitizer();

    public static MMQFQubitizer getInstance() {
        return ourInstance;
    }

    private  MMQFQubitizer() {
        MMLogger.d(TAG, "About to create the Qubit Engine.");
        qubitIntf = MMQFQubitEngineFactory.CreateQubitEngine();
        assert (qubitIntf != null);
        qubitStatisticsInterface = qubitIntf.getStatisticsInterface();
        qubitConfigurationInterface = qubitIntf.getConfigurationInterface();
    }

    public interface onInitializationCompleteListener {
        public void onInitializationComplete(MMQFQubitStatusCode status, String description);
    }

    public static String getVersion(){
        String version = "";
        if(MMQFQubitizer.getInstance().qubitIntf != null) {
            version = MMQFQubitizer.getInstance().qubitIntf.getSDKVersion();
        }else{
            MMLogger.e(TAG, "Qubitizer - Qubit Interface is NULL, Set version to UNKNOWN ...");
            version = "UNKNOWN";
        }
        return version;
    }

    public MMQFQubitStatusCode registerQBRSDK(String pName, String cID, String compName, String subsID, String dName){
        playerName = pName;
        customerID = cID;
        componentName = compName;
        subscriberId = subsID;
        domainName = dName;
        ConfigureQubitInterface();
        return new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess);
    }

    void ConfigureQubitInterface(){
        HashMap<String, String> kvps = new HashMap<String, String>();
        kvps.put(KCustomerID, customerID);
        kvps.put(KPlayerName, playerName);
        kvps.put(KDomainName, domainName);
        kvps.put(KComponentName, componentName);
        kvps.put(KSubsID, subscriberId);
        qubitConfigurationInterface.configureQubitEngine(kvps);
    }

    private boolean IsSupportedContent(String manifestURL){
        if (manifestURL.endsWith("/Manifest") || manifestURL.endsWith(".m3u8") || manifestURL.endsWith(".mpd")){
            return true;
        }
        return false;
    }

    public Integer initializeQubitizer(QubitizationMode qubitizationMode, String manifestURL, String qbrMetaURL, onInitializationCompleteListener observer, String assetID, String assetName){
        if(qubitIntf == null) {
            MMLogger.e(TAG, "Fatal - Failed to create Qubit Engine");
            return -1;
        }

        qubitIntf.invalidateQubitSession();

        appTrackId2MMTrackId = new HashMap<Integer, Integer>();
        mmTrackId2AppTrackid = new HashMap<Integer, Integer>();

        if( (qubitStatisticsInterface == null) || (qubitConfigurationInterface == null) ) {
            MMLogger.e(TAG, "Could not get Statistics or Configuration Interface");
            return -1;
        }

        if(manifestURL == null){
            return -1;
        }

        int qubitMode = MMQFQubitConfigurationInterface.QubitMode_Quality;
        if(qubitizationMode == QubitizationMode.QubitizationMode_SaveBits){
            qubitMode = MMQFQubitConfigurationInterface.QubitMode_Bits;
        }else if(qubitizationMode == QubitizationMode.QubitizationMode_CostSave){
            qubitMode = MMQFQubitConfigurationInterface.QubitMode_CostSave;
        }else if(qubitizationMode == QubitizationMode.QubitizationMode_CostSave){
            qubitMode = MMQFQubitConfigurationInterface.QubitMode_CostSave;
        }if(qubitizationMode == QubitizationMode.QubitizationMode_Disabled){
            qubitMode = MMQFQubitConfigurationInterface.QubitMode_Disabled;
        }
        qubitConfigurationInterface.setQubitMode(qubitMode);

        ConfigureQubitInterface();

        qubitIntf.setOnInitializationCompleteListener(this);
        qubitIntf.setOnQubitEngineErrorEventOccurred(this);
        qubitIntf.setOnQubitEngineInfoEventOccurredListener(this);
        qubitizerInitialized = false;

        try {
            presentationAdaptationSet = null;
            mmPresentationAdaptationSet = null;
            listener = observer;
            MMQFQubitStatusCode result = qubitIntf.initializeSDK((manifestURL!=null)?new URL(manifestURL):null, (qbrMetaURL!=null && qbrMetaURL.trim().length()>0)?new URL(qbrMetaURL):null, assetID, assetName);
            if(result.status() != MMQFQubitStatusCode.MMQFPending) {
                MMLogger.e(TAG, "Could not Q initialization command on SDK - statusCode" + result.status());
                return -1;
            }
            else {
                qubitInitializationCallbackPending = true;
                MMLogger.d(TAG, "Queued the initialization SDK command, waiting for callback");
            }
        }
        catch (MalformedURLException e) {
            MMLogger.e(TAG, "Malformed URL Exception ...");
            return -1;
        }
        return 1;
    }

    public static void disableManifestsFetch(boolean disable){
        try{
            MMQFQubitizer.getInstance().qubitConfigurationInterface.disableManifestsFetch(disable);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void cancelQubitInitialization(){
        qubitIntf.cancelQubitInitialization();
    }

    public void setPresentationInformation(MMPresentationInfo presentationInfo){
        MMQFPresentationVideoTrackInfo presentationVideoTracks[] = new MMQFPresentationVideoTrackInfo[presentationInfo.representations.size()];
        String maxResStr = "Unknown";
        String minResStr = "Unknown";
        int maxRes = -1;
        int minRes = -1;
        for (int i = 0; i < presentationInfo.representations.size(); i++) {
            MMQFPresentationVideoTrackInfo info = new MMQFPresentationVideoTrackInfo(presentationInfo.representations.get(i));
            presentationVideoTracks[i] = info;
            if (info.height > 0 && info.width > 0){
                int res = info.height * info.width;
                if(minRes <0 || (res < minRes) ){
                    minRes = res;
                    minResStr = "" + info.width + "x" + info.height;
                }

                if(res > maxRes){
                    maxRes = res;
                    maxResStr = "" + info.width + "x" + info.height;
                }
            }
        }

        qubitIntf.setPresentationInformation(presentationInfo);
        if(presentationInfo.representations.size() > 0) {
            setPresentationVideoTracks(presentationVideoTracks);
        }


        if(presentationInfo.duration > 0) {
            SDKExperienceProbe.getInstance().setDuration(presentationInfo.duration);
        }else if(presentationInfo.isLive){
            SDKExperienceProbe.getInstance().setPresentationLive(true);
        }

        //Call the method
        String formatStr = "Unknown";
        SDKExperienceProbe.getInstance().onPresentationInformationReceivedExternal(presentationInfo.duration, presentationInfo.isLive, formatStr, presentationInfo.representations.size(), minResStr, maxResStr, -1.0, -1.0);
    }


    public void setPresentationVideoTracks(MMQFPresentationVideoTrackInfo[] presentationVideoTracks){
        presentationAdaptationSet = presentationVideoTracks;
        adaptationSetSz = presentationAdaptationSet.length;
        associateAppTracksWithMMTracks();
    }

    public MMRepresentation getRepresentationInfo(int bitrate, int trackId){

        if(presentationAdaptationSet != null && presentationAdaptationSet.length > 0){
            for(int i=0; i < presentationAdaptationSet.length; i++){
                if(bitrate > 0 && presentationAdaptationSet[i].bitrate == bitrate){
                    return new MMRepresentation(presentationAdaptationSet[i].trackIndex,
                            presentationAdaptationSet[i].bitrate,
                            presentationAdaptationSet[i].width,
                            presentationAdaptationSet[i].height,
                            presentationAdaptationSet[i].codecInfo);
                }else if(trackId >= 0 && presentationAdaptationSet[i].trackIndex == trackId){
                    return new MMRepresentation(presentationAdaptationSet[i].trackIndex,
                            presentationAdaptationSet[i].bitrate,
                            presentationAdaptationSet[i].width,
                            presentationAdaptationSet[i].height,
                            presentationAdaptationSet[i].codecInfo);
                }
            }
        }
        return null;
    }

    public void associateAppTracksWithMMTracks(){
        synchronized(this) {
            //if (mmTrackId2AppTrackid.size() == 0 && appTrackId2MMTrackId.size() == 0)
            {
                populateQEnginePresentationInfo();
                if (mmPresentationAdaptationSet != null && presentationAdaptationSet != null) {
                    int mmPresentationSetSz = mmPresentationAdaptationSet.length;
                    for (int ii = 0; ii < adaptationSetSz; ii++) {
                        for (int jj = 0; jj < mmPresentationSetSz; jj++) {
                            if (presentationAdaptationSet[ii].bitrate == mmPresentationAdaptationSet[jj].bitrate) {
                                appTrackId2MMTrackId.put(ii, jj);
                                mmTrackId2AppTrackid.put(jj, ii);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public int getAppTrackIdFromMMTracks(int bitrate){
        synchronized(this) {
            if (mmTrackId2AppTrackid.size() != 0 && appTrackId2MMTrackId.size() != 0) {
                if (mmPresentationAdaptationSet != null && presentationAdaptationSet != null) {
                    int mmPresentationSetSz = mmPresentationAdaptationSet.length;
                    for (int ii = 0; ii < adaptationSetSz; ii++) {
                        for (int jj = 0; jj < mmPresentationSetSz; jj++) {
                            if (presentationAdaptationSet[ii].bitrate == bitrate) {
                                //return appTrackId2MMTrackId.get(ii);
                                return ii;
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    public int getAppBitrateFromMMTrackId(int TrackId){
        synchronized(this) {
            if (mmTrackId2AppTrackid.size() != 0 && appTrackId2MMTrackId.size() != 0) {
                if (mmPresentationAdaptationSet != null && presentationAdaptationSet != null) {
                    return presentationAdaptationSet[TrackId].bitrate;
                }
            }
        }
        return -1;
    }

    public void setSubscriberId(String subscriber){
        subscriberId = subscriber;
        if(qubitIntf != null && subscriber != null && subscriber.trim().length() != 0){
            qubitIntf.setSubscriberID(subscriber);
        }
    }

    public void setSubscriber(String subscriber, String subscriberType){
        subscriberId = subscriber;
        if(qubitIntf != null){
            qubitIntf.setSubscriber(subscriber, subscriberType);
        }
    }
    public void setSubscriber(String subscriber, String subscriberType, String subscriberTag){
        subscriberId = subscriber;
        if(qubitIntf != null){
            qubitIntf.setSubscriber(subscriber, subscriberType,subscriberTag);
        }
    }

    public Integer getQBRBandwidth(Integer qbrRepresentationTrackIdx, Integer defaultBandwidth, Long bufferLength, Long playbackPosition){
        Integer retval = defaultBandwidth;
        if (appTrackId2MMTrackId.size() > 0 || appTrackId2MMTrackId.get(qbrRepresentationTrackIdx) != null){
            int mmRepresentationTrackIdx = appTrackId2MMTrackId.get(qbrRepresentationTrackIdx);
            if(mmRepresentationTrackIdx != -1) {
                retval = qubitIntf.getQubitBandwidthRequirementsForProfile(mmRepresentationTrackIdx, (int) (playbackPosition / 1000), (int) (bufferLength / 1000));
            }
        }
        return retval;
    }

    public void updateDownloadRate(Long downloadRate){
        QubitSessionStats stats = QubitSessionStats.instance(qubitIntf.getStatisticsInterface());
        stats.updateDownloadRate(downloadRate);
    }

    public MMQFQubitRangedResouce getQubitResource(MMQFQubitRangedResouce rangedResource){
        //get qubit url
        String startIdxTag = "?startByte=";
        String segLenTag = "&endByte=";
        String inUri = null;
        MMQFQubitEngineInterface.MMQFQubitResource qubitResource = null;
        String rangedURL = rangedResource.uri + startIdxTag + rangedResource.start + segLenTag + (rangedResource.start + rangedResource.length - 1);
        inUri = (rangedResource.length <= 0) ? rangedResource.uri : rangedURL;
        qubitResource = new MMQFQubitEngineInterface.MMQFQubitResource();
        qubitResource.regularUrl = inUri;
        if (rangedResource.trackIdx != -1){
            qubitResource.hintInTrackidx = appTrackId2MMTrackId.get(rangedResource.trackIdx);
            MMLogger.i(TAG, "getQBRChunk - In Tracks:  [" + rangedResource.trackIdx + "->" + qubitResource.hintInTrackidx + "]");
        }else{
            qubitResource.hintInTrackidx = null;
        }

        qubitResource.hintInSeqNum = (rangedResource.seqIdx !=-1) ? new Integer(rangedResource.seqIdx) : null;
        String qubitizedURL = qubitIntf.getQubitResource(qubitResource);

        if( (qubitResource.trackIndex != null) && qubitResource.trackIndex != -1) {
            Integer mmTrackIdx = qubitResource.trackIndex;
            qubitResource.trackIndex = mmTrackId2AppTrackid.get(qubitResource.trackIndex);
            MMLogger.i(TAG, "getQBRChunk - Out Tracks:  [" + mmTrackIdx + "->" + qubitResource.trackIndex + "]");
            if (qubitResource.hintInTrackidx != null && qubitResource.hintInSeqNum != null){
                MMLogger.i(TAG, "getQBRChunk - Mapping:  [ " + qubitResource.hintInSeqNum + " |" + qubitResource.hintInTrackidx + "->" + mmTrackIdx + "]");
            }else if(inUri!= null && qubitizedURL != null){
                MMLogger.i(TAG, "getQBRChunk - Mapping:  [ " + ((inUri.compareTo(qubitizedURL) == 0)?"Identity    ":"QBR Switched" ) + " | " +  inUri + " -> " + qubitizedURL + "]");
            }
        }

        MMQFQubitRangedResouce retval = null;
        if(qubitResource.trackIndex!=null) {
            retval = new MMQFQubitRangedResouce(qubitizedURL, -1, -1, qubitResource.trackIndex!= null?qubitResource.trackIndex: -1, qubitResource.hintInSeqNum!= null ? qubitResource.hintInSeqNum: -1);
        }

        //save the statistics ...
        QubitSessionStats sessionStats = QubitSessionStats.instance(qubitIntf.getStatisticsInterface());
        if (sessionStats != null && inUri != null && inUri.length() > 0) {
            MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo = null;
            if (qubitResource.hintInSeqNum == null || qubitResource.hintInTrackidx == null ) {
                sessionStats.saveStatsForUrl(inUri.toString(), null);
            }else{
                segmentInfo = new MMQFQubitPresentationInfoRetriever.SegmentInfoForURL();
                segmentInfo.segmentIndex = qubitResource.hintInSeqNum.intValue();
                segmentInfo.videoTrackInfo = mmPresentationAdaptationSet[qubitResource.hintInTrackidx.intValue()];
                sessionStats.saveStatsForUrl(inUri!=null?inUri.toString():null, segmentInfo);
            }
        }
        return retval;
    }


    public interface onSDKRegisterationCompleteObserver {
        public void onSDKRegisterationComplete(MMQFQubitStatusCode status);
    }

    onSDKRegisterationCompleteObserver sdkRegistrationObserver;

    @Override
    public void onInitializationComplete(MMQFQubitStatusCode status, String description) {
        qubitInitializationCallbackPending = false;
        if(status.status() == MMQFQubitStatusCode.MMQFSuccess) {
            QubitSessionStats sessionStats = QubitSessionStats.instance(qubitIntf.getStatisticsInterface());
            if(sessionStats != null)
            {
                sessionStats.registerPresentationBitrates(qubitIntf.getPresentationInformation());
            }
            MMLogger.d(TAG, "Initialization completed with successfully");
            qubitizerInitialized = true;
            associateAppTracksWithMMTracks();
        }
        if(listener != null) {
            listener.onInitializationComplete(status, description);
        }
    }

    @Override
    public void onQubitEngineErrorEventOccurred(MMQFQubitErrorEvent errorEvt) {
        MMLogger.e(TAG, "onQubitEngineErrorEventOccurred - Code = " + errorEvt.code);
    }

    @Override
    public void onQubitEngineInfoEventOccurred(MMQFQubitInfoEvent infoEvt) {
        MMLogger.i(TAG, "onQubitEngineInfoEventOccurred - Code = " + infoEvt.code);
    }

    /**
     * Some players may blacklist some of the representations set in setPresentationVideoTracks
     * This will be implemented in better way in future.
     * As of now the SDK will filter recommendation based on this input
     */
    public void blacklistRepresentation(int trackId, boolean blacklist){
        if (appTrackId2MMTrackId != null && trackId != -1 && qubitIntf != null){
            if(appTrackId2MMTrackId.size() > trackId) {
                qubitIntf.blacklistRepresentation(appTrackId2MMTrackId.get(trackId), blacklist);
            }
        }
    }

    private void populateQEnginePresentationInfo(){
        if (mmPresentationAdaptationSet == null){
            if (qubitIntf!= null){
                MMQFPresentationInfo presInfo = qubitIntf.getPresentationInformation();
                if (presInfo != null){
                    int trackCnt = presInfo.getVideoTracksCount();
                    if (trackCnt > 0) {
                        mmPresentationAdaptationSet = new MMQFPresentationVideoTrackInfo[trackCnt];
                        for (int i = 0; i< trackCnt; i++){
                            mmPresentationAdaptationSet[i] = presInfo.getVideoTrack(i);
                        }
                    }else {
                        mmPresentationAdaptationSet = null;
                    }
                }
            }
        }
    }

    ///////////////////Registration stuff////////////////////
    private static String customerID = null;
    private static String subscriberId = null;
    private static String playerName = null;
    private static String componentName = null;
    private static String domainName = null;

    private static String KCustomerID = "CustomerID";
    private static String KSubsID = "SubscriberID";
    private static String KPlayerName = "PlayerName";
    private static String KComponentName = "ComponentName";
    private static String KDomainName = "DomainName";
    /////////////////////////////////////////////////////////

    private onInitializationCompleteListener listener = null;
    private MMQFQubitEngineInterface qubitIntf = null;
    private MMQFQubitStatisticsInterface qubitStatisticsInterface = null;
    private MMQFQubitConfigurationInterface qubitConfigurationInterface = null;

    private boolean qubitizerInitialized = false;
    private boolean qubitInitializationCallbackPending = false;
    private static String TAG = "Qubitizer";

    private MMQFPresentationVideoTrackInfo[] presentationAdaptationSet;
    private MMQFPresentationVideoTrackInfo[] mmPresentationAdaptationSet;

    private Map<Integer, Integer> appTrackId2MMTrackId = null;
    private Map<Integer, Integer> mmTrackId2AppTrackid = null;

    private int adaptationSetSz = 0;



}
