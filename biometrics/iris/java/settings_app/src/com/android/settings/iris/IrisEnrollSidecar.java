/*
 * Copyright (c) 2015-2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 */

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.iris;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.hardware.iris.IrisManager;
import android.hardware.iris.IrisOperationStatus;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.android.settings.ChooseLockSettingsHelper;

/**
 * Sidecar fragment to handle the state around iris enrollment.
 */
public class IrisEnrollSidecar extends Fragment {
    private int mEnrollmentProgress = -1;
    private Listener mListener;
    private boolean mEnrolling;
    private CancellationSignal mEnrollmentCancel;
    private Handler mHandler = new Handler();
    private byte[] mToken;
    private static final int mEnrollTimeout = 50000;
    private static final String TAG = "IrisEnrollSidecar";
    private Surface mPreviewSurface = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mToken = activity.getIntent().getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mEnrolling) {
            startEnrollment();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            cancelEnrollment();
            mPreviewSurface = null;
        }
    }

    private void startEnrollment() {
        mHandler.postDelayed(mTimeoutRunnable, mEnrollTimeout);
        mEnrollmentProgress = 0;
        mEnrollmentCancel = new CancellationSignal();
        getActivity().getSystemService(IrisManager.class).setPreviewSurface(mPreviewSurface);
        getActivity().getSystemService(IrisManager.class).enroll(mToken, mEnrollmentCancel,
                0 /* flags */, mEnrollmentCallback);
        mEnrolling = true;
    }

    private void cancelEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        if (mEnrolling) {
            mEnrollmentCancel.cancel();
            mEnrolling = false;
            mEnrollmentProgress = -1;
        }
    }

    public void setPreviewSurface(Surface previewSurface) {
	    Log.e(TAG, "enter");
        mPreviewSurface = previewSurface;
        Log.e(TAG, "exit");
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public int getEnrollmentProgress() {
        return mEnrollmentProgress;
    }

    public boolean isDone() {
        return mEnrollmentProgress == 100;
    }

    private IrisManager.EnrollmentCallback mEnrollmentCallback
            = new IrisManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentStatus(IrisOperationStatus status) {
            Log.v(TAG, "onEnrollmentStatus");
            mEnrollmentProgress = status.mProgress;
            if (mListener != null) {
                mListener.onEnrollmentStatus(status);
            }
        }

        @Override
        public void onEnrollmentDone() {
            Log.v(TAG, "onEnrollmentDone ");
            int result = getActivity().getSystemService(IrisManager.class).postEnroll();
            if (result < 0) {
                Log.v(TAG, "postEnroll failed: result = " + result);
            }
            mEnrollmentProgress = 100;
            mListener.onEnrollmentDone();
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            if (mListener != null) {
                mListener.onEnrollmentError(errString);
            }
        }
    };

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            cancelEnrollment();
        }
    };

    public interface Listener {
        void onEnrollmentError(CharSequence errString);
        void onEnrollmentStatus(IrisOperationStatus status);
        void onEnrollmentDone();
    }
}
