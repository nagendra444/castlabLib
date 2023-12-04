package com.mediamelon.smartstreaming;

/**
 * Observer to look for the status of async processing of initialization of SDK
 */
public interface MMSmartStreamingObserver{
  /**
   * Callback method to tell the status of the completion of the initialisation API
   * @param status : status of the completion of the initialisation API
   * @param description : optional description of status accompanying the status
   */
  public void sessionInitializationCompleted(Integer initCmdId, MMSmartStreamingInitializationStatus status, String description);
};
