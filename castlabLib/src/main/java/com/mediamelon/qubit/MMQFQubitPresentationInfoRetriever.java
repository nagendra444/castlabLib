package com.mediamelon.qubit;
import android.util.Log;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Rupesh on 19-03-2015.
 */
public class MMQFQubitPresentationInfoRetriever implements MMQFQubitResourceDownloader_HTTP.OnQubitResourceDownloadedListener{

    public MMQFQubitPresentationInfoRetriever(URL aPresentationURL)
    {
        presentationURL = aPresentationURL;

    }

    public interface OnQubitPresentationInfoRetrievedListener {
        public abstract void onQubitPresentationInfoRetrieved(MMQFQubitStatusCode status, MMQFPresentationInfo presentationInfo);
    }

    public void setOnQubitPresentationInfoRetrieved(OnQubitPresentationInfoRetrievedListener lstnr)
    {
        listener = lstnr;
    }

    public MMQFQubitStatusCode RetrievePresentationInfo()
    {
        MMQFQubitStatusCode statusCode = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFPending);
        synchronized (TAG){
            curRequestId++;
            requestId = curRequestId;
            requestInProgress = true;
        }

        if(manifestDownloader == null)
        {
            manifestDownloader = new MMQFQubitResourceDownloader_HTTP();
            manifestDownloader.SetMaxResourceLen(4L * 1024 * 1024); // 4MB
            manifestDownloader.setOnQubitResourceDownloadedListener(this);
            MMQFResourceInfo info = new MMQFResourceInfo();
            info.resourceURL_ = presentationURL;
            manifestDownloader.execute(info);
        }

        return statusCode;
    }

    public SegmentInfoForURL getSegmentInfoForSegment(int bitrate, long startTimeMilliSec)        
    {        
        SegmentInfoForURL segmentInfo = null;        
        if(manifestParser != null && bitrate != -1 && startTimeMilliSec >= 0)        
        {        
            MMQFABRManifestParser.ParserSegmentInfoForURL parserSegmentInfo= manifestParser.getSegmentInfoForSegment(bitrate, startTimeMilliSec);        
            if(parserSegmentInfo != null) {        
                segmentInfo = new SegmentInfoForURL();        
                segmentInfo.segmentIndex = parserSegmentInfo.segmentIndex;        
                segmentInfo.videoTrackInfo = parserSegmentInfo.videoTrackInfo;        
            }        
        }        
        return segmentInfo;        
    }        
    
    public void CancelPendingRequests(){
        if (manifestDownloader!= null){
            manifestDownloader.cancel(true);
        }

        if (auxResDownloader != null){
            auxResDownloader.cancel(true);
        }
        requestInProgress = false;
    }

    public void OnQubitResourceDownloaded(MMQFQubitStatusCode status, byte[] downloadedMediaBytes, String contentType)
    {
        if (curRequestId != requestId || requestInProgress == false){
            //Cancelled  ...
            MMLogger.v("MMSmartStreaming.EPIntegration", "Presentation Download Request Cancelled " + curRequestId + " != " + requestId);
            listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFCancelled), null);
        }

        if(status.status() == MMQFQubitStatusCode.MMQFSuccess) {
            MMQFPresentationInfo presentationInfo = null;
            if(auxResDownloadingIndex == -1) {//Downloaded the presentation
                String downloadedMedia = new String(downloadedMediaBytes);
                presentation = downloadedMedia;

                boolean isUTF8Str = false;

                byte [] skipBytesStr = new byte[10];
                byte [] originalByteStr = new byte[10];
                String skipByteString = null;
                String originalByteString = null;
                //Pick first 10 bytes and create the String out of it
                if(downloadedMediaBytes.length > 20) {
                    for(int i=0; i<10; i++){
                        skipBytesStr[i] = downloadedMediaBytes[2*i];
                        originalByteStr[i] = downloadedMediaBytes[i];
                    }
                    skipByteString = new String(skipBytesStr);
                    originalByteString = new String(originalByteStr);
                }
                if((contentType!= null && contentType.toLowerCase().indexOf("mpegurl") > 0) || presentationURL.toString().indexOf(".m3u8") > 0 || ( originalByteString.indexOf("<?xml")>=0 && skipByteString.indexOf("<?xml") <0) || ( originalByteString.indexOf("<MPD")>=0) ){
                    isUTF8Str = true;
                }

                if((isUTF8Str == true) && presentation != null) {
                    manifestParser = MMQFABRManifestParserFactory.createABRStreamingManifestParser(presentationURL.toString(), contentType);
                    if(manifestParser != null) {
                        presentationInfo = manifestParser.parse(presentation, presentationURL);
                        if (manifestParser.needAuxiliaryResources() == true) {
                            auxResInfo = manifestParser.getAuxResourceInfo();
                            auxResDownloadingIndex = 0;
                        }
                    }else{
                        listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFOperationNotSupported), null);
                        return;
                    }
                }else{
                    listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFOperationNotSupported), null);
                    return;
                }
            }

            if(auxResDownloadingIndex == -1) {
                if (presentationInfo != null) {
                    listener.onQubitPresentationInfoRetrieved(status, presentationInfo);
                } else {
                    //...Done with it.
                    if(manifestParser != null) {
                        manifestParser.SetAuxResource(-1, downloadedMediaBytes);
                        if(presentation != null) {
                            presentationInfo = manifestParser.parse(presentation, presentationURL);
                        }
                        if (manifestParser.allAuxResDownloadSuccess()==true && manifestParser.isSupportedQBRPresentation() == true){
                            listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess), presentationInfo);
                        }else if(manifestParser.allAuxResDownloadSuccess()==true && manifestParser.isSupportedQBRPresentation() == false){
                            listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFABRNotSupported), presentationInfo);
                        }else {
                            listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure), null);
                        }
                    }else{
                        listener.onQubitPresentationInfoRetrieved(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure), null);
                    }
                }
            }
            else{
                if(auxResDownloader == null)
                {
                    auxResDownloader = new MMQFQubitResourceDownloader_HTTP();
                    auxResDownloader.setOnQubitResourceDownloadedListener(this);
                }else{
                    //Set the last downloaded aux res with parser
                    manifestParser.SetAuxResource(auxResDownloadingIndex, downloadedMediaBytes);
                    auxResDownloadingIndex++;
                }
                if(auxResDownloadingIndex < auxResInfo.size()) {
                    synchronized (TAG){
                        curRequestId++;
                        requestId = curRequestId;
                        requestInProgress = true;
                    }
                    MMQFResourceInfo info = new MMQFResourceInfo();
                    info.resourceURL_ = auxResInfo.get(auxResDownloadingIndex).url_;
                    info.range_.startByteIndex = auxResInfo.get(auxResDownloadingIndex).range_.startByteIndex;
                    info.range_.endByteIndex = auxResInfo.get(auxResDownloadingIndex).range_.endByteIndex;
                    auxResDownloader = new MMQFQubitResourceDownloader_HTTP();
                    auxResDownloader.setOnQubitResourceDownloadedListener(this);
                    auxResDownloader.execute(info);
                }
                else{
                    //...Done with it.
                    if(presentation != null) {
                        presentationInfo = manifestParser.parse(presentation, presentationURL);
                    }

                    if (manifestParser.allAuxResDownloadSuccess()==true && manifestParser.isSupportedQBRPresentation() == true){
                        status = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess);
                    }else if(manifestParser.allAuxResDownloadSuccess()==true && manifestParser.isSupportedQBRPresentation() == false){
                        status = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFABRNotSupported);
                    }else {
                        status = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure);
                    }

                    listener.onQubitPresentationInfoRetrieved(status, presentationInfo);
                }
            }
        }
        else
        {
            if(auxResDownloadingIndex == -1) {
                Log.e(TAG, "Could not retrieve the presentation information");
                listener.onQubitPresentationInfoRetrieved(status, null);
            }
            else{
                status = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure);
                listener.onQubitPresentationInfoRetrieved(status, null);
                return;
            }
        }
    }

    public static class SegmentInfoForURL{
        public MMQFPresentationVideoTrackInfo videoTrackInfo = null;
        public MMQFPresentationVideoTrackInfo qbrVideoTrackInfo = null;
        public int cbrTrackIndex = -1;
        public int qbrTrackIndex = -1;

        public int segmentIndex = -1;
    }

    public SegmentInfoForURL getSegmentInfoForURL(String url)
    {
        SegmentInfoForURL segmentInfo = null;
        if(manifestParser != null && url != null && url.length() > 0)
        {
            MMQFABRManifestParser.ParserSegmentInfoForURL parserSegmentInfo = manifestParser.getSegmentInfoForURL(url);
            if(parserSegmentInfo != null) {
                segmentInfo = new SegmentInfoForURL();
                segmentInfo.segmentIndex = parserSegmentInfo.segmentIndex;
                segmentInfo.videoTrackInfo = parserSegmentInfo.videoTrackInfo;
            }
            else{
                if(url.indexOf("audio")== -1) {
                    parserSegmentInfo = manifestParser.getSegmentInfoForURL(url);
                }
            }
        }
        return segmentInfo;
    }

    private final static String TAG = "MMQFPresenRetriever";
    private static Integer curRequestId= 0;
    private static boolean requestInProgress = false;
    private Integer requestId = 0;
    private URL presentationURL;
    private String presentation = null;
    private MMQFABRManifestParser manifestParser = null;
    private int auxResDownloadingIndex = -1;
    private ArrayList<MMQFABRManifestParser.AuxResourceInformation> auxResInfo = null;
    private MMQFQubitResourceDownloader_HTTP auxResDownloader = null;
    private MMQFQubitResourceDownloader_HTTP manifestDownloader = null;
    private OnQubitPresentationInfoRetrievedListener listener = null;
}
