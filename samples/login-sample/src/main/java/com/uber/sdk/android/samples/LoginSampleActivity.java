/*
 * Copyright (c) 2016 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.sdk.android.samples;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.uber.sdk.android.core.UberSdk;
import com.uber.sdk.android.core.auth.AccessTokenManager;
import com.uber.sdk.android.core.auth.AuthenticationError;
import com.uber.sdk.android.core.auth.LoginButton;
import com.uber.sdk.android.core.auth.LoginCallback;
import com.uber.sdk.android.core.auth.LoginManager;
import com.uber.sdk.android.rides.samples.BuildConfig;
import com.uber.sdk.android.rides.samples.R;
import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.rides.client.Session;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.error.ApiError;
import com.uber.sdk.rides.client.error.ErrorParser;
import com.uber.sdk.rides.client.model.ProductsResponse;
import com.uber.sdk.rides.client.model.UserProfile;
import com.uber.sdk.rides.client.services.RidesService;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.uber.sdk.android.core.utils.Preconditions.checkNotNull;
import static com.uber.sdk.android.core.utils.Preconditions.checkState;


/**
 * Activity that demonstrates how to use a {@link com.uber.sdk.android.core.auth.LoginManager}.
 */
public class LoginSampleActivity extends AppCompatActivity {
    //Please update CLIENT_ID and REDIRECT_URI below with your app's values.

    public static final String CLIENT_ID = BuildConfig.CLIENT_ID;
    public static final String REDIRECT_URI = BuildConfig.REDIRECT_URI;


    private static final String LOG_TAG = "LoginSampleActivity";

    private static final int LOGIN_BUTTON_CUSTOM_REQUEST_CODE = 1112;
    private static final int CUSTOM_BUTTON_REQUEST_CODE = 1113;


    private LoginButton blackButton;
    private LoginButton whiteButton;
    private Button customButton;
    private AccessTokenManager accessTokenManager;
    private LoginManager loginManager;
    private SessionConfiguration configuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        configuration = new SessionConfiguration.Builder()
                .setClientId(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .setEndpointRegion(SessionConfiguration.EndpointRegion.CHINA)
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.RIDE_WIDGETS))
                .build();

        validateConfiguration(configuration);

        accessTokenManager = new AccessTokenManager(this);

        //Create a button with a custom request code
        whiteButton = (LoginButton) findViewById(R.id.uber_button_white);
        whiteButton.setCallback(new SampleLoginCallback())
                .setSessionConfiguration(configuration);

        //Create a button using a custom AccessTokenManager
        //Custom Scopes are set using XML for this button as well in R.layout.activity_sample
        blackButton = (LoginButton) findViewById(R.id.uber_button_black);
        blackButton.setAccessTokenManager(accessTokenManager)
                .setCallback(new SampleLoginCallback())
                .setSessionConfiguration(configuration)
                .setRequestCode(LOGIN_BUTTON_CUSTOM_REQUEST_CODE);


        //Use a custom button with an onClickListener to call the LoginManager directly
        loginManager = new LoginManager(accessTokenManager,
                new SampleLoginCallback(),
                configuration,
                CUSTOM_BUTTON_REQUEST_CODE);

        customButton = (Button) findViewById(R.id.custom_uber_button);
        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginManager.login(LoginSampleActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loginManager.isAuthenticated()) {
            loadProfileInfo();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LOG_TAG, String.format("onActivityResult requestCode:[%s] resultCode [%s]",
                requestCode, resultCode));

        //Allow each a chance to catch it.
        whiteButton.onActivityResult(requestCode, resultCode, data);

        blackButton.onActivityResult(requestCode, resultCode, data);

        loginManager.onActivityResult(this, requestCode, resultCode, data);
    }

    private class SampleLoginCallback implements LoginCallback {

        @Override
        public void onLoginCancel() {
            Toast.makeText(LoginSampleActivity.this, R.string.user_cancels_message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginError(@NonNull AuthenticationError error) {
            Toast.makeText(LoginSampleActivity.this,
                    getString(R.string.login_error_message, error.name()), Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onLoginSuccess(@NonNull AccessToken accessToken) {
            loadProfileInfo();
        }

        @Override
        public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {
            Toast.makeText(LoginSampleActivity.this, getString(R.string.authorization_code_message, authorizationCode),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void loadProfileInfo() {
        Session session = loginManager.getSession();
        RidesService service = UberRidesApi.with(session).build().createService();
        service.getProducts(0, 0).enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {

            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {

            }
        });
        service.getUserProfile()
                .enqueue(new Callback<UserProfile>() {
                    @Override
                    public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(LoginSampleActivity.this, getString(R.string.greeting, response.body().getFirstName() + "|" + response.body().getPicture()), Toast.LENGTH_LONG).show();
                        } else {
                            ApiError error = ErrorParser.parseError(response);
                            Toast.makeText(LoginSampleActivity.this, error.getClientErrors().get(0).getTitle(), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserProfile> call, Throwable t) {

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        accessTokenManager = new AccessTokenManager(this);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear) {
            accessTokenManager.removeAccessToken();
            Toast.makeText(this, "AccessToken cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_copy) {
            AccessToken accessToken = accessTokenManager.getAccessToken();

            String message = accessToken == null ? "No AccessToken stored" : "AccessToken copied to clipboard";
            if (accessToken != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("UberSampleAccessToken", accessToken.getToken());
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Validates the local variables needed by the Uber SDK used in the sample project
     *
     * @param configuration
     */
    private void validateConfiguration(SessionConfiguration configuration) {
//        String nullError = "%s must not be null";
//        String sampleError = "Please update your %s in the gradle.properties of the project before " +
//                "using the Uber SDK Sample app. For a more secure storage location, " +
//                "please investigate storing in your user home gradle.properties ";
//
//        checkNotNull(configuration, String.format(nullError, "SessionConfiguration"));
//        checkNotNull(configuration.getClientId(), String.format(nullError, "Client ID"));
//        checkNotNull(configuration.getRedirectUri(), String.format(nullError, "Redirect URI"));
//        checkState(!configuration.getClientId().equals("qhjygl7-TQACLuyOMqDlPl3pdDcJomvA"),
//                String.format(sampleError, "Client ID"));
//        checkState(!configuration.getRedirectUri().equals("https://login.uber.com/oauth/"),
//                String.format(sampleError, "Redirect URI"));
    }
}
