package com.mediamelon.qubit;
import java.net.URL;

/**
 * Created by Rupesh on 23-07-2015.
 */
public class MMQFResourceInfo {
    public class MMQFResourceRange{
        MMQFResourceRange(){
            startByteIndex = endByteIndex = -1;
        }
        public long startByteIndex; //Setting Start Byte to "-" is interpreted as 0
        public long endByteIndex; //Setting end byte to "-" is interpreted as EOF
    }

    MMQFResourceInfo(){
        resourceURL_ = null;
        range_= new MMQFResourceRange();
    }

    URL resourceURL_;
    MMQFResourceRange range_;
}
