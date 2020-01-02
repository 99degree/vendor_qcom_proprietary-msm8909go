/*
 * Copyright (c) 2015 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 */

/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License.
 */
package android.hardware.iris;

import android.hardware.iris.IrisOperationStatus;

/**
 * Communication channel from the iris daemon back to IrisService.
 * @hide
 */
 interface IIrisDaemonCallback {
    void onError(long deviceId, int error);
    void onEnrollStatus(long deviceId,in IrisOperationStatus status);
    void onEnrollResult(long deviceId, int irisId, int groupId);
    void onAuthStatus(long deviceId, in IrisOperationStatus status);   
    void onAuthResult(long deviceId, boolean matched, int irisId, int groupId);
    void onRemoved(long deviceId, int irisId, int groupId);
}