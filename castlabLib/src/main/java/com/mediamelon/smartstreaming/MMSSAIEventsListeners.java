package com.mediamelon.smartstreaming;

/**
 * A listener for SSAI events.
 *
 */
public interface MMSSAIEventsListeners {

  /**
   * Called when the Ad Impression.
   *
   * @param ssaiAdInfo The ssaiAdInfo.
   */
  default void onAdImpression(mmAd ssaiAdInfo) {}

  /**
   * Called when the Ad Started.
   *
   * @param ssaiAdInfo The ssaiAdInfo.
   */
  default void onAdStarted(mmAd ssaiAdInfo) {}

    /**
     * Called when the Ad finishes first quartile.
     *
     * @param ssaiAdInfo The ssaiAdInfo.
     */
  default void onAdFirstQuartile(mmAd ssaiAdInfo) {}

    /**
     * Called when the Ad finishes midpoint.
     *
     * @param ssaiAdInfo The ssaiAdInfo.
     */
  default void onAdMidpoint(mmAd ssaiAdInfo) {}

    /**
     * Called when the finishes third quartile.
     *
     * @param ssaiAdInfo The ssaiAdInfo.
     */
  default void onAdThirdQuartile(mmAd ssaiAdInfo) {}

    /**
   * Called when the Ad Completed.
   *
   * @param ssaiAdInfo The ssaiAdInfo.
   */
  default void onAdComplete(mmAd ssaiAdInfo) {}
  
  /**
   * Called when the Ad Progress.
   *
   * @param ssaiAdInfo The ssaiAdInfo.
   */
  default void onAdProgress(mmAd ssaiAdInfo) {}

  /**
  * Called when a timeline cue is entered
  *
  * @param ssaiAdInfo The ssaiAdInfo.
  */
  default void onCueTimelineEnter(mmAd ssaiAdInfo){}

  /**
   * Called when a timeline cue is exited.
   *
   * @param ssaiAdInfo The ssaiAdInfo.
  */
  default void onCueTimelineExit(mmAd ssaiAdInfo){}

  /**
   * Called when a new cue have been added to the timeline.
   *
   * @param timelineInfo The timelineInfo.
  */
  default void onCueTimelineAdded(mmAdTimelineInfo timelineInfo){}
}
