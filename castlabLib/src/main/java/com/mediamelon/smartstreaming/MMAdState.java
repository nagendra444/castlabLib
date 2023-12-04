package com.mediamelon.smartstreaming;

/**
 * Advertisement playback state
 */
public enum MMAdState{
  /**
   * Ad is requested [initial state]]
   */
  AD_REQUEST(1),

  /**
   * Ad started playing or is unpaused
   */
  AD_PLAYING(2),

  /**
   * Ad is paused
   */
  AD_PAUSED(3),

  /**
   * Ad is skipped [terminal state]
   */
  AD_SKIPPED(4),

  /**
  * Ad completed play [terminal state]
   */
  AD_COMPLETED(5),

  /**
   * Error prevented Ad play [terminal state]
   */
  AD_ERROR(6),

  /**
   * Ad is blocked [terminal state]
   */
  AD_BLOCKED(7),

  /**
   * Based on the IAB definition of an ad impression
   */
  AD_IMPRESSION(8),

  /**
   * VPAID script signaled that it is starting
   */
  AD_STARTED(9),

  /**
   * User clicks an ad to be redirected to its landing page
   */
  AD_CLICKED(10),

  /**
   * Ad playback session resumed
   */
  AD_RESUMED(11),

   /**
   * Ad playback session Started to Play
   */
  AD_PLAY(12),

  AD_BUFFERING(13),

  AD_MIDPOINT(14),

  AD_FIRST_QUARTILE(15),

  AD_THIRD_QUARTILE(16),

  AD_ENDED(17);

  // Constructor
  private MMAdState(final Integer state) {
    this.state = state;
  }

  // Internal state
  private int state;

  public int getAdState() {
    return state;
  }
}
