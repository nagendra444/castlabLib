package com.mediamelon.qubit;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;
import java.io.IOException;
import 	java.nio.charset.Charset;

/**
 * Created by Rupesh on 19-03-2015.
 */
public class MSSManifestParser implements MMQFABRManifestParser,
    TemplatizedURLGeneratorInterface{
    class StreamingMediaInformation{
        StreamingMediaInformation()
        {
            majorVersion = -1;
            minorVersion = -1;
            timeScale = 10000000;
            duration = 0;
            isLive = false;
            lookAheadFragmentCount = 0;
            dvrWndLength = 0;
        }
        int majorVersion;
        int minorVersion;
        int timeScale;
        long duration;
        Boolean isLive;
        int lookAheadFragmentCount;
        int dvrWndLength;
    }

    public boolean isSupportedQBRPresentation(){
        return true;
    }

    class VideoStreamInformation{
        VideoStreamInformation()
        {
            noOfFragments = 0;
            noOfTracks = 0;
            uRLPattern = null;
            maxWidth = 0;
            maxHeight = 0;
            displayHeight = 0;
            displayHeight = 0;
            timeScale = -1;
            name = null;
            videoTracks = new Vector();
            streamFragments = new Vector();
        }
        public int noOfFragments;
        public int noOfTracks;
        public String uRLPattern;
        public int maxWidth;
        public int maxHeight;
        public int displayWidth;
        public int displayHeight;
        public int timeScale;
        public String name;
        public Vector videoTracks;
        public Vector streamFragments;
        public Vector streamFragmentsMetadata; //Todo, required for version 2.2 of MSS where fragment can have repeat attribute
    }

    class VideoTrackInfo{
        VideoTrackInfo()
        {
            index = bitrate = maxWidth = maxHeight = nALUnitLength = -1;
            codecPrivateData = null;
        }
        public int index;
        public int bitrate;
        public int maxWidth;
        public int maxHeight;
        public String codecPrivateData;
        public int nALUnitLength;
    }

    class StreamFragment
    {
        StreamFragment()
        {
            ordinal = repeatCnt = -1;
            time = duration = 0;
        }
        public int ordinal;
        public long duration;
        public long time;
        public int repeatCnt;
    }

    public MMQFPresentationInfo parse(String manifestFileData, URL manifestURL)
    {

        MMQFPresentationInfo retval = null;
        /*presentationInfo_ =  getDummyPresentationInfoFromMSSInfo();
        return presentationInfo_;*/

        manifestXML = manifestFileData;
        //Manifest data is in xml format
        try {
            streamingMediaInfo = new StreamingMediaInformation();
            parseManifest();
            assert(videoStreamInformation.noOfTracks == videoStreamInformation.videoTracks.size());
            retval = getGenericPresentationInfoFromMSSInfo();
            presentationInfo_ = retval;
        }
        catch(XmlPullParserException e)
        {
            MMLogger.e(TAG, "Exception - XmlPullParserException while parsing xml");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            MMLogger.e(TAG, "Exception - IOException while parsing xml");
            e.printStackTrace();
        }

        return retval;
    }

    void parseManifest() throws XmlPullParserException, IOException
    {
        MMLogger.i(TAG, "Entering the parse manifest ...");

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser xpp = factory.newPullParser();

        MMLogger.e(TAG, "------------------------------------------------");
        //byte[] xutf8 = manifestXML.getBytes(StandardCharsets.UTF_8);
        byte[] xutf8 = manifestXML.getBytes(Charset.forName("UTF-8"));

        ByteArrayInputStream g = new ByteArrayInputStream(xutf8);
        xpp.setInput(new ByteArrayInputStream(manifestXML.getBytes()), "UTF-8");

        MMLogger.e(TAG, manifestXML);
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_DOCUMENT) {
                MMLogger.i(TAG, "Start document");
            } else if(eventType == XmlPullParser.START_TAG) {
                String elementName = xpp.getName();
                parseStreamingMediaInformation(xpp, elementName);
                setVideoStreamIndexStopPending(xpp, elementName);
                parseVideoTrackInformation(xpp, elementName);
                parseStreamFragmentInformation(xpp, elementName);
            } else if(eventType == XmlPullParser.END_TAG) {
                String elementName = xpp.getName();
                resetVideoStreamIndexStopPending(xpp, elementName);
            }
            eventType = xpp.next();
            if(eventType == XmlPullParser.END_DOCUMENT) {
                MMLogger.i(TAG, "End document");
            }
        }

    }

    private Boolean parseStreamingMediaInformation(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KSmoothStreamingMediaElem))
        {
            int noOfAttributes = parser.getAttributeCount();
            for(int i = 0; i < noOfAttributes; i++)
            {
                String attributeName = parser.getAttributeName(i);
                String attributeValue = parser.getAttributeValue(i);
                if(attributeName != null && attributeValue != null) {
                    if (attributeName.equals(KSmoothStreamingMediaMajorVersionAttribute)) {
                        streamingMediaInfo.majorVersion = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KSmoothStreamingMediaMinorVersionAttribute)) {
                        streamingMediaInfo.minorVersion = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KSmoothStreamingMediaTimeScaleAttribute)) {
                        streamingMediaInfo.timeScale = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KSmoothStreamingMediaDurationAttribute)) {
                        streamingMediaInfo.duration = Long.parseLong(attributeValue);
                    } else if (attributeName.equals(KSmoothStreamingMediaIsLiveAttribute)) {
                        streamingMediaInfo.isLive = attributeValue.equals("TRUE");
                    } else if (attributeName.equals(KSmoothStreamingMediaLookAheadFragmentCountAttribute)) {
                        streamingMediaInfo.lookAheadFragmentCount = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KSmoothStreamingMediaDVRWindowLengthAttribute)) {
                        streamingMediaInfo.dvrWndLength = Integer.parseInt(attributeValue);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void setVideoStreamIndexStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KStreamElement) && parser.getAttributeValue(null, KStreamTypeAttribute).equals("video")) {
            videoStreamIndexStopPending = true;
            videoStreamInformation = new VideoStreamInformation();
            parseVideoStream(parser, elementName);
        }
    }

    private Boolean parseVideoStream(XmlPullParser parser, String elementName)
    {
        String tmp = parser.getAttributeValue(null, KStreamNameAttribute);
        if(tmp != null) {
            videoStreamInformation.name = tmp;
        }
        tmp = parser.getAttributeValue(null, KStreamTimeScaleAttribute);
        if(tmp != null) {
            videoStreamInformation.timeScale = Integer.parseInt(tmp);
        }
        tmp = parser.getAttributeValue(null, KStreamMaxWidthAttribute);
        if(tmp != null) {
            videoStreamInformation.maxWidth = Integer.parseInt(tmp);
        }
        tmp = parser.getAttributeValue(null, KStreamMaxHeightAttribute);
        if(tmp != null) {
            videoStreamInformation.maxHeight = Integer.parseInt(tmp);
        }
        tmp = parser.getAttributeValue(null, KDisplayHeightAttribute);
        if(tmp != null) {
            videoStreamInformation.displayHeight = Integer.parseInt(tmp);
        }
        tmp = parser.getAttributeValue(null, KDisplayWidthAttribute);
        if(tmp != null) {
            videoStreamInformation.displayWidth = Integer.parseInt(tmp);
        }
        tmp = parser.getAttributeValue(null, KStreamNumberOfFragmentsAttribute);
        if(tmp != null) {
            videoStreamInformation.noOfFragments = Integer.parseInt(tmp);
        }

        tmp = parser.getAttributeValue(null, KStreamNumberOfTracksAttribute);
        if(tmp != null) {
            videoStreamInformation.noOfTracks = Integer.parseInt(tmp);
        }

        tmp = parser.getAttributeValue(null, KStreamUrlAttribute);
        if(tmp != null) {
            videoStreamInformation.uRLPattern = tmp;
        }

        if(videoStreamInformation.timeScale == -1)
        {
            videoStreamInformation.timeScale = streamingMediaInfo.timeScale;
        }

        return true;
    }

    private Boolean parseVideoTrackInformation(XmlPullParser parser, String elementName)
    {
        if(videoStreamIndexStopPending && elementName.equals(KQualityLevel))
        {
            VideoTrackInfo videoTrackInfo = new VideoTrackInfo();
            int noOfAttributes = parser.getAttributeCount();
            for(int i = 0; i < noOfAttributes; i++)
            {
                String attributeName = parser.getAttributeName(i);
                String attributeValue = parser.getAttributeValue(i);
                if(attributeName != null && attributeValue != null) {
                    if (attributeName.equals(KIndexAttribute)) {
                        videoTrackInfo.index = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KBitrateAttribute)) {
                        videoTrackInfo.bitrate = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KMaxWidthAttribute)) {
                        videoTrackInfo.maxWidth = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KMaxHeightAttribute)) {
                        videoTrackInfo.maxHeight = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KCodecPrivateDataAttribute)) {
                        videoTrackInfo.codecPrivateData = attributeValue;
                    } else if (attributeName.equals(KNALUnitLengthFieldAttribute)) {
                        videoTrackInfo.nALUnitLength = Integer.parseInt(attributeValue);
                    }
                }
            }
            if(videoStreamInformation.videoTracks != null ) {
                videoStreamInformation.videoTracks.add(videoTrackInfo);
                return true;
            }
        }
        return false;
    }

    private Boolean parseStreamFragmentInformation(XmlPullParser parser, String elementName)
    {
        if(videoStreamIndexStopPending && elementName.equals(KStreamFragmentElement)) {
            StreamFragment fragment = new StreamFragment();
            int noOfAttributes = parser.getAttributeCount();
            for(int i = 0; i < noOfAttributes; i++) {
                String attributeName = parser.getAttributeName(i);
                String attributeValue = parser.getAttributeValue(i);
                if(attributeName != null && attributeValue != null) {
                    if (attributeName.equals(KStreamFragmentNumber)) {
                        fragment.ordinal = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KStreamFragmentDuration)) {
                        fragment.duration = Integer.parseInt(attributeValue);
                    } else if (attributeName.equals(KStreamFragmentTime)) {
                        fragment.time = Long.parseLong(attributeValue);
                    } else if (attributeName.equals(KStreamFragmentRepeat)) {
                        fragment.repeatCnt = Integer.parseInt(attributeValue);
                    }
                }
            }
            if(videoStreamInformation.streamFragments != null) {
                videoStreamInformation.streamFragments.add(fragment);
            }
            return true;
        }
        return false;
    }

    private void resetVideoStreamIndexStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KStreamElement)) {
            videoStreamIndexStopPending = false;
        }
    }

    public String generateURL(int bitrate, long startTime)
    {
        String bitrateStr = "" + bitrate;
        String startTimeStr = "" + startTime;
        String urlToReturn = new String(videoStreamInformation.uRLPattern);
        urlToReturn = urlToReturn.replace("{bitrate}", bitrateStr);
        urlToReturn = urlToReturn.replace("{start time}", startTimeStr);
        return urlToReturn;
    }

    public String getBaseURL(String inURL)
    {
        String retval = null;
        String strToProcess = videoStreamInformation.uRLPattern;
        int index = strToProcess.indexOf("{");
        if(index != -1)
        {
            String strToLocate = strToProcess.substring(0, index);
            int indexOfPattern = inURL.indexOf(strToLocate);
            if(indexOfPattern != -1)
            {
                retval = inURL.substring(0, indexOfPattern);
            }
        }
        return retval;
    }

    MMQFPresentationInfo getDummyPresentationInfoFromMSSInfo()
    {
        long duration = 2603333340L;
        streamingMediaInfo = new StreamingMediaInformation();
        MMQFPresentationInfo presentationInfo = new MMQFPresentationInfo("MSS", 0, 0, false, duration, streamingMediaInfo.timeScale);
        videoStreamInformation = new VideoStreamInformation();
        videoStreamInformation.uRLPattern = "QualityLevels({bitrate})/Fragments(video={start time})";
        videoStreamInformation.noOfTracks = 6;
        videoStreamInformation.noOfFragments = 130;
        videoStreamInformation.timeScale = 10000000;

        VideoTrackInfo temp = new VideoTrackInfo();
        temp.bitrate = 446787;
        videoStreamInformation.videoTracks.add(temp);

        VideoTrackInfo temp2 = new VideoTrackInfo();
        temp2.bitrate = 902319;
        videoStreamInformation.videoTracks.add(temp2);

        VideoTrackInfo temp3 = new VideoTrackInfo();
        temp3.bitrate = 1363046;
        videoStreamInformation.videoTracks.add(temp3);

        VideoTrackInfo temp4 = new VideoTrackInfo();
        temp4.bitrate = 1825998;
        videoStreamInformation.videoTracks.add(temp4);

        VideoTrackInfo temp5 = new VideoTrackInfo();
        temp5.bitrate = 2745322;
        videoStreamInformation.videoTracks.add(temp5);

        VideoTrackInfo temp6 = new VideoTrackInfo();
        temp6.bitrate = 3648912;
        videoStreamInformation.videoTracks.add(temp6);

        StreamFragment frag0 = new StreamFragment();
        frag0.duration = 20000000;
        frag0.ordinal = 0;
        frag0.time = 833333;
        videoStreamInformation.streamFragments.add(frag0);

        for(int i = 1; i<130; i++)
        {
            StreamFragment frag = new StreamFragment();
            frag.duration = 20000000;
            frag.ordinal = i;
            videoStreamInformation.streamFragments.add(frag);
        }

        //Create presentation video track
        for(int i=0; i< videoStreamInformation.videoTracks.size(); i++)
        {
            VideoTrackInfo trackInfo = (VideoTrackInfo)videoStreamInformation.videoTracks.elementAt(i);
            MMQFPresentationVideoTrackInfo videoTrack = new MMQFPresentationVideoTrackInfo(this);
            videoTrack.bitrate = trackInfo.bitrate;
            videoTrack.width = 1280;
            videoTrack.height = 720;
            videoTrack.codecInfo = "000000016764001FACD9405005BB011000000300100000030300F18319600000000168EBECB22C";

            //Create segment list for track
            StreamFragment prevFrag = null;
            for(int j=0; j< videoStreamInformation.noOfFragments; j++)
            {
                MMQFPresentationVideoTrackSegmentInfo segmentinfo = new MMQFPresentationVideoTrackSegmentInfo();
                //Assumption, stream fragment is not having repeat fragment
                StreamFragment frag = (StreamFragment) videoStreamInformation.streamFragments.elementAt(j);
                //Update the missing fields of streamFragment
                if(frag.time == -1)
                {
                    if(prevFrag!=null)
                    {
                        frag.time = prevFrag.time + prevFrag.duration;
                    }
                    else
                    {
                        frag.time = 0;
                    }
                }
                if(frag.duration == -1)
                {
                    if(prevFrag!=null)
                    {
                        frag.duration = frag.time - prevFrag.time;
                    }
                    else
                    {
                        StreamFragment nextFragment = null;
                        if(j < (videoStreamInformation.noOfFragments -1))
                        {
                            nextFragment = (StreamFragment) videoStreamInformation.streamFragments.elementAt(j+1);
                            if(nextFragment != null) {
                                assert (nextFragment.time != -1);
                                frag.duration = nextFragment.time - frag.time;
                            }
                        }
                        else
                        {
                            frag.duration = (int)streamingMediaInfo.duration;
                        }
                    }
                }

                segmentinfo.segmentStartTime = frag.time;
                segmentinfo.duration = frag.duration;
                int durationInSec = (int)(segmentinfo.duration/videoStreamInformation.timeScale); //in sec
                segmentinfo.segmentSz = durationInSec;
                videoTrack.trackSegmentInfoVect.add(segmentinfo);
                prevFrag = frag;
            }
            presentationInfo.addVideoPresentationTrack(videoTrack.width, videoTrack.height,videoTrack.codecInfo, videoTrack);
        }
        return presentationInfo;
    }

    MMQFPresentationInfo getGenericPresentationInfoFromMSSInfo()
    {
        MMQFPresentationInfo presentationInfo = new MMQFPresentationInfo("MSS",streamingMediaInfo.lookAheadFragmentCount, streamingMediaInfo.dvrWndLength, streamingMediaInfo.isLive, streamingMediaInfo.duration, streamingMediaInfo.timeScale);
        int trackCounter = 0;
        for(int i=0; i< videoStreamInformation.videoTracks.size(); i++)
        {
            VideoTrackInfo trackInfo = (VideoTrackInfo)videoStreamInformation.videoTracks.elementAt(i);
            MMQFPresentationVideoTrackInfo videoTrack = new MMQFPresentationVideoTrackInfo(this);
            videoTrack.bitrate = trackInfo.bitrate;
            videoTrack.width = (trackInfo.maxWidth != -1)?trackInfo.maxWidth:videoStreamInformation.maxWidth;
            videoTrack.height = (trackInfo.maxHeight != -1)?trackInfo.maxHeight:videoStreamInformation.maxHeight;
            videoTrack.codecInfo = trackInfo.codecPrivateData;

            //Create segment list for track
            StreamFragment prevFrag = null;
            for(int j=0; j< videoStreamInformation.noOfFragments; j++)
            {
                MMQFPresentationVideoTrackSegmentInfo segmentinfo = new MMQFPresentationVideoTrackSegmentInfo();
                //Assumption, stream fragment is not having repeat fragment
                StreamFragment frag = (StreamFragment) videoStreamInformation.streamFragments.elementAt(j);
                //Update the missing fields of streamFragment
                if(frag.time == 0)
                {
                    if(prevFrag!=null)
                    {
                        frag.time = prevFrag.time + prevFrag.duration;
                    }
                }
                if(frag.duration == 0)
                {
                    if(prevFrag!=null)
                    {
                        frag.duration = frag.time - prevFrag.time;
                    }
                    else
                    {
                        StreamFragment nextFragment = null;
                        if(j < (videoStreamInformation.noOfFragments -1))
                        {
                            nextFragment = (StreamFragment) videoStreamInformation.streamFragments.elementAt(j+1);
                            if(nextFragment != null) {
                                assert (nextFragment.time != -1);
                                frag.duration = nextFragment.time - frag.time;
                            }
                        }
                        else
                        {
                            frag.duration = (int)streamingMediaInfo.duration;
                        }
                    }
                }
                segmentinfo.segmentStartTime = frag.time;
                segmentinfo.duration = frag.duration;
                double durationInSec = ((double)segmentinfo.duration/videoStreamInformation.timeScale); //in sec
                segmentinfo.segmentSz = (int)(durationInSec *  videoTrack.bitrate);
                videoTrack.trackSegmentInfoVect.add(segmentinfo);
                videoTrack.timeScale = videoStreamInformation.timeScale;
                prevFrag = frag;
            }
            videoTrack.trackIndex = trackCounter++;
            presentationInfo.addVideoPresentationTrack(videoTrack.width, videoTrack.height,videoTrack.codecInfo, videoTrack);
        }
        return presentationInfo;
    }


    public ParserSegmentInfoForURL getSegmentInfoForSegment(int bitrate, long startTimeMilliSec){
        return null;
    }

    //If not a video frag url, send null
    public ParserSegmentInfoForURL getSegmentInfoForURL(String inUrl)
    {
        ParserSegmentInfoForURL retval = new ParserSegmentInfoForURL();
        String urlPattern = videoStreamInformation.uRLPattern;
        //Split the url based on urlPattern QualityLevels({bitrate})/Fragments(audio={start time})
        //Need to parse bitrate ans start time out of it
        String qualityLevelPrefix = "/QualityLevels(";

        //Start time prefix can have kind of regex to distinguish diff languages of same track type.
        //So look for { in videoStreamInformation.uRLPattern after the /Fragments to decide the startTimePrefix
        String startTimePrefix = ")/Fragments(video=";
        String fragStr = ")/Fragments";
        int indexOfFrag = videoStreamInformation.uRLPattern.indexOf(fragStr);
        if(indexOfFrag >=0)
        {
            int indexOfCurlBrace = videoStreamInformation.uRLPattern.indexOf("{", indexOfFrag);
            if(indexOfCurlBrace > 0)
            {
                startTimePrefix = videoStreamInformation.uRLPattern.substring(indexOfFrag, indexOfCurlBrace);
            }
        }


        int bitrate = 0;
        int index = inUrl.indexOf(qualityLevelPrefix);
        int endIndex = inUrl.indexOf(startTimePrefix);
        if((endIndex != 1 && index != -1) && (endIndex>index) )
        {
            String strToReturn = inUrl;
            String strToReplace = inUrl.substring(index + qualityLevelPrefix.length(), endIndex);
            bitrate = Integer.parseInt(strToReplace);
            for(int i = 0; i< presentationInfo_.getVideoTracksCount(); i++)
            {
                MMQFPresentationVideoTrackInfo videoTrackInfo = presentationInfo_.getVideoTrack(i);
                if(videoTrackInfo.bitrate == bitrate)
                {
                    retval.videoTrackInfo = videoTrackInfo;
                    break;
                }
            }
            if(retval.videoTrackInfo != null)
            {
                if(inUrl.endsWith(")"))
                {
                    String startTimeStr = inUrl.substring(endIndex + startTimePrefix.length(), inUrl.length() -1);
                    if(startTimeStr.length() > 0)
                    {
                        long startTime = Long.parseLong(startTimeStr);
                        if(startTime >=0 )
                        {
                            //heuristic for optimization - generally this call will be made for segments in sequence. So, start search from last looked up segment index
                            Vector trackSegVect = retval.videoTrackInfo.trackSegmentInfoVect;
                            if(lastSearchedSegmentIdx >= 0)
                            {
                                MMQFPresentationVideoTrackSegmentInfo segInfo = (MMQFPresentationVideoTrackSegmentInfo)trackSegVect.elementAt(lastSearchedSegmentIdx);
                                if(startTime == segInfo.segmentStartTime)
                                {
                                    retval.segmentIndex = lastSearchedSegmentIdx;
                                }
                            }
                            if(retval.segmentIndex == -1) {
                                for (int i = lastSearchedSegmentIdx + 1; i < trackSegVect.size(); i++) {
                                    MMQFPresentationVideoTrackSegmentInfo segInfo = (MMQFPresentationVideoTrackSegmentInfo) trackSegVect.elementAt(i);
                                    if (startTime == segInfo.segmentStartTime) {
                                        retval.segmentIndex = i;
                                        lastSearchedSegmentIdx = i;
                                        break;
                                    }
                                }
                            }
                            if(retval.segmentIndex == -1)
                            {
                                for(int i = 0; i<=lastSearchedSegmentIdx; i++)
                                {
                                    MMQFPresentationVideoTrackSegmentInfo segInfo = (MMQFPresentationVideoTrackSegmentInfo)trackSegVect.elementAt(i);
                                    if(startTime == segInfo.segmentStartTime)
                                    {
                                        retval.segmentIndex = i;
                                        lastSearchedSegmentIdx = i;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if( (retval!=null) && (retval.segmentIndex == -1))
        {
            retval = null;
        }
        return retval;
    }

    public boolean needAuxiliaryResources(){
        return false;
    }

    public ArrayList<AuxResourceInformation> getAuxResourceInfo(){
        return null;
    }

    public void SetAuxResource(int index, byte[] resource){
        //noop, maybe better assert failure...?
    }
    public boolean allAuxResDownloadSuccess(){return true;}
    public void AuxResRetrievalFailed(int index){}
    private StreamingMediaInformation streamingMediaInfo;
    private VideoStreamInformation videoStreamInformation;
    private Boolean videoStreamIndexStopPending = false;
    private MMQFPresentationInfo presentationInfo_;
    private int lastSearchedSegmentIdx = -1;

    private String manifestXML = null;
    private static String TAG = "MSSParser";

    private static final String KSmoothStreamingMediaElem = "SmoothStreamingMedia";
    private static final String KSmoothStreamingMediaMajorVersionAttribute = "MajorVersion";
    private static final String KSmoothStreamingMediaMinorVersionAttribute = "MinorVersion";
    private static final String KSmoothStreamingMediaTimeScaleAttribute = "TimeScale";
    private static final String KSmoothStreamingMediaDurationAttribute = "Duration";
    private static final String KSmoothStreamingMediaIsLiveAttribute = "IsLive";
    private static final String KSmoothStreamingMediaLookAheadFragmentCountAttribute = "LookAheadFragmentCount";
    private static final String KSmoothStreamingMediaDVRWindowLengthAttribute = "DVRWindowLength";

    private static final String KStreamElement = "StreamIndex";
    private static final String KStreamTypeAttribute = "Type";// ("video" / "audio" / "text")
    private static final String KStreamNumberOfFragmentsAttribute = "Chunks";
    private static final String KStreamNumberOfTracksAttribute = "QualityLevels";
    private static final String KStreamUrlAttribute = "Url";
    private static final String KStreamNameAttribute = "Name";
    private static final String KStreamTimeScaleAttribute = "TimeScale";
    private static final String KStreamMaxWidthAttribute = "MaxWidth";
    private static final String KStreamMaxHeightAttribute = "MaxHeight";
    private static final String KDisplayWidthAttribute = "DisplayWidth";
    private static final String KDisplayHeightAttribute = "DisplayHeight";

    private static final String KQualityLevel = "QualityLevel";
    private static final String KIndexAttribute = "Index";
    private static final String KBitrateAttribute = "Bitrate";
    private static final String KMaxWidthAttribute = "MaxWidth";
    private static final String KMaxHeightAttribute = "MaxHeight";
    private static final String KCodecPrivateDataAttribute = "CodecPrivateData";
    private static final String KNALUnitLengthFieldAttribute = "NALUnitLengthField";

    private static final String KStreamFragmentElement = "c";
    private static final String KStreamFragmentDuration = "d";
    private static final String KStreamFragmentTime = "t";
    private static final String KStreamFragmentNumber = "n";
    private static final String KStreamFragmentRepeat = "r";
}
