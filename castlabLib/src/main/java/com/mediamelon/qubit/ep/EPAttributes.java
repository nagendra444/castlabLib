package com.mediamelon.qubit.ep;

/**
 * Created by shri on 28/7/15.
 */
public interface EPAttributes {
    String UPSHIFTCOUNT = "upShiftCount";
    String DOWNSHIFTCOUNT = "downShiftCount";

    String PBTIME = "pbTime";
    String PLAYERNAME = "playerName";
    String DOMAINNAME = "domainName";
    String PLAYERMODE = "mode";
    Integer STATE_IDLE = 1;
    Integer STATE_PLAY = 2;
    Integer STATE_BUFFERING = 3;
    Integer STATE_PAUSED = 4;
    Integer STATE_ENDED = 5;
    String BOOTUPTIME = "sdkBootuptime";
    String SUBSCRIBERID = "subscriberId";
    String SUBSCRIBERTYPE = "subscriberType";
    String SUBSCRIBERTAG = "subscriberTag";
    String ISLIVE = "isLive";
    String DATASOURCE = "dataSrc";
    String CUSTID = "custId";
    String SESSIONID = "sessionId";
    String CDN = "cdn";
    String OPERATOR = "operator";
    String PLATFORM = "platform";
    String LOCATION = "location";
    String SCREENRES = "scrnRes";
    String NETWORKTYPE = "nwType";
    String MODEL = "model";
    String BRAND = "brand";
    String DEVVERSION = "version";
    String BANDWIDTH = "bwInUse";
    String LATENCY = "latency";
    String BUFFERING = "buffWait";
    String SUMBUFFERING = "sumBuffWait";
    String FRAMELOSS = "frameloss";
    String ASSETID = "assetId";
    String STREAMURL = "streamURL";
    String PLAYERSTATE = "playerState";
    String LASTTS = "lastTS";
    String MINRES = "minRes";
    String MAXRES = "maxRes";
    String SDKVERSION = "sdkVersion";
    String HINTFILEVERSION = "hFileVersion";
    String MAXFPS = "maxFps";
    String MINFPS = "minFps";
    String NUMPROFILE = "numOfProfile";
    String TOTALDURATION = "totalDuration";
    String RESOLUTION = "res";
    String QBRBITRATE = "qbrBitrate";
    String CBRBITRATE = "cbrBitrate";
    String QBRQUALITY = "qbrQual";
    String CBRQUALITY = "cbrQual";
    String DURATION = "dur";
    String FPS = "fps";
    String SEQNUM = "seqNum";
    String STARTTIME = "startTime";
    String PROFILENUM = "profileNum";
    String CBRSIZE = "cbrSize";
    String QBRSIZE = "qbrSize";
    String STREAMFORMAT = "streamFormat";

    String idle = "IDLE";
    String buffering = "BUFFERING";
    String play = "PLAY";
    String pause = "PAUSE";
    String end = "END";


    String WIFISSID = "wifissid";
    String WIFIDATARATE = "wifidatarate";
    String WIFISIGNALSTRENGTH = "signalstrength";
}
