package com.mediamelon.qubit.ep;

import com.mediamelon.qubit.MMQFQubitEngine;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by shri on 11/9/15.
 */
public class SDKInfo implements JSONAble {
    public String sdkVersion;
    public String hFileVersion;
    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sdkVersion",sdkVersion);
            jsonObject.put("hFileVersion",hFileVersion);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
