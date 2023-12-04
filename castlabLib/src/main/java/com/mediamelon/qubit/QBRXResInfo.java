package com.mediamelon.qubit;

/**
 * Created by rupesh on 17/2/18.
 */

public class QBRXResInfo {
    public QBRXResInfo(){
        Reset();
    }

    public void Reset(){
        cfVal = 0.0;
        maxStepsDown = maxStepsUp = 0;
        mode = "";
    }
    public Double cfVal;
    public Integer maxStepsUp;
    public Integer maxStepsDown;
    public String mode;
}
