
package com.mediamelon.smartstreaming;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.mediamelon.smartstreaming.Utils.trackingEvents.MMTrackingEventHandler;
import com.mediamelon.smartstreaming.Utils.trackingEvents.mmSSAIAdInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class MMSmartStreamingNowtilusSSAIPlugin implements MMSSAIEventsListeners{

  private static Document mVastDoc;
  private static java.net.URL url = null;
  private long basePlayPosInMs =0;
  private static long currentPlayerPosInMs =0;
  private static long livePlayPositionDiffAtSyncMs =0;
  private static String response_SSAIAd = null;
  private static String mediaURL = null;
  private static String vastURL = null;
  private static String variantManifestURL = null;
  private static long lastSyncVastTSInMs = -1;
  private boolean isLiveStream = false;
  private static String streamType = null;
  private static boolean isSSAIUrlParsingDone = false;
  private static JSONObject ssaiUrls = null;
  public static  ArrayList<mmSSAIAdInfo> adTimeline = new ArrayList<mmSSAIAdInfo>(); // stores ad info from Vast
  private static MMSSAIEventsCollector mmssaiEventsCollector = null;
  private MMTrackingEventHandler mmTrackingEventHandler = null;
  private static boolean isActiveAdPlaying = false;
  private static int activeAdIndex = 0;
  private static boolean timeSynced =false;
  private static boolean timeSyncedForDash = false;
  public static boolean isPollingEnabled =false;
  private  static volatile  Set<Long> VAST_Timestaps = new HashSet<>();// to solve the issue of multiple repeated vasts
  private static volatile Set<String> VAST_collect = new HashSet<>();
  private static boolean cueTimelineEnterSent = false;
  private static boolean cueTimelineExitSent = false;
  public static long prev_ended_at = 0;
  private static boolean playAlive = true;
  //private static List<String> clickTrackers=null;
  // private static String ClickThroughUrl=null;
  private static long adPlayTime=0L;
  // public static long curPos= 0;

  public MMSmartStreamingNowtilusSSAIPlugin(){
    mmssaiEventsCollector = new MMSSAIEventsCollector();
    mmTrackingEventHandler = new MMTrackingEventHandler();
    // Initialize SSAI Members
  }

  private static final Pattern XS_DURATION_PATTERN =
          Pattern.compile("^(-)?P(([0-9]*)Y)?(([0-9]*)M)?(([0-9]*)D)?"
                  + "(T(([0-9]*)H)?(([0-9]*)M)?(([0-9.]*)S)?)?$");


  public void closeSSAIAdManager(){

    timeSynced = false;
    isPollingEnabled = false;
    timeSyncedForDash = false;

    // if player is destroyed middle of Adbreak , this below parameters are to be reset
    cueTimelineEnterSent = false;
    adTimeline.clear();
    prev_ended_at = 0;
  }

  public static  void parseDashManifest2(String eventID ,long startTimeInMS,long durationInMS)
  {
    String vastURLToFetch = vastURL + "/" + eventID + ".xml";
    //updateDashLiveAdTimeline(eventID,startTimeInMS,durationInMS,vastURLToFetch);
    updateDashLiveAdTimeline(startTimeInMS,vastURLToFetch);
  }

  public static void parseDashManifest(String xmlString)
          throws ParserConfigurationException, IOException, SAXException {

    //  Log.i("DASH","parseDashManifest called for ");

    xmlString = xmlString.replaceFirst("<\\?.*\\?>", "");

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setCoalescing(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    mVastDoc = documentBuilder.parse(new InputSource(new StringReader(xmlString)));

    String type = null;

    long minUpdatePeriod = 0;
    long maxSegmentDuration = 0;

    NodeList nodeList_mpd = mVastDoc.getElementsByTagName("MPD");

    if (nodeList_mpd.getLength() > 0) {
      Node node_mpd = nodeList_mpd.item(0);
      type = node_mpd.getAttributes().getNamedItem("type").getNodeValue().trim();
      if(node_mpd.getAttributes().getNamedItem("minimumUpdatePeriod")!= null)
        minUpdatePeriod = (parseXsDuration(node_mpd.getAttributes().getNamedItem("minimumUpdatePeriod").getNodeValue().trim()))/1000;
      if(node_mpd.getAttributes().getNamedItem("maxSegmentDuration")!= null)
        maxSegmentDuration = (parseXsDuration(node_mpd.getAttributes().getNamedItem("maxSegmentDuration").getNodeValue().trim()))/1000;
      minUpdatePeriod = Math.max(minUpdatePeriod, maxSegmentDuration);
    }

    NodeList nodeList_period = mVastDoc.getElementsByTagName("Period");
    int totalPeriods = nodeList_period.getLength();

    if (nodeList_period.getLength() > 0) {
      for (int idx = 0; idx < totalPeriods; idx++) {
        Node node_period = nodeList_period.item(idx);
        String id = node_period.getAttributes().getNamedItem("id") + ""; //toString()
        long startInSec = 0;
        if(node_period.getAttributes().getNamedItem("start")!= null)
          startInSec = parseXsDuration(node_period.getAttributes().getNamedItem("start").getNodeValue().trim());

        for (int z = 0; z < node_period.getChildNodes().getLength(); z++) {
          if (node_period.getChildNodes().item(z).getNodeName().equals("EventStream")) {
            //  Log.i("JIO","**EVENTSTREAM RECEIVED**");
            Node eventStream = node_period.getChildNodes().item(z);
            if (eventStream != null) {
              String eventSchemeID = eventStream.getAttributes().getNamedItem("schemeIdUri").getNodeValue().trim();
              long timescale = Long.parseLong(eventStream.getAttributes().getNamedItem("timescale").getNodeValue().trim());
              String eventID = eventStream.getFirstChild().getNextSibling().getFirstChild().getNodeValue().split(":")[1].trim();
              long eventDuration = Long.parseLong(eventStream.getChildNodes().item(1).getAttributes().getNamedItem("duration").getNodeValue().trim());
              String vastURLToFetch = vastURL + "/" + eventID + ".xml";

              long startTimeInMS = startInSec;
              long durationInMS = (eventDuration / timescale) * 1000;


              updateDashLiveAdTimeline(startTimeInMS,vastURLToFetch);
            }
          }
        }
      }
    }

    if(type.equals("dynamic") && minUpdatePeriod > 0)
    {
      if(mediaURL != null)
      {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(
                new Runnable() {
                  public void run() {
                    //Log.v("calling.....","dashManifestMonitor........");
                    dashManifestMonitor(mediaURL);

                  }
                }, (minUpdatePeriod*1000)*2);
      }
    }
  }




  public static long parseXsDuration(String value) {
    Matcher matcher = XS_DURATION_PATTERN.matcher(value);
    if (matcher.matches()) {
      boolean negated = !TextUtils.isEmpty(matcher.group(1));
      // Durations containing years and months aren't completely defined. We assume there are
      // 30.4368 days in a month, and 365.242 days in a year.
      String years = matcher.group(3);
      double durationSeconds = (years != null) ? Double.parseDouble(years) * 31556908 : 0;
      String months = matcher.group(5);
      durationSeconds += (months != null) ? Double.parseDouble(months) * 2629739 : 0;
      String days = matcher.group(7);
      durationSeconds += (days != null) ? Double.parseDouble(days) * 86400 : 0;
      String hours = matcher.group(10);
      durationSeconds += (hours != null) ? Double.parseDouble(hours) * 3600 : 0;
      String minutes = matcher.group(12);
      durationSeconds += (minutes != null) ? Double.parseDouble(minutes) * 60 : 0;
      String seconds = matcher.group(14);
      durationSeconds += (seconds != null) ? Double.parseDouble(seconds) : 0;
      long durationMillis = (long) (durationSeconds * 1000);
      return negated ? -durationMillis : durationMillis;
    } else {
      return (long) (Double.parseDouble(value) * 3600 * 1000);
    }
  }

  public static void prepareNowtilusLIVE(String ssaiAdUrl) {

    StringBuilder resultSSAIAd = new StringBuilder();

    new Thread(new Runnable() {
      @Override
      public void run() {

        try {
          url = new URL(ssaiAdUrl);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
          if (url != null) {
            conn = (HttpURLConnection) url.openConnection();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          if (conn != null) {
            conn.setRequestMethod("GET");
          }
        } catch (ProtocolException e) {
          e.printStackTrace();
        }
        BufferedReader rd = null;
        try {
          if (conn != null) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        String line = null;
        while (true) {
          try {
            if (rd != null && (line = rd.readLine()) == null)
              break;
          } catch (IOException e) {
            e.printStackTrace();
          }
          resultSSAIAd.append(line);
        }
        try {
          rd.close();
        } catch (IOException e) {
          e.printStackTrace();
        }

        try {
          if (conn.getResponseCode() == 200) {
            response_SSAIAd = resultSSAIAd + "";//toString()
            triggerNowtilusLIVE(response_SSAIAd);

          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();

  }

  public static JSONObject getSSAIUrls()
  {
    if(ssaiUrls != null)
    {
      return ssaiUrls;
    }
    return null;
  }

  public static boolean getSSAIUrlParsingStatus()
  {
    return isSSAIUrlParsingDone;
  }

  public static void triggerNowtilusLIVE(String ssaiAdUrlResponse){

    JSONObject jsonSSAIObject = null;

    try {
      jsonSSAIObject = new JSONObject(ssaiAdUrlResponse);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    if(jsonSSAIObject != null)
    {
      try {

        mediaURL = jsonSSAIObject.get("mediaURL")+"";//toString()
        vastURL = jsonSSAIObject.get("vastURL")+"";//toString()

        ssaiUrls = jsonSSAIObject;

        getSSAIUrls();

        isSSAIUrlParsingDone = true;

      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    new Thread(new Runnable() {
      @Override
      public void run() {

        try {
          url = new URL(mediaURL);
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
          if (url != null) {
            conn = (HttpURLConnection) url.openConnection();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        try {
          if (conn != null) {
            conn.setRequestMethod("GET");
          }
        } catch (ProtocolException e) {
          e.printStackTrace();
        }


        Scanner scanner = null;
        try {
          scanner = new Scanner(conn.getInputStream());
        } catch (IOException e) {
          e.printStackTrace();
        }
        scanner.useDelimiter("</MPD>");
        String content = scanner.next();
        scanner.close();
        content = content + "\n" + "</MPD>";

        try {
          if(conn.getResponseCode() == 200) {
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  public void mmSSAIClientInit(String mediaurl, String vasturl,boolean isLive,boolean enablePollingforSSAI,JSONArray clips,boolean clientSideTracking)
  {
    isPollingEnabled = enablePollingforSSAI;
    if(mediaurl != null)
    {
      mediaURL = mediaurl;
      vastURL = vasturl;
      streamType = (mediaURL.indexOf(".mpd") > 0) ? "dash"
              : (mediaURL.indexOf(".m3u8") > 0) ? "hls" : "mp4";
    }

    if(vastURL == null){
      if(streamType.equals("dash")){
        // vastURL = mediaURL.substring(0, mediaURL.lastIndexOf(".mpd")) + "/vast";
        vastURL = mediaURL.replaceAll("master.m3u8","") + "/vast";
      }
      if(streamType.equals("hls")){
        // String sid = mediaURL.split("sid=")[1].split("&api-key")[0];
        // vastURL = mediaURL.substring(0, mediaURL.lastIndexOf("master.m3u8")) + sid + "/vast"; //commenting for JIO streams
        // vastURL =vastURL.substring(0,vastURL.lastIndexOf(";")) +")" + "/vast.xml";
      }
    }
    isLiveStream = isLive;
    if(isLiveStream && enablePollingforSSAI) {
      startNowtilusLive(mediaURL, vastURL);
    }else if( !isLiveStream){
      startNowtilusVod(clips); // for JIO currently VOD architecture is not QA'ed
    }
  }
  public static String getStreamType()
  {
    return streamType;
  }

  public static void dashManifestMonitor(String mediaurl)
  {
    //Log.i("MM","dashManifestMonitor called for "  + mediaurl);
    if(mediaurl != null && isPollingEnabled)
    {

      new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            url = new URL(mediaURL);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
          HttpURLConnection conn = null;
          try {
            if (url != null) {
              conn = (HttpURLConnection) url.openConnection();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            if (conn != null) {
              conn.setRequestMethod("GET");
            }
          } catch (ProtocolException e) {
            e.printStackTrace();
          }


          Scanner scanner = null;
          try {
            if (conn != null) {
              scanner = new Scanner(conn.getInputStream());
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          if (scanner != null) {
            scanner.useDelimiter("</MPD>");
          }
          String content = null;
          if (scanner != null) {
            content = scanner.next();
          }
          if (scanner != null) {
            scanner.close();
          }
          content = content + "\n" + "</MPD>";

          try {
            if (conn != null && conn.getResponseCode() == 200) {
              try {
                parseDashManifest(content);
              } catch (ParserConfigurationException e) {
                e.printStackTrace();
              } catch (SAXException e) {
                e.printStackTrace();
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();

    }
  }

  public static int fetchNowtilusHLSLiveVastInfo(long pdtInMS, String vastTrackingUrl ){
    if(vastTrackingUrl != null)
    {
      new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            url = new URL(vastTrackingUrl);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
          HttpURLConnection conn = null;
          try {
            if (url != null) {
              conn = (HttpURLConnection) url.openConnection();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            if (conn != null) {
              conn.setRequestMethod("GET");
            }
          } catch (ProtocolException e) {
            e.printStackTrace();
          }


          Scanner scanner = null;
          try {
            if (conn != null) {
              scanner = new Scanner(conn.getInputStream());
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          String content = null;
          if (scanner != null && scanner.hasNext()) {
            content = scanner.useDelimiter("\\A").next();
          }
          if (scanner != null) {
            scanner.close();
          }

          long startTimeInMS =0;


          JSONObject vastJsonObj = null;
          if(content != null && !content.isEmpty()){
            try {
              vastJsonObj = new JSONObject(content);
              startTimeInMS = vastJsonObj.getLong("time"); // start time of the ad in epoch time
              if(!VAST_Timestaps.add(startTimeInMS)) { // note  - check for duplicate  vast
                //  Log.i("YYYY","******** DUPLICATE VAST RECEIVED" + " At " + startTimeInMS + " - RETURNING *************");
                //Log.i("XADID","******** DUPLICATE VAST RECEIVED" + " At " + startTimeInMS + " - RETURNING *************");
                return;
              }
            } catch (Throwable t) {
              Log.e("SSAI", "Could not parse malformed JSON1: \"" + content + "\"");
            }
          }

          try {
            Log.i("MM","RESPONSE CODE FOR " + vastTrackingUrl + " is " + conn.getResponseCode());
          } catch (IOException e) {
            e.printStackTrace();
          }
          long prev_ended_at = 0;
          try {
            if (conn != null && conn.getResponseCode() == 200 && lastSyncVastTSInMs != startTimeInMS) {  // NOTE  - .getResponse() is a blocking call
              try {

                lastSyncVastTSInMs = startTimeInMS;
                // New Live Ad
                adTimeline.clear();
                long durationInMS = 0;
                mmSSAIAdInfo mmSSAIAdInfo = new mmSSAIAdInfo();
                mmSSAIAdInfo.trackerObj = new MMVastParser();
                mmSSAIAdInfo.trackerObj.parseVast(vastJsonObj.getString("vast"));
                MMVastParser trackerObj2 = mmSSAIAdInfo.trackerObj;

                List<String> adIds = mmSSAIAdInfo.trackerObj.getAdId();
                for (int i = 0; i < adIds.size(); i++) {

                  SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                  String time = mmSSAIAdInfo.trackerObj.getDuration().get(i);
                  String[] timeArray = time.split(":");
                  durationInMS= (long) ((Float.parseFloat(timeArray[0])*60*60 + Float.parseFloat(timeArray[1])*60 + Float.parseFloat(timeArray[2])) * 1000); // this is not in epoch
                  // Date mDate = sdf.parse(mmSSAIAdInfo.trackerObj.getDuration().get(i));
                  mmSSAIAdInfo ad2 = new mmSSAIAdInfo();
                  ad2.trackerObj = trackerObj2;
                  ad2.adId = adIds.get(i);
                  ad2.adIndex=i+1; // ad index is 1,2,3..... and not from 0
                  ad2.totalAds=adIds.size();
                  ad2.position = "MID";
                  ad2.adDuration = (durationInMS / 1000);
                  if(i>0)
                    startTimeInMS =  prev_ended_at;
                  ad2.isLinear = true;
                  ad2.startTime = startTimeInMS;
                  ad2.endTime = startTimeInMS + durationInMS;
                  ad2.startPos = (ad2.startTime - programDateTimeAtSyncMs); // subtracting start_Offset
                  ad2.endPos = (ad2.endTime - programDateTimeAtSyncMs); // subtracting start_Offset
                  prev_ended_at = startTimeInMS + durationInMS;
                  ad2.firstQuartile = (long) (startTimeInMS + (durationInMS * 0.25));
                  ad2.midPoint = (long) (startTimeInMS + (durationInMS * 0.50));
                  ad2.thirdQuartile = (long) (startTimeInMS + (durationInMS * 0.75));
                  ad2.adState = "ADSTATE.READY";
                  if(mmSSAIAdInfo.trackerObj.getAdSystem().size() > 0)
                    ad2.adServer = mmSSAIAdInfo.trackerObj.getAdSystem().get(i);
                  if(mmSSAIAdInfo.trackerObj.getAdTitle().size() > 0)
                    ad2.adTitle = mmSSAIAdInfo.trackerObj.getAdTitle().get(i);
                  ad2.adSkipOffset = ad2.trackerObj.getSkipoffsetInSecsForCurrentAD(ad2.adId);
                  ad2.impressionTrackers = ad2.trackerObj.getImpressionURLsForCurrentAD(ad2.adId);
                  ad2.startTrackers =ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"start");
                  ad2.firstQuartileTrackers =ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"firstQuartile");
                  ad2.midpointTrackers= ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"midpoint");
                  ad2.thirdQuartileTrackers = ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"thirdQuartile");
                  ad2.completeTrackers = ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"complete");
                  ad2.clickTrackingURLs =ad2.trackerObj.getClickTrackingURLsForCurrentAD(ad2.adId);
                  ad2.clickThroughURLs =ad2.trackerObj.getClickThroughURLsForCurrentAD(ad2.adId);
                  adTimeline.add(i,ad2);
                }
              }
              catch (ParserConfigurationException e) {
                e.printStackTrace();
              } catch (IOException e) {
                e.printStackTrace();
              } catch (SAXException e) {
                e.printStackTrace();
              } catch (Throwable t) {
                Log.e("SSAI", "Could not parse malformed JSON2: \"" + content + "\"");
              }

              if(adTimeline.size() > 0){
                // CUE TIMELINE ADDED
                notifyLiveAdCueTimelineUpdate();
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();

    }
    return 1;
  }


  public static void fetchNowtilusDashLiveVastInfo(String vastURL,long startTimeInMs2)
  {
    //Log.i("JIO","fetchNowtilusDashLiveVastInfo called for " + vastURL);
    if(vastURL != null)
    {
      Log.i("Castlab-fetch","%%%% FETCHING VAST STARTS %%%%%%% " + System.currentTimeMillis());

      new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            url = new URL(vastURL);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
          HttpURLConnection conn = null;
          try {
            if (url != null) {
              conn = (HttpURLConnection) url.openConnection();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            if (conn != null) {
              conn.setRequestMethod("GET");
            }
          } catch (ProtocolException e) {
            e.printStackTrace();
          }


          Scanner scanner = null;
          try {
            if (conn != null) {
              scanner = new Scanner(conn.getInputStream());
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          String content = null;
          if (scanner != null && scanner.hasNext()) {
            scanner.useDelimiter("</VAST>");
            content = scanner.next();
          }
          //String content = null;
          if (scanner != null) {
            scanner.close();
          }
          content = content + "\n" + "</VAST>";
          long start = startTimeInMs2;
          if(!VAST_Timestaps.add(start)) { // note  - check for duplicate  vast
            //   Log.i("YYYY","******** DUPLICATE VAST RECEIVED" + " At " + start + " - RETURNING *************");
            return;
          }
          //long prev_ended_at = 0;
          try {
            if (conn != null && conn.getResponseCode() == 200 && lastSyncVastTSInMs!=start) {
              try {
                //Log.i("MM","VAST URL  =" + vastURL);
                lastSyncVastTSInMs = start;
                // New Live Ad

                //adTimeline.clear(); //getting multiple vast files for 1 ad break

                //adInfo.trackerObj.parseVast(content);
                long durationInMS = 0;
                mmSSAIAdInfo mmSSAIAdInfo = new mmSSAIAdInfo();
                mmSSAIAdInfo.trackerObj = new MMVastParser();
                //mmSSAIAdInfo.trackerObj.parseVast(jj.getString("vast"));
                mmSSAIAdInfo.trackerObj.parseVast(content);
                MMVastParser trackerObj2 = mmSSAIAdInfo.trackerObj;

                List<String> adIds = mmSSAIAdInfo.trackerObj.getAdId();
                for (int i = 0; i < adIds.size(); i++) {

                  SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                  try {
                    String time = mmSSAIAdInfo.trackerObj.getDuration().get(i);
                    //Log.i("TIME_AD",time);

                    String[] timeArray = time.split(":");
                    //durationInMS=(Integer.parseInt(timeArray[0])*60*60 + Integer.parseInt(timeArray[1])*60 + Integer.parseInt(timeArray[2].split(".")[0])) * 1000; // this is not in epoch
                    durationInMS= (long) ((Float.parseFloat(timeArray[0])*60*60 + Float.parseFloat(timeArray[1])*60 + Float.parseFloat(timeArray[2])) * 1000); // this is not in epoch
                    Date mDate = sdf.parse(mmSSAIAdInfo.trackerObj.getDuration().get(i));
                  } catch (ParseException e) {
                    e.printStackTrace();
                  }
                  mmSSAIAdInfo ad2 = new mmSSAIAdInfo();
                  ad2.trackerObj = trackerObj2;
                  ad2.adId = adIds.get(i);
                  ad2.adIndex=i+1; // ad index is 1,2,3..... and not from 0
                  ad2.totalAds=adIds.size();
                  ad2.position = "MID";
                  ad2.adDuration = (durationInMS / 1000);
                  if(prev_ended_at!=0)
                    start=  prev_ended_at+50; // end time and start time of ads getting colliding
                  ad2.isLinear = true;
                  ad2.startTime = start;
                  ad2.endTime = (long)(start + (durationInMS - 200));
                  prev_ended_at = start+durationInMS;
                  ad2.startPos = (ad2.startTime - programDateTimeForDash); // subtracting start_Offset
                  ad2.endPos = (ad2.endTime - programDateTimeForDash);
                  ad2.firstQuartile = (long) (start + (durationInMS * 0.25));
                  ad2.midPoint = (long) (start+ (durationInMS * 0.50));
                  ad2.thirdQuartile = (long) (start + (durationInMS * 0.75));
                  ad2.adState = "ADSTATE.READY";
                  if(mmSSAIAdInfo.trackerObj.getAdSystem().size() > 0)
                    ad2.adServer = mmSSAIAdInfo.trackerObj.getAdSystem().get(i);
                  if(mmSSAIAdInfo.trackerObj.getAdTitle().size() > 0)
                    ad2.adTitle = mmSSAIAdInfo.trackerObj.getAdTitle().get(i);
                  ad2.adSkipOffset = ad2.trackerObj.getSkipoffsetInSecsForCurrentAD(ad2.adId);
                  ad2.impressionTrackers = ad2.trackerObj.getImpressionURLsForCurrentAD(ad2.adId);
                  ad2.startTrackers =ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"start");
                  ad2.firstQuartileTrackers =ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"firstQuartile");
                  ad2.midpointTrackers= ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"midpoint");
                  ad2.thirdQuartileTrackers = ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"thirdQuartile");
                  ad2.completeTrackers = ad2.trackerObj.getTrackingURLsForCurrentAD(ad2.adId,"complete");
                  ad2.clickTrackingURLs =ad2.trackerObj.getClickTrackingURLsForCurrentAD(ad2.adId);
                  ad2.clickThroughURLs =ad2.trackerObj.getClickThroughURLsForCurrentAD(ad2.adId);
                  adTimeline.add(i,ad2);
                }



                // TODO - might need a loop here for iterating through multiple ads and also create the ad object below and not previously


                // addAdTimeline(adInfo,0);

              } catch (ParserConfigurationException e) {
                e.printStackTrace();
              } catch (IOException e) {
                e.printStackTrace();
              } catch (SAXException e) {
                e.printStackTrace();
              }
              if(adTimeline.size() > 0){
                // CUE TIMELINE ADDED
                notifyLiveAdCueTimelineUpdate();
              }


            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();

    }
  }


  public static void parseHlsVariantManifest(Scanner variantManifestScanner) // the variant is polled only to get the pdtInMS value
          throws ParserConfigurationException, IOException{
    String content = null;
    int targetDuration = 1;
    long pdtInMS = 0;
    if (variantManifestScanner != null) {
      content = variantManifestScanner.next();
      //Log.i("CCCC",content);
      if(content.contentEquals("#EXTM3U")){
        do {
          content = variantManifestScanner.nextLine();
          if (content != null && !content.isEmpty() ) {
            if(content.contains("#EXT-X-PROGRAM-DATE-TIME")){
              String pdtStr = content.substring(content.indexOf(':')+1);
              if (pdtInMS==0 &&  pdtStr != null & !pdtStr.isEmpty()) {

                // Note: PDT in UTC here
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                  Date mDate = sdf.parse(pdtStr);
                  pdtInMS = mDate.getTime();
                } catch (ParseException e) {
                  e.printStackTrace();
                }
              }
              //Log.d("SSAI", "pdtInMS [" + pdtInMS + "]");

            }else if(content.contains("#EXT-X-TARGETDURATION")){
              targetDuration = Integer.parseInt(content.substring(content.indexOf(':')+1));
              // Log.d("SSAI", "targetDuration [" + targetDuration + "]");
            }else if(content.contains("#EXT-X-DATERANGE")){
                  Log.i("CCCC",content);
                  String[] arr = content.split(",");
                  for(int i=0;i<arr.length;i++){
                    if(arr[i].contains("X-AD-VAST")){
                      String vastUrl = arr[i].split("=")[1];
                      String vastUrlFromManifest = vastUrl.substring(1,vastUrl.length()-1);
                      //Log.i("CCCC",vastUrlFromManifest);
                      if(!VAST_collect.contains(vastUrlFromManifest)){
                        //Log.i("CCCC","New Vast Url added  " + vastUrlFromManifest);
                        VAST_collect.add(vastUrlFromManifest);
                        //syncPdtPosition(pdtInMS);
                        fetchNowtilusHLSLiveVastInfo(pdtInMS, vastUrlFromManifest);

                      }
                    }
                  }
            }
          }
        }while (variantManifestScanner.hasNext());
      }
      // Log.d("SSAI", "pdtInMS [" + pdtInMS + "]");
      syncPdtPosition(pdtInMS);

      //fetchNowtilusHLSLiveVastInfo(pdtInMS, vastURL); //  function called to keep polling the vastURL for 200 OK

    }
    if(variantManifestURL != null){
      // Call HLSManifestMonitor for Variant Manifest
      Handler handler = new Handler(Looper.getMainLooper());

      handler.postDelayed(
              new Runnable() {
                public void run() {
                  hlsManifestMonitor(variantManifestURL, true); // NOTE - weird recursive code
                }
              }, (1000 * (targetDuration / 2)));
    }
  }

  public static void parseHlsMasterManifest(Scanner masterManifestScanner) // this function is only called once to scan the response of mediaUrl and construct the variant url
          throws ParserConfigurationException, IOException{
    String content = null;
    if (masterManifestScanner != null) {
      content = masterManifestScanner.next();
      if(content.contentEquals("#EXTM3U")){
        do {
          content = masterManifestScanner.nextLine();
          if (content != null && !content.isEmpty() ) {


            if(content.contains("#EXT-X-STREAM-INF")){
              variantManifestURL = masterManifestScanner.nextLine();
              //   Log.d("SSAI", "variantManifestUrl [" + variantManifestURL + "]");
              if(variantManifestURL != null) {
                if(variantManifestURL.contains("http") == false){
                  if(vastURL == null){
                    vastURL =mediaURL.substring(0,mediaURL.indexOf("master"))+ variantManifestURL.substring(0,variantManifestURL.indexOf("/VAR")) + "/vast.xml";
                    //  Log.i("JIO","VASTURL "+ vastURL);
                  }
                  // Get Media Base URL
                  String baseUrl = null;
                  if(mediaURL != null){
                    baseUrl = mediaURL.substring(0, mediaURL.lastIndexOf("/")) + "/";
                  }
                  // Append Variant list URL
                  if(baseUrl != null) {
                    variantManifestURL = baseUrl + variantManifestURL;
                  }
                  //  Log.d("SSAI", "variantManifestUrl [" + variantManifestURL + "]");
                }
                // Generate VAST URL if not set from application


                break;
              }
            }
          }
        }while (masterManifestScanner.hasNext());
      }
      if(variantManifestURL != null){
        // Call HLSManifestMonitor for Variant Manifest
        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(
                new Runnable() {
                  public void run() {
                    hlsManifestMonitor(variantManifestURL, true);
                  }
                }, (1000)); // why 1 sec ?

//        Timer t = new Timer();
//        t.schedule(new TimerTask() {
//          @Override
//          public void run() {
//            hlsManifestMonitor(variantManifestURL, true);
//          }
//        },1000,1000);

      }

    }
  }

  public static void hlsManifestMonitor(String mediaurl, boolean isVariant) // this function gets called 1 . for getting response from the mediaUrl 2 . polled continously for getting the response of the variant url
  {
    //Log.i("MM","hlsManifestMonitor called for the URL " + mediaurl);
    if(mediaurl != null && isPollingEnabled)
    {
      new Thread(new Runnable() {
        @Override
        public void run() {

          try {
            url = new URL(mediaurl);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
          HttpURLConnection conn = null;
          try {
            if (url != null) {
              conn = (HttpURLConnection) url.openConnection();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            if (conn != null) {
              conn.setRequestMethod("GET");
            }
          } catch (ProtocolException e) {
            e.printStackTrace();
          }

          String content = null;
          Scanner scanner = null;
          try {
            if (conn != null) {
              scanner = new Scanner(conn.getInputStream());
            }
          } catch (IOException e) {
            e.printStackTrace();
          }

          //if (scanner != null) {
          //content = scanner.useDelimiter("\\A").next();
          //}

          try {
            if (conn != null && conn.getResponseCode() == 200) {
              try {
                if(isVariant){
                  parseHlsVariantManifest(scanner);
                }
                else {
                  parseHlsMasterManifest(scanner);
                }
              } catch (ParserConfigurationException e) {
                e.printStackTrace();
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }

          if (scanner != null) {
            scanner.close();
          }

        }
      }).start();

    }
  }

  public void startNowtilusLive(String mediaurl, String vasturl)
  {
    //  Log.i("JIO","startNowtilusLive called");
    mediaURL = mediaurl;
    vastURL = vasturl;

    if(mediaURL != null)
    {
      if(streamType == "dash") {
        dashManifestMonitor(mediaURL);
      }else{
        hlsManifestMonitor(mediaURL, false);
      }
    }
  }

//  public void startNowtilusVod(String mediaurl, String vasturl)
//  {
//    mediaURL = mediaurl;
//    vastURL = vasturl;
//
//    if(vastURL != null)
//    {
//      new Thread(new Runnable() {
//        @Override
//        public void run() {
//
//          try {
//            url = new URL(vastURL);
//          } catch (MalformedURLException e) {
//            e.printStackTrace();
//          }
//          HttpURLConnection conn = null;
//          try {
//            if (url != null) {
//              conn = (HttpURLConnection) url.openConnection();
//            }
//          } catch (IOException e) {
//            e.printStackTrace();
//          }
//          try {
//            if (conn != null) {
//              conn.setRequestMethod("GET");
//            }
//          } catch (ProtocolException e) {
//            e.printStackTrace();
//          }
//
//
//          Scanner scanner = null;
//          try {
//            if (conn != null) {
//              scanner = new Scanner(conn.getInputStream());
//            }
//          } catch (IOException e) {
//            e.printStackTrace();
//          }
//          String content = null;
//          if (scanner != null && scanner.hasNext()) {
//            content = scanner.useDelimiter("\\A").next();
//          }
//          if (scanner != null) {
//            scanner.close();
//          }
//
//          JSONArray vastArray = null;
//          if(content != null && !content.isEmpty()){
//            try {
//              vastArray = new JSONArray(content);
//            } catch (Throwable t) {
//              Log.e("SSAI", "Could not parse malformed JSON3: " + t.getMessage());
//            }
//          }
//
//          long startTimeInMS =0;
//          long clipTimelineStartTrackerInMs=0;
//          try {
//            if (conn != null && conn.getResponseCode() == 200) {
//              try {
//                if (vastArray != null) {
//                  for (int i = 0; i < vastArray.length(); i++) {
//                    String category = vastArray.getJSONObject(i).getString("category");
//                    long durationInMS = vastArray.getJSONObject(i).getInt("duration");
//                    if(category.equalsIgnoreCase("ad")) {
//                      mmSSAIAdInfo mmSSAIAdInfo = new mmSSAIAdInfo();
//                      mmSSAIAdInfo.trackerObj = new MMVastParser();
//                      mmSSAIAdInfo.trackerObj.parseJson(vastArray.getJSONObject(i));
//
//                      mmSSAIAdInfo.adId = (mmSSAIAdInfo.trackerObj.getAdId().size() > 0)?mmSSAIAdInfo.trackerObj.getAdId().get(0):"";
//                      mmSSAIAdInfo.position = "MID";
//                      if(clipTimelineStartTrackerInMs <= 0){
//                        mmSSAIAdInfo.position = "PRE";
//                      }else if(i >= (vastArray.length() - 1)){
//                        mmSSAIAdInfo.position = "POST";
//                      }
//                      startTimeInMS = clipTimelineStartTrackerInMs;
//                      mmSSAIAdInfo.adDuration = (durationInMS / 1000);
//                      mmSSAIAdInfo.isLinear = true;
//                      mmSSAIAdInfo.startTime = startTimeInMS;
//                      mmSSAIAdInfo.endTime = startTimeInMS + durationInMS;
//                      mmSSAIAdInfo.firstQuartile = (long) (startTimeInMS + (durationInMS * 0.25));
//                      mmSSAIAdInfo.midPoint = (long) (startTimeInMS + (durationInMS * 0.50));
//                      mmSSAIAdInfo.thirdQuartile = (long) (startTimeInMS + (durationInMS * 0.75));
//                      mmSSAIAdInfo.adState = "ADSTATE.READY";
//                      mmSSAIAdInfo.adIndex = adTimeline.size() + 1;
//                      addAdTimeline(mmSSAIAdInfo,i);
//                    }
//                    clipTimelineStartTrackerInMs += durationInMS;
//                  }
//
//                  if(adTimeline.size() > 0){
//                    // CUE TIMELINE ADDED
//                    notifyAdCueTimelineUpdate();
//                  }
//                }
//              } catch(Throwable t){
//                Log.e("SSAI", "Could not parse malformed JSON4: " + t.getMessage() );
//              }
//            }
//          } catch (IOException e) {
//            e.printStackTrace();
//          }
//        }
//      }).start();
//
//    }
//  }

  public void startNowtilusVod(JSONArray obj)
  {

    if(obj != null)
    {
      new Thread(new Runnable() {
        @Override
        public void run() {

          //JSON thing
          JSONArray vastArray = null;
          vastArray = obj;

          long startTimeInMS =0;
          long clipTimelineStartTrackerInMs=0;

          if (vastArray!=null) {
            try {
              if (vastArray != null) {
                int adCount = 0; //added to count category with ads by Nag
                for (int i = 0; i < vastArray.length(); i++) {
                  Log.d("Castlab-Test","Adding.....");
                  String category = vastArray.getJSONObject(i).getString("category");
                  long durationInMS = vastArray.getJSONObject(i).getInt("duration");
                  if(category.equalsIgnoreCase("ad")) {

                    mmSSAIAdInfo mmSSAIAdInfo = new mmSSAIAdInfo();
                    mmSSAIAdInfo.trackerObj = new MMVastParser();
                    mmSSAIAdInfo.trackerObj.parseJson(vastArray.getJSONObject(i));

                    mmSSAIAdInfo.adId = (mmSSAIAdInfo.trackerObj.getAdId().size() > 0)?mmSSAIAdInfo.trackerObj.getAdId().get(0):"";
                    mmSSAIAdInfo.adTitle = (mmSSAIAdInfo.trackerObj.getAdTitle().size() > 0)?mmSSAIAdInfo.trackerObj.getAdTitle().get(0):"";
                    mmSSAIAdInfo.adServer = (mmSSAIAdInfo.trackerObj.getAdSystem().size() > 0)?mmSSAIAdInfo.trackerObj.getAdSystem().get(0):"";


                    mmSSAIAdInfo.position = "MID";
                    if(clipTimelineStartTrackerInMs <= 0){
                      mmSSAIAdInfo.position = "PRE";
                    }else if(i >= (vastArray.length() - 1)){
                      mmSSAIAdInfo.position = "POST";
                    }

                    startTimeInMS = clipTimelineStartTrackerInMs;
                    mmSSAIAdInfo.adDuration = (durationInMS / 1000);
                    mmSSAIAdInfo.isLinear = true;
                    mmSSAIAdInfo.startTime = startTimeInMS;
                    mmSSAIAdInfo.endTime = startTimeInMS + durationInMS;
                    mmSSAIAdInfo.firstQuartile = (long) (startTimeInMS + (durationInMS * 0.25));
                    mmSSAIAdInfo.midPoint = (long) (startTimeInMS + (durationInMS * 0.50));
                    mmSSAIAdInfo.thirdQuartile = (long) (startTimeInMS + (durationInMS * 0.75));
                    mmSSAIAdInfo.adState = "ADSTATE.READY";
                    mmSSAIAdInfo.adIndex = adTimeline.size() + 1;


                    mmSSAIAdInfo.startTrackers = mmSSAIAdInfo.trackerObj.getVideoStartTrackers();
                    mmSSAIAdInfo.impressionTrackers = mmSSAIAdInfo.trackerObj.getImpressionTrackers();
                    mmSSAIAdInfo.firstQuartileTrackers = mmSSAIAdInfo.trackerObj.getVideoFirstQuartileTrackers();
                    mmSSAIAdInfo.midpointTrackers = mmSSAIAdInfo.trackerObj.getVideoMidpointTrackers();
                    mmSSAIAdInfo.thirdQuartileTrackers = mmSSAIAdInfo.trackerObj.getVideoThirdQuartileTrackers();
                    mmSSAIAdInfo.completeTrackers = mmSSAIAdInfo.trackerObj.getVideoCompleteTrackers();

                    //addAdTimeline(mmSSAIAdInfo,adCount); //Adding ad info to adTimeline array
                    adTimeline.add(adCount,mmSSAIAdInfo);
                    adCount = adCount+1;
                  }
                  clipTimelineStartTrackerInMs += durationInMS;
                }

                if(adTimeline.size() > 0){
                  // CUE TIMELINE ADDED
                  notifyAdCueTimelineUpdate();
                }
              }
            } catch(Throwable t){
              Log.e("SSAI", "Could not parse malformed JSON4: " + t.getMessage() );
            }
          }
        }
      }).start();

    }
  }


  private static long playPositionAtSyncMs = 0;
  private static long programDateTimeAtSyncMs = 0;

  private static void syncPdtPosition(long programDateTimeMs){
    if( !timeSynced && programDateTimeAtSyncMs != programDateTimeMs) { // TODO should  !timeSynced be removed??
      timeSynced=true;
      playPositionAtSyncMs = currentPlayerPosInMs;
      programDateTimeAtSyncMs = programDateTimeMs;
      livePlayPositionDiffAtSyncMs = Math.abs(currentPlayerPosInMs - programDateTimeMs);

    }
  }
  public static void syncPdtPosition2(long programDateTimeMs)
  {
    syncPdtPosition(programDateTimeMs);
  }

  private long getDateTimeMs(long currentPosInMs) {
    currentPlayerPosInMs = currentPosInMs;
    //Log.d("SSAI","getDateTimeMs PDT:"+programDateTimeAtSyncMs + " curTime:" + currentPosInMs);
    return (
            programDateTimeAtSyncMs +
                    (currentPosInMs * 1000 - playPositionAtSyncMs)
    );
  }


  private long DASHAdjustment = 0; // this is to adjust the time to epoch time in ms

  private long getSyncPosInMs(long currentPosInMs){
    currentPlayerPosInMs = currentPosInMs;
    if(isLiveStream){
      if(streamType.equalsIgnoreCase("hls")){
        return (currentPosInMs + livePlayPositionDiffAtSyncMs);
      }
      else {

        if ((basePlayPosInMs == 0) && (currentPosInMs > 0)) {  //TODO - set the epoch time here for playback start(at t==0) - DONE
          basePlayPosInMs = currentPosInMs;
        }

        if (basePlayPosInMs >= 0) {
          return (currentPosInMs - basePlayPosInMs);
        }

      }
    }
    //  Log.i("JIO","FOR NOT LIVE got value =" + currentPosInMs + " returned value= " + (currentPosInMs ));
    return currentPosInMs;
  }

  private void clearAdTimeline(long positionInMs, int index)
  {
    if(adTimeline.size() > 0 && adTimeline.get(index).adState.equals("ADSTATE.COMPLETE"))
    {
//      if (positionInMs >= adTimeline.get(0).endTime + 300000) // why ??
//      {
//        adTimeline.remove(0);
//      }
      if (positionInMs >= adTimeline.get(index).endTime)
      {
        //Log.i("JIO-ADS","------ AD REMOVED FROM TIMELINE ------- " + " AD ID " + adTimeline.get(index).adId+" AD TITLE " + adTimeline.get(index).adTitle);
        adTimeline.remove(index);

      }
    }
  }
  private static long programDateTimeForDash = 0;
  public void setPDTimeForDash(long positionInMs){
    //Log.d("Castlab-Test","Program time is assigned");
    programDateTimeForDash = positionInMs;

  }

  public static int TotalAdsInPod()
  {
    return MMSmartStreamingNowtilusSSAIPlugin.adTimeline.size();
  }

  public void reportPlaybackPosition(long positionInMs) { // this function is the glue that is holding everything together,all ad events reporting is dependent on this
    //positionInMs = getSyncPosInMs(positionInMs) ; //getting correct Epoch Time from the player
    if(timeSyncedForDash == false && streamType == "dash" && isLiveStream){
      timeSyncedForDash = true;
      setPDTimeForDash(positionInMs);
    }
    int index = 0;
    int index1 = 0;
    for (; index < adTimeline.size(); index++) {

      if (positionInMs <= adTimeline.get(index).endTime){
        if (positionInMs >= adTimeline.get(index).startTime) {
          isActiveAdPlaying = true;
          activeAdIndex = index;
          index1 = index;
          adTimeline.get(index).active = true;
          mmSSAIAdInfo currentAdInfo = adTimeline.get(index);

          //Log.i("JIO-ADS", "######### CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
          adTimeline.get(index).adCurrentPlaybackTimeInSec = Math.abs(adTimeline.get(index).startTime - positionInMs) / 1000;

          if(cueTimelineEnterSent == false){
            cueTimelineExitSent = false;
            cueTimelineEnterSent = true;
            notifyAdEventsExt("onCueTimelineEnter",currentAdInfo);
          }


          //changes here
          if (currentAdInfo.adTrackerInfo.isAdImpressionSent == false) {
            try {
              currentAdInfo.trackerObj.tracker("impression", currentAdInfo.getImpressionTrackers());

            } catch (IOException e) {
              e.printStackTrace();
            }
            //Log.i("JIO-ADS", "######### IMPRESSION FOR CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
            notifyAdEventsExt("impression", adTimeline.get(index));
            adTimeline.get(index).adTrackerInfo.isAdImpressionSent = true;

          }
          //change
          if (currentAdInfo.adTrackerInfo.isAdStartSent == false) {

            try {
              currentAdInfo.trackerObj.tracker("start", currentAdInfo.getStartTrackers());
              //Log.i("JIO-ADS " , "********* CURRENT AD Title **************** " + currentAdInfo.adTitle);

            } catch (IOException e) {
              e.printStackTrace();
            }
            //Log.i("JIO-ADS", "######### START CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
            notifyAdEventsExt("start", adTimeline.get(index));
            adTimeline.get(index).adTrackerInfo.isAdStartSent = true;
            adTimeline.get(index).adTrackerInfo.isAdCompleteSent = false;

          }

          // AD Progress

          notifyAdEventsExt("progress", adTimeline.get(index));

          //For First Quartile

          if (positionInMs >= adTimeline.get(index).firstQuartile && adTimeline.get(index).adTrackerInfo.isFirstQuartileSent == false) {

            try {

              currentAdInfo.trackerObj.tracker("firstQuartile", currentAdInfo.getFirstQuartileTrackers());
              //Log.i("JIO-ADS " , "********* FIRST CURRENT AD Title **************** " + currentAdInfo.adTitle);


            } catch (IOException e) {
              e.printStackTrace();
            }
            //Log.i("JIO-ADS", "######### FIRST CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
            notifyAdEventsExt("firstQuartile", adTimeline.get(index));
            adTimeline.get(index).adTrackerInfo.isFirstQuartileSent = true;

          }

          //for second Quartile change


          if (positionInMs >= adTimeline.get(index).midPoint && currentAdInfo.adTrackerInfo.isMidQuartileSent == false) {

            try {
              currentAdInfo.trackerObj.tracker("midpoint", currentAdInfo.getMidpointTrackers());
              //Log.i("JIO-ADS " , "********* CURRENT AD Title **************** " + currentAdInfo.adTitle);

            } catch (IOException e) {
              e.printStackTrace();
            }
            //Log.i("JIO-ADS", "######### SECOND CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
            notifyAdEventsExt("midpoint", adTimeline.get(index));
            adTimeline.get(index).adTrackerInfo.isMidQuartileSent = true;

          }

          //Third quartile changes

          if (positionInMs >= adTimeline.get(index).thirdQuartile && currentAdInfo.adTrackerInfo.isThirdQuartileSent == false) {

            try {
              currentAdInfo.trackerObj.tracker("thirdQuartile", currentAdInfo.getThirdQuartileTrackers());
              //Log.i("JIO-ADS " , "********* CURRENT AD Title **************** " + currentAdInfo.adTitle);

            } catch (IOException e) {
              e.printStackTrace();
            }
            //Log.i("JIO-ADS", "######### THIRD CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
            notifyAdEventsExt("thirdQuartile", adTimeline.get(index));
            adTimeline.get(index).adTrackerInfo.isThirdQuartileSent = true;

          }

          if (positionInMs >= (adTimeline.get(index).endTime - 200)) {
            adTimeline.get(index).adState = "ADSTATE.COMPLETE";
            isActiveAdPlaying = false;
            adTimeline.get(index).active = false;

            if (currentAdInfo.adTrackerInfo.isAdCompleteSent == false) {

              try {
                currentAdInfo.trackerObj.tracker("complete", currentAdInfo.getCompleteTrackers());
                //Log.i("JIO-ADS " , "********* CURRENT AD Title **************** " + currentAdInfo.adTitle);
              } catch (IOException e) {
                e.printStackTrace();
              }
              //Log.i("JIO-ADS", "######### COMPLETE CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());
              notifyAdEventsExt("complete", adTimeline.get(index));
              adTimeline.get(index).adTrackerInfo.setAdComplete();

              //clearAdTimeline(positionInMs,index);
            }

          }


        }
      }

      if(isActiveAdPlaying == true && adTimeline.get(index).active == true && positionInMs >= (adTimeline.get(index).endTime)) // adTimeline.get(index).endTime - 200
      {
        adTimeline.get(index).adState = "ADSTATE.COMPLETE";
        isActiveAdPlaying = false;
        adTimeline.get(index).active = false;

        if(adTimeline.get(index).adTrackerInfo.isAdCompleteSent == false)
        {
          try {
            adTimeline.get(index).trackerObj.tracker("complete",adTimeline.get(index).getCompleteTrackers());
            //Log.i("JIO-ADS", "######### CURRENT AD ID ######### " + currentAdInfo.getAdId()  + "######### CURRENT AD TITLE ######### " + currentAdInfo.getAdTitle());


          } catch (IOException e) {
            e.printStackTrace();
          }
          //Log.i("JIO-ADS","COMPLETED  " + adTimeline.get(index).getAdId());
          notifyAdEventsExt("complete",adTimeline.get(index));
          adTimeline.get(index).adTrackerInfo.setAdComplete();

          if((cueTimelineEnterSent == true && cueTimelineExitSent == false && (index1 == adTimeline.size()-1))||(cueTimelineEnterSent == true && cueTimelineExitSent == false && !isLiveStream &&( (index == adTimeline.size()-1)||(adTimeline.get(index).endTime < adTimeline.get(index+1).startTime)))){
            if("dash".equals(streamType)) prev_ended_at = 0;
            //Log.i("JIO-DEBUG", String.valueOf(index1) + "    "+ adTimeline.size());
            cueTimelineEnterSent = false;
            cueTimelineExitSent = true;
            Log.i("APP","onCueTimeLineExit");
            notifyAdEventsExt("onCueTimelineExit",adTimeline.get(index)); //call back
          }

          clearAdTimeline(positionInMs,index);
        }
      }
    }
  }

  public static boolean isAdPlaying()
  {
    return isActiveAdPlaying;
  }


  public void addListener(MMSSAIEventsListeners listeners){
    if(mmssaiEventsCollector != null){
      mmssaiEventsCollector.addListener(listeners);
    }
  }

  public void removeListener(MMSSAIEventsListeners listeners){
    if(mmssaiEventsCollector != null){
      mmssaiEventsCollector.removeListener(listeners);
    }
  }

  private static void notifyLiveAdCueTimelineUpdate(){
    // Do specific to live
    notifyAdCueTimelineUpdate();
  }

  private static void notifyAdCueTimelineUpdate (){
    mmAdTimelineInfo timelineInfo = new mmAdTimelineInfo();
    if(adTimeline.size() > 0) {
      for (int i = 0; i < adTimeline.size(); i++) {
        // Create External AdInfo
        mmAd adInfo = new mmAd();
        adInfo.adId = adTimeline.get(i).getAdId();
        adInfo.adDuration = adTimeline.get(i).getAdDuration();
        adInfo.adTitle = adTimeline.get(i).getAdTitle();
        adInfo.streamType = streamType;
        adInfo.position = adTimeline.get(i).getPosition();
        adInfo.adIndex = adTimeline.get(i).getAdIndex();
        adInfo.startTimeInMs = adTimeline.get(i).getStartTime();
        adInfo.startPos = adTimeline.get(i).getStartPos();
        adInfo.endPos = adTimeline.get(i).getEndPos();
        // ADDing extra info
        adInfo.totalAds=adTimeline.get(i).getTotalAds();
        adInfo.adSkipOffset=adTimeline.get(i).getAdSkipOffset();
        adInfo.impressionTrackers =adTimeline.get(i).getImpressionTrackers();
        adInfo.startTrackers =adTimeline.get(i).getStartTrackers();
        adInfo.firstQuartileTrackers =adTimeline.get(i).getFirstQuartileTrackers();
        adInfo.midpointTrackers=adTimeline.get(i).getMidpointTrackers();
        adInfo.thirdQuartileTrackers = adTimeline.get(i).getThirdQuartileTrackers();
        adInfo.completeTrackers = adTimeline.get(i).getCompleteTrackers();
        adInfo.clickThroughURLs =adTimeline.get(i).getClickThroughURLs();
        adInfo.clickTrackingURLs=adTimeline.get(i).getClickTrackingURLs();
        timelineInfo.adInfos.add(adInfo);
      }
      timelineInfo.totalAds = adTimeline.size();
      //Log.i("JIO-ADS","SIZE    " + timelineInfo.totalAds);
      if (mmssaiEventsCollector != null) {
        mmssaiEventsCollector.notifyExternalAdCueTimelineUpdate(timelineInfo);
      }
    }
  }

  private void notifyAdEventsExt(String eventName, mmSSAIAdInfo ssaiAdInfo){
    if(eventName != null && !eventName.isEmpty() && (ssaiAdInfo != null)){
      // Create External AdInfo
      mmAd adInfo = new mmAd();
      adInfo.adId = ssaiAdInfo.getAdId();
      adInfo.adCurrentPlaybackTimeInSec = ssaiAdInfo.getAdCurrentPlaybackTimeInSec();
      adInfo.adCreativeId = ssaiAdInfo.getAdCreativeId();
      adInfo.adDuration = ssaiAdInfo.getAdDuration();
      adInfo.adServer = ssaiAdInfo.getAdServer();
      adInfo.adTitle = ssaiAdInfo.getAdTitle();
      adInfo.isLinear = ssaiAdInfo.isLinear;
      adInfo.streamType = streamType;
      adInfo.position = ssaiAdInfo.getPosition();
      adInfo.adIndex = ssaiAdInfo.getAdIndex();
      adInfo.totalAds = ssaiAdInfo.getTotalAds();
      //ADDing extra info
      adInfo.impressionTrackers =ssaiAdInfo.getImpressionTrackers();
      adInfo.adSkipOffset=ssaiAdInfo.getAdSkipOffset();
      adInfo.completeTrackers=ssaiAdInfo.getCompleteTrackers();
      adInfo.thirdQuartileTrackers=ssaiAdInfo.getThirdQuartileTrackers();
      adInfo.midpointTrackers= ssaiAdInfo.getMidpointTrackers();
      adInfo.firstQuartileTrackers=ssaiAdInfo.getFirstQuartileTrackers();
      adInfo.startTrackers= ssaiAdInfo.getStartTrackers();
      adInfo.clickTrackingURLs=ssaiAdInfo.getClickTrackingURLs();
      adInfo.clickThroughURLs = ssaiAdInfo.getClickThroughURLs();
      // Log.d("AD_EVENT","NOTIFY "+adInfo.adId+" -> "+"duration "+adInfo.adDuration);
      // Notify to external Listeners
      if(mmssaiEventsCollector != null) {
        mmssaiEventsCollector.notifyExternalAdEvents(eventName, adInfo);
      }
    }
  }

  public static int updateDashLiveAdTimeline(long startTimeInMS,String vastURL )
  {

    if(vastURL != null){
      fetchNowtilusDashLiveVastInfo(vastURL,startTimeInMS);
    }

    return 0;
  }

  public static  void addAdTimeline(mmSSAIAdInfo mmSSAIAdInfo,int index)
  {

    if(mmSSAIAdInfo.trackerObj.getAdSystem().size() > 0)
    {
      mmSSAIAdInfo.adServer = mmSSAIAdInfo.trackerObj.getAdSystem().get(index);
    }

    if(mmSSAIAdInfo.trackerObj.getAdTitle().size() > 0)
    {
      mmSSAIAdInfo.adTitle = mmSSAIAdInfo.trackerObj.getAdTitle().get(index);
      //Log.i("JIO-ADS " , "AD title " + mmSSAIAdInfo.adTitle);
    }
    adTimeline.add(index,mmSSAIAdInfo);
  }

}
