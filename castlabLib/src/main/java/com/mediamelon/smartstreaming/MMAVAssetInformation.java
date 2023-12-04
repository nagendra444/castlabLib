package com.mediamelon.smartstreaming;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.mediamelon.smartstreaming.MMQBRMode.QBRModeBitsave;
import static com.mediamelon.smartstreaming.MMQBRMode.QBRModeCostsave;
import static com.mediamelon.smartstreaming.MMQBRMode.QBRModeDisabled;
import static com.mediamelon.smartstreaming.MMQBRMode.QBRModeQuality;

public class MMAVAssetInformation {
    /*
     * Creates an object to hold information identifying the asset that is to be played back on the AVPlayer
     * User must specify URL of the asset.
     * User may optionally specify the identifier identifying the asset, its name and collection to which this asset belongs
     */
    public MMAVAssetInformation(URL aAssetURL, String aAssetID, String aAssetName, String aVideoId){
        assetURL = aAssetURL;
        assetID = aAssetID;
        assetName = aAssetName;
        videoId = aVideoId;
        customKVPs = new HashMap<String,String>();
        qbrMode = QBRModeDisabled;
    }

    /*
     * Creates an object to hold information identifying the asset that is to be played back on the AVPlayer
     * User must specify URL of the asset.
     * User may optionally specify the identifier identifying the asset, its name and collection to which this asset belongs
     */
    public MMAVAssetInformation(String aAssetID, String aAssetName, String aVideoId){
        assetURL = null;//Need to get it from the player source information
        assetID = aAssetID;
        assetName = aAssetName;
        videoId = aVideoId;
        customKVPs = new HashMap<>();
        qbrMode = QBRModeDisabled;
    }

    public MMAVAssetInformation(JSONObject mmVideoAssetInfo){
        setDefaults();

        if(mmVideoAssetInfo != null){
            try {
                assetName = mmVideoAssetInfo.getString(KAssetName);
                assetID = mmVideoAssetInfo.getString(KAssetID);
                videoId = mmVideoAssetInfo.getString(KVideoID);
		        try {
		            if(!mmVideoAssetInfo.isNull(KMetafileURL)) {
                        metafileURL = new URL(mmVideoAssetInfo.getString(KMetafileURL));
                    }
        	    } catch (MalformedURLException e) {
            		e.printStackTrace();
        	    }
                JSONObject customTags = mmVideoAssetInfo.getJSONObject(KCustomTag);

                String qbrModeStr = mmVideoAssetInfo.getString(KQBRMode);
                if(qbrModeStr != null) {
                    switch (qbrModeStr) {
                        case "QBRModeBitsave": {
                            qbrMode = QBRModeBitsave;
                        }
                        break;
                        case "QBRModeQuality": {
                            qbrMode = QBRModeQuality;
                        }
                        break;
                        case "QBRModeCostsave": {
                            qbrMode = QBRModeCostsave;
                        }
                        break;
                        case "QBRModeDisabled": {
                            qbrMode = QBRModeDisabled;
                        }
                        break;
                    }
                }

                if((customTags.length() > 0) && (customKVPs == null)){
                    customKVPs = new HashMap<>();
                }

                Iterator itt = customTags.keys();
                while(itt.hasNext())
                {
                    String key = itt.next().toString();
                    String value = customTags.getString(key);
                    if(key != null && value != null){
                        customKVPs.put(key, value);
                    }
                }
            } catch (JSONException e) {

            }
        }
    }

    public void setDefaults(){
        assetURL = null;
        assetID = null;
        assetName = null;
        videoId = null;
        qbrMode = QBRModeDisabled;
        metafileURL = null;
        customKVPs = null;
    }

    public String toJSON(){
        String retVal = "";
        try {
            JSONObject mmVideoAssetInfo = new JSONObject();
            if(assetURL != null) {
                mmVideoAssetInfo.put(KAssetURL, assetURL);
            }

            if(assetName != null) {
                mmVideoAssetInfo.put(KAssetName, assetName);
            }

            if(assetID != null) {
                mmVideoAssetInfo.put(KAssetID, assetID);
            }

            if(videoId != null) {
                mmVideoAssetInfo.put(KVideoID, videoId);
            }

            if(metafileURL != null) {
                mmVideoAssetInfo.put(KMetafileURL, metafileURL);
            }

            JSONObject customTags = new JSONObject();
            if (customKVPs.size() > 0){
                for (Map.Entry<String, String> entry : customKVPs.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if(key != null && value != null && key.length() > 0 && value.length() >0) {
                        customTags.put(key, value);
                    }
                }
            }
            mmVideoAssetInfo.put(KCustomTag, customTags);

            switch (qbrMode){
                case QBRModeBitsave:{
                    mmVideoAssetInfo.put(KQBRMode, "QBRModeBitsave");
                }
                break;
                case QBRModeQuality:{
                    mmVideoAssetInfo.put(KQBRMode, "QBRModeQuality");
                }
                break;
                case QBRModeCostsave:{
                    mmVideoAssetInfo.put(KQBRMode, "QBRModeCostsave");
                }
                break;
                case QBRModeDisabled:{
                    mmVideoAssetInfo.put(KQBRMode, "QBRModeDisabled");
                }
                break;
            }
            retVal = mmVideoAssetInfo.toString();
        } catch (JSONException e) {
            // Handle Exception
        }
        return retVal;
    }

    /*
     * Lets user specify the custom metadata to be associated with the asset, for example - Genre, DRM etc
     *
     * Call to this API is optional
     */
    public void addCustomKVP(String key, String value){
        if(key != null && value != null) {
            customKVPs.put(key, value);
        }else{
            Log.e("MMIntegration", "Invalid custom tag {" + key + " : " + value + "}");
        }
    }

    /*
     * Sets the mode to be used for QBR and the meta file url from where content metadata can be had.
     * Meta file URL is to be provided only if metadata cant be had on the default location.
     *
     * Please note that call to this method is needed only if QBR is integrated to the player.
     */
    public void setQBRMode(MMQBRMode mode, URL metaURL){ //Needed only when QBR is to be integrated
        qbrMode = mode;
        metafileURL = metaURL;
    }

    public URL assetURL; //URL of the Asset
    public String assetID; //optional identifier of the asset
    public String assetName; //optional name of the asset
    public String videoId; //optional identifier of the asset group (or) sub asset
    public MMQBRMode qbrMode; //Needed only when QBR is to be integrated
    public URL metafileURL; //Needed only when QBR is to be integrated
    public HashMap<String, String> customKVPs; //Custom Metadata of the asset


    private String KAssetURL = "assetURL";
    private String KAssetID = "assetID";
    private String KAssetName = "assetName";
    private String KVideoID = "videoId";
    private String KQBRMode = "qbrMode";
    private String KCustomTag = "customTags";
    private String KMetafileURL = "metafileURL";
}
