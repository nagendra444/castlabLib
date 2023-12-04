package com.mediamelon.smartstreaming;

/**
 * Specifies the device network connection type.
 */
public enum MMConnectionInfo{
  /**
   * Connection type is cellular (generic). If user explicitly knows the kind of connection, 2G or 3G for example, then they should use explicit enum value corresponding to that connection type.
   */
  Cellular,

  /**
   * Connection type is 2G cellular.
   */
  Cellular_2G,

  /**
   * Connection type is 3G cellular.
   */
  Cellular_3G,

  /**
   * Connection type is 4G cellular.
   */
  Cellular_4G,

  /**
   * Connection type is LTE cellular.
   */
  Cellular_LTE,

  /**
   * Connection type is 5G cellular.
   */
  Cellular_5G,

  /**
   * Connection non reachable.
   */
  NotReachable,

  /**
   * Connection type is WiFi.
   */
  Wifi,

  /**
   * Connection type is wired.
   */
  WiredNetwork
}
