/**
 * Created by shri on 20/5/15.
 */
package com.mediamelon.qubit.ep;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class StreamID implements JSONAble {
    public String streamURL;
    public String assetId;
    public String assetName;
    public String videoId;
    public Long custId;
    public String subscriberId;
    public String subscriberType;
    public String subscriberTag;
    public String sessionId;
    public String mode;
    public Boolean isLive;
    public String dataSrc;
    public String playerName;
    public String domainName;
    public String pId;
    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("streamURL", streamURL);
            jsonObject.put("assetId", assetId);
            jsonObject.put("videoId", videoId);
            jsonObject.put("assetName", assetName);
            jsonObject.put("custId", custId);
            jsonObject.put("subscriberId", subscriberId);
            jsonObject.put("subscriberType", subscriberType);
            jsonObject.put("subscriberTag", subscriberTag);
            jsonObject.put("sessionId", sessionId);
            jsonObject.put("mode", mode);
            jsonObject.put("isLive", isLive);
            jsonObject.put("dataSrc", dataSrc);
            jsonObject.put("playerName", playerName);
            jsonObject.put("domainName", domainName);
            jsonObject.put("pId", pId);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
