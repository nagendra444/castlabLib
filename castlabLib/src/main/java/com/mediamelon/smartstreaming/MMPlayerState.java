package com.mediamelon.smartstreaming;

/**
 * Media player state
 */
public enum MMPlayerState{
  /**
   * Player is playing available content to the screen. Delays due to rebuffering are still considered PLAYING.
   */
  PLAYING(1),
  /**
   * Player is paused
   */
  PAUSED(2),

  /**
   * Playback is stopped, eithre due to user request or reaching the end of the content. 
   * <p><p><b>Note:</b> When the player enters the STOPPED state the current video session is terminated. 
   * Re-entering the PLAYING state will result in a new session being logged for the video. 
   * The most common reason for this occurring is when a video plays to the end and then the 
   * user seeks back to a point earlier in the video.
   */
  STOPPED(3);

  // Constructor
  private MMPlayerState(final Integer state) {
    this.state = state;
  }

  // Internal state
  private int state;

  public int getState() {
    return state;
  }
}
