package com.mediamelon.smartstreaming;

/**
 * Identifies the chunk/segment resource in a representation.
 */
public class MMChunkInformation{

    /**
     * To identify the chunk in unambiguous way, provide any of following combinations:
     * <p>[Option 1] <b>trackIdx</b> and <b>sequence</b> (preferred)
     * <p>[Option 2] <b>bitrate</b> and <b>sequence</b>
     * <p>[Option 3] <b>resourceURL</b>, <b>startTime</b>, and <b>duration</b>
     * <p>[Option 4] <b>resourceURL</b>, <b>startByte</b>, and <b>endByte</b>
     * <p><p>To specify the complete entity, use Option 4 and set <b>startByte</b> to 0 
     * and <b>endByte<b> to -1
     * @param [trackIdx] [Option 1] Chunk Track ID
     * @param [bitrate] [Option 2] Chunk bitrate in bits per second
     * @param [sequence] [Optiona 1] [Option 2] Chunk sequence numbe
     * @param [resourceURL] [Option 3] [Option 4] Chunk resource URL
     * @param [duration] [Option 3] Chunk duration in milliseconds
     * @param [startTime] [Option 3] Chunk starting time inmilliseconds
     * @param [startByte] [Option 4] Chunk starting byte
     * @param [endByte] [Option 4] Chunk ending byte
     * @return The MMChunkInformation object
     */
    public MMChunkInformation(){
      startTime = startByte = endByte = duration = -1L;
      trackIdx = bitrate = sequence = -1;
      resourceURL = null;
    }

    /**
     * Sets the resource URL for the chunk
     * @param inResourceURL URL of the source containing the chunk
     */
    void setResourceURL(String inResourceURL){
      resourceURL = null;
      if(inResourceURL != null) {
        resourceURL = new String(inResourceURL);
      }
    }

    /**
     * Makes a copy of the MMChunkInformation object and returns the copy
     * @return The new MMChunkInformation object
     */
    public MMChunkInformation Clone(){
      MMChunkInformation    chunkInfo = new MMChunkInformation();
      chunkInfo.trackIdx = trackIdx;
      chunkInfo.bitrate = bitrate;
      chunkInfo.sequence = sequence;
      chunkInfo.startTime = startTime;
      chunkInfo.startByte = startByte;
      chunkInfo.endByte = endByte;
      chunkInfo.duration = duration;
      if(resourceURL != null) {
          chunkInfo.resourceURL = new String(resourceURL);
      }else{
          chunkInfo.resourceURL = null;
      }
      return chunkInfo;
    }

    /**
     * Returns a string representing the MMChunkInformation object
     * @return String representing the MMChunkInformation object
     */
    public String toString() {
      return "MMChunkInformation[trackIdx=" + trackIdx.toString() + " ,bitrate= " + bitrate.toString() + ", sequence= " + sequence.toString() + ", startTime= " + startTime + ", startByte= " + startByte.toString() + ", endByte=" + endByte.toString() + ", duration= " + duration.toString() + ", resourceURL" + (resourceURL!=null?resourceURL:"") + "]";
    }

    /**
     * Chunk track id
     */
    public Integer trackIdx;

    /**
     * Chunk bitrate in bits per second
     */
    public Integer bitrate;

    /**
     * Chunk sequence number
     */
    public Integer sequence;

    /**
     * Chunk start time in milliseconds
     */
    public Long startTime;

    /**
     * Chunk starting byte
     */
    public Long startByte;

    /**
     * Chunk ending byte
     */
    public Long endByte;

    /**
     * Chunk duration in milliseconds
     */
    public Long duration;

    /**
     * Chunk resource url
     */
    public String resourceURL;
}
