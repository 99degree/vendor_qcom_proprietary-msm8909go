/*
 *Copyright (c) 2014, 2017 Qualcomm Technologies, Inc.
 *All Rights Reserved.
 *Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.autoregistration;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.util.XmlUtils;

public abstract class RegistrationPairs extends Handler {

    private static final String TAG = "RegistrationPairs";
    private static final String AUTHORITY = "com.qualcomm.qti.service.GET_DEVICE_INFO";
    private static final String METHOD_NAME = "get_device_info";
    private static final boolean DBG = false;
    private int mCtSlotId = 0;
    private int mPrimarySimPhoneId = 0;

    private static class Pair {
        private String key;
        private String keyPost;
        private String value;
        private boolean replied;

        @Override
        public String toString() {
            return "[" + key + ", " + keyPost + ", " + value + ", " + replied + "]";
        }
    }

    private Map<String, Pair> mPairs = new HashMap<String, Pair>();

    private final Context mContext;

    public RegistrationPairs(Context context, int ctSlotId, int primarySimPhoneId) {
        mContext = context;
        mCtSlotId = ctSlotId;
        mPrimarySimPhoneId = primarySimPhoneId;
        Log.d(TAG, "ctSlotId id = " + ctSlotId
                + " primary sim = " + primarySimPhoneId);
        loadParams();
    }

    private void loadParams() {
        Resources r = mContext.getResources();
        XmlResourceParser parser = r.getXml(R.xml.post_params);
        try {
            XmlUtils.beginDocument(parser, "params");
            XmlUtils.nextElement(parser);
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                Pair pair = new Pair();
                pair.key = parser.getAttributeValue(null, "key");
                pair.keyPost = parser.getAttributeValue(null, "post_key");
                mPairs.put(pair.key, pair);
                XmlUtils.nextElement(parser);
            }
        } catch (Exception e) {
            Log.w(TAG, "failed to load post_params", e);
        } finally {
            parser.close();
        }
        // Temporary workaround, per spec, the field SIM2IMSI is removed already
        // but no the field under class a, cannot register successfully
        boolean isClassA = SystemProperties.get("persist.radio.carrier_mode",
                "default").equals("ct_class_a");
        if (isClassA) {
            Pair pair = new Pair();
            pair.key = "SIM2IMSI";
            pair.keyPost = "SIM2IMSI";
            mPairs.put(pair.key, pair);
        }
        if (DBG) {
            Log.d(TAG, "params loaded:" + mPairs);
        }
    }

    public void load() {
        for (String key : mPairs.keySet()) {
            getDeviceInfo(key);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        String key = getDeviceInfoKey(msg.what);
        if (key == null) {
            return;
        }
        if (DBG) {
            Log.d(TAG, "reponse of device info: " + key);
        }
        Pair pair = mPairs.get(key);
        if (msg.obj != null && msg.obj instanceof Bundle) {
            pair.value = ((Bundle) msg.obj).getString("result");
        }
        pair.replied = true;
        notifyDeviceInfoGotIfNeed();
    }

    private void getDeviceInfo(String key) {
        final Uri CONTENT_URI_DEVICE_INFO = Uri
                .parse("content://" + AUTHORITY);
        Bundle response = null;
        if (mContext.getContentResolver().acquireProvider(CONTENT_URI_DEVICE_INFO) != null) {
            if (DBG) {
                Log.d(TAG, "request to get device info: " + key);
            }
            response = mContext.getContentResolver().call(CONTENT_URI_DEVICE_INFO,
                    METHOD_NAME, key, getBundleForDeviceInfoQuery(key));
        }
        if (response == null || !response.getBoolean("result")) {
            if (DBG) {
                Log.d(TAG, "no reponse of device info: " + key);
            }
            mPairs.get(key).replied = true;
            notifyDeviceInfoGotIfNeed();
        }
    }

    private Bundle getBundleForDeviceInfoQuery(String key) {
        Bundle bundle = new Bundle();
        Message callback = obtainMessage(getEventId(key),
                mCtSlotId, mPrimarySimPhoneId);
        callback.replyTo = new Messenger(this);
        bundle.putParcelable("callback", callback);
        return bundle;
    }

    private int getEventId(String key) {
        int postion = 0;
        for (String str : mPairs.keySet()) {
            postion++;
            if (key.equals(str)) {
                break;
            } else if (postion == mPairs.size()) {
                postion = 0;
            }
        }
        return postion;
    }

    private String getDeviceInfoKey(int eventId) {
        int postion = 0;
        for (String key : mPairs.keySet()) {
            postion++;
            if (eventId == postion) {
                return key;
            }
        }
        return null;
    }

    private void notifyDeviceInfoGotIfNeed() {
        JSONObject json = new JSONObject();
        for (String key : mPairs.keySet()) {
            Pair pair = mPairs.get(key);
            if (!pair.replied) {
                if (DBG) {
                    Log.d(TAG, pair.key + " is not responsed!");
                }
                return;
            }
            try {
                json.put(pair.keyPost, pair.value == null ? "" : pair.value);
            } catch (JSONException e) {
                Log.w(TAG, "failed to get params", e);
            }
        }
        Log.d(TAG, "Params: " + json);
        onDeviceInfosGot(json);
    }

    public abstract void onDeviceInfosGot(JSONObject data);
}
