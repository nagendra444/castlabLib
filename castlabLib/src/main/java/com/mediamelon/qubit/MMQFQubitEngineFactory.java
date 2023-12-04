package com.mediamelon.qubit;

import com.mediamelon.qubit.MMQFQubitLogger;
import com.mediamelon.qubit.MMQFQubitEngineInterface;
import com.mediamelon.qubit.MMQFQubitEngine;

public class MMQFQubitEngineFactory {
    /***
     * \brief Creates the instance of MMQFQubitEngine.
     * @return : returns the instance of MMQFQubitEngineInterface
     */
    static public MMQFQubitEngineInterface CreateQubitEngine() {
        MMQFQubitEngineInterface qubitEngine = null;
        try {
            qubitEngine = MMQFQubitEngine.getInstance();
        } catch (RuntimeException e) {
            MMLogger.e("MMQFQubitEngineInterface", e.toString());
        }
        return qubitEngine;
    }
}
