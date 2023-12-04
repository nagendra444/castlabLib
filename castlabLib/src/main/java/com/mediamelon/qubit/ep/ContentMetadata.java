package com.mediamelon.qubit.ep;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ContentMetadata implements JSONAble {
    public String assetId; //Unique identifier for the episode(for example)
    public String assetName; //Unique name of the asset, aka Episode Title
    public String contentType; //Type of content (Movie / Special / Clip / Scene Epis Lifts)
    public String drmProtection; //Widevine, Fairplay, Playready etc. Unknown means content is protected, but protection type is unknown. For clear contents, do not set this field
    public String episodeNumber; //Sequence Number of the Episode. COuld have been int as well. But to allow flexibility, and consistency, keeping it str for now
    public String genre; //Genre of the content
    public String season; //Season For example - Season1,2,3 etc
    public String seriesTitle; //Title of the series
    public String videoId; //For example - Prison Break (aka Series ID)
    public String videoType; // new parameter as asked by samit on July 2021
    public ContentMetadata(){
        assetId = "";
        assetName = "";
        contentType = "";
        drmProtection = "";
        episodeNumber = "";
        genre = "";
        season = "";
        seriesTitle = "";
        videoType = "";
        videoId = "";
    }
    public JSONObject getJSONObject()
    {
                JSONObject jsonObject = new JSONObject();
                try {
                    	jsonObject.put("assetName", assetName);
                    	jsonObject.put("videoId", videoId);
                    	jsonObject.put("assetId", assetId);
                    	jsonObject.put("contentType", contentType);
                    	jsonObject.put("drmProtection", drmProtection);
                    	jsonObject.put("episodeNumber", episodeNumber);
                   		jsonObject.put("genre", genre);
                    	jsonObject.put("season", season);
                    	jsonObject.put("seriesTitle", seriesTitle);
                        jsonObject.put("videoType", videoType);

                    } catch (JSONException e) {

                            }
                return jsonObject;
    }
}
