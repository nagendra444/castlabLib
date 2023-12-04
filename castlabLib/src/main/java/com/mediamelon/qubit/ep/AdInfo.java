package com.mediamelon.qubit.ep;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by rupesh on 18/6/18.
 */

public class AdInfo implements JSONAble {
     //next five fields added by abhinav
//    public String adResolution;
//    public String adCreativeId;
//    public String adUrl;
//    public String adTitle;
//    public int adBitrate;

    String adClient;
    String adId;
    Double adDuration;
    Double adInterval;
    String adPosition;
    String adLinear;
    String adCreative;
    String adSystem;
    Boolean isBumper;
    //ag
    String adCreativeId;
    String adResolution;
    String adUrl;
    String adTitle;
    int adBitrate;

    int    adPodPosition;
    int    adPodIndex;
    int    adPodLength;
    //added by abhinav

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("adClient",adClient);
            jsonObject.put("adId",adId);
            jsonObject.put("adDuration",adDuration);
            jsonObject.put("adInterval",adInterval);
            jsonObject.put("adPosition",adPosition);
            jsonObject.put("adLinear",adLinear);
            jsonObject.put("adResolution",adResolution);
            jsonObject.put("adCreativeType",adCreative);
            jsonObject.put("adSystem",adSystem);
            jsonObject.put("adPodIndex",adPodIndex);
            jsonObject.put("adPodLength",adPodLength);
            jsonObject.put("adPodPosition",adPodPosition);
            jsonObject.put("isBumper",isBumper);
            //ag
            jsonObject.put("adCreativeId",adCreativeId);
            jsonObject.put("adUrl",adUrl);
            jsonObject.put("adTitle",adTitle);
            jsonObject.put("adBitrate",adBitrate);

        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
