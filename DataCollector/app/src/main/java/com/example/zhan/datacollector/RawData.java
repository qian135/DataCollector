package com.example.zhan.datacollector;

import cn.bmob.v3.BmobObject;

/**
 * Created by zhang on 11/3/2015.
 */
public class RawData extends BmobObject {

    private String mRawData, mAttention, mMeditation;

    public String getRawData() {
        return mRawData;
    }

    public void setRawData(String rawData) {
        mRawData = rawData;
    }

}
