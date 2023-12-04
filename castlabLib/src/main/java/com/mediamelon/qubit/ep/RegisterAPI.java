package com.mediamelon.qubit.ep;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.mediamelon.qubit.MMLogger;
import com.mediamelon.qubit.PropertyReader;
import com.mediamelon.qubit.QBRInstallation;
import com.mediamelon.qubit.ep.RegisterResponse;
import com.mediamelon.qubit.ep.SDKExperienceProbe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class RegisterAPI extends AsyncTask<String,String,RegisterResponse>{
    public class RegisterAPIStatusCode {
        RegisterAPIStatusCode(int code)
        {
            statusCode = code;
        }

        public int status()
        {
            return statusCode;
        }
        public static final int MMQFErrInvalidArguments = -2;
        public static final int MMQFFailure = -1;
        public static final int MMQFUnknown = 0;
        public static final int MMQFSuccess = 1;
        public static final int MMQFPending = 2;
        public static final int MMQFCancelled = 3;

        private int statusCode;
    }

    private SDKExperienceProbe sdkExperienceProbe = SDKExperienceProbe.getInstance();

    private Long customerID;
    private String manifestURL;
    private String mode;
    private String component;
    private String platform;
    private String sdkVersion;
    private String hintfileVersion = "2.0.0";
    private String epSchemaVersion = EPDataFormat.version;

    private String deviceBrand;
    private String deviceModel;
    private String deviceOSVersion;
    private String telecomOperator;
    private String scrnRes;

    private Integer cmdRegistrationId = 0;
    static private String logStringTag = "MMSmartStreaming.Registeration";
    private static Integer registrationId = 0;

    public interface onRegistrationCompleteObserver {
        public void onRegisterComplete(RegisterResponse response, RegisterAPIStatusCode statusCode);
    }

    public void setDeviceInformation(String dBrand, String dModel, String dOSVersion, String tOperator, String sRes){
        deviceBrand = dBrand;
        deviceModel = dModel;
        deviceOSVersion = dOSVersion;
        telecomOperator = tOperator;
        scrnRes = sRes;
    }

    private String getDeviceInformation(){
        String retVal = "";
        retVal += ((deviceModel!=null) ? ("&model=" + deviceModel): "");
        retVal += ((deviceBrand!=null) ? ("&brand=" + deviceBrand): "");
        retVal += ((deviceOSVersion!=null) ? ("&version=" + deviceOSVersion): "");
        retVal += ((telecomOperator!=null) ? ("&operator=" + telecomOperator): "");
        retVal += ((scrnRes!=null) ? ("&scrnRes=" + scrnRes): "");
        return retVal;
    }

    public onRegistrationCompleteObserver observer = null;
    public RegisterAPI(String url, String comp, String plat, String version, String aMode) {
        manifestURL = url;
        component = comp;
        platform = plat;
        mode = aMode;
        sdkVersion = version;
        setPropertyDetails();
        sdkExperienceProbe.cleanupEventStructures(manifestURL, customerID);
        synchronized(logStringTag){
            registrationId++;
            cmdRegistrationId = registrationId;
        }
    }

    public void setPropertyDetails() {
        try {
            String cstIdStr = PropertyReader.getInstance().getProperty("CustomerID");
            MMLogger.v(logStringTag, "Read the property file and cstIdStr=" + cstIdStr);
            customerID = Long.parseLong(cstIdStr);
        } catch (Exception e) {
            MMLogger.e(logStringTag,"Error while initializing custId: "+e.getMessage());
            customerID = 99999L;
        }
    }

    @Override
    public void onPostExecute(RegisterResponse registerResponse) {
        if(registerResponse != null){
            MMLogger.v(logStringTag, " Stats || Registration Success!");
        }

        if(registerResponse != null && manifestURL != null && cmdRegistrationId == registrationId) {
            MMLogger.v(logStringTag, "inside onPostExecute method producerUrl=" + registerResponse.producerURL + " statsInterval=" + registerResponse.statsInterval + "server ts=" + registerResponse.timestamp);
            if(observer != null) {
                sdkExperienceProbe.initialize(customerID, registerResponse.statsInterval, registerResponse.producerURL, manifestURL, registerResponse.timestamp, (registerResponse.mode != null)?registerResponse.mode: mode, registerResponse.cfVal, registerResponse.telephonyMetricsFetchInterval, registerResponse.maxStepsUp, registerResponse.maxStepsDown);
            }else{
                sdkExperienceProbe.initialize(customerID, registerResponse.statsInterval, registerResponse.producerURL, manifestURL, registerResponse.timestamp, "QBRDisabled", 1.0, -1, -1, -1);
            }
            if(observer != null){
                observer.onRegisterComplete(registerResponse, new RegisterAPIStatusCode(RegisterAPIStatusCode.MMQFSuccess));
            }
        }else if (cmdRegistrationId != registrationId){
            //Cancelled.
            MMLogger.w(logStringTag, "Registration cancelled " + cmdRegistrationId + " != " + registrationId);
            sdkExperienceProbe.initializationCancelled();
            if(observer != null){
                observer.onRegisterComplete(null, new RegisterAPIStatusCode(RegisterAPIStatusCode.MMQFCancelled));
            }
        }else if (registerResponse == null){
            MMLogger.e(logStringTag, "Registration failed");
            sdkExperienceProbe.initizationFailed();
            if(observer != null){
                observer.onRegisterComplete(null, new RegisterAPIStatusCode(RegisterAPIStatusCode.MMQFFailure));
            }
        }
    }

    @Override
    public RegisterResponse doInBackground(String... uri) {
        setPropertyDetails();
        RegisterResponse registerResponse = null;
        String apiServerUrl = PropertyReader.getInstance().getProperty("RegisterURL");
        String registrationUrl = null;
        try {
            if(apiServerUrl != null) {
                String qUri = "component=" + component + "&platform=" +  platform + "&sdkVersion=" + sdkVersion + "&hintFileVersion=" + hintfileVersion + "&EP_SCHEMA_VERSION=" + epSchemaVersion + getDeviceInformation();
                registrationUrl = apiServerUrl + customerID + "?" + qUri;
                registrationUrl += "&mode=" + mode;
                registrationUrl = registrationUrl.replace(" ",  "_");
                URLConnection urlConnection = new URL(registrationUrl).openConnection();
                MMLogger.v(logStringTag, "Registering the SDK to url: " + registrationUrl);

                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(
                                    urlConnection.getInputStream()));
                    String decodedString;
                    String responseJson = "";
                    while ((decodedString = in.readLine()) != null) {
                        responseJson += decodedString;
                    }
                    in.close();
                    MMLogger.v(logStringTag, "Response received after register=" + responseJson);

                    registerResponse = getResponseObj(responseJson);
                    MMLogger.v(logStringTag,"producerUrl=" + registerResponse.producerURL+" statsInterval=" + registerResponse.statsInterval);
                }
            }

        } catch (Exception e) {
            MMLogger.e(logStringTag,"Exception in register response and message is:" + e.getMessage());
        }
        return registerResponse;
    }
    public RegisterResponse getResponseObj(String regResp) {
        RegisterResponse registerResponse = new RegisterResponse();
        try {
            JSONObject respJO = new JSONObject(regResp);
            if(respJO.has("producerURL")) registerResponse.producerURL = respJO.getString("producerURL");
            if(respJO.has("statsInterval")) registerResponse.statsInterval = respJO.getInt("statsInterval");
            if(respJO.has("hintFileUrl")) registerResponse.hintFileUrl = respJO.getString("hintFileUrl");
            if(respJO.has("mode")) registerResponse.mode = respJO.getString("mode");
            if(respJO.has("cfVal")) registerResponse.cfVal = respJO.getDouble("cfVal");
            if(respJO.has("maxSteps")) registerResponse.maxSteps = respJO.getInt("maxSteps");
            if(respJO.has("maxStepsUp")) registerResponse.maxStepsUp = respJO.getInt("maxStepsUp");
            if(respJO.has("maxStepsDown")) registerResponse.maxStepsDown = respJO.getInt("maxStepsDown");
            if(respJO.has("telephonyMetricsFetchInterval")) registerResponse.telephonyMetricsFetchInterval = respJO.getInt("telephonyMetricsFetchInterval");
            if(respJO.has("hintfileName")) registerResponse.hintfileName = respJO.getString("hintfileName");
            if(respJO.has("timestamp")) registerResponse.timestamp = respJO.getLong("timestamp");
            if(respJO.has("metaFileMap")) {
                JSONObject metaFileObj = respJO.getJSONObject("metaFileMap");
                HintfileMapping hfMapping = new HintfileMapping();
                if (metaFileObj != null && metaFileObj.has("contentServer")) {
                    hfMapping.contentServer = metaFileObj.getString("contentServer");
                }
                if (metaFileObj != null && metaFileObj.has("hintfileServer")) {
                    hfMapping.hintfileServer = metaFileObj.getString("hintfileServer");
                }
                registerResponse.metaFileMap = hfMapping;
            }
        } catch (JSONException e) {
            //
            MMLogger.e("ERROR","JSON EXception while RegisterResponse "+e.getMessage());
        }
        return registerResponse;
    }
}
