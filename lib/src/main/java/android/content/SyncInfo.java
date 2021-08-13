/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.content;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about the sync operation that is currently underway.
 */
public class SyncInfo implements Parcelable {
    /**
     * Used when the caller receiving this object doesn't have permission to access the accounts
     * on device.
     * @See Manifest.permission.GET_ACCOUNTS
     */
    private static final Account REDACTED_ACCOUNT = new Account("*****", "*****");

    /** @hide */
    public final int authorityId;

    /**
     * The {@link Account} that is currently being synced.
     */
    public final Account account;

    /**
     * The authority of the provider that is currently being synced.
     */
    public final String authority;

    /**
     * The start time of the current sync operation in milliseconds since boot.
     * This is represented in elapsed real time.
     * See {@link android.os.SystemClock#elapsedRealtime()}.
     */
    public final long startTime;

    /**
     * Creates a SyncInfo object with an unusable Account. Used when the caller receiving this
     * object doesn't have access to the accounts on the device.
     * @See Manifest.permission.GET_ACCOUNTS
     * @hide
     */
    public static SyncInfo createAccountRedacted(
            int authorityId, String authority, long startTime) {
        throw new RuntimeException("Stub!");
    }

    /** @hide */
    public SyncInfo(int authorityId, Account account, String authority, long startTime) {
        throw new RuntimeException("Stub!");
    }

    /** @hide */
    public SyncInfo(SyncInfo other) {
        throw new RuntimeException("Stub!");
    }

    /** @hide */
    public int describeContents() {
        return 0;
    }

    /** @hide */
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(authorityId);
        parcel.writeParcelable(account, flags);
        parcel.writeString(authority);
        parcel.writeLong(startTime);
    }

    SyncInfo(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<SyncInfo> CREATOR = new Creator<SyncInfo>() {
        public SyncInfo createFromParcel(Parcel in) {
            return new SyncInfo(in);
        }

        public SyncInfo[] newArray(int size) {
            return new SyncInfo[size];
        }
    };
}
