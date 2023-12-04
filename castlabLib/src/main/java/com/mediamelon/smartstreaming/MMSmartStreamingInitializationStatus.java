package com.mediamelon.smartstreaming;

/**
 * Status of the Initialization API.
 */
public enum MMSmartStreamingInitializationStatus{
  /**
   * Initialisation not completed yet, and is in pending state
   */
  Pending,
  /**
   * Initialisation completed successfully
   */
  Success,
  /**
   * Initialisation failed
   */
  Failure,
  /**
   * Initialisation Cancelled
   */
  Cancelled
};
