package com.mediamelon.qubit;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;
import android.os.AsyncTask;

public class MMQFQubitResourceDownloader_HTTP extends AsyncTask<MMQFResourceInfo, Void, byte[]>{

    public interface OnQubitResourceDownloadedListener {
        void OnQubitResourceDownloaded(MMQFQubitStatusCode status, byte[] downloadedMedia, String contentType);
    }

    public void setOnQubitResourceDownloadedListener(OnQubitResourceDownloadedListener lstnr)
    {
        listener = lstnr;
    }

    Long maxAllowedResourceLen = -1L; //10 MB
    String contentMimeStr = null;

    public void SetMaxResourceLen(Long len){ //If set to -1, then content length of any length is fine
        maxAllowedResourceLen = len;
    }

    public String GetContentMime(){
        return contentMimeStr;
    }

    @Override
    protected byte[] doInBackground(MMQFResourceInfo... params) {
        assert(params.length == 1);
        Log.e(TAG, "Params length!!! " + params.length);
        String data = "";
        byte[] retval = null;
        HttpURLConnection httpUrlConnection = null;
        try {
            Log.e(TAG, "Going to open connection for " + params[0].resourceURL_.toString());
            //MMLogger.e(TAG, "Going to open connection for " + params[0].resourceURL_.toString());
            httpUrlConnection = (HttpURLConnection) params[0].resourceURL_.openConnection();
            httpUrlConnection.setReadTimeout(10000); //in millisec
            httpUrlConnection.setConnectTimeout(5000); //in millise
            String cookieStr = "";
            if((CookieManager)CookieHandler.getDefault() != null) {
                CookieStore cookieJar = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
                List<HttpCookie> cookies = cookieJar.getCookies();
                for (HttpCookie cookie : cookies) {
                    if (cookieStr != "") {
                        cookieStr = cookieStr + "; ";
                    }
                    cookieStr = cookieStr + cookie.toString();
                }
            }
            httpUrlConnection.setRequestProperty("Cookie", cookieStr);
            if(params[0].range_ != null && params[0].range_.startByteIndex!=-1) {
                String byteRange = "bytes=" + params[0].range_.startByteIndex;
                if(params[0].range_.endByteIndex!=-1){
                    byteRange += "-";
                    byteRange += params[0].range_.endByteIndex;
                }
                httpUrlConnection.setRequestMethod("GET");
                httpUrlConnection.setRequestProperty("Range", byteRange);
            }
            else{
                //httpUrlConnection.setRequestProperty("Accept-Encoding", "identity");
                //httpUrlConnection.setRequestMethod("HEAD");
                //httpUrlConnection.setDoInput(true);
                //if(httpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    //int lengthofFile = httpUrlConnection.getContentLength();
                    //String contentLengthHeader_ = httpUrlConnection.getHeaderField("Content-Length");
                    //httpUrlConnection.disconnect();
                    httpUrlConnection = (HttpURLConnection) params[0].resourceURL_.openConnection();
                    httpUrlConnection.setRequestProperty("Accept-Encoding", "identity");
                    httpUrlConnection.setRequestMethod("GET");
                    String byteRange = "bytes=0-";// + (lengthofFile - 1);
                    httpUrlConnection.setRequestProperty("Range", byteRange);
                //}
            }
            httpUrlConnection.setDoInput(true);

            int responseCode;
            try {
                responseCode = httpUrlConnection.getResponseCode();
            } catch (IOException e) {
                throw e;
            }
            if (responseCode < 200 || responseCode > 299){
                MMLogger.e(TAG, "Bad response code - " + responseCode);
                return null;
            }
            long contentLength = 0;
            String contentLengthHeader = httpUrlConnection.getHeaderField("Content-Length");
//            String contentType = httpUrlConnection.getHeaderField("Content-Type");
//            MMLogger.e("CheckContentType~~~~~~~~~~", contentType);
            boolean contentLenFromRangeStr = false;
            contentMimeStr = httpUrlConnection.getHeaderField("Content-Type");
            if(contentLengthHeader == null){
                String contentRangeStr = httpUrlConnection.getHeaderField("Content-Range");
                if(contentRangeStr!=null){
                    String rangeValue = "bytes ";

                    contentRangeStr += rangeValue.length();
                    //get content length form here
                    int startByteBeginIdx = contentRangeStr.indexOf(rangeValue);
                    int startByteEndIdx = contentRangeStr.indexOf("-");
                    String startByteStr = contentRangeStr.substring(startByteBeginIdx + rangeValue.length(), startByteEndIdx);
                    int endByteIdx = contentRangeStr.indexOf("/");
                    String endByteStr = contentRangeStr.substring(startByteEndIdx + 1, endByteIdx);
                    contentLength = Long.parseLong(endByteStr) - Long.parseLong(startByteStr) + 1;
                    contentLenFromRangeStr = true;
                }
                else {
                    //Iterate over the headers
                    Map<String, List<String>> hdrs = httpUrlConnection.getHeaderFields();
                    for (Map.Entry<String, List<String>> entry : hdrs.entrySet()) {
                        MMLogger.d(TAG, "Entry key " + entry.getKey());
                        for (int i = 0; i < entry.getValue().size(); i++) {
                            MMLogger.d(TAG, "value [" + i + "] is = " + entry.getValue().get(i));
                        }
                        System.out.println(entry.getKey() + "/" + entry.getValue());
                    }
                    httpUrlConnection.disconnect();
                    return null;
                }
            }

            if (!TextUtils.isEmpty(contentLengthHeader)) {
                try {
                    contentLength = Long.parseLong(contentLengthHeader);
                } catch (NumberFormatException e) {
                    MMLogger.e(TAG, "Unexpected Content-Length [" + contentLengthHeader + "]");
                }
            }
            if(contentLength != 0 && ( (maxAllowedResourceLen == -1) || contentLength < (maxAllowedResourceLen))){ //Lets have maximum resource length less than 10 MB, unless range str
                retval = new byte[(int)contentLength];
            }else{
                httpUrlConnection.disconnect();
                return null;
            }
            InputStream strm = httpUrlConnection.getInputStream();
            int x = -1;
            int remainingBytes = (int)contentLength;
            int bytesRead = 0;
            do {
                x = strm.read(retval, bytesRead, remainingBytes);
                if(x>0) {
                    remainingBytes -= x;
                    bytesRead += x;
                }
            }while( (x!=-1) && (remainingBytes != 0));

            //MMLogger.d(TAG, "Done with reading " + x + " bytes.");
            //InputStream in = new BufferedInputStream(httpUrlConnection.getInputStream());
            //data = readStream(in);
        } catch (MalformedURLException exception) {
            MMLogger.e(TAG, "MalformedURLException");
            retval = null;
            exception.printStackTrace();
        } catch (IOException exception) {
            MMLogger.e(TAG, "IOException");
            retval = null;
            exception.printStackTrace();
            return null;
        } catch (Exception e){
            MMLogger.e(TAG, "Exception");
            retval = null;
            e.printStackTrace();
        }
        finally {
            if (null != httpUrlConnection)
                httpUrlConnection.disconnect();
        }
        //return data;
        return retval;
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer data = new StringBuffer("");
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (IOException e) {
            MMLogger.e(TAG, "IOException");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data.toString();
    }

    @Override
    protected void onPostExecute(byte[] result) {
        if(result!=null) {
            MMQFQubitStatusCode status = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFSuccess);
            listener.OnQubitResourceDownloaded(status, result, contentMimeStr);
        }
        else{
            MMQFQubitStatusCode status = new MMQFQubitStatusCode(MMQFQubitStatusCode.MMQFFailure);
            listener.OnQubitResourceDownloaded(status, result, contentMimeStr);
        }
    }

    private static final String TAG = "MMQFDownloader_HTTP";
    private OnQubitResourceDownloadedListener listener = null;
}
