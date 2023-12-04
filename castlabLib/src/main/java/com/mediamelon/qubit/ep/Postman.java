package com.mediamelon.qubit.ep;


import android.util.Log;

import com.mediamelon.qubit.MMLogger;
import com.mediamelon.smartstreaming.MMCellInfo;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import static com.mediamelon.qubit.ep.Postman.PostmanState.PostmanState_Flushed;
import static com.mediamelon.qubit.ep.Postman.PostmanState.PostmanState_Started;
import static com.mediamelon.qubit.ep.Postman.PostmanState.PostmanState_Stopped;


public class Postman implements Runnable {
    enum PostmanState{
        PostmanState_Prepared, //Registration got completed, and it is possible for the postman to send posts
        PostmanState_Started,  //Registration got completed, SDK initialized, and Presentation info finalized. Can post any message
        PostmanState_Flushed, //Can process message in flushQ only
        PostmanState_Stopped
    }

    class MMMetricsBundle{
        RecordStructure record;
        boolean vitalMessage;
    }
    static String TAG = "Postman";
    boolean sendEvents = false;
    String qbrMode = null;
    Double totalDuration = null;
    Boolean isLiveSession = null;

    Double maxFPS = 0.0;
    Double minFPS = 0.0;
    String maxResStr = "Unknown";
    String minResStr = "Unknown";
    String streamFormat = "Unknown";
    Integer profileCnt = -1;

    boolean shutdownEventor = false;

    boolean mediaMelonQOE = false;

    Integer statInterval = 0;
    long onLoadSentAtTS = 0L;
    URL producerUrl;
    URL flushRecordURL;

    static Postman postman = null;
    PostmanState state = PostmanState.PostmanState_Stopped;
    Semaphore sem = new Semaphore(1);
    ArrayList<MMMetricsBundle> immediateRecords;//Events that can be queued in Stopped and Prepared state, and pushed out in Prepared state (Load, Error)
    ArrayList<MMMetricsBundle> eventRecords;    //Events that can be queued and posted in started state only
    ArrayList<MMMetricsBundle> flushQ;    //Events belonging to previous session, that should be sent out ASAP

    static public Postman instance(){
        if(postman == null){
            postman = new Postman();
        }
        return postman;
    }

    public void SetQBRMode(String mode){
        qbrMode = mode;
    }

    public void SetSessionInfo(Long durationMS, Boolean live){
        isLiveSession = live;
        if(durationMS > 0) {
            totalDuration = new Double(((double) durationMS / 1000));
        }else{
            totalDuration = -1.0;
        }
    }

    public void SetStreamInfo(String aStreamFormat,  int aProfileCount, String aMinRes, String aMaxRes, Double aMinFPS, Double aMaxFPS){
        maxFPS = aMaxFPS;
        minFPS = aMinFPS;
        maxResStr = aMaxRes;
        minResStr = aMinRes;
        streamFormat = aStreamFormat;
        profileCnt = aProfileCount;
    }

    private Postman() {
        immediateRecords = new ArrayList<MMMetricsBundle>();
        eventRecords = new ArrayList<MMMetricsBundle>();
        flushQ = new ArrayList<MMMetricsBundle>();
        try {
            sem.acquire();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    boolean isHiPriEvent(RecordStructure record){
        //isEvent record and is of kind Error or OnLoad
        if (isEventRecord(record)){
            if ( (record.qubitData.get(0).pbEventInfo.event == "ONLOAD") ||
                    (record.qubitData.get(0).pbEventInfo.event == "ERROR")){
                return true;
            }
        }else{
            return false;
        }
        return false;
    }

    boolean isEventRecord(RecordStructure record){
        if (record.qubitData!= null &&
                record.qubitData.get(0) != null &&
                record.qubitData.get(0).pbEventInfo!= null &&
                record.qubitData.get(0).pbEventInfo.event != null){
//            MMLogger.v(TAG, " POSTMAN isEventRecord " + record.timestamp + " " + record.qubitData.get(0).pbEventInfo.event);
            return true;
        }else{
//            MMLogger.v(TAG, " POSTMAN isStatsRecord " + record.timestamp);
            return false;
        }
    }
    public void closeMediaMelonSDK(){
        mediaMelonQOE = true;
    }
    public void startMediaMelonSDK() {mediaMelonQOE = false;}

    public void Queue(RecordStructure inEvent){
        if(!mediaMelonQOE) Queue(inEvent, true);
    }

    public void Queue(RecordStructure inEvent, boolean vitalMessage){
        synchronized (TAG) {
            boolean wasEmpty = immediateRecords.size() == 0 && eventRecords.size() == 0;
            //Log.i("ONLOAD","ONLOAD----------");
            RecordStructure event = CloneRecord(inEvent);
            MMMetricsBundle bundle = new MMMetricsBundle();
            bundle.record = event;
            bundle.vitalMessage = vitalMessage;
            if(isHiPriEvent(event)) {
                immediateRecords.add(bundle);
            }else{
                eventRecords.add(bundle);
                if(isEventRecord(event) == false){
                    SDKExperienceProbe.getInstance().setLastQBRStatsSentTs(System.currentTimeMillis());
                }
            }
            if (event.qubitData.get(0).pbEventInfo != null){
                MMLogger.v(TAG, "Queued events record "+ event.qubitData.get(0).pbEventInfo.event + " id => " + event.timestamp);
            }else{
                MMLogger.v(TAG, "Queued stats record id => "+ event.timestamp);
            }
            if (wasEmpty){
                sem.release();
            }
        }
    }


    public String getChecksum(Long custId, String  sessionId, Long  timestamp){
        String  encryptedKey = SDKExperienceProbe.getInstance().getHashed(Long.toString(custId) + sessionId + Long.toString(timestamp));
        return (encryptedKey.substring(8,16).concat(encryptedKey.substring(24,32)).concat(encryptedKey.substring(0,8)).concat(encryptedKey.substring(16,24)));
    }


    public void decorateRecord(RecordStructure record){
        record.timestamp += SDKExperienceProbe.getInstance().serverlocalClockDiff();

        if(isEventRecord(record)) {
            record.interval = statInterval;
            if (record.qubitData.get(0).pbEventInfo.event == "ONLOAD") {
                onLoadSentAtTS = record.timestamp; // Store TimeStamp of Onload
            }

            if ((onLoadSentAtTS > 0) && (record.qubitData.get(0).pbEventInfo.event == "START" || record.qubitData.get(0).pbEventInfo.event == "START_AFTER_AD")) {
                record.timestamp = onLoadSentAtTS; // TimeStamp of Onload and Start should be same.
                //onLoadSentAtTS = 0L;
            }

            if(record.qubitData.get(0).pbEventInfo.event.startsWith("AD_")) {
                record.qubitData.get(0).streamID.mode = null;  // Skip Mode for Ads Events
            }else{
                record.qubitData.get(0).streamID.mode = qbrMode;
            }
        }
        else {
            record.qubitData.get(0).streamID.mode = qbrMode;
        }



        record.qubitData.get(0).streamID.playerName = SDKExperienceProbe.getInstance().getPlayerName();
        record.qubitData.get(0).streamInfo.totalDuration = totalDuration;
        record.qubitData.get(0).streamID.isLive = isLiveSession;

        record.qubitData.get(0).streamInfo.maxFps = maxFPS;
        record.qubitData.get(0).streamInfo.minFps = minFPS;
        record.qubitData.get(0).streamInfo.maxRes = maxResStr;
        record.qubitData.get(0).streamInfo.minRes = minResStr;
        record.qubitData.get(0).streamInfo.streamFormat = streamFormat;
        record.qubitData.get(0).streamInfo.numOfProfile = profileCnt;

        for (int i = 0; i < record.qubitData.get(0).pbInfo.size(); i++) {
            record.qubitData.get(0).pbInfo.get(i).timestamp += SDKExperienceProbe.getInstance().serverlocalClockDiff();
        }
        record.qubitData.get(0).streamID.pId = getChecksum(record.qubitData.get(0).streamID.custId, record.qubitData.get(0).streamID.sessionId, record.timestamp);

    }

    URL postalAddress = null;
    RecordStructure getRecordToPost(){
        synchronized (TAG) {
            RecordStructure record = null;
            boolean isRecordFromFlushQ = false;
            postalAddress = producerUrl;
            Log.i(TAG,"PostalAddress/Producer = " + postalAddress);
            if (state == PostmanState.PostmanState_Flushed) {
                if(flushQ.size()>0){
                    isRecordFromFlushQ = true;
                    record = flushQ.remove(0).record;
                }
            }else if (state == PostmanState.PostmanState_Prepared) {
                if(flushQ.size()>0){
                    isRecordFromFlushQ = true;
                    record = flushQ.remove(0).record;
                }else if (immediateRecords.size() > 0) {
                    record = immediateRecords.remove(0).record;
                }
            } else if (state == PostmanState.PostmanState_Started) {
                if(flushQ.size()>0){
                    record = flushQ.remove(0).record;
                    isRecordFromFlushQ = true;
                }else if (immediateRecords.size() > 0) {
                    record = immediateRecords.remove(0).record;
                } else if (eventRecords.size() > 0) {
                    record = eventRecords.remove(0).record;
                }
            }

            if (record != null) {
                if(isRecordFromFlushQ == false) {
                    decorateRecord(record);
                }else{
                    postalAddress = flushRecordURL;
                }
                if (record.qubitData.get(0).pbEventInfo != null){
                    MMLogger.v(TAG, "Pushing out events record "+ record.qubitData.get(0).pbEventInfo.event + " id => " + (record.timestamp - SDKExperienceProbe.getInstance().serverlocalClockDiff()));
                }else{
                    MMLogger.v(TAG, "Pushing out stats record id => "+ (record.timestamp - SDKExperienceProbe.getInstance().serverlocalClockDiff()));
                }
            }


            return record;
        }
    }

    void Prepare(){
        if (state == PostmanState_Stopped || state == PostmanState_Flushed){
            state = PostmanState.PostmanState_Prepared;
            sem.release();
        }
    }

    void Start(){
        if(state != PostmanState_Started) {
            state = PostmanState.PostmanState_Started;
            sem.release();
        }
    }

    public boolean isStarted(){
        return (state == PostmanState_Started);
    }

    void Clear(){
        synchronized (TAG) {
            immediateRecords.clear();
            eventRecords.clear();
            qbrMode = null;
            totalDuration = null;
            isLiveSession = null;

            maxFPS = 0.0;
            minFPS = 0.0;
            maxResStr = "Unknown";
            minResStr = "Unknown";
            streamFormat = "Unknown";
            profileCnt = -1;

            state = PostmanState.PostmanState_Flushed;
        }
    }

    void Flush(){
        synchronized (TAG) {
            flushQ.clear(); //No backlog for more than one session
            flushRecordURL = producerUrl;
            while(immediateRecords.size() > 0) {
                MMMetricsBundle bundle = immediateRecords.remove(0);
                if(isEventRecord(bundle.record)) {
                    MMLogger.d("POSTMAN", "FLUSHING IMMEDIATE REC from the  Q " + bundle.record.qubitData.get(0).pbEventInfo.event);
                }
                decorateRecord(bundle.record);
                flushQ.add(bundle);
            }
            while(eventRecords.size() >0){
                MMMetricsBundle bundle = eventRecords.remove(0);
                if(bundle.vitalMessage) {
                    if(isEventRecord(bundle.record)) {
                        MMLogger.d("POSTMAN", "Getting from the Event Q " + bundle.record.qubitData.get(0).pbEventInfo.event);
                    }
                    decorateRecord(bundle.record);
                    flushQ.add(bundle);
                }else{
                    if(isEventRecord(bundle.record)) {
                        MMLogger.d("POSTMAN", "DISCARDING from the Event Q " + bundle.record.qubitData.get(0).pbEventInfo.event);
                    }
                }
            }
            qbrMode = null;
            totalDuration = null;
            isLiveSession = null;

            maxFPS = 0.0;
            minFPS = 0.0;
            maxResStr = "Unknown";
            minResStr = "Unknown";
            streamFormat = "Unknown";
            profileCnt = -1;

            state = PostmanState.PostmanState_Flushed;
        }
    }

    void Stop(){
        state = PostmanState.PostmanState_Stopped;
    }

    public void SetInterval(Integer interval){
        statInterval = interval;
    }

    public void SetProducerUrl(URL url){
        producerUrl = url;
    }

    void Shutdown(){
        Stop();
        shutdownEventor = true;
        sem.release();
    }

    public void run() {
        while (true) {
            try {
                sem.acquire();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (state != PostmanState.PostmanState_Stopped) {
                RecordStructure rsEvent = null;
                while((rsEvent = getRecordToPost()) != null){
                    String jsonTobeSent = rsEvent.toJson();
                    try {
                        HttpURLConnection httpConnection;
                        httpConnection = (HttpURLConnection) postalAddress.openConnection();
                        httpConnection.setRequestMethod("POST");
                        httpConnection.setRequestProperty("content-type", "application/json");

                        httpConnection.setDoInput(true);
                        httpConnection.setDoOutput(true);
                        MMLogger.v(TAG, " POSTMAN - " + jsonTobeSent);
                        OutputStream os = httpConnection.getOutputStream();
                        os.write(jsonTobeSent.getBytes());
                        os.flush();

                        MMLogger.v(TAG, "POSTMAN Response - " + httpConnection.getResponseCode() + " {" + httpConnection.getResponseMessage() + "} ");
                        httpConnection.disconnect();
                    } catch (Exception e) {
                        MMLogger.e(TAG, " POSTMAN exception in sending post " + e.getMessage());
                    }
                }
            }

            if(shutdownEventor == true){
                break;
            }
        }
        eventRecords.clear();
    }

    RecordStructure CloneRecord(RecordStructure in){
        if (in == null){
            return null;
        }
        RecordStructure rs = new RecordStructure();
        rs.qubitData = new CopyOnWriteArrayList<QBRMetric>();
        rs.interval = in.interval;
        rs.playDur = in.playDur;
        rs.pbTime = in.pbTime;
        rs.timestamp = in.timestamp;
        rs.version = in.version;
        for (int i =0 ; i< in.qubitData.size(); i++) {
            rs.qubitData.add(CloneQubitMetric(in.qubitData.get(i)));
        }
        return rs;
    }

    QBRMetric CloneQubitMetric(QBRMetric in){
        if (in == null){
            return null;
        }
        QBRMetric clone = new QBRMetric();
        clone.streamID = CloneStreamID(in.streamID);
        clone.streamInfo = CloneStreamInfo(in.streamInfo);
        //ag
        clone.contentMetadata = CloneContentMetadata(in.contentMetadata);
        if(in.segInfo != null) {
            clone.segInfo = new CopyOnWriteArrayList<SegmentInfo>();
            for (int i = 0; i < in.segInfo.size(); i++) {
                clone.segInfo.add(CloneSegInfo(in.segInfo.get(i)));
            }
        }else{
            clone.segInfo = null;
        }
        clone.clientInfo = CloneClientInfo(in.clientInfo);
        if(in.pbInfo!= null) {
            clone.pbInfo = new CopyOnWriteArrayList<PlaybackInfo>();
            for (int j = 0; j < in.pbInfo.size(); j++) {
                clone.pbInfo.add(ClonePBInfo(in.pbInfo.get(j)));
            }
        }else{
            clone.pbInfo = null;
        }


        clone.sdkInfo = CloneSDKInfo(in.sdkInfo);
        clone.adInfo = CloneAdInfo(in.adInfo);
        clone.diagnostics = CloneDiagnostics(in.diagnostics);
        clone.pbEventInfo = CloneEventInfo(in.pbEventInfo);
        clone.customTags = (in!= null && in.customTags != null)?(HashMap)in.customTags.clone():null;
        return clone;
    }

    StreamID CloneStreamID(StreamID in){
        if (in == null){
            return null;
        }
        StreamID clone = new StreamID();
        clone.streamURL = in.streamURL;
        clone.assetId = in.assetId;
        clone.assetName = in.assetName;
        clone.videoId = in.videoId;
        clone.custId = in.custId;
        clone.subscriberId = in.subscriberId;
        clone.subscriberType = in.subscriberType;
        clone.subscriberTag = in.subscriberTag;
        clone.sessionId = in.sessionId;
        clone.mode = in.mode;
        clone.isLive = in.isLive;
        clone.dataSrc = in.dataSrc;
        clone.playerName = in.playerName;
        clone.domainName = in.domainName;
        clone.pId = in.pId;
        return clone;
    }

    StreamInfo CloneStreamInfo(StreamInfo in){
        if (in == null){
            return null;
        }
        StreamInfo clone = new StreamInfo();
        clone.maxRes = in.maxRes;
        clone.minRes = in.minRes;
        clone.maxFps = in.maxFps;
        clone.minFps = in.minFps;
        clone.numOfProfile = in.numOfProfile;
        clone.totalDuration = in.totalDuration;
        clone.streamFormat = in.streamFormat;
        return clone;
    }

    //ag
    ContentMetadata CloneContentMetadata(ContentMetadata in){
                if (in == null){
                        return null;
                    }
                ContentMetadata clone = new ContentMetadata();
                clone.contentType = in.contentType;
                clone.assetId = in.assetId;
                clone.assetName = in.assetName;
                clone.videoId = in.videoId;
                clone.drmProtection = in.drmProtection;
               clone.episodeNumber = in.episodeNumber;
                clone.genre = in.genre;
                clone.season = in.season;
                clone.seriesTitle = in.seriesTitle;

                clone.videoType=in.videoType;
                return clone;
            }


    SegmentInfo CloneSegInfo(SegmentInfo in){
        if (in == null){
            return null;
        }
        SegmentInfo clone = new SegmentInfo();
        clone.timestamp = in.timestamp; //millisec
        clone.res = in.res;
        clone.qbrRes = in.qbrRes;
        clone.qbrBitrate = in.qbrBitrate; //bps
        clone.cbrBitrate = in.cbrBitrate; //bps
        clone.qbrQual = in.qbrQual;
        clone.cbrQual = in.cbrQual;
        clone.dur = in.dur;  //seconds
        clone.seqNum = in.seqNum;
        clone.startTime = in.startTime;
        clone.fps = in.fps;
        clone.profileNum = in.profileNum;
        clone.cbrProfileNum = in.cbrProfileNum;
        clone.cbrSize = in.cbrSize;
        clone.qbrSize = in.qbrSize;
        clone.vCodec = in.vCodec;
        clone.aCodec = in.aCodec;
        clone.downloadRate = in.downloadRate;
        clone.bufferLength = in.bufferLength;
        return clone;
    }

    PlaybackInfo ClonePBInfo(PlaybackInfo in){
        if (in == null){
            return null;
        }
        PlaybackInfo clone = new PlaybackInfo();
        clone.timestamp = in.timestamp; // in milliseconds
        clone.latency = in.latency; // in milliseconds
        clone.buffWait = in.buffWait; // in milliseconds
        clone.frameloss = in.frameloss;
        clone.bwInUse = in.bwInUse;// in bits per second
        clone.pbTime = in.pbTime;// in seconds
        clone.sumBuffWait = in.sumBuffWait;
        clone.upShiftCount = in.upShiftCount;
        clone.downShiftCount = in.downShiftCount;
        clone.pauseDuration = in.pauseDuration;// in milliseconds
        return clone;
    }

    SDKInfo CloneSDKInfo(SDKInfo in){
        if (in == null){
            return null;
        }
        SDKInfo clone = new SDKInfo();
        clone.hFileVersion = in.hFileVersion;
        clone.sdkVersion = in.sdkVersion;
        return clone;
    }

    Diagnostics CloneDiagnostics(Diagnostics in){
        if (in == null){
            return null;
        }
        Diagnostics clone = new Diagnostics();
        clone.sdkBootuptime = in.sdkBootuptime;
        return clone;
    }

    PBEventInfo CloneEventInfo(PBEventInfo in){
        if (in == null){
            return null;
        }
        PBEventInfo clone = new PBEventInfo();
        clone.pbTime = in.pbTime;
        clone.id = in.id;
        clone.event = in.event;
        clone.desc = in.desc;
        return clone;
    }

    AdInfo CloneAdInfo(AdInfo in) {
        if (in == null){
            return null;
        }
        AdInfo clone = new AdInfo();
        clone.adClient = in.adClient;
        clone.adId = in.adId;
        clone.adPosition = in.adPosition;
        clone.adSystem = in.adSystem;
        clone.adCreative = in.adCreative;
        clone.adLinear = in.adLinear;
        clone.adDuration = in.adDuration;
       // clone.adClient = in.adClient;
        //ag
        clone.adResolution = in.adResolution;
        clone.adInterval = in.adInterval;
        clone.adPodIndex = in.adPodIndex;
        clone.adPodLength = in.adPodLength;
        clone.adPodPosition = in.adPodPosition;
        clone.isBumper = in.isBumper;
        //ag
        clone.adCreativeId = in.adCreativeId;
        clone.adUrl = in.adUrl;
        clone.adTitle = in.adTitle;
        clone.adBitrate = in.adBitrate;

        return clone;
    }

    public ClientInfo CloneClientInfo(ClientInfo in){
        if (in == null){
            return null;
        }
        ClientInfo clone = new ClientInfo();
        clone.location = in.location;
        clone.latitude = in.latitude;
        clone.longitude = in.longitude;
        clone.cellInfo = CloneCellInfo(in.cellInfo);
        clone.operator = in.operator;
        clone.cdn = in.cdn;
        clone.nwType = in.nwType;
        clone.wifidatarate = in.wifidatarate;
        clone.wifissid = in.wifissid;
        clone.userAgent = in.userAgent;
        clone.device = in.device;
        clone.signalstrength = in.signalstrength;
        clone.scrnRes = in.scrnRes;
        clone.version = in.version;
        clone.brand = in.brand;
        clone.model = in.model;
        clone.platform = in.platform;
        return clone;
    }

    public MMCellInfo CloneCellInfo(MMCellInfo in){
        if (in == null){
            return null;
        }
        MMCellInfo clone = new MMCellInfo();
        clone.mCellRadio = in.mCellRadio;
        clone.mMcc = in.mMcc;
        clone.mMnc = in.mMnc;
        clone.mCid = in.mCid;
        clone.mLac = in.mLac;
        clone.mAsu = in.mAsu;
        clone.mTa = in.mTa;
        clone.mPsc = in.mPsc;
        clone.mSignalStrength = in.mSignalStrength;
        return clone;
    }

    public PBEventInfo ClonePBEventInfo(PBEventInfo in){
        if (in == null){
            return null;
        }
        PBEventInfo clone = new  PBEventInfo();
        clone.desc = in.desc;
        clone.event = in.event;
        clone.id = in.id;
        clone.pbTime = in.pbTime;
        return clone;
    }
}
