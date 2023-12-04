package com.mediamelon.qubit.ep;
import com.mediamelon.qubit.MMLogger;
import java.net.URL;
import java.util.TimerTask;

public class StatsSender extends TimerTask {
    SDKExperienceProbe sdkExperienceProbe = SDKExperienceProbe.getInstance();

    public StatsSender(String url) {
        if(url != null){
            try{
                Postman.instance().SetProducerUrl(new URL(url));
            }catch(Exception e){
                MMLogger.e("StatsSender", "Exception creating producer url");
            }
        }
    }

    public void run() {
        try {
            if (sdkExperienceProbe.canSendProbes) {
                SDKExperienceProbe.getInstance().increasePlayDur();
                MMLogger.v("Postman", "Fetching stats for periodic QOE");
                RecordStructure rs = SDKExperienceProbe.getInstance().getQBRStats();
                if (rs != null && rs.qubitData.size() == 1) {
                    Postman.instance().Queue(rs);

                    if (!rs.qubitData.get(0).segInfo.isEmpty())
                        SDKExperienceProbe.getInstance().purgeSegInfo();
                    if (!rs.qubitData.get(0).pbInfo.isEmpty())
                        SDKExperienceProbe.getInstance().purgePbInfo();
                }

                if (!sdkExperienceProbe.integrationEnabled) {
                    sdkExperienceProbe.stopMonitoring();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
