package com.mediamelon.qubit;

/**
 * Created by rupesh on 23/10/18.
 */

public interface MMSmartStreamingRegistrationObservor {
    public void onRegistrationWithSmartSightCompleted();
    public void onPresentationInformationReceived(Long durationMillisec, boolean isLive, String aStreamFormat,  int aProfileCount, String aMinRes, String aMaxRes, Double aMinFPS, Double aMaxFPS);
    public void onQBRModeFinalised(String mode);
}
