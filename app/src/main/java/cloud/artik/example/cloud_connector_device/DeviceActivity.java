package cloud.artik.example.cloud_connector_device;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.artik.client.ApiCallback;
import cloud.artik.client.ApiException;
import cloud.artik.model.Device;
import cloud.artik.model.DeviceEnvelope;
import cloud.artik.model.User;
import cloud.artik.model.UserEnvelope;

public class DeviceActivity extends Activity {
    private static final String TAG = "DeviceActivity";

    // The following is one of the ways to get device type id
    // - Go to ARTIK Cloud APIConsole
    // - Use /devicetypes and fill in the name of the device type like "Withings" or "Fitbit"
    // - In the response of the API call, you get device type id
    //
    // WITHINGS DEVICE com.samsung.sami.devices.withings_device dt29673f0481b4401bb73a622353b96150
    // Fitbit Device com.samsung.sami.devices.fitbit_device dt8e71cabde68b4028b106832247cd6d72
    //
    private static final String DEVICE_TYPE_ID = "dt29673f0481b4401bb73a622353b96150";

    private static final String DEVICE_NAME = "My Withings";

    private TextView mWelcome;
    private TextView mAddDeleteResponse;
    private Button mAddOrDeleteDeviceBtn;
    private Button mAuthDeauthBtn;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mAddOrDeleteDeviceBtn = (Button)findViewById(R.id.add_delete_btn);
        mAuthDeauthBtn = (Button)findViewById(R.id.auth_deauth_btn);
        mWelcome = (TextView)findViewById(R.id.welcome);
        mAddDeleteResponse = (TextView)findViewById(R.id.add_delete_response);

        getUserInfo();

    }

    private void enableAddOrRemoveAuthBtn()
    {
        mAuthDeauthBtn.setEnabled(true);
        Device d = ArtikCloudSession.getInstance().getDevice();
        if (d == null || d.getId() == null) {
            Log.e(TAG, "Null device or invalid device id. Cannot add or remove authorization. Early return");
            return;
        }
        if (d.getNeedProviderAuth()) {
            enableAuthBtn();
        } else {
            enableRemoveAuthBtn();
        }
    }

    private void enableAuthBtn()
    {
        mAuthDeauthBtn.setText("Authorize");
        mAuthDeauthBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, ": Authorization button is clicked.");
                startDeviceAuthWith3rdPartyCloud();
            }
        });
    }

    private void enableRemoveAuthBtn()
    {
        mAuthDeauthBtn.setText("Remove authorization");
        mAuthDeauthBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, ": Remove authorization button is clicked.");
                deAuthDevice();
            }
        });
    }

    private void disableAddOrRemoveAuthBtn()
    {
        mAuthDeauthBtn.setText("Authorize");
        mAuthDeauthBtn.setEnabled(false);
    }

    private void handleAddOrDeleteBtn(String info)
    {
        Device d = ArtikCloudSession.getInstance().getDevice();
        if (d == null || d.getId() == null) {
            enableAddDeviceBtn(info);
        } else {
            enableDeleteDeviceBtn(info);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void enableAddDeviceBtn(String info)
    {
        mAddOrDeleteDeviceBtn.setText("Add device");
        mAddOrDeleteDeviceBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, ": Add button is clicked.");
                addDevice();
            }
        });
        if (info != null) {
            mAddDeleteResponse.setText(info);
        }
        disableAddOrRemoveAuthBtn();
    }

    private void enableDeleteDeviceBtn(String info)
    {
        mAddOrDeleteDeviceBtn.setText("Delete device");
        mAddOrDeleteDeviceBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v(TAG, ": Delete button is clicked.");
                deleteDevice();
            }
        });
        Device d = ArtikCloudSession.getInstance().getDevice();
        if (info != null) {
            mAddDeleteResponse.setText(info);
        } else {
            mAddDeleteResponse.setText(deviceInfoString(d));
        }
        enableAddOrRemoveAuthBtn();
    }

    private void getUserInfo()
    {
        final String tag = TAG + " getSelfAsync";
        if (ArtikCloudSession.getInstance().getUserId() != null) {
            Log.d(tag, " uid = " + ArtikCloudSession.getInstance().getUserId());
            updateDeviceInfo();
            return;
        }
        try {
            ArtikCloudSession.getInstance().getUsersApi().getSelfAsync(new ApiCallback<UserEnvelope>() {
                @Override
                public void onFailure(ApiException exc, int statusCode, Map<String, List<String>> map) {
                    processFailure(tag, exc);
                }

                @Override
                public void onSuccess(UserEnvelope result, int statusCode, Map<String, List<String>> map) {
                    User user = result.getData();
                    Log.v(TAG, "getSelfAsync::setupArtikCloudApi self name = " + user.getFullName());
                    ArtikCloudSession.getInstance().setUserId(user.getId());
                    updateDeviceInfo();
                    updateWelcomeViewOnUIThread("Welcome " + user.getFullName());
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void startDeviceAuthWith3rdPartyCloud() {
        Log.d(TAG, "start device authorization with 3rd party cloud ...");
        // Open webview in new activity to run start subscription
        Intent authorizeCloudDeviceIntent = new Intent(this, AuthorizeDeviceActivity.class);
        startActivity(authorizeCloudDeviceIntent);
    }

    private void addDevice() {
        final String tag = TAG + " addDeviceAsync";
        Device device = new Device();
        device.setDtid(DEVICE_TYPE_ID);
        device.setUid(ArtikCloudSession.getInstance().getUserId());
        device.setName(DEVICE_NAME); //Note this is a limitation --the name is always this one.
        mProgressDialog = ProgressDialog.show(DeviceActivity.this, "", "Connect the device to ARTIK Cloud...");
        try {
            ArtikCloudSession.getInstance().getDevicesApi().addDeviceAsync(device, new ApiCallback<DeviceEnvelope>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    Log.e(tag, "onFailure: e = " + e + "; statusCode = " + statusCode);
                    processFailure(tag, e);
                }

                @Override
                public void onSuccess(DeviceEnvelope result, int statusCode, Map<String, List<String>> responseHeaders) {
                    Log.v(tag, " onSuccess " + result.toString());
                    ArtikCloudSession.getInstance().setDevice(result.getData());
                    Device d = result.getData();
                    String displayResult = "Successfully created:\n" + deviceInfoString(d);
                    handleAddOrDeleteBtnOnUIThread(displayResult);
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private String deviceInfoString(Device d) {
        if (d == null || d.getId() == null) {
            Log.e(TAG, "deviceInfoString received a null device or invalid device ID");
            return null;
        }
        return "device:\nID:" + d.getId() + "\nName:" + d.getName()
                + "\nDtid:" + d.getDtid() + "\nneedProviderAuth:" + d.getNeedProviderAuth();
    }

    private void deleteDevice() {
        final String tag = TAG + " deleteDeviceAsync";
        Device d = ArtikCloudSession.getInstance().getDevice();
        if (d == null || d.getId() == null) {
            Log.e(tag, "Cannot delete the device: Device is null or device.id is null!");
            return;
        }
        mProgressDialog = ProgressDialog.show(DeviceActivity.this, "", "Delete the device from ARTIK Cloud...");
        try {
            ArtikCloudSession.getInstance().getDevicesApi().deleteDeviceAsync(d.getId(), new ApiCallback<DeviceEnvelope>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    processFailure(tag, e);
                }

                @Override
                public void onSuccess(DeviceEnvelope result, int statusCode, Map<String, List<String>> responseHeaders) {
                    Log.v(tag, " onSuccess " + result.toString());
                    ArtikCloudSession.getInstance().setDevice(null);
                    Device d = result.getData();
                    String displayResult = "Successfully deleted\n" + deviceInfoString(d);
                    handleAddOrDeleteBtnOnUIThread(displayResult);
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void deAuthDevice() {
        final String tag = TAG + " deAuthDevice";
        String uri = ArtikCloudSession.getInstance().getDeAuthWith3rdPartyCloudUri();
        if (uri == null) {
            Log.e(tag, "Cannot remove the authorization from the device: Device is null or device.id is null!");
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.DELETE, uri, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, "onResponse"+ response.toString());
                // De-authorization succeeded. Make another API call to get the updated Device Info.
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                updateDeviceInfo();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                int statusCode = networkResponse.statusCode;
                Log.e(TAG, " " + "status code = " + statusCode +"; Error.Response "+ error.toString());
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Authorization", "bearer " + ArtikCloudSession.getInstance().getAccessToken());
                return params;
        }

        };
        queue.add(sr);
        mProgressDialog =  ProgressDialog.show(DeviceActivity.this, "", "De-authorizing...");
    }

    static void showErrorOnUIThread(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(activity.getApplicationContext(), text, duration);
                toast.show();
            }
        });
    }

    private void updateWelcomeViewOnUIThread(final String text) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWelcome.setText(text);
            }
        });
    }

    private void processFailure(final String context, ApiException exc) {
        String errorDetail = " onFailure with exception" + exc;
        Log.w(context, errorDetail);
        exc.printStackTrace();
        showErrorOnUIThread(context+errorDetail, DeviceActivity.this);
        handleAddOrDeleteBtnOnUIThread(null);
    }

    private void handleAddOrDeleteBtnOnUIThread(final String response) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleAddOrDeleteBtn(response);
            }
        });
    }


    private void updateDeviceInfo()
    {
        final String tag = TAG + " getDeviceAsync";
        Device d = ArtikCloudSession.getInstance().getDevice();
        if (d == null || d.getId() == null) {
            handleAddOrDeleteBtnOnUIThread(null);
            return;
        }
        mProgressDialog = ProgressDialog.show(DeviceActivity.this, "", "Updating device info...");
        try {
            ArtikCloudSession.getInstance().getDevicesApi().getDeviceAsync(ArtikCloudSession.getInstance().getDevice().getId(),
                    new ApiCallback<DeviceEnvelope>() {
                        @Override
                        public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                            processFailure(tag, e);
                        }

                        @Override
                        public void onSuccess(DeviceEnvelope result, int statusCode, Map<String, List<String>> responseHeaders) {
                            Log.v(tag, " onSuccess " + result.toString());
                            ArtikCloudSession.getInstance().setDevice(result.getData());
                            handleAddOrDeleteBtnOnUIThread(null);
                        }

                        @Override
                        public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                        }

                        @Override
                        public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                        }
                    });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }
} //DeviceActivity

