package com.mediamelon.qubit;
import android.net.Uri;

import com.google.android.exoplayer.mminclusion.C;
import com.google.android.exoplayer.mminclusion.dash.DashSegmentIndex;
import com.google.android.exoplayer.mminclusion.dash.mpd.AdaptationSet;
import com.google.android.exoplayer.mminclusion.dash.mpd.MediaPresentationDescription;
import com.google.android.exoplayer.mminclusion.dash.mpd.MediaPresentationDescriptionParser;
import com.google.android.exoplayer.mminclusion.dash.mpd.RangedUri;
import com.google.android.exoplayer.mminclusion.dash.mpd.Representation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import static java.sql.Types.NULL;

/**
 * Created by Rupesh on 19-03-2015.
 */
public class DashMPDParser implements MMQFABRManifestParser {
    DashMPDParser(){
        arrAuxRes_ = new ArrayList<AuxResourceInformation>();
        sidxBoxes_ = new ArrayList<MMQFSidxParser.MMQFSegmentIndex>();
        mpdparser_ = new MediaPresentationDescriptionParser();
    }

    public MMQFPresentationInfo parse(String manifestFileData, URL manifestURL)
    {
        if(presentationInfo_!=null){
            return presentationInfo_;
        }
        MMQFPresentationInfo retval = null;
        InputStream is = new ByteArrayInputStream( manifestFileData.getBytes() );
        try {
//
            String manifestUrlStr = manifestURL.toString();
            Uri android_uri = Uri.parse(manifestUrlStr);
            presentationDescription_ = mpdparser_.parse(android_uri, is);

            //Get the urls for the sidx boxes for all video tracks.
            //If the range is not avl, then fetch 0-1500 range as a trial, if sidx is missing in this as well,
            // then periodically increment the range by 1500 bytes and give up when no sidx is found in initial 6000 bytes as well

            if(presentationDescription_.periods.size()>0)
            {
                int noOfAdaptationSets = presentationDescription_.periods.get(0).adaptationSets.size();//for now let us do it for period 0 only
                if(noOfAdaptationSets > 0){
                    ArrayList<AdaptationSet> videoAdaptationSet = new ArrayList<AdaptationSet>();
                    for(int j = 0; j< noOfAdaptationSets; j++){

                        if(presentationDescription_.periods.get(0).adaptationSets.get(j).type == C.TRACK_TYPE_VIDEO){

                            videoAdaptationSet.add(presentationDescription_.periods.get(0).adaptationSets.get(j));
                        }
                    }

                    //Iterate thru all the representations of the adaptation set
                    for(int i = 0; i<videoAdaptationSet.size(); i++){
                        for(int r=0; r<videoAdaptationSet.get(i).representations.size(); r++){
                            Representation rep = videoAdaptationSet.get(i).representations.get(r);
                            RangedUri rUri = rep.getIndexUri();
                            if(rUri!=null) {
                                AuxResourceInformation info = new AuxResourceInformation();
                                info.url_ = new URL(rUri.resolveUriString(rep.baseUrl));
                                info.range_.startByteIndex = rUri.start;
                                info.range_.endByteIndex = info.range_.startByteIndex + rUri.length - 1;
                                arrAuxRes_.add(info);
                            }
                        }
                    }
                }
            }
        }
        catch(IOException ex){

        }
        return retval;
    }
    
    private MMQFPresentationVideoTrackInfo getVideoTrackForBitrate(int bitrate){
        MMQFPresentationVideoTrackInfo retval = null;
        for(int repIter = 0; repIter< presentationInfo_.getVideoTracksCount(); repIter++){
            MMQFPresentationVideoTrackInfo videoTrack = presentationInfo_.getVideoTrack(repIter);
            if(videoTrack.bitrate == bitrate){
                retval = videoTrack;
                break;
            }
        }
        
        return retval;
    }

    @Override
    public ParserSegmentInfoForURL getSegmentInfoForSegment(int bitrate, long startTimeMilliSec){
        //If the url is one among the adaptation set for video
        ParserSegmentInfoForURL retval = null;
        MMQFPresentationVideoTrackInfo vidTrack = getVideoTrackForBitrate(bitrate);
        if (vidTrack != null){
            MMQFPresentationVideoTrackInfo videoTrack = presentationInfo_.getVideoTrack(0);
            int noOfSegments = videoTrack.trackSegmentInfoVect.size();
            if(lastQueriedSegmentIdx == -1){
                for(int i=0; i<noOfSegments; i++){
                    MMQFPresentationVideoTrackSegmentInfo segInfo = vidTrack.getSegmentInfo(i);
                    long segStartTimeMs = (segInfo.segmentStartTime * 1000)/videoTrack.timeScale;
                    if (segStartTimeMs == startTimeMilliSec){
                        retval = new ParserSegmentInfoForURL();
                        retval.segmentIndex = i;
                        retval.videoTrackInfo = vidTrack;
                        lastQueriedSegmentIdx = i;
                        break;
                    }
                }
            }
            else{
                //heuristic, look from the last queried segment Idx
                for(int i=lastQueriedSegmentIdx; i<noOfSegments; i++){
                    MMQFPresentationVideoTrackSegmentInfo segInfo = vidTrack.getSegmentInfo(i);
                    long segStartTimeMs = (segInfo.segmentStartTime * 1000)/videoTrack.timeScale;
                    if (segStartTimeMs == startTimeMilliSec){
                        retval = new ParserSegmentInfoForURL();
                        retval.segmentIndex = i;
                        retval.videoTrackInfo = vidTrack;
                        lastQueriedSegmentIdx = i;
                        break;
                    }
                }

                if(retval == null){
                    for(int i=0; i<=lastQueriedSegmentIdx; i++){
                        MMQFPresentationVideoTrackSegmentInfo segInfo = vidTrack.getSegmentInfo(i);
                        long segStartTimeMs = (segInfo.segmentStartTime * 1000)/videoTrack.timeScale;
                        if (segStartTimeMs == startTimeMilliSec){
                            retval = new ParserSegmentInfoForURL();
                            retval.segmentIndex = i;
                            retval.videoTrackInfo = vidTrack;
                            lastQueriedSegmentIdx = i;
                            break;
                        }
                    }
                }
             }
        }
        return retval;
    }
    
    public ParserSegmentInfoForURL getSegmentInfoForURL(String url)
    {
        //If the url is one among the adaptation set for video
        boolean needQubitizedUrl = false;
        int repStartIndex = -1;
        for(int repIter = 0; repIter< presentationInfo_.getVideoTracksCount(); repIter++){
            MMQFPresentationVideoTrackInfo videoTrack = presentationInfo_.getVideoTrack(repIter);
            if(url.indexOf(videoTrack.baseURL) != -1){
                needQubitizedUrl = true;
                repStartIndex = repIter;
                break;
            }
        }
        if(needQubitizedUrl == false){
            return null;
        }
        ParserSegmentInfoForURL retval = null;
        MMQFPresentationVideoTrackInfo videoTrack = presentationInfo_.getVideoTrack(0);
        int noOfSegments = videoTrack.trackSegmentInfoVect.size();
        MMQFPresentationVideoTrackInfo vidTrack = presentationInfo_.getVideoTrack(repStartIndex);
        if(lastQueriedSegmentIdx == -1){
            for(int i=0; i<noOfSegments; i++){
                MMQFPresentationVideoTrackSegmentInfo segInfo = vidTrack.getSegmentInfo(i);
                if(segInfo.segmentURL.compareTo(url) == 0){
                    retval = new ParserSegmentInfoForURL();
                    retval.segmentIndex = i;
                    retval.videoTrackInfo = vidTrack;
                    lastQueriedSegmentIdx = i;
                    break;
                }
            }
        }
        else{
            //heuristic, look from the last queried segment Idx
            //MMQFPresentationVideoTrackInfo vidTrack = presentationInfo_.getVideoTrack(repStartIndex);
            for(int i=lastQueriedSegmentIdx; i<noOfSegments; i++){
                MMQFPresentationVideoTrackSegmentInfo segInfo = vidTrack.getSegmentInfo(i);
                if(segInfo.segmentURL.compareTo(url) == 0){
                    retval = new ParserSegmentInfoForURL();
                    retval.segmentIndex = i;
                    retval.videoTrackInfo = vidTrack;
                    lastQueriedSegmentIdx = i;
                    break;
                }
            }

            if(retval == null){
                for(int i=0; i<=lastQueriedSegmentIdx; i++){
                    //MMQFPresentationVideoTrackInfo vidTrack = presentationInfo_.getVideoTrack(repIdx);
                    MMQFPresentationVideoTrackSegmentInfo segInfo = vidTrack.getSegmentInfo(i);
                    if(segInfo.segmentURL.compareTo(url) == 0){
                        retval = new ParserSegmentInfoForURL();
                        retval.segmentIndex = i;
                        retval.videoTrackInfo = vidTrack;
                        lastQueriedSegmentIdx = i;
                        break;
                    }
                }
            }
        }
        return retval;
    }

    public boolean needAuxiliaryResources() {
        totalResToDownload_ = arrAuxRes_.size();
        return (arrAuxRes_.size()>0);
    }

    public ArrayList<AuxResourceInformation> getAuxResourceInfo() {
        return arrAuxRes_;
    }

    public void SetAuxResource(int index, byte[] resource){
        if(index >= 0) {
            totalResToDownload_--;
            MMQFSidxParser parser = new MMQFSidxParser();
            parser.startOffset = arrAuxRes_.get(index).range_.endByteIndex + 1;
            MMQFSidxParser.MMQFSegmentIndex sidx = parser.parse(resource);
            sidxBoxes_.add(index, sidx);
            if ((totalResToDownload_ == 0) && (allAuxResDownloadSuccess() == true)) {
                CreateQubitPresentationInfo();
            }
        }
        else{
            CreateQubitPresentationInfo();
        }
    }

    public void CreateQubitPresentationInfo(){
        if(presentationDescription_== null){
            return;
        }

        presentationInfo_ = new MMQFPresentationInfo("DASH", -1, -1, presentationDescription_.dynamic, presentationDescription_.durationMs, 1000);

        if(presentationDescription_.periods.size()>0)
        {
            int noOfAdaptationSets = presentationDescription_.periods.get(0).adaptationSets.size();//for now let us do it for period 0 only
            if(noOfAdaptationSets > 0){
                ArrayList<AdaptationSet> videoAdaptationSet = new ArrayList<AdaptationSet>();
                for(int j = 0; j< noOfAdaptationSets; j++){
//
                    if(presentationDescription_.periods.get(0).adaptationSets.get(j).type == C.TRACK_TYPE_VIDEO){

                        videoAdaptationSet.add(presentationDescription_.periods.get(0).adaptationSets.get(j));
                    }
                }
                //Iterate thru all the representations of the adaptation set
                int trackCounter = 0;
                for(int i = 0; i<videoAdaptationSet.size(); i++){
                    for(int r=0; r<videoAdaptationSet.get(i).representations.size(); r++){
                        Representation rep = videoAdaptationSet.get(i).representations.get(r);
                        MMQFPresentationVideoTrackInfo info = new MMQFPresentationVideoTrackInfo();
                        info.bitrate = rep.format.bitrate;
                        info.height = rep.format.height;
                        info.width = rep.format.width;
                        info.codecInfo = rep.format.codecs;
                        info.timeScale = 1000000;
                        RangedUri rangURI = rep.getIndexUri();
                        if(rangURI != null) {
//
                            info.baseURL = rep.getIndexUri().toString();

                        }
                        //Add segments info to the representation
                        //Use sidx box for the purpose
                        DashSegmentIndex idx = rep.getIndex();
                        if(idx == null){
                            //Segment info is external to mpd, use parsed sidx boxes for segment information
                            MMQFSidxParser.MMQFSegmentIndex sidx = sidxBoxes_.get(r);
                            if(sidx != null) {

                                RangedUri uri = rep.getIndexUri();
                                Uri x = Uri.parse(uri.toString());
                                String baseUrl = x.toString();
                                for (int sidxIdx = 0; sidxIdx < sidx.length; sidxIdx++) {
                                    MMQFPresentationVideoTrackSegmentInfo segInfo = new MMQFPresentationVideoTrackSegmentInfo();
                                    segInfo.duration = sidx.durationsUs[sidxIdx];
                                    segInfo.segmentSz = sidx.sizes[sidxIdx] * 8;//(int)((double)(segInfo.duration *  info.bitrate)/1000000);//sidx.sizes[sidxIdx];
                                    segInfo.segmentStartTime = sidx.timesUs[sidxIdx];
                                    segInfo.segmentURL = baseUrl + KStartIdxTag + sidx.offsets[sidxIdx] + KSegLenTag + (sidx.offsets[sidxIdx] + sidx.sizes[sidxIdx] - 1);
                                    //MMLogger.i("CheckQubit", segInfo.segmentURL);

                                    //update the segment url here
                                    info.trackSegmentInfoVect.add(segInfo);
                                }
                            }
                        }
                        else{
                            int totalseg = idx.getSegmentCount(C.msToUs(C.TIME_UNSET));
                            int first = (int) idx.getFirstSegmentNum();
                            for(int sidxIdx =(int)idx.getFirstSegmentNum(); sidxIdx<idx.getSegmentCount(C.msToUs(C.TIME_UNSET)); sidxIdx++){

                                MMQFPresentationVideoTrackSegmentInfo segInfo = new MMQFPresentationVideoTrackSegmentInfo();
                                segInfo.duration = idx.getDurationUs((long)sidxIdx,C.TIME_UNSET) / 1000;
                                segInfo.segmentSz = (int) (segInfo.duration * rep.format.bitrate);//(int)((double)(segInfo.duration *  info.bitrate)/1000000);//sidx.sizes[sidxIdx];
                                segInfo.segmentStartTime = idx.getTimeUs(sidxIdx);
                                segInfo.segmentURL = idx.getSegmentUrl(sidxIdx).toString();
                                info.trackSegmentInfoVect.add(segInfo);
                            }
                        }
                        info.trackIndex = trackCounter++;
                        presentationInfo_.addVideoPresentationTrack(info.width, info.height, info.codecInfo, info);
                    }
                }

            }
        }
    }

    public void AuxResRetrievalFailed(int index){
        totalResToDownload_--;
        resFailedDownload_++;
        sidxBoxes_.add(index, null);
    }

    public boolean allAuxResDownloadSuccess(){
        return(totalResToDownload_ == 0 && resFailedDownload_ == 0);
    }

    public boolean isSupportedQBRPresentation(){
        if (presentationInfo_ != null && presentationInfo_.canQubitBeApplied()){
            return true;
        }
        return false;
    }

    private int lastQueriedSegmentIdx = -1;
    private int resFailedDownload_ = 0;
    private int totalResToDownload_ = 0;
    private ArrayList<AuxResourceInformation> arrAuxRes_ = null;
    private ArrayList<MMQFSidxParser.MMQFSegmentIndex> sidxBoxes_ = null;
    private MediaPresentationDescriptionParser mpdparser_ = null;
    private MediaPresentationDescription presentationDescription_ = null;
    private MMQFPresentationInfo presentationInfo_ = null;
}
