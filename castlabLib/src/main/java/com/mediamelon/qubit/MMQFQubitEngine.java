package com.mediamelon.qubit;

import android.content.Context;

import com.mediamelon.qubit.ep.*;
import com.mediamelon.smartstreaming.MMPresentationInfo;
//import com.mediamelon.qubit.ep.RegisterResponse;
//import com.mediamelon.qubit.ep.SDKExperienceProbe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//import com.mediamelon.qubit.ep.integration.EPIntegration;

public class MMQFQubitEngine implements MMQFQubitEngineInterface
        , MMQFQubitStatisticsInterface
        , MMQFQubitConfigurationInterface
        , MMQFQubitPresentationInfoRetriever.OnQubitPresentationInfoRetrievedListener
        , MMQFQubitModel.OnQubitModelCreatedListener
        , RegisterAPI.onRegistrationCompleteObserver{

    public class VERSION {
        public static final int SDK_VERSION = 1;
        // Value of BUILD_VERSION is generated at build time.
        // Do not promote changes on it, keep it value as "NOT SPECIFIED"
        // when you promote
        public static final String BUILD_VERSION = "05022016.EP-1.0";
        public static final String METADATA_VERSION = "05022016.EP-1.0";
    }

    private Long sdkBootupTime;
    URL prsnttnMnfstURL;
    String hintFileURL;
    Context appContext;
    String _assetID;
    String _assetName;
    public static MMQFQubitEngine getInstance()
    {
        return instance;
    }
    public String getMetaDataVersion() {return VERSION.METADATA_VERSION;}
    public String getSDKVersion()
    {
        return PropertyReader.getInstance().getProperty("Version");
    }

    //Implementation of MMQFQubitEngineInterface begins
    public MMQFQubitStatisticsInterface getStatisticsInterface()
    {
        return this;
        //if(isQubitInitialized())return this;
        //else return null;
    }

    public  MMQFQubitConfigurationInterface getConfigurationInterface()
    {
        return this;
    }

    public boolean isQubitInitialized()
    {
        return isInitialized;
    }

    public void setOnInitializationCompleteListener(OnInitializationCompleteListener listener)
    {
        initializationCompleteListener = listener;
    }

    public void setCustomerID(int aCustomerID){
        customerID = aCustomerID;
    }
    
    private static String generateHintFileNameFromResponse(String hintfileName, String assetId, String assetName, String title) {
      String newhintfileName = hintfileName;
      if(hintfileName != null && !hintfileName.isEmpty()){
        if(newhintfileName.indexOf("$") != -1){
          if(assetId != null && !assetId.isEmpty()){
            newhintfileName = newhintfileName.replace("$assetid",assetId);
          }
          if(assetName != null && !assetName.isEmpty()){
            newhintfileName = newhintfileName.replace("$assetname",assetName);
          }
          if(title != null && !title.isEmpty()){
            newhintfileName = newhintfileName.replace("$title",title);
          }
        }
      }
      return newhintfileName;
    }
    private String getHintFileUrlFromResponse(String manifestURL, String metafileURL, RegisterResponse registerResponse) {
        if(metafileURL == "" || metafileURL == null)
        {
            String baseHintUrl = "";
            String finalHintfileName = "";
            String finalHintFileUrl = "";
            if((registerResponse != null) && (manifestURL != null) && (!manifestURL.isEmpty())){
                String title = null;
                int lastIndex = manifestURL.lastIndexOf("/");
                if (lastIndex != -1) {
                    String hintFileURL ="";
                    int extensionIndex = manifestURL.indexOf(".", lastIndex);
                    if(extensionIndex >= 0){
                        title = manifestURL.substring(lastIndex + 1, extensionIndex);
                    }
                }

                if((registerResponse.hintfileName != null) && 
                    (!registerResponse.hintfileName.isEmpty())){
                    
                    if((registerResponse.metaFileMap != null)
                    && (registerResponse.metaFileMap.contentServer != null)
                    && (registerResponse.metaFileMap.hintfileServer != null)
                    && (!registerResponse.metaFileMap.contentServer.isEmpty())
                    && (!registerResponse.metaFileMap.hintfileServer.isEmpty())
                    ){
                        baseHintUrl = manifestURL.replaceAll(registerResponse.metaFileMap.contentServer, registerResponse.metaFileMap.hintfileServer);
                    }
                    if((registerResponse.metaFileMap != null)
                        && ((registerResponse.metaFileMap.contentServer == null) || (registerResponse.metaFileMap.contentServer.isEmpty()))
                        && (registerResponse.metaFileMap.hintfileServer != null)
                        && (!registerResponse.metaFileMap.hintfileServer.isEmpty())
                    ){
                        baseHintUrl = registerResponse.metaFileMap.hintfileServer;
                    }
                    
                    finalHintfileName = generateHintFileNameFromResponse(registerResponse.hintfileName, _assetID , _assetName, title);
                }
                
                if(finalHintfileName.isEmpty()){
                    finalHintfileName = title + ".qbr";
                }
                
                if(!baseHintUrl.isEmpty()){
                    if (baseHintUrl.indexOf("/") != -1) {
                      baseHintUrl = baseHintUrl.substring(0, baseHintUrl.lastIndexOf("/") + 1);
                    }
                }
                else{
                    if (manifestURL.indexOf("/") != -1) {
                      baseHintUrl = manifestURL.substring(0, manifestURL.lastIndexOf("/") + 1);
                    }
                }
                // Create Full URL here
                finalHintFileUrl = baseHintUrl + finalHintfileName;
                return finalHintFileUrl;
            }
        }
        return metafileURL;
    }

    private static String profilingTag = "MMSmartStreaming.Profile";
    public void onRegisterComplete(RegisterResponse registerResponse, RegisterAPI.RegisterAPIStatusCode statusCode) {
        synchronized (this) {
            regResponse = registerResponse;
            MMLogger.p(profilingTag, "Registration Complete - " + System.currentTimeMillis());
            sessionObservor.onRegistrationWithSmartSightCompleted();
            if (registerResponse != null) {
                if (SDKExperienceProbe.getInstance().qbrXResInfo().mode.equals("QBRDisabled")) {
                    qubitisationDisabled = true;
                } else {
                    qubitisationDisabled = false;
                }
                // QPS-1148 Get the hintfile URL from backend reg response
                hintFileURL = getHintFileUrlFromResponse(prsnttnMnfstURL.toString(),hintFileURL,regResponse);
                proceedInitSDK();
            } else {
                if (statusCode.status() == RegisterAPI.RegisterAPIStatusCode.MMQFCancelled) {
                    //Stop the path here..., better to handle it all the way up, and notify the user ...
                    //But user not interested to handle it :(
                    MMLogger.d("MMSmartStreaming.QEngine", "Terminating Cancelled Initialization (Registration).");
                } else {
                    sdkBootupTime = System.currentTimeMillis() - sdkBootupTime;
                    MMQFQubitStatusCode code = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure);
                    sdkExperienceProbe.notifyMMSmartStreamingSDKInitialized(code);
                    initializationCompleteListener.onInitializationComplete(code, "Session Init with backend failed.");
                }
            }
        }
    }

    public  void invalidateQubitSession(){
        isInitialized = false;
        presentationInfo = null;
        if (qubitModel != null){
            qubitModel.CancelPendingRequests();
        }
        qubitModel = null;
        contentNotSupported = false;
        qubitNotRequired = false;
        if (presentationInfoRetriever != null){
            presentationInfoRetriever.CancelPendingRequests();
        }
        presentationInfoRetriever = null;
        regResponse = null;
    }

    public String getCustomerID(){
        return PropertyReader.getInstance().getProperty("CustomerId");
    }


    private boolean IsSupportedContent(String manifestURL){
        if (manifestURL.endsWith("/Manifest") || manifestURL.endsWith(".m3u8") || manifestURL.endsWith(".mpd")){
            return true;
        }
        return false;
    }

    private MMSmartStreamingRegistrationObservor sessionObservor = null;
    public MMQFQubitStatusCode initializeSDK (URL prsntMnfstURL, URL qbrMetaFileURL, String assetID, String assetName) {
        synchronized(this) {
            initCalled = true;
            initExecutionPending = false;
            presInfoExternal = null;
            disablePresentationFetch = disableManifestFetch;
            _assetID = assetID;
            _assetName = assetName;

            MMLogger.p("ProfileQubit", "Starting Initialization");
            MMLogger.v("EPIntegration", "InitializationSDK now calling register execute..");

            sdkBootupTime = System.currentTimeMillis();
            MMLogger.p("MMSmartStreaming.Profile", "SDK init started at - " + sdkBootupTime);
            prsnttnMnfstURL = prsntMnfstURL;
            hintFileURL = (qbrMetaFileURL != null) ? qbrMetaFileURL.toString() : null;
            qubitisationDisabled = true;
            sessionObservor = SDKExperienceProbe.getInstance();
            SDKExperienceProbe.getInstance().initializeEPSDK(prsntMnfstURL.toString(), getModeStrRepresentation(qubitMode), this);
        }
        return  new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFPending);
    }

    String getModeStrRepresentation(int mode){
        String retval = "Unknown";
        if (mode == 0){
            retval = "QBRBitsave";
        }else if(mode == 1){
            retval = "QBRQuality";
        }else if(mode == 2){
            retval = "QBRCostsave";
        }else if(mode == 3){
            retval = "QBRDisabled";
        }
        return retval;
    }

    public void resetStatistics(){
        if(qubitModel != null){
            qubitModel.resetRunningStatistics();
        }
    }

    public MMQFQubitStatusCode proceedInitSDK()
    {
        MMQFQubitStatusCode retval = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFPending);

        if(!initCalled) {
            initInProgress = false;
            initCalled = true;
            return retval;
        }

        presentationManifestURL = prsnttnMnfstURL;
        presentationMetadataURL = null;
        if(presentationManifestURL!=null) {

            String manifestURL = presentationManifestURL.toString();
            //Create hint file path out of manifest URL, however, there is no connection and hint file can be anywhere
            //Find second backslash
            int lastIndex = manifestURL.lastIndexOf("/");
            if((lastIndex != 1) && (hintFileURL == null))
            {
                int mpdIndex = manifestURL.indexOf(".mpd", lastIndex);
                if(mpdIndex >= 0){
                    hintFileURL = manifestURL.substring(0, mpdIndex) + ".qbr";
                }else {
                    int m3u8Index = manifestURL.indexOf(".m3u8", lastIndex);
                    if (m3u8Index >= 0){
                        hintFileURL = manifestURL.substring(0, m3u8Index) + ".qbr";
                    }else {
                        hintFileURL = manifestURL.substring(0, lastIndex)  + "/meta.qbr";
                    }
                }
                String ismName = ".ism/";
                int ismIndex = manifestURL.lastIndexOf(ismName);
                if(ismIndex != -1) {
                    if (ismIndex == (lastIndex - ismName.length() + 1)) {
                        lastIndex = manifestURL.lastIndexOf("/", lastIndex - 1);//MSS Served via IIS
                        if (lastIndex != -1) {
                            MMLogger.e(LOG_TAG,"See it has set meta.qbr");
                            hintFileURL = manifestURL.substring(0, lastIndex) + "/meta.qbr";
                        }
                    }
                }
            }
            if(hintFileURL!=null){
                try {
                    presentationMetadataURL = new URL(hintFileURL);
                }
                catch(MalformedURLException exp){
                    MMLogger.e("QubitIntgr", "Malformed hint file url - " + hintFileURL);
                }
            }else{
            }
        }
        isInitialized = false;
        initSubState = InitializationSubState.RequestPresentationInfo;
        completeInitialization();
        return retval;
    }

    public void configureQubitEngine(HashMap<String, String> kvps){
        PropertyReader.getInstance().setProperties(kvps);
    }

    private Boolean completeInitialization()
    {
        switch (initSubState)
        {
            case RequestPresentationInfo:
            {
                initExecutionPending = true;
                if (disablePresentationFetch == false) {
                    MMLogger.p(profilingTag, "Presentation fetch starts " + System.currentTimeMillis());
                    assert (presentationInfoRetriever == null);
                    presentationInfoRetriever = new MMQFQubitPresentationInfoRetriever(presentationManifestURL);
                    presentationInfoRetriever.setOnQubitPresentationInfoRetrieved(this);
                    initExecutionPending = false;
                    presentationInfoRetriever.RetrievePresentationInfo();
                } else {
                    if (presInfoExternal != null) {
                        initExecutionPending = false;
                    }
                }
                if (disablePresentationFetch == true && presInfoExternal != null){
                    if(presInfoExternal.isLivePresentation() || presInfoExternal.getVideoTracksCount() > 0 && presInfoExternal.getVideoTrack(0).getSegmentCount() > 0) {
                        onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess), presInfoExternal);
                    }else{
                        onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFVideoRepresentationInfoNotAvl), presInfoExternal);
                    }
                }
            }
            break;
            case CreateQubitModel:
            {
                assert(qubitModel == null);
                MMLogger.p(profilingTag, "Qubit Model Creation starts " + System.currentTimeMillis());
                qubitModel = new MMQFQubitModel(presentationInfo, presentationMetadataURL, (qubitisationDisabled== true)?MMQFQubitConfigurationInterface.QubitMode_CostSave:qubitMode, regResponse);
                qubitModel.setOnQubitModelCreatedListener(this);
                qubitModel.CreateQubitModel();
            }
            break;
            case Initialized:
            {
                sdkBootupTime = System.currentTimeMillis() - sdkBootupTime;
                sdkExperienceProbe.notifyMMSmartStreamingSDKInitialized(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess));
                initializationCompleteListener.onInitializationComplete(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess), SDKExperienceProbe.getInstance().qbrXResInfo().mode);
                isInitialized = true;
                if(qubitModel != null) {
                    qubitModel.LogQubitModel();
                }
                if(presentationInfo != null) {
                    sdkExperienceProbe.setDuration(new Long(presentationInfo.getDuration()));
                }
                MMLogger.p(profilingTag, "Initialization Completed -  " + sdkBootupTime);
            }
            break;
            default:
            {
                assert (false);
            }
        }
        return true;
    }

    public void cancelQubitInitialization(){
        synchronized(this) {
            if (disablePresentationFetch == true && presInfoExternal == null) {
                MMQFPresentationInfo presInfo = new MMQFPresentationInfo("Unknown", -1, -1, false, -1, 1000);
                presInfoExternal = presInfo;
                if(initExecutionPending) {
                    initExecutionPending = false;
                    onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFVideoRepresentationInfoNotAvl), null);
                }
            }
        }
    }

    public void setPresentationInformation(MMPresentationInfo presentationInfo) {
        synchronized(this) {
            if(blacklistedTracks.size() > 0){
                blacklistedTracks.clear();
            }
            if (presInfoExternal == null) {
                MMQFPresentationInfo presInfo = new MMQFPresentationInfo("Unknown", -1, -1, presentationInfo.isLive, presentationInfo.duration, 1000);
                for (int i = 0; i < presentationInfo.representations.size(); i++) {
                    MMQFPresentationVideoTrackInfo info = new MMQFPresentationVideoTrackInfo(presentationInfo.representations.get(i));
                    presInfo.addVideoPresentationTrack(info.width, info.height, info.codecInfo, info);
                }
                presInfoExternal = presInfo;
            }


            if (initExecutionPending == true) {
                initExecutionPending = false;

                if (presInfoExternal != null){
                    if(presInfoExternal.isLivePresentation() || ( presInfoExternal.getVideoTracksCount() > 0 && presInfoExternal.getVideoTrack(0).getSegmentCount() > 0)) {
                        onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess), presInfoExternal);
                    }else{
                        onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFVideoRepresentationInfoNotAvl), presInfoExternal);
                    }
                }
            }
        }
    }

    public MMQFPresentationInfo getPresentationInformation() {
        return presentationInfo;
    }

    public void setOnQubitEngineInfoEventOccurredListener(OnQubitEngineInfoEventListener listener) {
        infoEventListener = listener;
    }

    public void setOnQubitEngineErrorEventOccurred(OnQubitEngineErrorEventListener listener) {
        errorEventListener = listener;
    }

    public String getQubitResource(MMQFQubitResource qResource) {
        String retval = qResource.regularUrl;
        if (presentationInfoRetriever != null) {
            MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo = null;
            if (qResource.hintInSeqNum == null || qResource.hintInTrackidx == null ) {
                segmentInfo = presentationInfoRetriever.getSegmentInfoForURL(qResource.regularUrl);
            }else{
                segmentInfo = new MMQFQubitPresentationInfoRetriever.SegmentInfoForURL();
                segmentInfo.videoTrackInfo = getPresentationInformation().getVideoTrack(qResource.hintInTrackidx.intValue());
                if (segmentInfo.videoTrackInfo != null) {
                    segmentInfo.segmentIndex = qResource.hintInSeqNum.intValue() - segmentInfo.videoTrackInfo.startSequenceNum();
                }else{
                    segmentInfo.segmentIndex = -1;
                }
            }

            if (isInitialized && (qubitisationDisabled == false) &&  segmentInfo != null) {
                retval = qubitModel.getQubitUrl(qResource.regularUrl, segmentInfo, qResource);

                for (int var : blacklistedTracks)
                {
                    if(var == segmentInfo.qbrTrackIndex){
                        // Identity for blacklisted track
                        segmentInfo.qbrTrackIndex = segmentInfo.cbrTrackIndex = qResource.trackIndex = qResource.hintInTrackidx;
                        retval = qResource.regularUrl;
                        break;
                    }
                }
            }

            if(segmentInfo != null && retval != null) {
                MMLogger.i("getQubitResource", "Impl Mapping: [" + segmentInfo.segmentIndex + " | " + segmentInfo.cbrTrackIndex + " -> " + segmentInfo.qbrTrackIndex + "]");
            }
            sdkExperienceProbe.onSegRequest(qResource.regularUrl, segmentInfo);
        }
        qResource.regularUrl = retval;
        return retval;
    }

    public int getQubitBandwidthRequirementsForProfile(int representationProfileID, int playbackPos, int bufferLength)
    {
        int retval = 0;
        MMQFPresentationVideoTrackInfo trackInfo = presentationInfo.getVideoTrack(representationProfileID);
        if(isInitialized && (qubitisationDisabled == false) && presentationInfoRetriever != null) {
            retval = qubitModel.getQubitBandwidthRequirementsForProfile(trackInfo, playbackPos, bufferLength);
        }
        else {
            //Send the bandwidth for the input profile
            retval = trackInfo.bitrate;
        }
        return retval;
    }
    //Implementation of MMQFQubitEngineInterface ends

    boolean contentNotSupported = false;
    /**
     * Some players may blacklist some of the representations set in setPresentationVideoTracks
     * This will be implemented in better way in future.
     * As of now the SDK will filter recommendation based on this input
     */
    public void blacklistRepresentation(int trackId, boolean blacklist){
        if (trackId != -1){
            if (blacklist) {
                blacklistedTracks.add(new Integer(trackId));
            }else{
                if(blacklistedTracks.size() > trackId) {
                    blacklistedTracks.remove(new Integer(trackId));
                }
            }
        }
    }
    //Implementation of OnQubitPresentationInfoRetrievedListener
    public void onQubitPresentationInfoRetrieved(MMQFQubitStatusCode code, MMQFPresentationInfo aPresentationInfo)
    {
        if(code.status() == MMQFQubitStatusCode.MMQFSuccess)
        {
            sessionObservor.onPresentationInformationReceived(new Long(aPresentationInfo.getDuration()), aPresentationInfo.isLivePresentation(), aPresentationInfo.getStreamingFormat(), aPresentationInfo.getVideoTracksCount(), aPresentationInfo.getMinRes(), aPresentationInfo.getMaxRes(), aPresentationInfo.getMinFPS(), aPresentationInfo.getMaxFPS());
            if(aPresentationInfo!= null && aPresentationInfo.hasClientSideAdInsertion()){
                initSubState = InitializationSubState.Initialized;
                qubitNotRequired = true;
                completeInitialization();
                contentNotSupported = true;
                presentationInfoRetriever = null;
                SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-HasAdvertisements";
                SDKExperienceProbe.getInstance().setPlayerMode(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
                sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
            }else if(aPresentationInfo!= null && aPresentationInfo.isLivePresentation() == true) {
                initSubState = InitializationSubState.Initialized;
                qubitNotRequired = true;
                completeInitialization();
                contentNotSupported = true;
                presentationInfoRetriever = null;
                SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-LiveSessionNotSupported";
                SDKExperienceProbe.getInstance().setPlayerMode(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
                sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
            }else {
                    initSubState = InitializationSubState.CreateQubitModel;
                    presentationInfo = aPresentationInfo;
                    presentationInfo.AlignTrackInfoAsPerMetaFileExpectations();
                    completeInitialization();
            }

        }else if(code.status() == MMQFQubitStatusCode.MMQFVideoRepresentationInfoNotAvl){
            initSubState = InitializationSubState.Initialized;
            qubitNotRequired = true;
            SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-NoTrackPresentationInfo";
            SDKExperienceProbe.getInstance().setPlayerMode(SDKExperienceProbe.getInstance().qbrXResInfo().mode);

            completeInitialization();
            contentNotSupported = true;
            presentationInfoRetriever = null;

            sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
        }
        else if(code.status() == MMQFQubitStatusCode.MMQFCancelled){
            MMLogger.d("EPIntegration", "Cancelling Initialization .. (Presentation Information Retrieval))");
        }
        else if(code.status() == MMQFQubitStatusCode.MMQFOperationNotSupported || code.status() == MMQFQubitStatusCode.MMQFABRNotSupported){
            if (aPresentationInfo != null){
                presentationInfo = aPresentationInfo;
                sessionObservor.onPresentationInformationReceived(new Long(aPresentationInfo.getDuration()), aPresentationInfo.isLivePresentation(), aPresentationInfo.getStreamingFormat(), aPresentationInfo.getVideoTracksCount(), aPresentationInfo.getMinRes(), aPresentationInfo.getMaxRes(), aPresentationInfo.getMinFPS(), aPresentationInfo.getMaxFPS());
            }
            initSubState = InitializationSubState.Initialized;
            qubitNotRequired = true;
            completeInitialization();
            contentNotSupported = true;
            presentationInfoRetriever = null;
            if(code.status() == MMQFQubitStatusCode.MMQFABRNotSupported){
                SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-NoABR";
            }else {
                SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-ContentNotSupportedForQBR";
            }

            if(aPresentationInfo!= null && aPresentationInfo.isLivePresentation() == true) {
                SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-LiveSessionNotSupported";
            }
            SDKExperienceProbe.getInstance().setPlayerMode(SDKExperienceProbe.getInstance().qbrXResInfo().mode );
            sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
        }
        else {
            SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-NoPresentationInfo";
            SDKExperienceProbe.getInstance().setPlayerMode(SDKExperienceProbe.getInstance().qbrXResInfo().mode );
            if (code.status() == MMQFQubitStatusCode.MMQFQubitNotRequired) {
                qubitNotRequired = true;
                initSubState = InitializationSubState.InitializationCancelled;
            }
            else
            {
                 if(SDKExperienceProbe.getInstance().qbrXResInfo().mode.equals("QBRDisabled")){
                      qubitNotRequired = true;
                      initSubState = InitializationSubState.InitializationCancelled;
                      sdkBootupTime = System.currentTimeMillis() - sdkBootupTime;
                      sdkExperienceProbe.notifyMMSmartStreamingSDKInitialized(code);
                      initializationCompleteListener.onInitializationComplete(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess), SDKExperienceProbe.getInstance().qbrXResInfo().mode);
                      return;
                 }

                 qubitisationDisabled = true;
                 initSubState = InitializationSubState.InitializationError;
            }

            if(code.status() == MMQFQubitStatusCode.MMQFPending)
            {
                assert (false);
            }

            sdkBootupTime = System.currentTimeMillis() - sdkBootupTime;
            sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
            sdkExperienceProbe.notifyMMSmartStreamingSDKInitialized(code);
            initializationCompleteListener.onInitializationComplete(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess)/* (qubitisationDisabled == true) ? new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess):new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure) */, SDKExperienceProbe.getInstance().qbrXResInfo().mode);
        }
    }

    //Implementation of OnQubitPresentationInfoRetrievedListener ends
    private Boolean qubitisationDisabled;
    public MMQFSegmentSizeInfo getQBRSegmentInfoIfDidQubitisation(int bitrate, long segmentStartTime)
    {
            MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo info = null;
            MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segPresentationInfo = null;
            if(isInitialized == true) {
                 if (bitrate!= -1 && segmentStartTime != -1){
                      segPresentationInfo = presentationInfoRetriever.getSegmentInfoForSegment(bitrate, segmentStartTime);
                 }
                 if (qubitModel != null && segPresentationInfo != null) {
                      info = qubitModel.getSegmentSizeInfo(segPresentationInfo);
                 }
            }
            return info;
    }

    public MMQFSegmentInfo getSegmentInfoForSegment(int trackIdx, int sequenceNum){
           MMQFQubitStatisticsInterface.MMQFSegmentInfo info = null;
           if(isInitialized) {
               if(trackIdx != -1 && sequenceNum != -1){
                   info = qubitModel.getSegmentInfo(trackIdx, sequenceNum);
               }
           }
           return info;
    }
        
    public Integer getTrackIndex(Integer bitrate){
        if(isInitialized && qubitModel!= null) {
            return qubitModel.getTrackIndex(bitrate);
        }else{
            return null;
        }
    }
        
    public Integer getSequenceIndex(int bitrate,long startTime){
           MMQFQubitPresentationInfoRetriever.SegmentInfoForURL info = null;
           if(presentationInfoRetriever!=null && isInitialized) {
               info = presentationInfoRetriever.getSegmentInfoForSegment(bitrate, startTime);
               if (info == null){
                   MMLogger.v("MMSmartStreamingIntgr.Exception", "Could not peek segment for bitrate " + bitrate + "  " + " with STime " + startTime);
               }
           }
           return info!= null?new Integer(info.segmentIndex):null;
    }
        
    public MMQFSegmentInfo getQBRSegmentInfoForSegment(int trackIndex, int sequenceIndex){
           MMQFQubitStatisticsInterface.MMQFSegmentInfo info = null;
           if(isInitialized) {
                  if(trackIndex !=-1 && sequenceIndex != -1){
                       int qbrTrackIndex = qubitModel.getQBRTrackIndex(trackIndex, sequenceIndex);
                        info = qubitModel.getSegmentInfo(qbrTrackIndex, sequenceIndex);
                  }
           }
           return info;
    }

    //Implementation of MMQFQubitConfigurationInterface begins
    public  void setQubitMode(int mode)
    {
        qubitMode = mode;
        MMLogger.i("EPIntegration SetMode:", getModeStrRepresentation(mode));
    }

    static boolean disableManifestFetch = false;
    boolean disablePresentationFetch = false;
    MMQFPresentationInfo presInfoExternal = null;
    boolean initExecutionPending = false;
    public void disableManifestsFetch(boolean disable){
        disableManifestFetch = disable;
    }

    //Implementation of MMQFQubitConfigurationInterface ends

    //Implementation of OnQubitModelCreatedListener begins
    public void onOnQubitModelCreated(MMQFQubitStatusCode status)
    {
        if(status.status() == MMQFQubitStatusCode.MMQFSuccess)
        {
            initSubState = InitializationSubState.Initialized;
            if(qubitisationDisabled){
                SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-QMetric";
            }
            sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
            completeInitialization();
        }
        else if (status.status() == MMQFQubitStatusCode.MMQFCancelled){
            MMLogger.d("EPIntegration", "Initialisation cancelled ... Skipping (Model Creation)");
        }
        else
        {
            SDKExperienceProbe.getInstance().qbrXResInfo().mode = "QBRDisabled-NoMetafile";
            SDKExperienceProbe.getInstance().setPlayerMode(SDKExperienceProbe.getInstance().qbrXResInfo().mode );
            sessionObservor.onQBRModeFinalised(SDKExperienceProbe.getInstance().qbrXResInfo().mode);
            if (status.status() == MMQFQubitStatusCode.MMQFQubitNotRequired) {
                qubitNotRequired = true;
                initSubState = InitializationSubState.InitializationCancelled;
            }
            else
            {
                initSubState = InitializationSubState.InitializationError;
            }

            if(status.status() == MMQFQubitStatusCode.MMQFPending)
            {
                assert (false);
            }
            sdkBootupTime = System.currentTimeMillis() - sdkBootupTime;
            sdkExperienceProbe.notifyMMSmartStreamingSDKInitialized(status);
            initializationCompleteListener.onInitializationComplete(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess)/*(qubitisationDisabled == true)?new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess):status*/, SDKExperienceProbe.getInstance().qbrXResInfo().mode);
        }
    }
    ////Implementation of OnQubitModelCreatedListener begins

    //Implementation of MMQFQubitStatisticsInterface begins
    public MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo getSegmentQualityInfo(String inUrl, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo)
    {
        MMQFQubitStatisticsInterface.MMQFSegmentQualityInfo info = null;
        if(isInitialized && presentationInfoRetriever!=null) {
            if(segmentInfo == null) {
                segmentInfo = presentationInfoRetriever.getSegmentInfoForURL(inUrl);
            }
            if (qubitModel != null && segmentInfo != null) {
                info = qubitModel.getSegmentQualityInfo(segmentInfo);
                if (info!= null) {
                    info.height = segmentInfo.videoTrackInfo.height;
                    info.width = segmentInfo.videoTrackInfo.width;
                }
            }
        }
        return info;
    }

    public Long getSDKBootTime() {
        if(isQubitInitialized()) {
            return sdkBootupTime;
        } else {
            return 0L;
        }
    }
    public String getVideoCodecInfo() {
        if (qubitModel != null) {
            return qubitModel.getVideoCodecInfo();
        }
        return "UNKNOWN";
    }
    public String getAudioCodecInfo() {
        if (qubitModel != null) {
            return qubitModel.getAudioCodecInfo();
        }
        return "UNKNOWN";
    }
    public MMQFSegmentSizeInfo getSegmentSizeInfo(String inUrl, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo)
    {
        MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo info = null;
        if(isInitialized) {
            if(segmentInfo == null && inUrl != null && inUrl.length() > 0)
                segmentInfo = presentationInfoRetriever.getSegmentInfoForURL(inUrl);
            if (qubitModel != null && segmentInfo != null) {
                info = qubitModel.getSegmentSizeInfo(segmentInfo);
            }
        }
        return info;
    }

    public MMQFSegmentSizeInfo getAverageSegmentSizeInfo(String inUrl, MMQFQubitPresentationInfoRetriever.SegmentInfoForURL segmentInfo){
        MMQFQubitStatisticsInterface.MMQFSegmentSizeInfo info = null;
        if(isInitialized && presentationInfoRetriever!= null) {
            if(segmentInfo == null && inUrl != null && inUrl.length() > 0)
                segmentInfo = presentationInfoRetriever.getSegmentInfoForURL(inUrl);
            if (qubitModel != null && segmentInfo != null) {
                info = qubitModel.getAverageSegmentSizeInfo(segmentInfo);
            }
        }
        return info;
    }

    public long getPotentialStorageSavings()
    {
        return -1;
    }
    //Implementation of MMQFQubitStatisticsInterface ends

    private enum InitializationSubState
    {
        RequestPresentationInfo,
        CreateQubitModel,
        Initialized,
        InitializationError,
        InitializationCancelled
    }

    public  long getCQTotalBitsTransferred()
    {
        return qubitModel.runningStatistics.totalBitsCQ;
    }
    public  long getCBRTotalBitsTransferred()
    {
        return qubitModel.runningStatistics.totalBitsCBR;
    }
    public  int getPercentageBitSavings()
    {
        if(qubitModel.runningStatistics.totalBitsCBR != 0) {
            return (int) (((qubitModel.runningStatistics.totalBitsCBR - qubitModel.runningStatistics.totalBitsCQ) * 100) / qubitModel.runningStatistics.totalBitsCBR);
        }
        else {
            return 0;
        }
    }

    public  int getiMOSImprovementOccurences()
    {
        return (int)(qubitModel.runningStatistics.totaliMOSImprovements);
    }

    public  double getiMOSImprovementPercentage()
    {
        if(qubitModel.runningStatistics.totalSegments <= 0){
            return 0;
        }
        return (double)( (qubitModel.runningStatistics.totaliMOSImprovements) * 100/qubitModel.runningStatistics.totalSegments);
    }

    public  int getMinImosImprovementInPerc()
    {
        return qubitModel.runningStatistics.percentageMiniMOSImprovement;
    }
    public  int getMaxiMosImprovementInPerc()
    {
        return qubitModel.runningStatistics.percentageMaxiMOSImprovement;
    }

    public double getMaxImprovediMOS()
    {
        if(qubitModel!=null && qubitModel.runningStatistics!=null && qubitModel.runningStatistics.maxiMOSImpPoint!=null) {
            return qubitModel.runningStatistics.maxiMOSImpPoint.cqiMOS;
        }
        return -1;
    }

    public double getMaxImprovediMOSOriginaliMOS()
    {
        if(qubitModel!=null && qubitModel.runningStatistics!=null && qubitModel.runningStatistics.maxiMOSImpPoint!=null) {
            return qubitModel.runningStatistics.maxiMOSImpPoint.cbriMOS;
        }
        return -1;
    }

    public int getTotalDuration() {
        if (presentationInfo != null) {
            int dur = presentationInfo.getDuration();
            MMLogger.v("EP", "DURATION RETURNING DUR=" + dur);
            return dur;
        }else{
            return -1;
        }
    }
    public int getNumberOfProfile() {
        if (qubitModel!= null && qubitModel.getMetatDataArrayList()!= null) {
            return qubitModel.getMetatDataArrayList().size();
        }
        return -1;
    }

    public ResolutionObject getStreamResolution() {
        ResolutionObject resObj = new ResolutionObject();
        if (qubitModel != null && qubitModel.getMetatDataArrayList() != null) {
            for (MMQFQubitMetadataFileParser.VideoTrackMediaAttributes mediaAttributes : qubitModel.getMetatDataArrayList()) {
                MMLogger.v("EP", "Width=" + mediaAttributes.displayWidth + " Height=" + mediaAttributes.displayHeight);
//            MMLogger.v("EP","Height="    mediaAttributes.displayHeight);

                if (resObj.maxWidth < mediaAttributes.displayWidth) {
                    resObj.maxWidth = mediaAttributes.displayWidth;
                    resObj.maxHeight = mediaAttributes.displayHeight;
                }
                if (resObj.minWidth > mediaAttributes.displayWidth) {
                    resObj.minWidth = mediaAttributes.displayWidth;
                    resObj.minHeight = mediaAttributes.displayHeight;
                }
            }
        }else{
            resObj.maxWidth = -1;
            resObj.maxHeight = -1;
            resObj.minWidth = -1;
            resObj.minHeight = -1;
        }
        return resObj;
    }

    public Boolean isLiveStreaming() {
        if (presentationInfo != null) {
            return presentationInfo.isLivePresentation();
        }
        return false;
    }
    public Double getFrameRate() {
        if (qubitModel!= null && qubitModel.commonMetadata != null) {
            return qubitModel.commonMetadata.frameRate;
        }
        return -1.0;
    }

    public  int getQubitPlaybackDuration()
    {
        if (qubitModel!= null && qubitModel.runningStatistics != null) {
            return (int) (qubitModel.runningStatistics.durationOfPlayback);
        }
        return -1;
    }

    public double getiMOSVarianceCQ()
    {
        return qubitModel.runningStatistics.getiMOSVarianceCQ();
    }

    public double getiMOSStdDeviationCQ()
    {
        return qubitModel.runningStatistics.getiMOSStdDeviationCQ();
    }

    public double getiMOSVarianceCBR()
    {
        return qubitModel.runningStatistics.getiMOSVarianceCBR();
    }

    public double getiMOSStdDeviationCBR()
    {
        return qubitModel.runningStatistics.getiMOSStdDeviationCBR();
    }
    
    public void setSubscriberID(String subsID){
        sdkExperienceProbe.setSubscriberId(subsID);
    }

    public void setSubscriber(String subsID, String subsType){
        sdkExperienceProbe.setSubscriber(subsID, subsType);
    }
    public void setSubscriber(String subsID, String subsType, String subscriberTag){
        sdkExperienceProbe.setSubscriber(subsID, subsType,subscriberTag);
    }


    private static MMQFQubitEngine instance = new MMQFQubitEngine();
    private OnInitializationCompleteListener initializationCompleteListener;
    private OnQubitEngineErrorEventListener  errorEventListener;
    private OnQubitEngineInfoEventListener   infoEventListener;
    private InitializationSubState initSubState;
    //private Vector<MMQFQubitStatisticsInterface.MMQFABRSwitchInfo> abrSwitchInfoVect;
    private int qubitMode;
    private MMQFPresentationInfo presentationInfo;
    private MMQFQubitModel qubitModel;
    private MMQFQubitPresentationInfoRetriever presentationInfoRetriever;
    private Boolean qubitNotRequired;
    private URL presentationManifestURL;
    private URL presentationMetadataURL;
    private static String LOG_TAG = "MMQFQubitEngine";
    private Boolean isInitialized = false;
    private SDKExperienceProbe sdkExperienceProbe = SDKExperienceProbe.getInstance();
    //    private EPIntegration epIntegrationObj = EPIntegration.getInstance();
    private static String streamProducerURL = "http://192.168.1.7/StreamProducer";
    //    private static String streamProducerURL = "http://lee.mediamelon.com:8080/StreamProducer10";
    private int customerID = 847859967; //Default for NexStreaming Player
    private Boolean initCalled = false;
    private Boolean initInProgress = false;
    private RegisterResponse regResponse;
    private Set<Integer> blacklistedTracks = new HashSet<Integer>();;
}


