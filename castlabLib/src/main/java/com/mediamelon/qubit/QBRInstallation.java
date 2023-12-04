package com.mediamelon.qubit;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;


public class QBRInstallation {
    private static String sID = null;
    private static final String INSTALLATION = "com.example.qbr.PREFERENCE_FILE_KEY";
    private static final String QBRUUIDKey = "QBRUUID";

    public synchronized static String id(Context context) {
        if (sID == null && context != null) {
            SharedPreferences sharedPref =context.getSharedPreferences(INSTALLATION, Context.MODE_PRIVATE);
            try {
                String uuid = "";
                uuid = sharedPref.getString(QBRUUIDKey, uuid);

                if( (uuid==null) || (uuid.length() < 16)){
                    String uuidNew = UUID.randomUUID().toString();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(QBRUUIDKey, uuidNew);
                    editor.commit();
                    uuid = uuidNew;
                }
                sID = uuid;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    public static String hashedId(Context context) {
        String str = QBRInstallation.id(context);
        String retval = null;
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(sID.getBytes());
            // Converting the digest from byte format to hex string.
            retval = new BigInteger(1, digester.digest())
                    .toString(16);
        }catch(Exception e){

        }
        return retval;
    }
}
