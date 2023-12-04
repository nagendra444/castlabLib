package com.mediamelon.qubit;

/**
 * Created by Rupesh on 19-03-2015.
 */
public class MMQFABRManifestParserFactory {
    private static String streamFormat = "UNKNOWN";
    public static MMQFABRManifestParser createABRStreamingManifestParser(String manifestURL, String contentType)
    {
    	String manifestURL_lowercase = manifestURL.toLowerCase();
        //Get the last part after "/"
        MMQFABRManifestParser parser = null;
        if((contentType!= null && contentType.toLowerCase().indexOf("mpegurl") > 0) || manifestURL_lowercase.indexOf(".m3u8") > 0)
        {
            parser = new HLSPlaylistParser();
            streamFormat = "HLS";
        }
        else if(manifestURL_lowercase.endsWith("/manifest"))
        {
            parser = new MSSManifestParser();
            streamFormat = "MSS";
        }
        else if(manifestURL_lowercase.endsWith(".mpd") || contentType.indexOf("dash+xml")!= -1)
        {
            parser = new DashMPDParser();
            streamFormat = "MPEG-DASH";
        }
        return parser;
    }
    public static String getStreamFormat() {
        return streamFormat;
    }
}
