package com.mediamelon.qubit.ep;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by shri on 11/9/15.
 */
public class Diagnostics implements JSONAble {
    public Long sdkBootuptime;
    public Diagnostics() {
        sdkBootuptime = 333L;
    }
    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sdkBootuptime",sdkBootuptime);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
