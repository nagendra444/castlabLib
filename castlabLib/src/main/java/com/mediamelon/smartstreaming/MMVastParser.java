package com.mediamelon.smartstreaming;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MMVastParser {

  private  final String MM_ROOT_TAG = "MMVastParser";
  private  final String MM_ROOT_TAG_START = "<" + MM_ROOT_TAG + ">";
  private  final String MM_ROOT_TAG_END = "</" + MM_ROOT_TAG + ">";

  enum VastElem {
    IMPRESSION_TRACKER ("Impression"),
    VIDEO_TRACKER ("Tracking"),
    CLICK_THROUGH ("ClickThrough"),
    CLICK_TRACKER ("ClickTracking"),
    MEDIA_FILE ("MediaFile"),
    VAST_AD_TAG ("VASTAdTagURI"),
    MP_IMPRESSION_TRACKER ("MP_TRACKING_URL"),
    DURATION ("Duration"),
    ERROR ("Error"),
    AD_SYSTEM ("AdSystem"),
    AD_TITLE ("AdTitle");

    private final String name;

    private VastElem(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  };

  enum VastElemAttributeName {
    EVENT ("event");

    private final String name;

    private VastElemAttributeName(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  };


  enum VastElemAttributeValue {
    START ("start"),
    FIRST_QUARTILE ("firstQuartile"),
    MIDPOINT ("midpoint"),
    THIRD_QUARTILE ("thirdQuartile"),
    COMPLETE ("complete");

    private final String value;

    private VastElemAttributeValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  };

  private Document mVastDoc;
  private JSONObject mJsonObject;
  public void MMVastParser()
  {
    mVastDoc = null;
    mJsonObject = null;
  }

  public void parseJson(JSONObject inJObject){
    if(inJObject != null){
      mJsonObject = inJObject;
    }
  }

  public void parseVast(String xmlString) throws ParserConfigurationException, IOException, SAXException {

    xmlString = xmlString.replaceFirst("<\\?.*\\?>", "");

    String documentString = MM_ROOT_TAG_START + xmlString + MM_ROOT_TAG_END;

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setCoalescing(true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    mVastDoc = documentBuilder.parse(new InputSource(new StringReader(documentString)));
  }

  /**
   *
   * @param id  - id of the currently playing AD
   * @return the list of the  impression  click tracking urls
    */

  public List<String> getImpressionURLsForCurrentAD(String id)
  {
    Element AD_Node = mVastDoc.getElementById(id);
    NodeList impressionNode   = AD_Node.getElementsByTagName("Impression");
    List<String> listOfTrackers = new ArrayList<>();
    for(int i=0;i< impressionNode.getLength();i++)
    {
      try {
        listOfTrackers.add(impressionNode.item(i).getFirstChild().getNodeValue());
      }
      catch (Exception e )
      {
       Log.e("MM","Exception while getting the impression trackers " + e.getMessage());
      }
    }
    return listOfTrackers;
  }

  /**
   *
   * @param id - id of the currently playing AD
   * @param event event name(examples are start, firstQuartile etc, except impression) for which the click trackers are to be fetched
   * @return the list of click trackers
   */
  public List<String> getTrackingURLsForCurrentAD(String id,String event)
  {
    Element AD_Node = mVastDoc.getElementById(id);
    NodeList trackingNode = AD_Node.getElementsByTagName("Tracking");
    List<String> listOfTrackers  = new ArrayList<>();
    for(int j = 0 ;j< trackingNode.getLength();j++)
    {
      if(trackingNode.item(j).getAttributes()!=null && trackingNode.item(j).getAttributes().getNamedItem("event")!=null && trackingNode.item(j).getAttributes().getNamedItem("event").getNodeValue()!=null && event.equals(trackingNode.item(j).getAttributes().getNamedItem("event").getNodeValue()))
      {
        String url = trackingNode.item(j).getFirstChild().getNodeValue();
        listOfTrackers.add(url);
      }
    }
    return listOfTrackers;
  }

    /**
     *
     * @param id  = the AD id of the current AD
     * @return the skipOffset
     */
  public int getSkipoffsetInSecsForCurrentAD(String id)
  {
    Element AD_Node = mVastDoc.getElementById(id);
    NodeList trackingNode = AD_Node.getElementsByTagName("Linear");
    String offset =  null;
    if(trackingNode!=null && trackingNode.item(0).getAttributes()!=null &&  trackingNode.item(0).getAttributes().getNamedItem("skipoffset")!=null) {
        offset = trackingNode.item(0).getAttributes().getNamedItem("skipoffset").getNodeValue();
        String[] offset_array = offset.split(":");
        int hr = Integer.parseInt(offset_array[0]) * 60 * 60;
        int min = Integer.parseInt(offset_array[1]) * 60;
        int secs = Integer.parseInt(offset_array[2]) * 1;
        return hr + min + secs;
    }
    else
        return -1;
  }

  /**
   *
   * @param id - id of the current playing AD
   * @return the list of click through URLS
   */
  public List<String> getClickThroughURLsForCurrentAD(String id)
  {
    List<String> clickThroughURLs = new ArrayList<>();
    Element AD_Node = mVastDoc.getElementById(id);
    NodeList trackingNode = AD_Node.getElementsByTagName("ClickThrough");
    // trackingNode.item(0).getFirstChild().getNodeValue()
    for(int i=0; i<trackingNode.getLength();i++)
    {
      if(trackingNode.item(i)!=null && trackingNode.item(i).getFirstChild()!=null && trackingNode.item(i).getFirstChild().getNodeValue()!=null)
      {
        clickThroughURLs.add(trackingNode.item(i).getFirstChild().getNodeValue());
      }
    }
    return  clickThroughURLs;
  }


  public List<String> getClickTrackingURLsForCurrentAD(String id)
  {
    List<String> clickTrackingURLs = new ArrayList<>();
    Element AD_Node = mVastDoc.getElementById(id);
    NodeList trackingNode = AD_Node.getElementsByTagName("ClickTracking");
    // trackingNode.item(0).getFirstChild().getNodeValue()
    for(int i=0; i<trackingNode.getLength();i++)
    {
      if(trackingNode.item(i)!=null && trackingNode.item(i).getFirstChild()!=null && trackingNode.item(i).getFirstChild().getNodeValue()!=null)
      {
        clickTrackingURLs.add(trackingNode.item(i).getFirstChild().getNodeValue());
      }
    }
    System.out.println();
    return  clickTrackingURLs;
  }


  String getVastAdTagURI() {
    List<String> uriWrapper = getStringDataAsList(VastElem.VAST_AD_TAG);
    return (uriWrapper.size() > 0) ? uriWrapper.get(0) : null;
  }


  public List<String> getImpressionTrackers() {
    List<String> impressionTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      impressionTrackers = getStringDataAsList(VastElem.IMPRESSION_TRACKER);
      impressionTrackers.addAll(getStringDataAsList(VastElem.MP_IMPRESSION_TRACKER));
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.has("impressionUrlTemplates")) {
          JSONArray impressionUrlTemplates = mJsonObject.getJSONArray("impressionUrlTemplates");
          if (impressionUrlTemplates != null) {
            for (int i = 0; i < impressionUrlTemplates.length(); i++) {
              impressionTrackers.add(impressionUrlTemplates.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return impressionTrackers;
  }

  public  List<String> getAdTitle() {
    ArrayList<String> result = new ArrayList<String>();
    if(mVastDoc != null) {
      return getStringDataAsList(VastElem.AD_TITLE);
    }else if (mJsonObject != null){
      try {
        result.add(mJsonObject.getString("title"));
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return result;
  }

  public  List<String> getAdSystem() {
    ArrayList<String> result = new ArrayList<String>();
    if(mVastDoc != null) {
      return getStringDataAsList(VastElem.AD_SYSTEM);
    }else if (mJsonObject != null){
      result.add("nowtilus");
    }
    return result;
  }

  List<String> getCreativeId() {
    ArrayList<String> result = new ArrayList<String>();
    if(mVastDoc != null) {
      NodeList nodes = mVastDoc.getElementsByTagName("Creative");
      if (nodes.getLength() > 0) {
        Node node = nodes.item(0);
        String creative_id = node.getAttributes().getNamedItem("id").toString();
        String[] creative_list = creative_id.split("=");
        String c_id = creative_list[1];
        result.add(c_id.trim());
        return result;
      }
    }
    return result;
  }

  List<String> getRootId() {
    ArrayList<String> result = new ArrayList<String>();
    if(mVastDoc != null) {
      NodeList nodes = mVastDoc.getElementsByTagName("root");
      if (nodes.getLength() > 0) {
        Node node = nodes.item(0);
        String creative_id = node.getAttributes().getNamedItem("id").toString();
        String[] creative_list = creative_id.split("=");
        String c_id = creative_list[1];
        result.add(c_id.trim());
        return result;
      }
    }
    return result;
  }


  public  List<String> getAdId() {
    ArrayList<String> result = new ArrayList<String>();
    if(mVastDoc != null) {
      NodeList nodes = mVastDoc.getElementsByTagName("Ad");
//      if (nodes.getLength() > 0) {
//        Node node = nodes.item(0);
//        String ad_id = node.getAttributes().getNamedItem("id").getNodeValue().trim();
//        result.add(ad_id.trim());
//        return result;
//      }
      for(int i=0;i<nodes.getLength();i++)
      {
        Node node = nodes.item(i);
        String ad_id = node.getAttributes().getNamedItem("id").getNodeValue().trim();
        result.add(ad_id.trim());
      }
    }else if(mJsonObject != null){
      try {
        result.add(mJsonObject.getString("titleId"));
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return result;
  }

  List<String> getVideoErrorTrackers() {
    List<String> errorTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      errorTrackers = getStringDataAsList(VastElem.ERROR);
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.getJSONObject("trackingEvents").has("error")) {
          JSONArray trackingUrlTemplates = mJsonObject.getJSONObject("trackingEvents").getJSONArray("error");
          if (trackingUrlTemplates != null) {
            for (int i = 0; i < trackingUrlTemplates.length(); i++) {
              errorTrackers.add(trackingUrlTemplates.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return errorTrackers;
  }

  List<String> getDuration() {
    ArrayList<String> result = new ArrayList<String>();
    if(mVastDoc != null) {
      return getStringDataAsList(VastElem.DURATION);
    }
    else if(mJsonObject != null){
      try {
        result.add(mJsonObject.getString("duration"));
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return result;
  }

  List<String> getVideoStartTrackers() {
    List<String> startTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      startTrackers = getVideoTrackerByAttribute(VastElemAttributeValue.START);
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.getJSONObject("trackingEvents").has("start")) {
          JSONArray trackingEventUrls = mJsonObject.getJSONObject("trackingEvents").getJSONArray("start");
          if (trackingEventUrls != null) {
            for (int i = 0; i < trackingEventUrls.length(); i++) {
              startTrackers.add(trackingEventUrls.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return startTrackers;
  }

  public  List<String> getVideoFirstQuartileTrackers() {
    List<String> firstQuartileTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      firstQuartileTrackers = getVideoTrackerByAttribute(VastElemAttributeValue.FIRST_QUARTILE);
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.getJSONObject("trackingEvents").has("firstQuartile")) {
          JSONArray trackingEventUrls = mJsonObject.getJSONObject("trackingEvents").getJSONArray("firstQuartile");
          if (trackingEventUrls != null) {
            for (int i = 0; i < trackingEventUrls.length(); i++) {
              firstQuartileTrackers.add(trackingEventUrls.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return firstQuartileTrackers;
  }

  public  List<String> getVideoMidpointTrackers() {
    List<String> midpointTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      midpointTrackers = getVideoTrackerByAttribute(VastElemAttributeValue.MIDPOINT);
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.getJSONObject("trackingEvents").has("midpoint")) {
          JSONArray trackingEventUrls = mJsonObject.getJSONObject("trackingEvents").getJSONArray("midpoint");
          if (trackingEventUrls != null) {
            for (int i = 0; i < trackingEventUrls.length(); i++) {
              midpointTrackers.add(trackingEventUrls.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return midpointTrackers;
  }

  public  List<String> getVideoThirdQuartileTrackers() {
    List<String> thirdQuartileTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      thirdQuartileTrackers = getVideoTrackerByAttribute(VastElemAttributeValue.THIRD_QUARTILE);
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.getJSONObject("trackingEvents").has("thirdQuartile")) {
          JSONArray trackingEventUrls = mJsonObject.getJSONObject("trackingEvents").getJSONArray("thirdQuartile");
          if (trackingEventUrls != null) {
            for (int i = 0; i < trackingEventUrls.length(); i++) {
              thirdQuartileTrackers.add(trackingEventUrls.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return thirdQuartileTrackers;
  }

  public  List<String> getVideoCompleteTrackers() {
    List<String> completeTrackers = new ArrayList<String>();
    if(mVastDoc != null) {
      completeTrackers = getVideoTrackerByAttribute(VastElemAttributeValue.COMPLETE);
    }else if (mJsonObject != null){
      try {
        if(mJsonObject.getJSONObject("trackingEvents").has("complete")) {
          JSONArray trackingEventUrls = mJsonObject.getJSONObject("trackingEvents").getJSONArray("complete");
          if (trackingEventUrls != null) {
            for (int i = 0; i < trackingEventUrls.length(); i++) {
              completeTrackers.add(trackingEventUrls.getString(i));
            }
          }
        }
      }catch (JSONException e){
        e.printStackTrace();
      }
    }
    return completeTrackers;
  }

  String getClickThroughUrl() {
    List<String> clickUrlWrapper = getStringDataAsList(VastElem.CLICK_THROUGH);
    return (clickUrlWrapper.size() > 0) ? clickUrlWrapper.get(0) : null;
  }

  List<String> getClickTrackers() {
    return getStringDataAsList(VastElem.CLICK_TRACKER);
  }

  String getMediaFileUrl() {
    List<String> urlWrapper = getStringDataAsList(VastElem.MEDIA_FILE);
    return (urlWrapper.size() > 0) ? urlWrapper.get(0) : null;
  }

  private  List<String> getVideoTrackerByAttribute(VastElemAttributeValue attributeValue) {
    return getStringDataAsList(VastElem.VIDEO_TRACKER, VastElemAttributeName.EVENT, attributeValue);
  }

  private  List<String> getStringDataAsList(VastElem elementName) {
    return getStringDataAsList(elementName, null, null);
  }

  private  List<String> getStringDataAsList(VastElem elementName, VastElemAttributeName attributeName, VastElemAttributeValue attributeValue) {
    ArrayList<String> results = new ArrayList<String>();

    if (mVastDoc == null) {
      return results;
    }

    NodeList nodes = mVastDoc.getElementsByTagName(elementName.getName());

    if (nodes == null) {
      return results;
    }

    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);

      if (node != null && nodeMatchFilter(node, attributeName, attributeValue)) {
        Node textChild = node.getFirstChild();
        if (textChild != null) {
          String textValue = textChild.getNodeValue();
          if (textValue != null) {
            results.add(textValue.trim());
          }
        }
      }
    }

    return results;
  }

  private  boolean nodeMatchFilter(Node node, VastElemAttributeName attributeName, VastElemAttributeValue attributeValue) {
    if (attributeName == null || attributeValue == null) {
      return true;
    }

    NamedNodeMap attrMap = node.getAttributes();
    if (attrMap != null) {
      Node attrNode = attrMap.getNamedItem(attributeName.getName());
      if (attrNode != null && attributeValue.getValue().equals(attrNode.getNodeValue())) {
        return true;
      }
    }

    return false;
  }

  public  void tracker(String event,List<String> URLs) throws IOException {

    new Thread(new Runnable() {
      @Override
      public void run() {
        // Do network action in this function

        for (int i=0;i<URLs.size();i++)
        {
              StringBuilder result = new StringBuilder();
              URL url = null;
              try {
                String urlToTrack = URLs.get(i);

                if(urlToTrack != null && !urlToTrack.isEmpty()) {
                  try {
                    urlToTrack = URLDecoder.decode(urlToTrack, "UTF-8");
                  } catch (UnsupportedEncodingException e) {
                    // not going to happen - value came from JDK's own StandardCharsets
                  }
                  urlToTrack = urlToTrack.replaceAll("\\\\", "");
                  url = new URL(urlToTrack);
                }else{
                  continue;
                }
              } catch (MalformedURLException e) {
                e.printStackTrace();
              }
              HttpURLConnection conn = null;
              try {
                if(url == null) continue;
                conn = (HttpURLConnection) url.openConnection();
              } catch (IOException e) {
                e.printStackTrace();
              }
              try {
                conn.setRequestMethod("GET");
              } catch (ProtocolException e) {
                e.printStackTrace();
              }

              try {

                if(conn.getResponseCode() == 200)
                    {
                      System.out.println("Track success for event: "+event);
                      Log.i("MM","Track success for event: "+event + " URL " + url );
                    }
              } catch (IOException e) {
                e.printStackTrace();
              }

        }
      }
    }).start();

  }

}

