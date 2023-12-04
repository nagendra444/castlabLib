package com.mediamelon.smartstreaming;

import java.util.concurrent.CopyOnWriteArraySet;
import com.mediamelon.smartstreaming.mmAd;
import com.mediamelon.smartstreaming.mmAdTimelineInfo;

/**
 * Data collector which is able to forward analytics events by
 * listening to all available listeners.
 */
public class MMSSAIEventsCollector {

  /** Factory for an analytics collector. */
  public static class Factory {

    /**
     * Creates an mmSSAIEventsCollector.
     *
     * @return An mmSSAIEventsCollector.
     */
    public MMSSAIEventsCollector createMMSSAIEventsCollector() {
      return new MMSSAIEventsCollector();
    }
  }

  private final CopyOnWriteArraySet<MMSSAIEventsListeners> listeners;
//  private final Window window;


  /**
   * Creates an mmSSAIEventsCollector.
   *
   */
  protected MMSSAIEventsCollector() {
    listeners = new CopyOnWriteArraySet<>();
  }

  /**
   * Adds a listener for analytics events.
   *
   * @param listener The listener to add.
   */
  public void addListener(MMSSAIEventsListeners listener) {
    listeners.add(listener);
  }

  /**
   * Removes a previously added analytics event listener.
   *
   * @param listener The listener to remove.
   */
  public void removeListener(MMSSAIEventsListeners listener) {
    listeners.remove(listener);
  }


  // External events.
  public final void notifyExternalAdCueTimelineUpdate(mmAdTimelineInfo timelineInfo){
    if(timelineInfo != null){
        for (MMSSAIEventsListeners listener : listeners) {
            listener.onCueTimelineAdded(timelineInfo);
        }
    }
  }

  public final void notifyExternalAdEvents(String eventName, mmAd mmSSAIAdInfo) {

    if (!eventName.isEmpty()) {

      switch(eventName){
          case "onCueTimelineEnter":
              for (MMSSAIEventsListeners listener : listeners) {
                  listener.onCueTimelineEnter(mmSSAIAdInfo);
              }
              break;
          case "onCueTimelineExit":
              for (MMSSAIEventsListeners listener : listeners) {
                  listener.onCueTimelineExit(mmSSAIAdInfo);
              }
              break;
        case "impression":
              for (MMSSAIEventsListeners listener : listeners) {
                listener.onAdImpression(mmSSAIAdInfo);
              }
              break;
        case "start":
              for (MMSSAIEventsListeners listener : listeners) {
                //listener.onCueTimelineEnter(mmSSAIAdInfo);
                listener.onAdStarted(mmSSAIAdInfo);
              }
              break;

        case "progress":
              for (MMSSAIEventsListeners listener : listeners) {
                  listener.onAdProgress(mmSSAIAdInfo);
              }
              break;
        case "firstQuartile":
              for (MMSSAIEventsListeners listener : listeners) {
                  listener.onAdFirstQuartile(mmSSAIAdInfo);
              }
              break;
        case "midpoint":
              for (MMSSAIEventsListeners listener : listeners) {
                  listener.onAdMidpoint(mmSSAIAdInfo);
              }
              break;
        case "thirdQuartile":
              for (MMSSAIEventsListeners listener : listeners) {
                  listener.onAdThirdQuartile(mmSSAIAdInfo);
              }
              break;
        case "complete":
              for (MMSSAIEventsListeners listener : listeners) {
                listener.onAdComplete(mmSSAIAdInfo);
                //listener.onCueTimelineExit(mmSSAIAdInfo);
              }
              break;
        default: // Handle Unsupported events
      }
    }
  }
  
}
