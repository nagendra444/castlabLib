package com.mediamelon.smartstreaming;

import java.util.ArrayList;

/**
 * Specifies the representation information.
 */
public class MMRepresentation{
    /**
     * Constructor for the MMRepresentation object
     * @param aTrackIdx Representation Track ID
     * @param aBitrate Representation bitrate in bits per second
     * @param aWidth Representation width in pixels
     * @param aHeight Representation height in pixels
     * @param aCodec Representation codec identifier
     * @return The <b>MMRepresentation</b> object
     * @see blacklistRepresentation
     * @see MMPresentationInfo
     */
  
    public MMRepresentation(Integer aTrackIdx, Integer aBitrate, Integer aWidth, Integer aHeight, String aCodec){
    trackIdx = aTrackIdx;
    bitrate = aBitrate;
    width = aWidth;
    height = aHeight;
    codecIdentifier = aCodec;
  }

  /**
   * Returns the representation bitrate in bits per second
   */
  public Integer getBitrate(){
    return bitrate;
  }

  /**
   * Returns the representation track ID
   */
  public Integer getTrackId(){
    return trackIdx;
  }

  /**
   * Returns the representation width in pixels
   */
  public Integer getWidth(){
    return width;
  }

  /**
   * Returns the representation height in pixels
   */
  public Integer getHeight(){
    return height;
  }

  /**
   * Returns the representation codec identifier
   */
  public String codecId(){
    return codecIdentifier;
  }

  /**
   * Adds a chunk to the representation
   * @param chunkInfo The <b>MMChunkInformation</b> object to be added to the representation
   * @see MMChunkInformation
   */
  public void addChunkToRepresentation(MMChunkInformation chunkInfo){
    if(chunkInfos_ == null){
      chunkInfos_ = new ArrayList<MMChunkInformation>();
    }
    chunkInfos_.add(chunkInfo.Clone());
  }

  /**
   * Gets the number of chunks in the representation
   * @return Number of chunks in the representation
   */
  public Integer getChunkCount(){
    return chunkInfos_!=null? chunkInfos_.size() : 0;
  }

  /** 
   * Gets the <b>MMChunkInformation</b> object at a particular chunk index
   * @param index <b>MMChunkInformation</b>object to return
   * @return Selected <B>MMChunkInformation</b> object
   * @see MMChunkInformation
   */
  public MMChunkInformation getChunkAtIdx(int index){
    if (index >= 0 && chunkInfos_!= null){
      return chunkInfos_.get(index);
    }
    return null;
  }

  MMRepresentation(MMRepresentation rep){
    trackIdx = rep.trackIdx;
    bitrate = rep.bitrate;
    width = rep.width;
    height = rep.height;

    if(rep.codecIdentifier != null){
      codecIdentifier = new String(rep.codecIdentifier);
    }else{
      codecIdentifier = null;
    }

    for (int i = 0; i<rep.chunkInfos_.size(); i++) {
      addChunkToRepresentation(rep.chunkInfos_.get(i));
    }

  }

    /**
     * Representation Track ID
     */
    Integer trackIdx;

    /**
     * Representaiton bitrate in bits per second
     */
    Integer bitrate;

    /**
     * Representation width in pixels
     */
    Integer width;

    /**
     * Representation height in pixels
     */
    Integer height;

    /**
     * Codec identifier
     */
    String codecIdentifier;


    /**
     * Array of information on the segments in representation. If this information is not
     * provided, then SDK will try to have it itself (if needed)
     */
  ArrayList<MMChunkInformation> chunkInfos_;
}
