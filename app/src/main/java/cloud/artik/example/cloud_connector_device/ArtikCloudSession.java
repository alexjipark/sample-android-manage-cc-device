/*
 * Copyright (C) 2016 Samsung Electronics Co., Ltd.
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

package cloud.artik.example.cloud_connector_device;

import android.util.Log;

import cloud.artik.api.DevicesApi;
import cloud.artik.api.UsersApi;
import cloud.artik.client.ApiClient;
import cloud.artik.model.Device;

public class ArtikCloudSession {
    private final static String TAG = ArtikCloudSession.class.getSimpleName();

    // Copy them from the corresponding application in the Developer Dashboard
    public static final String CLIENT_ID = "<YOUR CLIENT ID>";
    public static final String REDIRECT_URL = "app://redirect";

    public static final String ARTIK_CLOUD_AUTH_BASE_URL = "https://accounts.artik.cloud";
    public static final String AUTHORIZE_API_URL_PART_1 = "https://api.artik.cloud/v1.1/devices/";
    public static final String AUTHORIZE_API_URL_PART_2 = "/providerauth";
    public static final String REFERER = "http://myapp/subscribe";

    private static ArtikCloudSession ourInstance = new ArtikCloudSession();

    private UsersApi mUsersApi = null;
    private DevicesApi mDevicesApi = null;
    private String mAccessToken = null;
    private String mUserId = null;

    private Device mDevice = null;

    public static ArtikCloudSession getInstance() {
        return ourInstance;
    }

    private ArtikCloudSession() {
    }

    public void setAccessToken(String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Attempt to set an invalid token");
            mAccessToken = null;
            return;
        }
        mAccessToken = token;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setDevice(Device d) {mDevice = d;}
    public Device getDevice() {return mDevice;}

    public void setupArtikCloudRestApis() {
        ApiClient apiClient = new ApiClient();
        apiClient.setAccessToken(mAccessToken);
        apiClient.setDebugging(true);

        mUsersApi = new UsersApi(apiClient);
        mDevicesApi = new DevicesApi(apiClient);
    }

    public UsersApi getUsersApi() {
        return mUsersApi;
    }

    public DevicesApi getDevicesApi() {
        return mDevicesApi;
    }

    public String getUserId() {return mUserId; }

    public String getAuthorizationRequestUri() {
        //https://accounts.artik.cloud/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=android-app://redirect
        return ARTIK_CLOUD_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URL;
    }

    public String getAuthWith3rdPartyCloudUri() {
        String uri = getDeAuthWith3rdPartyCloudUri();
        if (uri == null) {
            return uri;
        }
        return uri + "?mobile=true";
    }

    public String getDeAuthWith3rdPartyCloudUri() {
        if (getDevice() == null || getDevice().getId() == null) {
            Log.w(TAG, "getAuthWith3rdPartyCloudUri: null device or device id");
            return null;
        }
        return AUTHORIZE_API_URL_PART_1 + getDevice().getId() + AUTHORIZE_API_URL_PART_2;

    }

    public void reset() {
        mUsersApi = null;
        mAccessToken = null;
        mUserId = null;
    }

    public void setUserId(String uid) {
        if (uid == null || uid.length() <= 0) {
            Log.w(TAG, "setUserId() get null uid");
        }
        mUserId = uid;
    }
}
