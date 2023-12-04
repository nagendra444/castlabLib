package com.mediamelon.qubit.ep;

import java.io.Serializable;
/*
Upon adding or updating any member please make sure to update RegisterAPI.getResponseObj() method.
There RegisterResponse oject is created from Json.
 */
public class RegisterResponse implements Serializable {
    public String producerURL = "";
    public Integer statsInterval = -1;
    public String hintFileUrl = "";
    public String mode = "";
    public Double cfVal = 0.0;
    public Integer maxSteps = 0;
    public Integer maxStepsUp = 0;
    public Integer maxStepsDown = 0;
    public Integer telephonyMetricsFetchInterval = 0;
    public Long timestamp = -1L;
    public String hintfileName = "";
    public HintfileMapping metaFileMap;

    @Override
    public String toString() {
        return "producerUrl ="+producerURL+" statsInterval="+statsInterval+" hintFileUrl="+hintFileUrl+
                " mode="+mode+" cfVal="+cfVal+" maxSteps="+maxSteps+" maxStepsUp="+maxStepsUp+" maxStepsDown="+maxStepsDown+
                " hintFileNAme="+hintFileUrl+" timestamp="+timestamp;
    }
}
