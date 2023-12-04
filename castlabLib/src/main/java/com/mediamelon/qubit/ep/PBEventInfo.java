package com.mediamelon.qubit.ep;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by shri on 11/9/15.
 */
public class PBEventInfo implements JSONAble {
    public String event;
    public String desc;
    public String id;
    public Double pbTime;
    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("event",event);
            jsonObject.put("desc",desc);
            jsonObject.put("id",id);
            jsonObject.put("pbTime",pbTime);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
