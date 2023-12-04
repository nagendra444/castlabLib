package com.mediamelon.smartstreaming;

/**
 * Specifies the QBR operationg mode.
 * <p>Valid values are:
 * <p><b>QBRModeDisabled:</b> Disable QBR optimization; analytics data will be reported.
 * <p><b>QBRModeQuality:</b> Emphasis is on maximizing quality; saving bandwidth is a secondary objective.
 * <p><b>QBRModeBitsave:</b> Emphasis is on savings bandwidth; maximizing quality as a secondary objective.
 * <p><b>QBRModeCostsave:</b> Emphasis is on saving bandwidth without degrading quality.
 */
public enum MMQBRMode{
  /**
   * Disable QBR optimization. Analytics data will be reported.
   */
  QBRModeDisabled,

  /**
   * Emphasis is on maximizing quality; saving bandwidth is a secondary objective.
   */
  QBRModeQuality,

  /**
   * Emphasis is on savings bandwidth; maximizing quality as a secondary objective.
   */
  QBRModeBitsave,

  /**
   * Emphasis is on saving bandwidth without degrading quality.
   */
  QBRModeCostsave
}
