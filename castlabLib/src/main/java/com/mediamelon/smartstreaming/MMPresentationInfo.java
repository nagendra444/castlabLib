package com.mediamelon.smartstreaming;

import java.util.ArrayList;

/**
 * Tells the QBR SmartStreaming engine which representations that the player can present. 
 * Representations that are not in this list will not be selected by the QBR SmartStreaming engine.
 * @param isLive True if presentation is live; false if presentation is VOD
 * @param duration Content length in milliseconds. Use -1 for live
 * @param representations Array of MMRepresentation objests that are selected by the player for playback
 * @return The MMPresentationInfo object
 * @see MMRepresentation
 */
public class MMPresentationInfo{
  public MMPresentationInfo(){
    isLive = false;
    duration = -1L;
  }

  /**
   * True if presentation is live; false if presentation is VOD
   */
  public boolean isLive;

  /**
   * Content length in milliseonds. Use -1 for live.
   */
  public Long duration;

  /**
   * Array of <b>MMRepresentation<b> objects that are selected by the player for the playback.
   */
  public ArrayList<MMRepresentation> representations = new ArrayList<MMRepresentation>();
}
