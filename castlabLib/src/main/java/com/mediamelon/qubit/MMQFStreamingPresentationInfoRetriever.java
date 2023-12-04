package com.mediamelon.qubit;

import java.net.URL;

/**
 * Created by Rupesh on 12-10-2015.
 */
public class MMQFStreamingPresentationInfoRetriever implements MMQFQubitPresentationInfoRetriever.OnQubitPresentationInfoRetrievedListener{

    public  boolean presentationInfoRetrievalSuccess(){
        return presentationInfoRetrievalSuccess;
    }

    public static MMQFStreamingPresentationInfoRetriever getInstance() {
        return instance;
    }

    public interface OnPresentationInfoRetrievedListener {
        public void onPresentationInfoRetrieved(MMQFQubitStatusCode status);
    }

    public void setPresentationInfoRetrievedListener(OnPresentationInfoRetrievedListener listener){
        presentationInfoRetrievedListener = listener;
    }

    public void retrievePresentationInfo(URL manifestURL){
        presentationInfoRetriever  = new MMQFQubitPresentationInfoRetriever(manifestURL);
        presentationInfoRetrievalSuccess = false;
        presentationInfoRetriever.setOnQubitPresentationInfoRetrieved(this);
        presentationInfoRetriever.RetrievePresentationInfo();
    }

    public void onQubitPresentationInfoRetrieved(MMQFQubitStatusCode code, MMQFPresentationInfo aPresentationInfo)
    {
        if(code.status() == MMQFQubitStatusCode.MMQFSuccess){
            presentationInfoRetrievalSuccess = true;
            presentationInfo = aPresentationInfo;
        }
        if(presentationInfoRetrievedListener!= null){
            presentationInfoRetrievedListener.onPresentationInfoRetrieved(code);
        }
    }

    public MMQFQubitPresentationInfoRetriever.SegmentInfoForURL getSegmentInfoForURL(String url)
    {
        return presentationInfoRetriever.getSegmentInfoForURL(url);
    }

    public void resetPresentationInfo(){
        presentationInfo = null;
        presentationInfoRetrievalSuccess = false;
    }

    public MMQFPresentationInfo presentationInfo;
    private boolean presentationInfoRetrievalSuccess;
    private MMQFQubitPresentationInfoRetriever presentationInfoRetriever;
    private OnPresentationInfoRetrievedListener presentationInfoRetrievedListener;
    private static MMQFStreamingPresentationInfoRetriever instance = new MMQFStreamingPresentationInfoRetriever();
}
