package com.mediamelon.smartstreaming;

import android.content.Context;
import android.util.Log;

import com.mediamelon.qubit.MMQFQubitRangedResouce;
import com.mediamelon.qubit.MMQFQubitStatusCode;
import com.mediamelon.qubit.MMQFQubitizer;
import com.mediamelon.qubit.ep.EPAttributes;
import com.mediamelon.qubit.ep.SDKExperienceProbe;

import org.json.JSONException;
import org.json.JSONObject;


public class MMSmartStreaming implements MMQFQubitizer.onInitializationCompleteListener {
  /**
   * Gets the SDK instance
   * @return SDK instance
   */
  public static MMSmartStreaming getInstance(){
    try{
      if(myObj == null){
        myObj = new MMSmartStreaming();
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return myObj;
  }

  /**
   * Gets the SDK version
   * @return SDK version (major.minor.patch)
   */
  public static String getVersion(){
    String version  = null;
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "getVersion");
      }
      version = MMQFQubitizer.getVersion();
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return version;
  }


  public void closeMediaMelonSDK(){
      logSTrace = false; //for stopping logs after stopping MM SDK
      SDKExperienceProbe.getInstance().closeMediaMelonSDK();
  }

  /**
   * Gets the registration status (done via registerMMSmartStreaming)
   * @return true if the SDK has successfully registered with the registerMMSmartStreaming method;
   * otherwise returns false.
   * @see registerMMSmartStreaming
   */
  public static boolean getRegistrationStatus(){
    boolean retVal = false;
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "getRegistrationStatus");
      }
      retVal = SDKExperienceProbe.getInstance().isSDKuserInfoRegistered();
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return retVal;
  }

  /**
   * Registers the QBR SmartStreaming engine and performs a license verification. This API should
   * be called once when player starts. The QBR SmartStreaming engine must be successfully
   * registered before initialization.
   * <p>This is a synchronous call. Registration status can be checked at any time using the
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
  public static void registerMMSmartStreaming(String playerName, String customerID, String componentName, String subscriberID, String domainName){
    registerMMSmartStreaming(playerName,customerID,componentName,subscriberID,domainName,null,null,true);
  }

  /**
   * Registers the QBR SmartStreaming engine and performs a license verification. This API should
   * be called once when player starts. The QBR SmartStreaming engine must be successfully
   * registered before initialization.
   * <p>This is a synchronous call. Registration status can be checked at any time using the
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
   * @param [subscriberTag] Viewer's subscriber tag apart from subscriber id using which their data can be tracked upon
   *
   * @see getRegistrationStatus
   * @see updateSubscriberID
   */
  public static void registerMMSmartStreaming(String playerName, String customerID, String componentName, String subscriberID, String domainName, String subscriberType){
    registerMMSmartStreaming(playerName,customerID,componentName,subscriberID,domainName,subscriberType,null,true);
  }

    public static void registerMMSmartStreaming(String playerName, String customerID, String componentName, String subscriberID, String domainName, String subscriberType, String subscriberTag,boolean hashSubscriberId){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "registerMMSmartStreaming - playerName=" + playerName + ", custID=" + customerID + ", component Name=" + componentName + ", subs ID=" + subscriberID + ", domain Name=" + domainName + ", subscriber Type=" + subscriberType);
      }
      MMQFQubitizer.getInstance().registerQBRSDK(playerName, customerID, componentName, subscriberID, domainName);
      SDKExperienceProbe.getInstance().registerSDKUserInfo(playerName, customerID, componentName, subscriberID, domainName, subscriberType,subscriberTag,hashSubscriberId);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

 /**
  * Disables the fetching of manifests by the SDK to determine the presentation information of the content.
  * SDK will rely completely on presentation information provided as part of setPresentationInformation.
  * @param [disable] Disables/Enables the manifest fetch by the SDK
  * @see setPresentationInformation
  */
  public static void disableManifestsFetch(boolean disable){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "disableManifestsFetch " + disable);
      }
      MMQFQubitizer.getInstance().disableManifestsFetch(disable);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * After the registration, user may will like to update the subscriber ID,
   * for example - user logged off from the Video service website, and logs in again with different
   * user.
   * @param subscriberID New Subscriber ID
   * @see registerMMSmartStreaming
   */
  public static void updateSubscriberID(String subscriberID){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "updateSubscriberID - subscriberID=" + subscriberID);
      }
      MMQFQubitizer.getInstance().setSubscriberId(subscriberID);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * After the registration, user may will like to update the subscriber ID,
   * for example - user logged off from the Video service website, and logs in again with different
   * user.
   * @param subscriberID New Subscriber ID
   * @param subscriberType New Subscriber Type
   * @see registerMMSmartStreaming
   */
  public static void updateSubscriber(String subscriberID, String subscriberType){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "updateSubscriberID - subscriberID=" + subscriberID + "subscriberType=" + subscriberType);
      }
      MMQFQubitizer.getInstance().setSubscriber(subscriberID, subscriberType);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
  public void setInternalSubscriberID(Context appCtx){

                try{
            if(logSTrace){
                Log.v(StackTraceLogTag, "Application Context " + appCtx);
              }
           SDKExperienceProbe.getInstance().setInternalSubscriberID(appCtx,true);

                  }
        catch(Exception e){
            e.printStackTrace();
          }
      }
  
  /**
   * After the registration, user may will like to update the subscriber ID,
   * for example - user logged off from the Video service website, and logs in again with different
   * user.
   * @param subscriberID New Subscriber ID
   * @param subscriberType New Subscriber Type
   * @param subscriberTag New Subscriber Tag
   * @see registerMMSmartStreaming
   */
  public static void updateSubscriber(String subscriberID, String subscriberType, String subscriberTag){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "updateSubscriberID - subscriberID=" + subscriberID + "subscriberType=" + subscriberType + " subscriberTag=" + subscriberTag);
      }
      MMQFQubitizer.getInstance().setSubscriber(subscriberID, subscriberType, subscriberTag);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the physical device characteristics to analytics. All values are optional;
   * use a NULL pointer if the value is unknown or inapplicable. Use -1 for for unknown integer
   * values.
   *
   * @param brand Device brand name.
   * @param deviceModel Device model name.
   * @param os Device operating system name.
   * @param osVersion Device operating system name.
   * @param telecomOperator Device mobile network operator.
   * @param screenWidth Device screen / display window horizontal resolution (in integer pixels).
   *                    If not known, set it to -1.
   * @param screenHeight Device screen / display window vertical resolution (in integer pixels).
   *                     If not known, set it to -1.
   */
  public static void reportDeviceInfo(String brand, String deviceModel, String os, String osVersion, String telecomOperator, Integer screenWidth, Integer screenHeight){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportDeviceInfo - brand=" + brand + ", deviceModel=" + deviceModel + ", os=" + os + ", osVersion=" + osVersion + ", telecomOperator=" + telecomOperator + ", screenWidth=" + screenWidth.toString() + ", screenHeight=" + screenHeight.toString());
      }
      SDKExperienceProbe.getInstance().setDeviceInformation(brand, deviceModel, os, osVersion, telecomOperator, screenWidth, screenHeight);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the media player characteristics to analytics.
   * <p>Use a NULL pointer if the value is unknown or inapplicable.
   *
   * @param brand Brand of the player. For example - Brand could be Organisation Name.
   * @param model Model of the player. For example - This could be a variant of player.
   *              Say name of third party player used by organisation. Or any human readable name of
   *              the player.
   * @param version Version of the player.
   */
  public static void reportPlayerInfo(String brand, String model, String version){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportPlayerInfo - brand=" + brand + ", model=" + model + ", version=" + version);
      }
      SDKExperienceProbe.getInstance().setPlayerInformation(brand, model, version);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Initializes the session for playback with QBR optimization. This API should be called once for
   * every media session and is asynchronous. Its completion is indicated via callback to
   * MMSmartStreamingObserver::sessionInitializationCompleted that user may choose to ignore.
   *
   * @param mode QBR operating mode.
   * @param manifestURL URL of the media manifest
   * @param [metaURL] URL of the media metadata. If it is null, and QBR operating mode is
   *                Bitsave, CostSave, or Quality, a metadata file with manifestUrl base name will
   *                be used. If the metadata cannot be retrieved, mode will default to Disabled.
   * @param [assetID] Content identifier i.e parentid in group
   * @param [assetName] Content name
   * @param [videoId] Sub content id
   * @param observer MMSmartStreamingObserver that will receive the callback on initialization
   *                 completion.
   *
   */
  public Integer initializeSession(MMQBRMode mode, String manifestURL, String metaURL, String assetID, String assetName, MMSmartStreamingObserver observer,boolean isLive){
    return initializeSession(mode,manifestURL,metaURL,assetID,assetName,null,observer,isLive);
  }
  public Integer initializeSession(MMQBRMode mode, String manifestURL, String metaURL, String assetID, String assetName, String videoId, MMSmartStreamingObserver observer,boolean isLive){
      Integer ret = -1;
    try{
      engineStarted = false;
      isgetQbrChunkUsed = false;
      if(logSTrace){
        Log.v(StackTraceLogTag, "initializeSession mode=" + mode.toString() + ", manifestURL=" + (manifestURL!=null?manifestURL:"") + ", metaURL=" + (metaURL!=null?metaURL:"") + ", assetID=" + assetID + ", assetName=" + assetName + (observer!=null?", observer is set":", observer is NOT set"));
      }
      if (manifestURL == null || mode == null){
        return -1;
      }
      MMQFQubitizer.QubitizationMode qMode = null;
      if (mode == MMQBRMode.QBRModeQuality){
        qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_ImproveQuality;
      }else if(mode == MMQBRMode.QBRModeCostsave){
        qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_CostSave;
      }else if (mode == MMQBRMode.QBRModeBitsave){
        qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_SaveBits;
      }
     else if (mode == MMQBRMode.QBRModeDisabled){
        qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_Disabled;
     }

      initObserver = observer;
      ret = MMQFQubitizer.getInstance().initializeQubitizer(qMode, manifestURL, metaURL, this, assetID, assetName);
      if (ret == -1){
        engineStarted = false;
      }else {
        engineStarted = true;
        initObserver = observer;
        SDKExperienceProbe.getInstance().setAssetInformation(assetID, assetName,videoId);

//          ContentMetadata cm  = new ContentMetadata();
//          cm.videoId=videoId;
//          cm.seriesTitle="SERIES-TITLE";
//          cm.season="SEASON";
//          cm.genre="GENRE";
//          cm.episodeNumber="EPISODE-NUMBER";
//          cm.drmProtection="DRM";
//          cm.contentType="CONTENT-TYPE";
//          cm.assetName=assetName;
//          cm.assetId=assetID;
//          JSONObject j = cm.getJSONObject();
        //SDKExperienceProbe.getInstance().setContentMetadata(j);
      }
        if(isLive) SDKExperienceProbe.getInstance().setPresentationLive(true);
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return ret;
  }
  //ag
  public Integer initializeSession(MMQBRMode mode, String manifestURL, String metaURL, JSONObject contentMetadata, MMSmartStreamingObserver observer,boolean isLive){

        Integer ret = -1;
        try{
            engineStarted = false;
            isgetQbrChunkUsed = false;
            if(logSTrace){
                Log.v(StackTraceLogTag, "initializeSession mode=" + mode.toString() + ", manifestURL=" + (manifestURL!=null?manifestURL:"") + ", metaURL=" + (metaURL!=null?metaURL:"") + ", contentMetadata=" + contentMetadata + (observer!=null?", observer is set":", observer is NOT set"));
              }
            if (manifestURL == null || mode == null){
                return -1;
              }
            MMQFQubitizer.QubitizationMode qMode = null;
            if (mode == MMQBRMode.QBRModeQuality){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_ImproveQuality;
              }else if(mode == MMQBRMode.QBRModeCostsave){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_CostSave;
              }else if (mode == MMQBRMode.QBRModeBitsave){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_SaveBits;
              }else if (mode == MMQBRMode.QBRModeDisabled){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_Disabled;
              }

            String assetName = null;
            String assetID = null ;
            String videoId = null;

                    if(contentMetadata.has("assetName")){
              try {
          	  assetName = contentMetadata.get("assetName").toString();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

                    if(contentMetadata.has("assetId")){
              try {
                    assetID = contentMetadata.get("assetId").toString();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

                    if(contentMetadata.has("videoId")){
              try {
                    videoId = contentMetadata.get("videoId").toString();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }

                   // try {
                           //     assetName = contentMetadata.get("assetName").toString();
                                   //     assetID = contentMetadata.get("assetId").toString();
                                           //     videoId = contentMetadata.get("videoId").toString();
                                                   // } catch (JSONException e) {
                                                           //   e.printStackTrace();
                                                                   // }


                                                                                            initObserver = observer;
            ret = MMQFQubitizer.getInstance().initializeQubitizer(qMode, manifestURL, metaURL, this, assetID, assetName);
            if (ret == -1){
                engineStarted = false;
              }else {
                engineStarted = true;
                initObserver = observer;

                SDKExperienceProbe.getInstance().setAssetInformation(assetID, assetName,videoId);
                SDKExperienceProbe.getInstance().setContentMetadata(contentMetadata);
                if(isLive) SDKExperienceProbe.getInstance().setPresentationLive(true);
              }
          }
        catch(Exception e){
            e.printStackTrace();
          }
        return ret;
      }

    public Integer initializeSession(MMQBRMode mode, String manifestURL, String metaURL, JSONObject contentMetadata, MMSmartStreamingObserver observer){

        Integer ret = -1;
        try{
            engineStarted = false;
            isgetQbrChunkUsed = false;
            if(logSTrace){
                Log.v(StackTraceLogTag, "initializeSession mode=" + mode.toString() + ", manifestURL=" + (manifestURL!=null?manifestURL:"") + ", metaURL=" + (metaURL!=null?metaURL:"") + ", contentMetadata=" + contentMetadata + (observer!=null?", observer is set":", observer is NOT set"));
            }
            if (manifestURL == null || mode == null){
                return -1;
            }
            MMQFQubitizer.QubitizationMode qMode = null;
            if (mode == MMQBRMode.QBRModeQuality){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_ImproveQuality;
            }else if(mode == MMQBRMode.QBRModeCostsave){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_CostSave;
            }else if (mode == MMQBRMode.QBRModeBitsave){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_SaveBits;
            }else if (mode == MMQBRMode.QBRModeDisabled){
                qMode = MMQFQubitizer.QubitizationMode.QubitizationMode_Disabled;
            }

            String assetName = null;
            String assetID = null ;
            String videoId = null;

            if(contentMetadata.has("assetName")){
                try {
                    assetName = contentMetadata.get("assetName").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(contentMetadata.has("assetId")){
                try {
                    assetID = contentMetadata.get("assetId").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if(contentMetadata.has("videoId")){
                try {
                    videoId = contentMetadata.get("videoId").toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // try {
            //     assetName = contentMetadata.get("assetName").toString();
            //     assetID = contentMetadata.get("assetId").toString();
            //     videoId = contentMetadata.get("videoId").toString();
            // } catch (JSONException e) {
            //   e.printStackTrace();
            // }


            initObserver = observer;
            ret = MMQFQubitizer.getInstance().initializeQubitizer(qMode, manifestURL, metaURL, this, assetID, assetName);
            if (ret == -1){
                engineStarted = false;
            }else {
                engineStarted = true;
                initObserver = observer;

                SDKExperienceProbe.getInstance().setAssetInformation(assetID, assetName,videoId);
                SDKExperienceProbe.getInstance().setContentMetadata(contentMetadata);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return ret;
    }


    @Override
  public void onInitializationComplete(MMQFQubitStatusCode status, String description) {
    try{
      MMSmartStreamingInitializationStatus initStatus = MMSmartStreamingInitializationStatus.Failure;
      if (status.status() == MMQFQubitStatusCode.MMQFSuccess) {
        initStatus = MMSmartStreamingInitializationStatus.Success;
      }else if(status.status() == MMQFQubitStatusCode.MMQFPending) {
        initStatus = MMSmartStreamingInitializationStatus.Pending;
      }

      if (initObserver!= null && engineStarted == true){
        initObserver.sessionInitializationCompleted(1, initStatus,description);
        initObserver = null;
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Tells the QBR SmartStreaming engine which representations that the player can present.
   * Representations that are not in this list will not be selected by the QBR SmartStreaming engine.
   * @param presentationInfo PresentationInformation specifying the representations selected by
   *                         the player for playback.
   * @see blacklistRepresentation
   */
  public void setPresentationInformation(MMPresentationInfo presentationInfo){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "setPresentationInformation -  " + presentationInfo);
        Log.v(StackTraceLogTag, "setPresentationInformation -  Live? " + presentationInfo.isLive + " Duration => " + presentationInfo.duration);
        if(presentationInfo.representations.size() > 0) {
          for(int i = 0; i< presentationInfo.representations.size(); i++){
            Log.v(StackTraceLogTag, "setPresentationInformation -  Bitrate : " + presentationInfo.representations.get(i).bitrate);
          }
        }
      }
      MMQFQubitizer.getInstance().setPresentationInformation(presentationInfo);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Removes a representation from the list previously defined by setPresentationInformation. This
   * would typically be used to stop referring to a representation that is listed in the manifest
   * but not currently available.
   *
   * @param representationIdx Representation Index for the representation to be (un)blacklisted.
   * @param blacklistRepresentation True to blacklist the representation; False to un-blacklist
   *                                the representation.
   * @see setPresentationInformation
   */
  public void blacklistRepresentation(Integer representationIdx, boolean blacklistRepresentation){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "blacklistRepresentation - representationIdx=" + representationIdx + ", blacklistRepresentation=" + Boolean.toString(blacklistRepresentation));
      }

      MMQFQubitizer.getInstance().blacklistRepresentation(representationIdx, blacklistRepresentation);
    }
    catch(Exception e){
      e.printStackTrace();
    }
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
    Integer retval = defaultBitrate;
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "getQBRBandwidth - TrackIdx=" + representationTrackIdx.toString() + " defaultBandwidth=" + defaultBitrate.toString() + " bufferLength=" + bufferLength.toString() + " playbackPosition=" + playbackPosition.toString());
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "getQBRBandwidth - Engine not initialized properly returning default bitrate");
        return retval;
      }

      retval = MMQFQubitizer.getInstance().getQBRBandwidth(representationTrackIdx, defaultBitrate, bufferLength, playbackPosition);
      if (logSTrace) {
        Log.v(StackTraceLogTag, "getQBRBandwidth - TrackIdx=" + representationTrackIdx.toString() + " retval " + retval);
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }

    return retval;
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
    MMChunkInformation retval = null;
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "getQBRChunk - cbrChunk=" + cbrChunk);
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "getQBRChunk - Engine not initialized properly returning Identity");
        return cbrChunk;
      }
      isgetQbrChunkUsed = true;
      if(cbrChunk.trackIdx == -1 && cbrChunk.bitrate > 0){
        cbrChunk.trackIdx = MMQFQubitizer.getInstance().getAppTrackIdFromMMTracks(cbrChunk.bitrate);
      }
      long chunkSize = (cbrChunk.endByte != -1 && cbrChunk.startByte != -1)? ((cbrChunk.endByte - cbrChunk.startByte) + 1):-1L;
      MMQFQubitRangedResouce qubitResource = new MMQFQubitRangedResouce(cbrChunk.resourceURL, cbrChunk.startByte, chunkSize, cbrChunk.trackIdx, cbrChunk.sequence);
      MMQFQubitRangedResouce qbrChunk = MMQFQubitizer.getInstance().getQubitResource(qubitResource);
      if (qbrChunk != null) {
        retval = new MMChunkInformation();
        retval.trackIdx = qbrChunk.trackIdx;
        retval.sequence = qbrChunk.seqIdx;
        retval.resourceURL = qbrChunk.uri;
        retval.startTime = qbrChunk.start;
      }
      if(logSTrace){
        Log.v(StackTraceLogTag, "getQBRChunk - retval=" + retval);
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return retval;
  }

  /**
   * Reports the chunk request to analytics. This method is not used when QBR optimization is
   * enabled.
   * @param chunkInfo Chunk selected by the player.
   */
  public void reportChunkRequest(MMChunkInformation chunkInfo){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportChunkRequest - chunkInfo=" + chunkInfo); //showing
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportChunkRequest - Engine not initialized properly returning");
        return;
      }
      
      if (isgetQbrChunkUsed){
        Log.v(StackTraceLogTag, "reportChunkRequest - getQbrChunk is Used returning");
        return;
      }

      long size = -1L;
      if (chunkInfo.endByte != -1 && chunkInfo.startByte != -1){
        size = chunkInfo.endByte - chunkInfo.startByte;
      }

      int height = -1;
      int width = -1;
      String codecInfo = null;
      if(chunkInfo.bitrate > 0 || chunkInfo.trackIdx >= 0){
          MMRepresentation repInfo = MMQFQubitizer.getInstance().getRepresentationInfo(chunkInfo.bitrate,chunkInfo.trackIdx);
          if(repInfo != null){
              height = repInfo.height;
              width = repInfo.width;
              codecInfo = repInfo.codecId();
          }
      }
      SDKExperienceProbe.getInstance().trackSegment(chunkInfo.startTime, size, chunkInfo.bitrate, height, width, -1, codecInfo);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports current download rate (rate at which chunk is downloaded) to analytics. This should be
   * reported for every chunk download (if possible). If this value is not available on every
   * chunk download, then last updated value with player should be reported every 2 seconds.
   *
   * @param downloadRate Download rate as measured by the player (in bits per second)
   */
  public void reportDownloadRate(Long downloadRate){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportDownloadRate - downloadRate=" + downloadRate.toString());//showing
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportDownloadRate - Engine not initialized properly returning");
        return;
      }

      MMQFQubitizer.getInstance().updateDownloadRate(downloadRate);
      SDKExperienceProbe.getInstance().saveBandwidthSample(downloadRate * 1.0);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports current buffer length to analytics. This should be
   * reported for every chunk download (if possible). If this value is not available on every
   * chunk download, then last updated value with player should be reported every 2 seconds.
   *
   * @param bufferLength Download rate as measured by the player (in bits per second)
   */
  public void reportBufferLength(Long bufferLength){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportBufferLength - bufferLength=" + bufferLength.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportBufferLength - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().reportBufferLength(bufferLength);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports custom metadata, in the form of a key-value pair, to analytics.
   *
   * @param key Custom metadata key.
   * @param value Custom metadata value.
   */
  public void reportCustomMetadata(String key, String value){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportCustomMetadata - " + key + ":" + value);
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportCustomMetadata - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().reportCustomMetadata(key, value);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports current playback position in media to analytics. This should be reported every two
   * seconds if possible.
   *
   * @param playbackPos Current playback position (in milliseconds).
   */
  public void reportPlaybackPosition(Long playbackPos){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportPlaybackPosition - playbackPos=" + playbackPos.toString()); //showing
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportPlaybackPosition - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().setPbTime(playbackPos * 1.0 / 1000);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the communications network type to analytics.
   * @param networkType : Connection Info.
   */
  public void reportNetworkType(MMConnectionInfo networkType){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportNetworkType - networkType=" + networkType.toString());
      }

      SDKExperienceProbe.getInstance().setNetworkType(networkType);
    }
    catch(Exception e){
      e.printStackTrace();
    }
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
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportMetricValue - metric=" + metric.toString() + " value " + value);
      }
      SDKExperienceProbe.getInstance().reportMetricValue(metric, value);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the current player state to analytics.
   *
   * @param playerState : Player State
   */
  public void reportPlayerState(MMPlayerState playerState){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportPlayerState playerState=" + playerState.toString());
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportPlayerState - Engine not initialized properly returning");
        return;
      }

      if(playerState == MMPlayerState.STOPPED){

      }
      SDKExperienceProbe.getInstance().onPlayerStateChanged(playerState);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the start of the buffering.
   */
  public void reportBufferingStarted(){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportBufferingStarted");
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportBufferingStarted - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().onBufferingBegin();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the completion of the buffering.
   */
  public void reportBufferingCompleted(){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportBufferingCompleted");
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportBufferingCompleted - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().onBufferingEnd();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports that user initiated the playback session.
   * This should be called at different instants depending on the mode of operation of player.
   * In Auto Play Mode, should be the called when payer is fed with the manifest URL for playback
   * In non-Auto Play Mode, should be called when the user presses the play button on the
   * player
   */
  public void reportUserInitiatedPlayback(){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportUserInitiatedPlayback");
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportUserInitiatedPlayback - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().reportUserInitiatedPlayback();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the ABR bitrate changes to the analytics. This API should be called when neither
   * getQBRChunk nor reportChunkRequest is called by the player.
   * @param prevBitrate Previous ABR bitrate in bits per second.
   * @param newBitrate New ABR bitrate in pers per second.
   */
  public void reportABRSwitch(Integer prevBitrate, Integer newBitrate){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportABRSwitch prevBitrate=" + prevBitrate.toString() + ", newBitrate=" + newBitrate.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportABRSwitch - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().reportABRSwitch(prevBitrate, newBitrate);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports cumulative frame loss count to analytics.
   * @param lossCnt Cumulative count of frames lost in playback session.
   */
  public void reportFrameLoss(Integer lossCnt){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportFrameLoss - lossCnt=" + lossCnt.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportFrameLoss - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().setPBStats(EPAttributes.FRAMELOSS, lossCnt);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports an error encountered during playback.
   * @param error Error encountered during playback session.
   * @param playbackPosMilliSec Playback position in millisec when error occurred.
   */
  public void reportError(String error, Long playbackPosMilliSec){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportError - error=" + error + " playbackPosMilliSec=" + ((playbackPosMilliSec != null) ? playbackPosMilliSec.toString():""));
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportError - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().onPlayerError(error, playbackPosMilliSec);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports that a seek event is complete, with the new playback starting position.
   * @param seekEndPos Playback position(in milliseconds) when seek completed. This is point from
   *                   which playback will start after the seek.
   */
  public void reportPlayerSeekCompleted(Long seekEndPos){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportPlayerSeekCompleted - seekEndPos=" + seekEndPos.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportPlayerSeekCompleted - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().reportPlayerSeeked(seekEndPos);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the presentation size of the stream being playbacked.
   * Call to this API is optional. It is recommended to call this API (when QBR is not integrated)
   * if user did not provided the stream resolution information as part of the setPresentationInfo API.
   */
  public void reportPresentationSize(Integer width, Integer height){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportPresentationSize - width=" + width.toString() + ", height=" + height);
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportPresentationSize - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().reportPresentationSize(width, height);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  public Integer getLocationUpdateInterval(){
    Integer interval = -1;
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "getLocationUpdateInterval");
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "getLocationUpdateInterval - Engine not initialized properly returning");
        return interval;
      }

      interval = SDKExperienceProbe.getInstance().getTelephonyMetricsUpdateInterval();
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return interval;
  }

  public void reportCellularInformation(MMCellInfo info){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportCellularInformation - info=" + info);
      }

      SDKExperienceProbe.getInstance().reportCellularInformation(info);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  public void reportLocation(Double latitude, Double longitude){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportLocation - location=" + latitude + ", " + longitude);
      }

      SDKExperienceProbe.getInstance().reportLocation(latitude, longitude);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the WiFi Service Set Identifier (SSID).
   * @param ssid WiFi Service Set Identifier (SSID).
   */
  public void reportWifiSSID(String ssid){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportWifiSSID - ssid=" + ssid);
      }

      SDKExperienceProbe.getInstance().reportWifiSSID(ssid);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the WiFi signal strength. This may be useful, if someone is analyzing a
   * back playback session using smartsight's microscope feature, and wants to know if Wifi signal
   * strength is the cause fo poor performance of that session. This API is relevant if Wifi is used
   * for the playback sesssion.
   *
   * @param strength Strength of Wifi signal in %
   */
  public void reportWifiSignalStrengthPercentage(Double strength){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportWifiSignalStrenthPercentage - strength=" + strength.toString());
      }

      SDKExperienceProbe.getInstance().reportWifiSignalStrengthPercentage(strength);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the WiFi maximum data rate.
   * @param dataRate WiFi data rate (in kbps)
   */
  public void reportWifiDataRate(Integer dataRate){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportWifiDataRate - dataRate=" + dataRate.toString());
      }

      SDKExperienceProbe.getInstance().reportWifiDataRate(dataRate);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports advertisement playback state
   * @param adState State of the advertisement
   * @see MMAdState
   */
  public void reportAdState(MMAdState adState){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportAdState - adState=" + adState.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportAdState - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().reportAdState(adState);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports advertisement-related information
   *
   * @param adClient Client used to play the ad, eg: VAST
   * @param adURL Tag represented by the ad.
   * @param adDuration Length of the video ad (in milliseconds).
   * @param adPosition Position of the ad in the video  playback; one of "pre", "post" or "mid"
   *                   that represents that the ad played before, after or during playback respectively.
   * @param adType Type of advertisement : {LINEAR etc}
   * @param adCreativeType Ad MIME type
   * @param adServer Ad server (ex. DoubleClick, YuMe, AdTech, Brightroll, etc.)
   * @param adResolution Advertisement video resolution
   * @param adPodIndex Position of the ad within the ad group (Index should start from 1)
   * @param adPositionInPod Position of the ad within the pod
   * @param adPodLength Total number of ads in the ad group
   * @param isBumper True if bumper Ad else false
   * @param adScheduledTime
   */
  //public void reportAdInfo(String adClient, String adURL, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer, String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime){
  public void reportAdInfo(String adClient, String adId, Long adDuration, String adPosition, MMAdType adType, String adCreativeType, String adServer, String adResolution, int adPodIndex, int adPositionInPod, int adPodLength, boolean isBumper, double adScheduledTime, String adCreativeId, String adUrl, String adTitle, int adBitrate)
  {
  try{
      if(logSTrace){
       // Log.v(StackTraceLogTag, "reportAdInfo - adClient=" + adClient + ", adURL=" + adURL + ", adDuration=" + adDuration.toString() + ", adPosition=" + adPosition + ", adType=" + adType + ", adCreativeType=" + adCreativeType + ", adServer=" + adServer + ", adResolution=" + adResolution);
        Log.v(StackTraceLogTag, "reportAdInfo - adClient=" + adClient + ", adId=" + adId + ", adDuration=" + adDuration.toString() + ", adPosition=" + adPosition + ", adType=" + adType + ", adCreativeType=" + adCreativeType + ", adServer=" + adServer + ", adResolution=" + adResolution);
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportAdInfo - Engine not initialized properly returning");
        return;
      }
     // SDKExperienceProbe.getInstance().reportAdInfo(adClient, adURL, adDuration, adPosition, adType, adCreativeType, adServer, adResolution, adPodIndex, adPositionInPod, adPodLength, isBumper, adScheduledTime);
    SDKExperienceProbe.getInstance().reportAdInfo(adClient, adId, adDuration, adPosition, adType, adCreativeType, adServer, adResolution, adPodIndex, adPositionInPod, adPodLength, isBumper, adScheduledTime, adCreativeId, adUrl, adTitle, adBitrate);
  }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports advertisement-related information
   *
   * @param adInfo
   */
  public void reportAdInfo(MMAdInfo adInfo){
    try{
      if(adInfo != null) {
        if (logSTrace) {
          Log.v(StackTraceLogTag,
              "reportAdInfo - adClient=" + adInfo.adClient + ", adID=" + adInfo.adID
                  + ", adDuration=" + adInfo.adDuration.toString() + ", adPosition="
                  + adInfo.adPosition + ", adType=" + adInfo.adType + ", adCreativeType="
                  + adInfo.adCreativeType + ", adServer=" + adInfo.adServer + ", adResolution="
                  + adInfo.adResolution);
        }
        if (!engineStarted) {
          Log.v(StackTraceLogTag, "reportAdInfo - Engine not initialized properly returning");
          return;
        }
        SDKExperienceProbe.getInstance()
            .reportAdInfo(adInfo.adClient, adInfo.adID, adInfo.adDuration, adInfo.adPosition,
                adInfo.adType, adInfo.adCreativeType, adInfo.adServer, adInfo.adResolution,
                adInfo.adPodIndex, adInfo.adPositionInPod, adInfo.adPodLength, adInfo.isBumper,
                adInfo.adScheduledTime,adInfo.adCreativeId,adInfo.adUrl,adInfo.adTitle,adInfo.adBitrate);
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
  /**
   * Reports current advertisement playback position
   * @param playbackPosition Current playback position in the Ad (in milliseconds)
   */
  public void reportAdPlaybackTime(Long playbackPosition){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportAdPlaybackTime - AdvertisementPlaybackTime=" + playbackPosition.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportAdPlaybackTime - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().reportAdPlaybackTime(playbackPosition);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports error encountered during the advertisement playback
   * @param error Error encountered during advertisement playback
   * @param pos Playback position (in milliseconds) where error occurred
   */
  public void reportAdError(String error, Long pos){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportAdError - error=" + error + ", playbackPosMilliSec=" + pos.toString());
      }
      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportAdError - Engine not initialized properly returning");
        return;
      }
      SDKExperienceProbe.getInstance().reportAdError(error, pos);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the start of the buffering for Ads.
   */
  public void reportAdBufferingStarted(){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportAdBufferingStarted");
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportAdBufferingStarted - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().onAdBufferingBegin();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Reports the completion of the buffering for Ads.
   */
  public void reportAdBufferingCompleted(){
    try{
      if(logSTrace){
        Log.v(StackTraceLogTag, "reportAdBufferingCompleted");
      }

      if (!engineStarted){
        Log.v(StackTraceLogTag, "reportAdBufferingCompleted - Engine not initialized properly returning");
        return;
      }

      SDKExperienceProbe.getInstance().onAdBufferingEnd();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Enables/Disables console logs for the SDK methods. This is to help in debugging and testing
   * of the player to SDK integration.
   * @param logStTrace True to enable console logs; false to disable console logs.
   */
  public static void enableLogTrace(boolean logStTrace){
    logSTrace = logStTrace;
  }

  private boolean isgetQbrChunkUsed = false;
  private boolean engineStarted = false;
  private MMSmartStreaming(){}
  private MMSmartStreamingObserver initObserver = null;
  private static boolean logSTrace = false;
  private static String StackTraceLogTag = "MMSmartStreamingIntgr.StackTrace";
  private static MMSmartStreaming myObj;
}
