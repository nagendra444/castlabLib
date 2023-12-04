/**
 * Created by shri on 20/5/15.
 */
package com.mediamelon.qubit.ep;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class SegmentInfo implements JSONAble {
    public Long timestamp; //millisec
    public String res;
    public String qbrRes;
    public Long qbrBitrate; //bps
    public Long cbrBitrate; //bps
    public Double qbrQual;
    public Double cbrQual;
    public Double dur;  //seconds
    public Integer seqNum;
    public Double startTime;
    public Double fps;
    public Integer profileNum;
    public Integer cbrProfileNum;
    public Integer cbrSize;
    public Integer qbrSize;
    public String vCodec;
    public String aCodec;
    public Double downloadRate;
    public Long bufferLength;

    @Override
    public String toString(){
        return "SegInfo{ res: "+res+", qbrBitrate: "+qbrBitrate+", cbrBitrate: "+cbrBitrate+", cbrQual: "+
                cbrQual+", qbrQual: "+qbrQual+", dur: "+dur+", seqNum: "+seqNum+", startTime: "+startTime+
                "cbrSize="+cbrSize+" qbrSize="+qbrSize+" fps="+fps+" profileNum="+profileNum+" downloadRate="+downloadRate+"}";
    }
    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("timestamp",timestamp);
            jsonObject.put("res",res);
            jsonObject.put("qbrRes",qbrRes);
            jsonObject.put("qbrBitrate",qbrBitrate);
            jsonObject.put("cbrBitrate",cbrBitrate);
            jsonObject.put("qbrQual",qbrQual);
            jsonObject.put("cbrQual",cbrQual);
            jsonObject.put("dur",dur);
            jsonObject.put("seqNum",seqNum);
            jsonObject.put("startTime",startTime);
            jsonObject.put("fps",fps);
            jsonObject.put("profileNum",profileNum);
            jsonObject.put("cbrProfileNum",cbrProfileNum);
            jsonObject.put("cbrSize",cbrSize);
            jsonObject.put("qbrSize",qbrSize);
            jsonObject.put("vCodec",vCodec);
            jsonObject.put("aCodec",aCodec);
            jsonObject.put("downloadRate",downloadRate);
            jsonObject.put("bufferLength",bufferLength);
        } catch (JSONException e) {

        }
        return jsonObject;
    }

}
