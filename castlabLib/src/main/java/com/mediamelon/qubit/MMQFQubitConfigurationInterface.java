package com.mediamelon.qubit;

import java.util.HashMap;

public interface MMQFQubitConfigurationInterface {

    /**
     * To configure some kvps with the Engine
     * For example - Player Name
     */
    public abstract void configureQubitEngine(HashMap<String, String> kvps);
    /**
     * @param mode : Specifies the mode in which qubit engine should work
     */
    public abstract void setQubitMode(int mode);

    /**
     * @param disable : Disables the fetch of the manifest
     * */
    public abstract void disableManifestsFetch(boolean disable);

    /** This mode runs Qubit in a mode to give preference to saving bandwidth while delivering the same
     quality. This mode can be used, for example when data is being streamed through cellular networks with
     higher data costs and data capping*/
    public static final int QubitMode_Bits = 0;

    /** This is the default mode to run Qubit which provides the best trade-off between saving bits and
     improving quality*/
    public static final int QubitMode_Quality = 1;

    /** This mode runs Qubit in a mode to give preference to saving bandwidth while delivering the same
     quality. In this mode no attempt is made at any point in time to improve the quality wrt the quality user would have received with regular ABR in curent network conditions*/
    public static final int QubitMode_CostSave = 2;

    /** This mode disables the Qubitization feature*/
    public static final int QubitMode_Disabled = 3;

    static String KVPKeyPlayerName = "QBRPlayerName";
}
