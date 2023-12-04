package com.mediamelon.qubit;
import android.util.Log;
import com.mediamelon.qubit.MMQFABRManifestParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;             

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
/**
 * Created by Rupesh on 19-03-2015.
 */

public class HLSPlaylistParser implements MMQFABRManifestParser{
    private static final String TAG = "HLSPlaylistParser";
    private static final String VERSION_TAG = "#EXT-X-VERSION";
    private static final String STREAM_INF_TAG = "#EXT-X-STREAM-INF";
    private static final String MEDIA_TAG = "#EXT-X-MEDIA";
    private static final String DISCONTINUITY_TAG = "#EXT-X-DISCONTINUITY";
    private static final String DISCONTINUITY_SEQUENCE_TAG = "#EXT-X-DISCONTINUITY-SEQUENCE";
    private static final String MEDIA_DURATION_TAG = "#EXTINF";
    private static final String MEDIA_SEQUENCE_TAG = "#EXT-X-MEDIA-SEQUENCE";
    private static final String TARGET_DURATION_TAG = "#EXT-X-TARGETDURATION";
    private static final String ENDLIST_TAG = "#EXT-X-ENDLIST";
    private static final String KEY_TAG = "#EXT-X-KEY";
    private static final String BYTERANGE_TAG = "#EXT-X-BYTERANGE";

    private static final String BANDWIDTH_ATTR = "BANDWIDTH";
    private static final String CODECS_ATTR = "CODECS";
    private static final String RESOLUTION_ATTR = "RESOLUTION";
    private static final String LANGUAGE_ATTR = "LANGUAGE";
    private static final String NAME_ATTR = "NAME";
    private static final String TYPE_ATTR = "TYPE";
    private static final String METHOD_ATTR = "METHOD";
    private static final String URI_ATTR = "URI";
    private static final String IV_ATTR = "IV";

    private static final String AUDIO_TYPE = "AUDIO";
    private static final String VIDEO_TYPE = "VIDEO";
    private static final String SUBTITLES_TYPE = "SUBTITLES";
    private static final String CLOSED_CAPTIONS_TYPE = "CLOSED-CAPTIONS";

    private static final String METHOD_NONE = "NONE";
    private static final String METHOD_AES128 = "AES-128";

    private static final Pattern BANDWIDTH_ATTR_REGEX =
            Pattern.compile(BANDWIDTH_ATTR + "=(\\d+)\\b");
    private static final Pattern CODECS_ATTR_REGEX =
            Pattern.compile(CODECS_ATTR + "=\"(.+?)\"");
    private static final Pattern RESOLUTION_ATTR_REGEX =
            Pattern.compile(RESOLUTION_ATTR + "=(\\d+x\\d+)");
    private static final Pattern MEDIA_DURATION_REGEX =
            Pattern.compile(MEDIA_DURATION_TAG + ":([\\d.]+)\\b");
    private static final Pattern MEDIA_SEQUENCE_REGEX =
            Pattern.compile(MEDIA_SEQUENCE_TAG + ":(\\d+)\\b");
    private static final Pattern TARGET_DURATION_REGEX =
            Pattern.compile(TARGET_DURATION_TAG + ":(\\d+)\\b");
    private static final Pattern VERSION_REGEX =
            Pattern.compile(VERSION_TAG + ":(\\d+)\\b");
    private static final Pattern BYTERANGE_REGEX =
            Pattern.compile(BYTERANGE_TAG + ":(\\d+(?:@\\d+)?)\\b");

    private static final Pattern METHOD_ATTR_REGEX =
            Pattern.compile(METHOD_ATTR + "=(" + METHOD_NONE + "|" + METHOD_AES128 + ")");
    private static final Pattern URI_ATTR_REGEX =
            Pattern.compile(URI_ATTR + "=\"(.+?)\"");
    private static final Pattern IV_ATTR_REGEX =
            Pattern.compile(IV_ATTR + "=([^,.*]+)");
    private static final Pattern TYPE_ATTR_REGEX =
            Pattern.compile(TYPE_ATTR + "=(" + AUDIO_TYPE + "|" + VIDEO_TYPE + "|" + SUBTITLES_TYPE + "|"
                    + CLOSED_CAPTIONS_TYPE + ")");
    private static final Pattern LANGUAGE_ATTR_REGEX =
            Pattern.compile(LANGUAGE_ATTR + "=\"(.+?)\"");
    private static final Pattern NAME_ATTR_REGEX =
            Pattern.compile(NAME_ATTR + "=\"(.+?)\"");


    private class MMQBRHlsPlaylist{
        public final static int TYPE_MASTER = 0;
        public final static int TYPE_MEDIA = 1;
        public final String baseUri;
        public final int type;
        protected MMQBRHlsPlaylist(String baseUri, int type) {
            this.baseUri = baseUri;
            this.type = type;
        }
    }

    public final class C {

        /**
         * The number of microseconds in one second.
         */
        public static final long MICROS_PER_SECOND = 1000000L;

        /**
         * Represents an unbounded length of data.
         */
        public static final int LENGTH_UNBOUNDED = -1;
    }

    public final class MMQBRVariant {
        public final String url;
        public final String mimeType;
        public final int width;
        public final int height;
        public final int frameRate;
        public final int bitrate;
        public final String codecs;

        public MMQBRVariant(String uRL, String mType, int w, int h, int fRate, int brate, String cInfo) {
            url = uRL;
            mimeType = mType;
            width = w;
            height = h;
            frameRate = fRate;
            bitrate = brate;
            codecs = cInfo;
        }
    }

    /**
     * Thrown when an error occurs parsing media data.
     */
    private static class MMParserException extends IOException {

        public MMParserException(String message) {
            super(message);
        }

        public MMParserException(Throwable cause) {
            super(cause);
        }

        public MMParserException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    private class MMQBRHlsMasterPlaylist extends MMQBRHlsPlaylist {
        public final List<MMQBRVariant> variants;
        public MMQBRHlsMasterPlaylist(String baseUri, List<MMQBRVariant> variants) {
            super(baseUri, MMQBRHlsPlaylist.TYPE_MASTER);
            this.variants = Collections.unmodifiableList(variants);
        }
    }

    public final class MMSegment implements Comparable<Long> {
        public final String url;
        public final double durationSecs;
        public final int discontinuitySequenceNumber;
        public final long startTimeUs;
        public final int byterangeOffset;
        public final int byterangeLength;

        public MMSegment(String uri, double durationSecs, int discontinuitySequenceNumber,
                         long startTimeUs, int byterangeOffset, int byterangeLength) {
            this.url = uri;
            this.durationSecs = durationSecs;
            this.discontinuitySequenceNumber = discontinuitySequenceNumber;
            this.startTimeUs = startTimeUs;
            this.byterangeOffset = byterangeOffset;
            this.byterangeLength = byterangeLength;
        }

        @Override
        public int compareTo(Long startTimeUs) {
            return this.startTimeUs > startTimeUs ? 1 : (this.startTimeUs < startTimeUs ? -1 : 0);
        }
    }

    public final class MMQBRHlsMediaPlaylist extends MMQBRHlsPlaylist {
        public static final String ENCRYPTION_METHOD_NONE = "NONE";
        public static final String ENCRYPTION_METHOD_AES_128 = "AES-128";

        public final int mediaSequence;
        public final int targetDurationSecs;
        public final int version;
        public final List<MMSegment> segments;
        public final boolean live;
        public final long durationUs;
        boolean hasClientSideAdInsertion = false;

        public MMQBRHlsMediaPlaylist(String baseUri, int mediaSequence, int targetDurationSecs, int version,
                                     boolean live, List<MMSegment> segments, boolean hasAdInsertion) {
            super(baseUri, MMQBRHlsPlaylist.TYPE_MEDIA);
            this.mediaSequence = mediaSequence;
            this.targetDurationSecs = targetDurationSecs;
            this.version = version;
            this.live = live;
            this.segments = segments;

            if (!segments.isEmpty()) {
                MMSegment last = segments.get(segments.size() - 1);
                durationUs = last.startTimeUs + (long) (last.durationSecs * C.MICROS_PER_SECOND);
            } else {
                durationUs = 0;
            }
            this.hasClientSideAdInsertion = hasAdInsertion;
        }
    }

    private MMQBRHlsMasterPlaylist parseMasterPlaylist(LineIterator iterator, String baseUri) throws IOException {
        ArrayList<MMQBRVariant> variants = new ArrayList<MMQBRVariant>();
        int bitrate = 0;
        String codecs = null;
        int width = -1;
        int height = -1;
        String name = null;

        boolean expectingStreamInfUrl = false;
        String line;
        while (iterator.hasNext()) {
            line = iterator.next();
            if (line.startsWith(MEDIA_TAG)) {
                Log.e(TAG, "EXT-X-MEDIA :: " + line);
            } else if (line.startsWith(STREAM_INF_TAG)) {
                bitrate = parseIntAttr(line, BANDWIDTH_ATTR_REGEX, BANDWIDTH_ATTR);
                codecs = parseOptionalStringAttr(line, CODECS_ATTR_REGEX);
                name = parseOptionalStringAttr(line, NAME_ATTR_REGEX);
                String resolutionString = parseOptionalStringAttr(line, RESOLUTION_ATTR_REGEX);
                if (resolutionString != null) {
                    String[] widthAndHeight = resolutionString.split("x");
                    width = Integer.parseInt(widthAndHeight[0]);
                    if (width <= 0) {
                        // Width was invalid.
                        width = -1;
                    }
                    height = Integer.parseInt(widthAndHeight[1]);
                    if (height <= 0) {
                        // Height was invalid.
                        height = -1;
                    }
                } else {
                    width = -1;
                    height = -1;
                }
                if (codecs == null || (codecs != null && codecs.indexOf("avc")!= -1)){
                    expectingStreamInfUrl = true;
                }
            } else if (!line.startsWith("#") && expectingStreamInfUrl) {
                if (name == null) {
                    name = Integer.toString(variants.size());
                }
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    variants.add(new MMQBRVariant(line, APPLICATION_M3U8, width, height, -1, bitrate, codecs));
                }else{
                    variants.add(new MMQBRVariant(baseUri + line, APPLICATION_M3U8, width, height, -1, bitrate, codecs));
                }
                bitrate = 0;
                codecs = null;
                name = null;
                width = -1;
                height = -1;
                expectingStreamInfUrl = false;
            }
        }
        return new MMQBRHlsMasterPlaylist(baseUri, variants);
    }
    public static final String APPLICATION_M3U8 =  "application/x-mpegURL";
    private MMQBRHlsMediaPlaylist parseMediaPlaylist(LineIterator iterator, String baseUri)
            throws IOException {
        int mediaSequence = 0;
        int targetDurationSecs = 0;
        int version = 1; // Default version == 1.
        boolean live = true;
        List<MMSegment> segments = new ArrayList<MMSegment>();

        double segmentDurationSecs = 0.0;
        int discontinuitySequenceNumber = 0;
        long segmentStartTimeUs = 0;
        int segmentByterangeOffset = 0;
        int segmentByterangeLength = C.LENGTH_UNBOUNDED;
        int segmentMediaSequence = 0;

        boolean isEncrypted = false;
        String encryptionKeyUri = null;
        String encryptionIV = null;
        boolean hasClientSideAdInsertion = false;
        String line;
        while (iterator.hasNext()) {
            line = iterator.next();
            if (line.startsWith(TARGET_DURATION_TAG)) {
                targetDurationSecs = parseIntAttr(line, TARGET_DURATION_REGEX,
                        TARGET_DURATION_TAG);
            } else if (line.startsWith(MEDIA_SEQUENCE_TAG)) {
                mediaSequence = parseIntAttr(line, MEDIA_SEQUENCE_REGEX, MEDIA_SEQUENCE_TAG);
                segmentMediaSequence = mediaSequence;
            } else if (line.startsWith(VERSION_TAG)) {
                version = parseIntAttr(line, VERSION_REGEX, VERSION_TAG);
            } else if (line.startsWith(MEDIA_DURATION_TAG)) {
                segmentDurationSecs = parseDoubleAttr(line, MEDIA_DURATION_REGEX,
                        MEDIA_DURATION_TAG);
            } else if (line.startsWith(KEY_TAG)) {
            } else if (line.startsWith(BYTERANGE_TAG)) {
                String byteRange = parseStringAttr(line, BYTERANGE_REGEX, BYTERANGE_TAG);
                String[] splitByteRange = byteRange.split("@");
                segmentByterangeLength = Integer.parseInt(splitByteRange[0]);
                if (splitByteRange.length > 1) {
                    segmentByterangeOffset = Integer.parseInt(splitByteRange[1]);
                }
            } else if (line.startsWith(DISCONTINUITY_SEQUENCE_TAG)) {
                discontinuitySequenceNumber = Integer.parseInt(line.substring(line.indexOf(':') + 1));
                hasClientSideAdInsertion = true;
            } else if (line.equals(DISCONTINUITY_TAG)) {
                discontinuitySequenceNumber++;
                hasClientSideAdInsertion = true;
            } else if (!line.startsWith("#")) {
                segmentMediaSequence++;
                if (segmentByterangeLength == C.LENGTH_UNBOUNDED) {
                    segmentByterangeOffset = 0;
                }
                segments.add(new MMSegment(line, segmentDurationSecs, discontinuitySequenceNumber,
                        segmentStartTimeUs, segmentByterangeOffset, segmentByterangeLength));
                segmentStartTimeUs += (long) (segmentDurationSecs * C.MICROS_PER_SECOND);
                segmentDurationSecs = 0.0;
                if (segmentByterangeLength != C.LENGTH_UNBOUNDED) {
                    segmentByterangeOffset += segmentByterangeLength;
                }
                segmentByterangeLength = C.LENGTH_UNBOUNDED;
            } else if (line.equals(ENDLIST_TAG)) {
                live = false;
                break;
            }
        }
        return new MMQBRHlsMediaPlaylist(baseUri, mediaSequence, targetDurationSecs, version, live,
                Collections.unmodifiableList(segments), hasClientSideAdInsertion);
    }

    public MMQFPresentationInfo parse(String manifestFileData, URL manifestURL)
    {
        if (masterPlaylist != null && allAuxResDownloadSuccess() && presentationInfo != null){
            return presentationInfo;
        }

        MMQFPresentationInfo retval = null;
        //TODO check if the manifest is valid
        //if it is master playlist with variant streams?
        //if it is media playlist?
        manifestFileData.trim();
        if (manifestFileData.startsWith("#EXTM3U")){
            BufferedReader reader = new BufferedReader(new StringReader(manifestFileData));
            Queue<String> extraLines = new LinkedList<String>();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        // Do nothing.
                    } else if (line.startsWith(STREAM_INF_TAG)) {
                        extraLines.add(line);
                        String manifestPath = manifestURL.getProtocol() + "://" + manifestURL.getHost() + manifestURL.getPath();
                        String path = manifestPath.substring(0, manifestPath.lastIndexOf('/') + 1);
                        masterPlaylist = parseMasterPlaylist(new LineIterator(extraLines, reader), path);
                        videoTracks = new MMQFPresentationVideoTrackInfo[masterPlaylist.variants.size()];
                        mediaPlaylists = new MMQBRHlsMediaPlaylist[masterPlaylist.variants.size()];
                        for (int i = 0; i< masterPlaylist.variants.size(); i++){
                            mediaPlaylists[i] = null;
                            MMQFPresentationVideoTrackInfo trackInfo = new MMQFPresentationVideoTrackInfo();
                            MMQBRVariant variant = masterPlaylist.variants.get(i);
                            trackInfo.baseURL = variant.url.substring(0, variant.url.lastIndexOf('/') + 1);
                            trackInfo.bitrate = variant.bitrate;
                            trackInfo.codecInfo = variant.codecs;
                            trackInfo.height = variant.height;
                            trackInfo.width = variant.width;
                            trackInfo.timeScale = 1000;
                            trackInfo.trackIndex = i;
                            trackInfo.trackSegmentInfoVect = new Vector<MMQFPresentationVideoTrackSegmentInfo>();
                            videoTracks[i] = trackInfo;
                        }
                    } else {
                        extraLines.add(line);
                    }
                }
            }
            catch(Exception e){
                Log.e(TAG, e.toString());
            }

            try{
                reader.close();
            }
            catch (Exception e){
                Log.e(TAG, e.toString());
            }
        }
        return retval;
    }

    public boolean needAuxiliaryResources(){
        return (masterPlaylist!=null && masterPlaylist.variants.size()>0) ?true:false;
    }

    public ArrayList<AuxResourceInformation> getAuxResourceInfo(){
        ArrayList<AuxResourceInformation> retval = null;
        if (needAuxiliaryResources()) {
            retval = new ArrayList<AuxResourceInformation>();
            for (int i = 0; i<masterPlaylist.variants.size(); i++){
                AuxResourceInformation auxInfo = new AuxResourceInformation();
                try {
                    auxInfo.url_ = new URL(masterPlaylist.variants.get(i).url);
                    retval.add(auxInfo);
                }
                catch (Exception e){
                    Log.e(TAG, "Exception while creating AUX resource" + e.toString());
                }
            }
        }
        return retval;
    }

    public void AdaptMediaPlaylistInfoToVideoTrack(MMQBRHlsMediaPlaylist mediaPlaylist, MMQFPresentationVideoTrackInfo videoTrackInfo){
        for (int i = 0; i<mediaPlaylist.segments.size(); i++){
            MMQFPresentationVideoTrackSegmentInfo info = new MMQFPresentationVideoTrackSegmentInfo();
            info.segmentURL = mediaPlaylist.segments.get(i).url;
            info.duration = (long)(mediaPlaylist.segments.get(i).durationSecs * 1000);
            info.segmentStartTime = (long)((mediaPlaylist.segments.get(i).startTimeUs) /1000);
            info.segmentSz = (int)(videoTrackInfo.bitrate * mediaPlaylist.segments.get(i).durationSecs);
            info.segmentIndex = i;
            videoTrackInfo.startSequenceNum = mediaPlaylist.mediaSequence;
            videoTrackInfo.trackSegmentInfoVect.add(info);
        }
        videoTrackInfo.hasClientSideAdInsertionInTrack = mediaPlaylist.hasClientSideAdInsertion;
    }

    public void SetAuxResource(int index, byte[] resource){
        MMQFPresentationInfo retval = null;
        //TODO check if the manifest is valid
        //if it is master playlist with variant streams?
        //if it is media playlist?
        InputStream is = new ByteArrayInputStream(resource);
        BufferedReader reader = new BufferedReader(new BufferedReader(new InputStreamReader(is)));
        Queue<String> extraLines = new LinkedList<String>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    // Do nothing.
                } else if (line.startsWith(STREAM_INF_TAG)) {
                    assert (false);
                } else if (line.startsWith(TARGET_DURATION_TAG)
                        || line.startsWith(MEDIA_SEQUENCE_TAG)
                        || line.startsWith(MEDIA_DURATION_TAG)
                        || line.startsWith(KEY_TAG)
                        || line.startsWith(BYTERANGE_TAG)
                        || line.equals(DISCONTINUITY_TAG)
                        || line.equals(DISCONTINUITY_SEQUENCE_TAG)
                        || line.equals(ENDLIST_TAG)) {
                    extraLines.add(line);
                    MMQBRHlsMediaPlaylist mediaPlaylist = parseMediaPlaylist(new LineIterator(extraLines, reader), masterPlaylist.variants.get(index).url);
                    mediaPlaylists[index] = mediaPlaylist;
                    if(mediaPlaylist.live) {
                        durationMillis = -1;
                    }else{
                        durationMillis = mediaPlaylist.durationUs / 1000;
                    }
                    //Adapt media presentation to VideoPresentation
                    AdaptMediaPlaylistInfoToVideoTrack(mediaPlaylist, videoTracks[index]);
                } else {
                    extraLines.add(line);
                }
            }
        }
        catch(Exception e){
            Log.e(TAG, e.toString());
        }

        try {
            reader.close();
        }
        catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

    public boolean allAuxResDownloadSuccess(){
        if (masterPlaylist == null) {
            return true;
        }
        for (int i = 0; i < masterPlaylist.variants.size(); i++) {
            if (mediaPlaylists[i] == null) {
                return false;
            }
        }


        presentationInfo = new MMQFPresentationInfo("HLS", -1, -1, (durationMillis > 0)?false:true, durationMillis, 1000);
        for (int i = 0; i< videoTracks.length; i++){
            if (videoTracks[i]!=null){
                presentationInfo.addVideoPresentationTrack(videoTracks[i].width, videoTracks[i].height, videoTracks[i].codecInfo, videoTracks[i]);
            }
        }
        return true;
    }

    public void AuxResRetrievalFailed(int index){}

    public final boolean hasClientSideAdInsertion = false;

    private MMQFPresentationVideoTrackInfo getVideoTrackForBitrate(int bitrate){
        MMQFPresentationVideoTrackInfo videoTrack = null;
        for (int i = 0; i< videoTracks.length; i++){
            if (videoTracks[i].bitrate == bitrate){
                videoTrack = videoTracks[i];
                break;
            }
        }
        return videoTrack;
    }

    @Override
    public ParserSegmentInfoForURL getSegmentInfoForSegment(int bitrate, long startTimeMilliSec) {
        ParserSegmentInfoForURL retval = null;
        MMQFPresentationVideoTrackInfo info = getVideoTrackForBitrate(bitrate);
        MMQFPresentationVideoTrackSegmentInfo segInfo = info.getSegmentInfoAtTime(startTimeMilliSec);
        if (segInfo!= null){
            retval = new ParserSegmentInfoForURL();
            retval.videoTrackInfo = info;
            retval.segmentIndex = segInfo.segmentIndex;
        }else{
        }
        return retval;
    }

    public ParserSegmentInfoForURL getSegmentInfoForURL(String url) {
        assert (false); //Not implemented for now, user should better use sequence numbers Or maybe the start time (if needed)
        return null;
    }

    private static class LineIterator {

        private final BufferedReader reader;
        private final Queue<String> extraLines;

        private String next;

        public LineIterator(Queue<String> extraLines, BufferedReader reader) {
            this.extraLines = extraLines;
            this.reader = reader;
        }

        public boolean hasNext() throws IOException {
            if (next != null) {
                return true;
            }
            if (!extraLines.isEmpty()) {
                next = extraLines.poll();
                return true;
            }
            while ((next = reader.readLine()) != null) {
                next = next.trim();
                if (!next.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        public String next() throws IOException {
            String result = null;
            if (hasNext()) {
                result = next;
                next = null;
            }
            return result;
        }
    }

    MMQBRHlsMasterPlaylist masterPlaylist;
    MMQBRHlsMediaPlaylist[] mediaPlaylists;
    MMQFPresentationVideoTrackInfo[] videoTracks;
    // ArrayList<MMQBRHlsMediaPlaylist> mediaPlaylists;

    MMQFPresentationInfo presentationInfo = null;
    //  ArrayList<MMQFPresentationVideoTrackInfo> videoTracks;
    long durationMillis = 0;
    //UTIL Functions
    private static final String BOOLEAN_YES = "YES";
    private static final String BOOLEAN_NO = "NO";

    public static String parseStringAttr(String line, Pattern pattern, String tag)
            throws MMParserException {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        }
        throw new MMParserException("Couldn't match " + tag + " tag in " + line);
    }

    public boolean isSupportedQBRPresentation(){
        if (videoTracks != null && videoTracks.length >1 ) {
            return true;
        }
        return false;
    }

    public static int parseIntAttr(String line, Pattern pattern, String tag)
            throws MMParserException {
        return Integer.parseInt(parseStringAttr(line, pattern, tag));
    }

    public static double parseDoubleAttr(String line, Pattern pattern, String tag)
            throws MMParserException {
        return Double.parseDouble(parseStringAttr(line, pattern, tag));
    }

    public static String parseOptionalStringAttr(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean parseOptionalBooleanAttr(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return BOOLEAN_YES.equals(matcher.group(1));
        }
        return false;
    }

    public static Pattern compileBooleanAttrPattern(String attrName) {
        return Pattern.compile(attrName + "=(" + BOOLEAN_YES + "|" + BOOLEAN_NO + ")");
    }
}
