package com.example.zhan.datacollector;

import cn.bmob.v3.BmobObject;

/**
 * Created by zhang on 11/3/2015.
 */
public class Data extends BmobObject {

    private String mAttention, mMeditation;

    public String getAttention() {
        return mAttention;
    }

    public void setAttention(String attention) {
        mAttention = attention;
    }

    public String getMeditation() {
        return mMeditation;
    }

    public void setMeditation(String meditation) {
        mMeditation = meditation;
    }

}
