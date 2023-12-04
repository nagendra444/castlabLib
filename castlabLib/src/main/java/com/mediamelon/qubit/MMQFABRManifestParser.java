package com.mediamelon.qubit;
import com.mediamelon.qubit.MMQFPresentationInfo;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Rupesh on 19-03-2015.
 */

public interface MMQFABRManifestParser {
    class ParserSegmentInfoForURL{
        ParserSegmentInfoForURL()
        {
            segmentIndex = -1;
        }
        public MMQFPresentationVideoTrackInfo videoTrackInfo;
        int segmentIndex;
    }

    public class AuxResRange{
        AuxResRange(){
            startByteIndex = endByteIndex = -1;
        }
        public long startByteIndex; //Setting Start Byte to "-" is interpreted as 0
        public long endByteIndex; //Setting end byte to "-" is interpreted as EOF
    }

    public class AuxResourceInformation{
        AuxResourceInformation(){
            range_ = new AuxResRange();
            url_ = null;
        }
        public URL url_;
        public AuxResRange range_;
    }

    public abstract MMQFPresentationInfo parse(String manifestFileData, URL manifestURL);
    public abstract ParserSegmentInfoForURL getSegmentInfoForURL(String url);
    public abstract ParserSegmentInfoForURL getSegmentInfoForSegment(int bitrate, long startTimeMilliSec);
    public abstract boolean needAuxiliaryResources();
    public abstract ArrayList<AuxResourceInformation> getAuxResourceInfo();
    public abstract void SetAuxResource(int index, byte[] resource);
    public abstract void AuxResRetrievalFailed(int index);
    public abstract boolean allAuxResDownloadSuccess();

    public boolean isSupportedQBRPresentation();

    static String KStartIdxTag = "?startByte="; //for ranged URis
    static String KSegLenTag = "&endByte=";
}


