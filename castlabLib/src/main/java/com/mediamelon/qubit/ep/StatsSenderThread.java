package com.mediamelon.qubit.ep;

import com.mediamelon.qubit.MMLogger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.TimerTask;


public class StatsSenderThread extends Thread {
    RecordStructure qubitStatsToSend = null;

    private String TAG = "StatsSenderThread.onDemandQOE";
    public void loadQubitStatsData(String url) {
        try {
            MMLogger.v("Postman", "Fetching stats for onDemand QOE");
            SDKExperienceProbe.getInstance().increasePlayDur();
            qubitStatsToSend = SDKExperienceProbe.getInstance().getQBRStats();
        } catch (Exception e) {
            qubitStatsToSend = null;
        }
    }

    public void run() {
        try {
            if(qubitStatsToSend != null && qubitStatsToSend.qubitData.size() == 1) {
                Postman.instance().Queue(qubitStatsToSend);
                //flush pbInfo after sending stats payload
                if (!qubitStatsToSend.qubitData.get(0).pbInfo.isEmpty())
                    SDKExperienceProbe.getInstance().purgePbInfo();
            }
        } catch (Exception e) {
            MMLogger.v(TAG, " StatsSenderThread Exception: " + e.getMessage());
        }catch(Throwable e) {
         e.printStackTrace();
        }
    }
}
