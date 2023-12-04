/**
 * Created by shri on 20/5/15.
 */
package com.mediamelon.qubit.ep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class QBRMetric implements JSONAble {
    public StreamID streamID;
    public StreamInfo streamInfo;
    //ag
    public ContentMetadata contentMetadata;
    public CopyOnWriteArrayList<SegmentInfo> segInfo;
    public ClientInfo clientInfo;
    public CopyOnWriteArrayList<PlaybackInfo> pbInfo;
    public SDKInfo sdkInfo;
    public AdInfo adInfo;
    public Diagnostics diagnostics;
    public PBEventInfo pbEventInfo;
    public HashMap<String, String> customTags;
    public QBRMetric() {
        streamInfo = null;
        segInfo = new CopyOnWriteArrayList<>();
        clientInfo = null;
        pbInfo = new CopyOnWriteArrayList<>();
        streamID = null;
        //ag
        contentMetadata = null;
        sdkInfo = null;
        diagnostics = null;
        pbEventInfo = null;
        customTags = new HashMap<String, String>();
    }
    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if(streamID != null) jsonObject.put("streamID",streamID.getJSONObject());
            //ag
            if(contentMetadata != null) jsonObject.put("contentMetadata",contentMetadata.getJSONObject());
            if(streamInfo != null) jsonObject.put("streamInfo",streamInfo.getJSONObject());
            if(segInfo != null) {
                JSONArray segArr = new JSONArray();
                for (SegmentInfo segObj : segInfo) {
                    segArr.put(segObj.getJSONObject());
                }
                jsonObject.put("segInfo", segArr);
            }
            if(clientInfo != null) jsonObject.put("clientInfo",clientInfo.getJSONObject());
            if(pbInfo != null) {
                JSONArray pbInfoArr = new JSONArray();
                for (PlaybackInfo pbObj : pbInfo) {
                    pbInfoArr.put(pbObj.getJSONObject());
                }
                jsonObject.put("pbInfo", pbInfoArr);
            }
            if(sdkInfo != null) jsonObject.put("sdkInfo",sdkInfo.getJSONObject());
            if(adInfo != null) jsonObject.put("adInfo",adInfo.getJSONObject());
            if(diagnostics != null) jsonObject.put("diagnostics",diagnostics.getJSONObject());
            if(pbEventInfo != null) jsonObject.put("pbEventInfo",pbEventInfo.getJSONObject());
            if(!customTags.isEmpty()) {
                JSONObject custTagObj = new JSONObject(customTags);
                jsonObject.put("customTags",custTagObj);
            }

        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
