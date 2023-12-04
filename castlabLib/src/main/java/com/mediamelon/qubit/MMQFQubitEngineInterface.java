package com.mediamelon.qubit;

import android.content.Context;
import com.mediamelon.qubit.ep.RegisterResponse;
import com.mediamelon.smartstreaming.MMPresentationInfo;

import java.net.URL;

public interface MMQFQubitEngineInterface {
    public class MMQFQubitResource{
        public String regularUrl = null;
        public Integer trackIndex = null;
        public Integer hintInTrackidx = null;
        public Integer hintInSeqNum = null;
    }

    public class MMQFQubitFailureStatusReasonCode{
        /**
         * Cannot connect to network
         */
        public static final int MMQFNetworkNotReachable = -1;

        /**
         * Presentation information not available.
         */
        public static final int MMQFPresentationInfoNotAvailable = -3;

        /**
         * Metadata required by Qubit engine is not available.
         */
        public static final int MMQFMetadataNotAvailable = -2;

        /**
         * Parsing of presentation failed.
         *  Possible reasons could be inconsistent tags in presentation manifest
         *  Unknown tags will be ignored
         */
        public static final int MMQFPresentationInfoParsingFailure = -3;

        /**
         *  Parsing of metadata failed
         *  Possible reasons include
         *      Some vital information missing
         *      Some inconsistent information in the metadata
         */
        public static final int MMQFMetadataParsingFailure = -4;
    }

    /**
     * Gets the version of the SDK. Format SDKVERSION/BUILDNO
     * @return SDK version
     */
    public abstract String getSDKVersion();

    public abstract String getMetaDataVersion();

    /**
     * Gets the instance of statistics interface.
     */
    public abstract MMQFQubitStatisticsInterface getStatisticsInterface();

    /**
     * Gets the instance of Configuration interface to tune the Qubit engine
     */
    public abstract MMQFQubitConfigurationInterface getConfigurationInterface();

    public abstract boolean isQubitInitialized();
    public abstract void cancelQubitInitialization();

    //For debug use only, do not use from client side
    public abstract void setCustomerID(int aCustomerID);

    public abstract String getCustomerID();

    public abstract void setSubscriberID(String subsID);

    public abstract void setSubscriber(String subsID, String subsType);
    /**
     *
     * @param subsID : subscriber id like email etc. note that SDK will hash this
     * @param subsType : type of subscriber
     * @param subscriberTag : tag the subscriber with any label that you want to for example like internal id etc.
     * @return nothing
     */
    public abstract void setSubscriber(String subsID, String subsType, String subscriberTag);

    /**
     * Interface definition of a callback to be invoked indicating the
     * completion of a initialization operation.
     */
    public interface OnInitializationCompleteListener {
        public void onInitializationComplete(MMQFQubitStatusCode status, String description);
    }
    public abstract void setOnInitializationCompleteListener(OnInitializationCompleteListener listener);

    /**
     *
     * @param presentationManifestURL : presentation URL of the presentation
     * @param presentationManifestURL : presentation URL of the presentation
     * @param presentationManifestURL : presentation URL of the presentation
     * @param presentationManifestURL : presentation URL of the presentation
     * @return Status code indication the status. It can be MMQFPending as well. In that case,
     * completion of initialization operation is indicated via OnInitializationCompleteListener:setOnInitializationCompleteListener
     */
    public abstract MMQFQubitStatusCode initializeSDK(URL presentationManifestURL, URL qbrMetaFileURL, String assetID, String assetName);

    /**
     * As of now Qubit SDK could handle qubitization of only one session.
     * Calling this API will invalidate the qubitization of presentation
     * This will ensure that information of one qubit session is not used by mistake in some other future presentation
     */
    public abstract void invalidateQubitSession();

    public void setPresentationInformation(MMPresentationInfo presentationInfo);
    /**
     *
     * @return Presentation information of the content
     */
    public abstract MMQFPresentationInfo getPresentationInformation();

    public abstract void blacklistRepresentation(int trackId, boolean blacklist);
    /**
     * Interface definition of a callback to be invoked indicating the
     * occurrence of some info event.
     */
    public interface OnQubitEngineInfoEventListener {
        public void onQubitEngineInfoEventOccurred(MMQFQubitInfoEvent infoEvt);
    }
    public abstract void setOnQubitEngineInfoEventOccurredListener(OnQubitEngineInfoEventListener listener);

    /**
     * Interface definition of a callback to be invoked indicating the
     * occurrence of some error event.
     */
    public interface OnQubitEngineErrorEventListener {
        public void onQubitEngineErrorEventOccurred(MMQFQubitErrorEvent errorEvt);
    }
    public abstract void setOnQubitEngineErrorEventOccurred(OnQubitEngineErrorEventListener listener);

    /**
     * Provides the Qubitized url for the input URL.
     * Player is expected to request Qubitized URL before making request for any segment.
     * Qubitized URL is to be used for requesting the segment instead of input URL.
     * Resource reffered by both qubitized url and the input url refers to same part in playback
     * timeline of the representation
     * Note: QubitUrl returned by this API can be same as the segment Url.
     * @param inUrl URL of the segment for which player is about to make request
     * @return qubitized URL .
     */
    public abstract String getQubitResource(MMQFQubitResource resource);

    /**
     * QUBIT enables VBR streaming of the representation.
     * So, due to VBR nature of the representation, the bandwidth requirements for streaming the
     * representation wont be const all along the presentation.
     * Depending on the position of the playback and amount of media buffered, the minimum bandwidth
     * requirement for the smooth playback of rest of the representation will be different from the
     * average bandwith of the media.
     * This API will consider the content information, and provide the minimum bandwidth requirement
     * for the playback of the rebuffering free representation.
     * This API would be of interest while considering the Bitrate Switching decisions.
     * @param representationProfileID Profile ID for the representation for which bandwidth is to be computed. Profile ID can be had by using API getPresentationInformation.
     * @param playbackPos playback position of player (in millisec)
     * @param bufferLength amount of media buffered from the playback position (in millisec)
     * @return Bandwidth required in bps
     */
    public abstract int getQubitBandwidthRequirementsForProfile(int representationProfileID, int playbackPos, int bufferLength);
}
