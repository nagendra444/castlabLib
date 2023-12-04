/**
 * Created by shri on 20/5/15.
 */

package com.mediamelon.qubit.ep;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class PlaybackInfo implements JSONAble {
    public Long timestamp; // in milliseconds
    public Double latency; // in milliseconds
    public Double buffWait; // in milliseconds
    public Integer frameloss;
    public Double bwInUse;// in bits per second
    public Double pbTime;// in seconds
    public Double sumBuffWait;
    public Integer upShiftCount;
    public Integer downShiftCount;
    public Long pauseDuration;// in milliseconds

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("timestamp",timestamp);
            jsonObject.put("latency",latency);
            jsonObject.put("buffWait",buffWait);
            jsonObject.put("frameloss",frameloss);
            jsonObject.put("bwInUse",bwInUse);
            jsonObject.put("pbTime",pbTime);
            jsonObject.put("sumBuffWait",sumBuffWait);
            jsonObject.put("upShiftCount",upShiftCount);
            jsonObject.put("downShiftCount",downShiftCount);
            jsonObject.put("pauseDuration",pauseDuration);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
}
