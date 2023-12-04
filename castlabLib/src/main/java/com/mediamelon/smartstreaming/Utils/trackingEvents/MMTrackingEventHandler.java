package com.mediamelon.smartstreaming.Utils.trackingEvents;


import com.mediamelon.smartstreaming.MMSSAIEventsListeners;
import com.mediamelon.smartstreaming.Utils.http.mmHTTPClient;
import com.mediamelon.smartstreaming.Utils.http.mmHTTPException;
import java.io.IOException;
import java.util.List;
import org.json.JSONObject;

public class MMTrackingEventHandler{

    public MMTrackingEventHandler(){
        
    }

    public void trackUrl(List<String> URLs) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mmHTTPClient httpClient = mmHTTPClient.create();
                for (int i=0;i<URLs.size();i++) {
                    try
                    {
                        String urlToTrack = URLs.get(i);
                        if(urlToTrack != null && !urlToTrack.isEmpty() && urlToTrack.length() > 5) {
                            httpClient.get(urlToTrack).connectTimeout(2000).ensureSuccess().asVoid();
                            System.out.println("Track success for event");
                        }
                    }catch(mmHTTPException e)
                    {
                        System.out.println("TrackingEventURL " + e.getMessage());
                    }
                }
            }
        }).start();
    }

}
