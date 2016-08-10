/*
*
* AuthorizeCloudDevices.java
*
* Created by Namrata Garach on 4/1/16
*
* */

package cloud.artik.example.cloud_connector_device;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class AuthorizeDeviceActivity extends Activity {
    private static final String TAG = "AuthorizeDevice";

    private WebView mWebView;
    private ProgressDialog mProgressBar;

    boolean mAuthSucceeded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorize_device);

        mWebView = (WebView) findViewById(R.id.authorize_webview);

        String authUrl = ArtikCloudSession.getInstance().getAuthWith3rdPartyCloudUri();

        mAuthSucceeded = false;
        loadWebView(authUrl);

        /*
         * Enable javascript in the WebView (off by default). The annotation
         */
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
    }

    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Entering shouldOverrideUrlLoading with url:" + url);

            if (url.contains(ArtikCloudSession.REFERER)) {
                Log.d(TAG, "Catch Referer");
                mAuthSucceeded = true;
                handleAuthSuccess();
                return true;
            }

            boolean override = super.shouldOverrideUrlLoading(view, url);
            Log.d(TAG, "Leaving shouldOverrideUrlLoading with url:" + url + " shouldOverride = " + override);
            return override;
        }


        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Log.i("Request", request.getUrl() + request.getRequestHeaders().toString());
            return null;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "YWU Entering onPageFinished with url:" + url + " authSucceeded = " + mAuthSucceeded);
            if (mProgressBar.isShowing()) {
                mProgressBar.dismiss();
            }
         }
    };

    private void loadWebView(final String url) {
        Log.d(TAG, "loadWebView url " + url);
        //String postData = "token=" + ArtikCloudSession.getInstance().getAccessToken();
        mProgressBar = ProgressDialog.show(AuthorizeDeviceActivity.this, "", "Loading...");
        mWebView.setWebViewClient(webViewClient);
        //mWebView.postUrl(url, EncodingUtils.getBytes(postData, "BASE64"));

        String content = getString(R.string.start_subscription_page, url, ArtikCloudSession.getInstance().getAccessToken());
        mWebView.loadDataWithBaseURL(ArtikCloudSession.REFERER, content, "text/html", "UTF-8", null); //should be http or https to set referer

    }

    private void handleAuthSuccess()
    {
        Log.w(TAG, "Authorization succeeded");
        startDeviceActivity();
        int duration = Toast.LENGTH_LONG;
        String text = "Authorization workflow succeeded";
        Toast toast = Toast.makeText(AuthorizeDeviceActivity.this, text, duration);
        toast.show();
    }

    private void startDeviceActivity()
    {
        startActivity(new Intent(this, DeviceActivity.class));
    }



}
