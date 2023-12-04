package com.mediamelon.qubit.ep;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by shri on 21/5/15.
 */
/*
    This is the main record structure that JSON will be coming in.
    In future keep adding the type of records we want StreamProducer to process
    making the fields access as public since we want it to access from JSON converter
    and do not want the getter setter for no purpose
 */
public class RecordStructure implements JSONAble {
    public List<QBRMetric> qubitData;
    public Integer interval;
    public Integer playDur;
    public Double pbTime;
    public Long timestamp;
    public String version;

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray qubitDataObjs = new JSONArray();
            for(QBRMetric qm: qubitData) {
                qubitDataObjs.put(qm.getJSONObject());
            }
            jsonObject.put("qubitData",qubitDataObjs);
            jsonObject.put("interval",interval);
            jsonObject.put("playDur",playDur);
            jsonObject.put("pbTime",pbTime);
            jsonObject.put("timestamp",timestamp);
            jsonObject.put("version",version);
        } catch (JSONException e) {

        }
        return jsonObject;
    }
    public String toJson() {
        return getJSONObject().toString();
    }
}