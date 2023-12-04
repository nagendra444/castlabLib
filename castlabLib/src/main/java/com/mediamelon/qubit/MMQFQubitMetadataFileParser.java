package com.mediamelon.qubit;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Rupesh on 19-03-2015.
 */
public class MMQFQubitMetadataFileParser implements MMQFQubitResourceDownloader_HTTP.OnQubitResourceDownloadedListener{
    MMQFQubitMetadataFileParser()
    {

    }
    public interface OnQubitMetadataFileParsedListener {
        public abstract void onQubitMetadataFileParsed(MMQFQubitStatusCode status);
    }

    public void setOnQubitMetadataFileParsedListener(OnQubitMetadataFileParsedListener list)
    {
        listener = list;
    }
    public ArrayList<VideoTrackMediaAttributes> getVideoTracksMetaDataArrayList() {
        return videoTracksMetadataCtrArrayList;
    }

    public void retrieveAndParseMetadataFile(URL metadtaFileURL)
    {
        metadataFileDownloader = new MMQFQubitResourceDownloader_HTTP();
        MMQFResourceInfo info = new MMQFResourceInfo();
        info.resourceURL_ = metadtaFileURL;
        metadataFileDownloader.execute(info);
        metadataFileDownloader.setOnQubitResourceDownloadedListener(this);
        synchronized (TAG){
            currCommandId++;
            commandId = currCommandId;
            requestInProgress = true;
        }
    }

    public CommonMetadata getCommonMetadata()
    {
        return commonMetadata;
    }

    public VideoTrackMediaAttributes getVideoTrackMediaAttributesForTrack(int width, int height, int bitrate, int index)
    {
        if(index < videoTracksMetadataCtrArrayList.size())
        {
            return (VideoTrackMediaAttributes)(videoTracksMetadataCtrArrayList.get(index));
        }
        return null;
    }

    public void OnQubitResourceDownloaded(MMQFQubitStatusCode status, byte[] downloadedMediaBytesEncrypted, String contentType)
    {
        if (currCommandId != commandId || requestInProgress == false){
            MMLogger.d(TAG, "Cancelling the meta loading...");
            listener.onQubitMetadataFileParsed(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFCancelled));
            return;
        }
        if( (downloadedMediaBytesEncrypted!=null) && status.status() == MMQFQubitStatusCode.MMQFSuccess)
        {
            String downloadedMedia = null;
            if(downloadedMediaBytesEncrypted.length > 10){
                byte [] tmpBytes = new byte[10];
                for(int i =0; i< 10; i++){
                    tmpBytes[i] = downloadedMediaBytesEncrypted[i];
                }
                String tmpString = new String(tmpBytes);
                if(tmpString.indexOf("xml")<0){
                    //AES encrypted, lets decrypt it -
                    byte key[] = {0x12,0x34,0x56,0x78,0x12,0x34,0x56,0x78, 0,0,0,0,0,0,0,0};
                    byte ivBytes[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
                    SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
                    byte[] downloadedMediaBytes = downloadedMediaBytesEncrypted;
                    try {
                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
   					    cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                        downloadedMediaBytes = cipher.doFinal(downloadedMediaBytesEncrypted);
						downloadedMedia = new String(downloadedMediaBytes);
                    }
                    catch (InvalidAlgorithmParameterException e) {
						MMLogger.e(TAG, "Decryption of metadata failed - InvalidAlgorithmParameterException");
					}
                    catch(NoSuchAlgorithmException algo){
                        MMLogger.e(TAG, "Decryption of metadata failed - NoSuchAlgorithmException");
                    }
                    catch(NoSuchPaddingException padding){
                        MMLogger.e(TAG, "Decryption of metadata failed - NoSuchPaddingException");
                    }
                    catch (InvalidKeyException invalidKey){
                        MMLogger.e(TAG, "Decryption of metadata failed - InvalidKeyException");
                    }
                    catch(IllegalBlockSizeException illegalBlkSz){
                        MMLogger.e(TAG, "Decryption of metadata failed - IllegalBlockSizeException");
                    }
                    catch(BadPaddingException badPadding){
                        MMLogger.e(TAG, "Decryption of metadata failed - BadPaddingException");
                    }
                    
                }
                else{
					MMLogger.e(TAG, "XMLDEbug:Detected clear metadata");
                    downloadedMedia = new String(downloadedMediaBytesEncrypted);
                }
            }

            if(downloadedMedia != null)
            {
	            metadataToParse = downloadedMedia;
	            try {
	                commonMetadata = new CommonMetadata();
	                videoTracksMetadataCtrArrayList = new ArrayList(10);
	                parseMetadata();
	                commonMetadata.noOfVideoTracks = videoTracksMetadataCtrArrayList.size();
	                commonMetadata.printCommonMetadata();
	                for(int i =0; i<videoTracksMetadataCtrArrayList.size(); i++)
	                {
	                    VideoTrackMediaAttributes videoTrackAttr = (VideoTrackMediaAttributes)videoTracksMetadataCtrArrayList.get(i);
	                    //videoTrackAttr.printDescription();
	                }
	                if(listener != null) {
	                    listener.onQubitMetadataFileParsed(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess));
	                }
	            }
	            catch(XmlPullParserException e)
	            {
	                MMLogger.e(TAG, "Exception - XmlPullParserException while parsing xml");
	                e.printStackTrace();
                    listener.onQubitMetadataFileParsed(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFMetaFileMalformed));
	            }
	            catch (IOException e)
	            {
	                MMLogger.e(TAG, "Exception - IOException while parsing xml");
	                e.printStackTrace();
                    listener.onQubitMetadataFileParsed(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFMetaFileMalformed));
	            }
            }else{
				if(listener != null) {
                	listener.onQubitMetadataFileParsed(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFMetaFileMalformed));
            	}
			}
        }
        else
        {
            if(listener != null) {
                listener.onQubitMetadataFileParsed(new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFMetaFileNotReachable));
            }
        }
    }

    public void parseMetadata() throws XmlPullParserException, IOException
    {
        //MMLogger.i(TAG, "Entering the parse metadata ...");
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        metadataToParse = metadataToParse.replace("<?xpacket end=\"r\"?>", "");//Workaround for last unwanted tag in xml <?..
        xpp.setInput(new StringReader (metadataToParse));
        int eventType = xpp.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_DOCUMENT) {
                MMLogger.i(TAG, "Start document");
            } else if(eventType == XmlPullParser.START_TAG) {
                String elementName = xpp.getName();
                if(setRDFStreamDescriptionElementStopPending(xpp, elementName) ||
                        setEqvsStreamArrayElementStopPending(xpp, elementName) ||
                        setEQVSACPEntriesStopPending(xpp, elementName) ||
                        setEQVSCBCEntriesStopPending(xpp, elementName) ||
                        setEQVSDSREntriesStopPending(xpp, elementName) ||
                        setEQVSIMOSEntriesStopPending(xpp, elementName) ||
                        setEQVSXResSwitchEntriesStopPending(xpp, elementName))
                {
                    //MMLogger.i(TAG, "processed the element - " + elementName );
                }

                parseDescription(xpp, elementName);
                parseTrackInfo(xpp, elementName);
            } else if(eventType == XmlPullParser.END_TAG) {
                String elementName = xpp.getName();
                if(resetRDFStreamDescriptionElementStopPending(xpp, elementName) ||
                resetEqvsStreamArrayElementStopPending(xpp, elementName) ||
                resetEQVSACPEntriesStopPending(xpp, elementName) ||
                resetEQVSCBCEntriesStopPending(xpp, elementName) ||
                resetEQVSDSREntriesStopPending(xpp, elementName) ||
                resetEQVSIMOSEntriesStopPending(xpp, elementName) ||
                resetEQVSXResEntriesStopPending(xpp, elementName))
                {
                    //MMLogger.i(TAG, "resetted the element - " + elementName);
                }
            }
            eventType = xpp.next();
            if(eventType == XmlPullParser.END_DOCUMENT) {
                //MMLogger.i(TAG, "End document");
                if(videoTrackMediaAttributes != null) {
                    //MMLogger.i(TAG, "Pushing the last element");
                    videoTracksMetadataCtrArrayList.add(videoTrackMediaAttributes);
                    videoTrackMediaAttributes = null;
                }
            }
        }
    }

    private void parseTrackInfo(XmlPullParser parser, String elementName)
    {
        if(eqvsStreamArrayElementStopPending && rdfStreamDescriptionElementStopPending)
        {
            parseStreamDescription(parser, elementName);
            if(elementName.equals(KRdfStreamMetadataKVP) && parser.getPrefix().equals(KRdfNameSpace)) {
                parseDSREntries(parser, elementName);
                parseCBCEntries(parser, elementName);
                parseACPEntries(parser, elementName);
                parseIMOSEntries(parser, elementName);
                parseXResolutionFeasibleSwitches(parser, elementName);
            }
        }
    }
/*
    TO-DO implement the function to return correct values based on Hintfile values;
 */
    public String getVideoCodec() {
        return "avc1.64";
    }
/*
    TO-DO implement the function to return correct values based on Hintfile values;
 */
    public String getAudioCodec() {
        return "mp4a.40.2";
    }

    private Boolean parseStreamDescription(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KRdfDescriptionElement) && parser.getPrefix().equals(KRdfNameSpace) && rdfStreamDescriptionElementStopPending ) {
            if(videoTrackMediaAttributes != null)
            {
                videoTracksMetadataCtrArrayList.add(videoTrackMediaAttributes);
                //MMLogger.i(TAG, "Adding the track with name - " + videoTrackMediaAttributes.streamName);
                videoTrackMediaAttributes = null;
            }
            videoTrackMediaAttributes = new VideoTrackMediaAttributes();
            int noOfAttributes = parser.getAttributeCount();
            for(int i = 0; i < noOfAttributes; i++)
            {
                if(parser.getAttributePrefix(i).equals(KEqvsNameSpace)) {
                    if(parser.getAttributeName(i).equals(KEqvsStreamDescription_Name)){
                        videoTrackMediaAttributes.streamName = parser.getAttributeValue(i);
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsStreamDescription_Bitrate)){
                        videoTrackMediaAttributes.averageBitRate = Integer.parseInt(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsStreamDescription_AvgiMOS)){
                        videoTrackMediaAttributes.averageiMOS = Integer.parseInt(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsStreamDescription_Resolution)){
                        String resolution = parser.getAttributeValue(i);
                        resolution = resolution.toLowerCase();
                        int xIndex = resolution.indexOf('x');
                        if(xIndex != -1)
                        {
                            videoTrackMediaAttributes.displayWidth = Integer.parseInt(resolution.substring(0, xIndex));
                            videoTrackMediaAttributes.displayHeight = Integer.parseInt(resolution.substring(xIndex+1));
                        }
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_FrameRate)) {
                        videoTrackMediaAttributes.frameRate = Double.parseDouble(parser.getAttributeValue(i));
                    }
                }
            }
            return true;
        }
        return false;
    }

    private SparseEntry parseSparseEntry(XmlPullParser parser, String elementName)
    {
        SparseEntry entry = null;
        try
        {
            String kvp = parser.nextText();
            String[] arr = kvp.split(",");
            entry = new SparseEntry();
            entry.seqNo = Long.parseLong(arr[0]);
            entry.value = Long.parseLong(arr[1]);
        }
        catch(XmlPullParserException e)
        {
            e.printStackTrace();
            MMLogger.e(TAG, "XmlPullParserException while parsing sparse entry");
        }
        catch(IOException f)
        {
            f.printStackTrace();
            MMLogger.e(TAG, "IOException while parsing sparse entry");
        }
        return entry;
    }

    private void parseDSREntries(XmlPullParser parser, String elementName)
    {
        if(eqvsDSREntriesStopPending) {
            SparseEntry entry = parseSparseEntry(parser, elementName);
            videoTrackMediaAttributes.dsrValues.add(entry);
        }
    }

    private void parseCBCEntries(XmlPullParser parser, String elementName)
    {
        if(eqvsCBCEntriesStopPending) {
            SparseEntry entry = parseSparseEntry(parser, elementName);
            videoTrackMediaAttributes.cbcEntries.add(entry);
        }
    }

    private void parseACPEntries(XmlPullParser parser, String elementName)
    {
        if(eqvsACPEntriesStopPending) {
            SparseEntry entry = parseSparseEntry(parser, elementName);
            videoTrackMediaAttributes.acpEntries.add(entry);
        }
    }

    private void parseXResolutionFeasibleSwitches(XmlPullParser parser, String elementName)
    {
        if(eqvsXResolutionFeasibleSwitchesStopPending) {
            try {
                String xResSwitchesStr = parser.nextText();
                for (int i = 0; i < xResSwitchesStr.length(); i++) {
                    if(xResSwitchesStr.charAt(i) >= 'a' && xResSwitchesStr.charAt(i) <= 'z'){
                        videoTrackMediaAttributes.xResSwitches.add(xResSwitchesStr.charAt(i) - 'a');
                    }else{
                        videoTrackMediaAttributes.xResSwitches.add(-1);
                    }
                }
            }
            catch (XmlPullParserException e)
            {
                e.printStackTrace();
                MMLogger.e(TAG, "XmlPullParserException while parsing xResSwitch entries");
            }
            catch(IOException f)
            {
                f.printStackTrace();
                MMLogger.e(TAG, "IOException while parsing xResSwitch entries");
            }
        }
    }


    private void parseIMOSEntries(XmlPullParser parser, String elementName)
    {
        if(eqvsIMOSEntriesStopPending) {
            try {
                String xResSwitchesStr = parser.nextText();
                String[] arr = xResSwitchesStr.split(",");
                long startSeqNo = Long.parseLong(arr[0]);
                assert(videoTrackMediaAttributes.imosValues.size() == startSeqNo);
                String imosStr = arr[1];

                for (int i = 0; i < imosStr.length(); i++) {
                    double code = (double)GetImosScore(imosStr.charAt(i));
                    videoTrackMediaAttributes.imosValues.add(code);
                }
            }
            catch (XmlPullParserException e)
            {
                e.printStackTrace();
                MMLogger.e(TAG, "XmlPullParserException while parsing imos entries");
            }
            catch(IOException f)
            {
                f.printStackTrace();
                MMLogger.e(TAG, "IOException while parsing imos entries");
            }
        }
    }

    private int GetImosScore(int imosCharASCIICode)
    {
        int asciiCodeOfA = 'A';
        int asciiCodeOfa = 'a';
        int asciiCodeOfZ = 'Z';
        int asciiCodeOfz = 'z';

        //validate the range
        if((imosCharASCIICode >= asciiCodeOfA) && (imosCharASCIICode<= asciiCodeOfZ))
        {
            return imosCharASCIICode - asciiCodeOfA;
        }
        else if((imosCharASCIICode >= asciiCodeOfa) && (imosCharASCIICode<= asciiCodeOfz))
        {
            return imosCharASCIICode - asciiCodeOfa + 26;
        }
        return -1;
    }

    private Boolean setEqvsStreamArrayElementStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStreamArray) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsStreamArrayElementStopPending = true;
            return true;
        }
        return false;
    }

    private Boolean setEQVSDSREntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_DSR) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsDSREntriesStopPending = true;
            return true;
        }
        return false;
    }

    private Boolean setEQVSCBCEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_CBC) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsCBCEntriesStopPending = true;
            return true;
        }
        return false;
    }

    Boolean setEQVSACPEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_ACP) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsACPEntriesStopPending = true;
            return true;
        }
        return false;
    }


    private Boolean setEQVSIMOSEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_iMOS) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsIMOSEntriesStopPending = true;
            return true;
        }
        return false;
    }

    private Boolean setEQVSXResSwitchEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_XRes) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsXResolutionFeasibleSwitchesStopPending = true;
            return true;
        }
        return false;
    }

    private Boolean setRDFStreamDescriptionElementStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KRdfDescriptionElement) && parser.getPrefix().equals(KRdfNameSpace)) {
            rdfStreamDescriptionElementStopPending = true;
            return true;
        }
        return false;
    }

    private Boolean resetEqvsStreamArrayElementStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStreamArray) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsStreamArrayElementStopPending = false;
            return true;
        }
        return false;
    }

    private Boolean resetEQVSDSREntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_DSR) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsDSREntriesStopPending = false;
            return true;
        }
        return false;
    }

    private Boolean resetEQVSCBCEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_CBC) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsCBCEntriesStopPending = false;
            return true;
        }
        return false;
    }

    private Boolean resetEQVSACPEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_ACP) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsACPEntriesStopPending = false;
            return true;
        }
        return false;
    }

    private Boolean resetEQVSIMOSEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_iMOS) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsIMOSEntriesStopPending = false;
            return true;
        }
        return false;
    }

    private Boolean resetEQVSXResEntriesStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KEqvsStream_XRes) && parser.getPrefix().equals(KEqvsNameSpace)) {
            eqvsXResolutionFeasibleSwitchesStopPending = false;
            return true;
        }
        return false;
    }

    private Boolean resetRDFStreamDescriptionElementStopPending(XmlPullParser parser, String elementName)
    {
        if(elementName.equals(KRdfDescriptionElement) && parser.getPrefix().equals(KRdfNameSpace)) {
            rdfStreamDescriptionElementStopPending = false;
            return true;
        }
        return false;
    }

    private void parseDescription(XmlPullParser parser, String elementName)
    {
        if(parser.getPrefix().equals(KRdfNameSpace) && elementName.equals(KRdfDescriptionElement) && (eqvsStreamArrayElementStopPending == false) )
        {
            //Parse the description for the common metadata
            int noOfAttributes = parser.getAttributeCount();
            for(int i = 0; i < noOfAttributes; i++)
            {
                if(parser.getAttributePrefix(i).equals(KEqvsNameSpace))
                {
                    if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_Type))
                    {
                        commonMetadata.type = parser.getAttributeValue(i).equals(KEqvsDescriptionAttribValue_TypeVBR)?ContentType.VariableBitRate:ContentType.ConstantBitRate;
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_FrameRate))
                    {
                        commonMetadata.frameRate = Double.parseDouble(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_Version))
                    {
                        commonMetadata.version = Integer.parseInt(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_NoOfSegments))
                    {
                        commonMetadata.noOfSegments = Integer.parseInt(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_FramesPerSegment))
                    {
                        commonMetadata.framesPerSegment = Integer.parseInt(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_ACPStartUpDelay))
                    {
                        commonMetadata.acpStartUpDelay = Double.parseDouble(parser.getAttributeValue(i));
                    }
                    else if(parser.getAttributeName(i).equals(KEqvsDescriptionAttrib_RateErrorFactor))
                    {
                        commonMetadata.rateErrorFactor = Double.parseDouble(parser.getAttributeValue(i));
                    }
                }
            }
        }
    }

    public enum ContentType{
        ConstantBitRate,
        VariableBitRate
    }

    public class CommonMetadata{
        public ContentType type;
        public double frameRate;
        public int version;
        //TODO: Add the resolution tag
        public int noOfSegments;
        public int framesPerSegment;
        public double acpStartUpDelay;
        public double rateErrorFactor;
        public int noOfVideoTracks;
        public void printCommonMetadata()
        {
            MMLogger.d(TAG, "type : " + type + " framerate : " + frameRate + " noOfSegments : " + noOfSegments + " framesPerSegment : " + framesPerSegment + " ACP Startup Delay : " + acpStartUpDelay + " Rate Error Factor : " + rateErrorFactor + " VideoTrackCnt : " + noOfVideoTracks);
        }
        private String TAG = "MMQFCommonMetadata";
    }

    class SparseEntry{
        long seqNo;
        long value;
    }

    class VideoTrackMediaAttributes{
        VideoTrackMediaAttributes()
        {
            dsrValues = new ArrayList<SparseEntry>(5);
            cbcEntries = new ArrayList<SparseEntry>(5);
            acpEntries = new ArrayList<SparseEntry>(5);
            imosValues = new ArrayList<Double>(5);
            xResSwitches = new ArrayList<Integer>(5);
        }

        public void printDescription()
        {
            MMLogger.d(TAG, "streamName " + streamName + " avgBitRate " + averageBitRate + " averageiMOS " + averageiMOS);
            int i = 0;
            for(i =0 ; i < dsrValues.size(); i++)
            {
                SparseEntry entry = (SparseEntry)dsrValues.get(i);
                MMLogger.d(TAG, "seqNo " + entry.seqNo + " value " + entry.value);
            }

            for(i =0 ; i < cbcEntries.size(); i++)
            {
                SparseEntry entry = (SparseEntry)cbcEntries.get(i);
                MMLogger.d(TAG, "seqNo " + entry.seqNo + " value " + entry.value);
            }

            for(i =0 ; i < acpEntries.size(); i++)
            {
                SparseEntry entry = (SparseEntry)acpEntries.get(i);
                MMLogger.d(TAG, "seqNo " + entry.seqNo + " value " + entry.value);
            }

            for(i =0 ; i < imosValues.size(); i++)
            {
                Double entry = (Double)imosValues.get(i);
                MMLogger.d(TAG, "seqNo " + i + " value " + entry.intValue());
            }
        }

        public String TAG = "VideoTrackMediaAttributes";
        public String streamName = null;
        public int averageBitRate = 0;
        public int averageiMOS = 0;
        public double frameRate = -1;

        public int displayWidth = -1;
        public int displayHeight = -1;
        public ArrayList<SparseEntry> dsrValues = null; //Vect of sparseEntries
        public ArrayList<SparseEntry> cbcEntries = null; //Vect of sparseEntries
        public ArrayList<SparseEntry> acpEntries = null; //Vect of acpEntries
        public ArrayList<Double> imosValues = null; // each element is iMOS score of the segment
        public ArrayList<Integer> xResSwitches = null;
    }

    public void CancelPendingRequests(){
        requestInProgress = false;
        if (metadataFileDownloader != null){
            metadataFileDownloader.cancel(true);
        }
    }
    private static final String TAG = "MMQFMetadataParser";
    private MMQFQubitResourceDownloader_HTTP metadataFileDownloader = null;
    private String metadataToParse = null;
    private CommonMetadata commonMetadata = null;
    private VideoTrackMediaAttributes videoTrackMediaAttributes = null;
    private ArrayList videoTracksMetadataCtrArrayList;
    private Boolean parseTrackCharacteristics;
    private OnQubitMetadataFileParsedListener listener = null;

    private Boolean eqvsStreamArrayElementStopPending = false;
    private Boolean rdfStreamDescriptionElementStopPending = false;
    private Boolean eqvsDSREntriesStopPending = false;
    private Boolean eqvsCBCEntriesStopPending = false;
    private Boolean eqvsACPEntriesStopPending = false;
    private Boolean eqvsIMOSEntriesStopPending = false;
    private Boolean eqvsXResolutionFeasibleSwitchesStopPending = false;

    private static final String KEqvsNameSpace = "eqvs";
    private static final String KRdfNameSpace = "rdf";
    private static final String KRdfDescriptionElement = "Description";
    private static final String KEqvsDescriptionAttrib_Type = "type";
    private static final String KEqvsDescriptionAttrib_FrameRate = "frame_rate";
    private static final String KEqvsDescriptionAttrib_Version = "ver";
    private static final String KEqvsDescriptionAttrib_NoOfSegments = "number_of_gops";
    private static final String KEqvsDescriptionAttrib_FramesPerSegment = "frames_per_gop";
    private static final String KEqvsDescriptionAttrib_ACPStartUpDelay = "acp_sud";
    private static final String KEqvsDescriptionAttrib_RateErrorFactor = "rate_error_factor";
    private static final String KEqvsDescriptionAttribValue_TypeVBR = "vbr";
    private static final String KEqvsDescriptionAttribValue_TypeCBR = "cbr";

    private static final String KEqvsStreamArray = "stream_array";
    private static final String KEqvsStreamDescription_Name = "stream_name";
    private static final String KEqvsStreamDescription_Bitrate = "av_bit_rate";
    private static final String KEqvsStreamDescription_AvgiMOS = "av_imos";
    private static final String KEqvsStreamDescription_Resolution = "resolution";

    private static final String KEqvsStream_DSR = "dsr";
    private static final String KEqvsStream_CBC = "cbc";
    private static final String KEqvsStream_ACP = "acp";
    private static final String KEqvsStream_iMOS = "imos";
    private static final String KEqvsStream_XRes = "res_switchable";

    private static final String KRdfStreamMetadata = "Seq";
    private static final String KRdfStreamMetadataKVP = "li";

    private static Integer currCommandId = 0;
    private static  boolean requestInProgress = false;
    private Integer commandId = 0;
}
