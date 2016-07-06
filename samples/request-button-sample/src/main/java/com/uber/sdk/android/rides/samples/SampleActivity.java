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

package com.uber.sdk.android.rides.samples;

import android.app.Activity;
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
import com.uber.sdk.android.core.auth.LoginCallback;
import com.uber.sdk.android.core.auth.LoginManager;
import com.uber.sdk.android.rides.RideParameters;
import com.uber.sdk.android.rides.RideRequestActivity;
import com.uber.sdk.android.rides.RideRequestActivityBehavior;
import com.uber.sdk.android.rides.RideRequestButton;
import com.uber.sdk.android.rides.RideRequestButtonCallback;
import com.uber.sdk.android.rides.RideRequestViewError;
import com.uber.sdk.core.auth.AccessToken;
import com.uber.sdk.core.auth.Scope;
import com.uber.sdk.core.auth.internal.OAuthScopes;
import com.uber.sdk.rides.auth.OAuth2Credentials;
import com.uber.sdk.rides.client.ServerTokenSession;
import com.uber.sdk.rides.client.Session;
import com.uber.sdk.rides.client.SessionConfiguration;
import com.uber.sdk.rides.client.UberRidesApi;
import com.uber.sdk.rides.client.error.ApiError;
import com.uber.sdk.rides.client.model.Product;
import com.uber.sdk.rides.client.model.ProductsResponse;
import com.uber.sdk.rides.client.services.RidesService;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.uber.sdk.android.core.utils.Preconditions.checkNotNull;
import static com.uber.sdk.android.core.utils.Preconditions.checkState;

/**
 * Activity that demonstrates how to use a {@link com.uber.sdk.android.rides.RideRequestButton}.
 */
public class SampleActivity extends AppCompatActivity implements RideRequestButtonCallback {
    //国内
    private static final String DROPOFF_ADDR = "";
    private static final Double DROPOFF_LAT = 22.579968;
    private static final Double DROPOFF_LONG = 113.908252 ;
    private static final String DROPOFF_NICK = "前海自贸区高新奇";
    private static final String ERROR_LOG_TAG = "UberSDK-SampleActivity";
    private static final String PICKUP_ADDR = "1455 Market Street, San Francisco";
    private static Double PICKUP_LAT = 22.524023;
    private static Double PICKUP_LONG = 113.908252;
    private static final String PICKUP_NICK = "";
    private static String UBERX_PRODUCT_ID = "e3292ff6-3da7-4212-b251-7210416f923f";


    private static final String CLIENT_ID = "";
    private static final String REDIRECT_URI = "";
    private static final String SERVER_TOKEN = "";

    /******
     * //国外
     * private static final String DROPOFF_ADDR = "One Embarcadero Center, San Francisco";
     * private static final Double DROPOFF_LAT = 37.795079;
     * private static final Double DROPOFF_LONG = -122.397805;
     * private static final String DROPOFF_NICK = "Embarcadero";
     * private static final String ERROR_LOG_TAG = "UberSDK-SampleActivity";
     * private static final String PICKUP_ADDR = "1455 Market Street, San Francisco";
     * private static final Double PICKUP_LAT = 37.775304;
     * private static final Double PICKUP_LONG = -122.417522;
     * private static final String PICKUP_NICK = "Uber HQ";
     * private static String UBERX_PRODUCT_ID = "a1111c8c-c720-46c3-8534-2fcdd730040d";
     * private static final int WIDGET_REQUEST_CODE = 1234;
     * <p/>
     * private static final String CLIENT_ID = ""; //WORLD
     * private static final String REDIRECT_URI = ""; //WORLD
     * private static final String SERVER_TOKEN = ""; //WORLD
     ***/
    private AccessTokenManager accessTokenManager;
    private LoginManager loginManager;
    private static final int CUSTOM_BUTTON_REQUEST_CODE = 1113;
    private static final int WIDGET_REQUEST_CODE = 1234;
    private Button customButton;
    RideParameters rideParametersForProduct;
    private SessionConfiguration configuration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);


        configuration = new SessionConfiguration.Builder()
                .setRedirectUri(REDIRECT_URI)
                .setClientId(CLIENT_ID)
                .setServerToken(SERVER_TOKEN)
                .setEnvironment(SessionConfiguration.Environment.SANDBOX) //Useful for testing your app in the sandbox environment
                .setScopes(Arrays.asList(Scope.PROFILE, Scope.RIDE_WIDGETS)) //Your scopes for authentication here
                .setEndpointRegion(SessionConfiguration.EndpointRegion.CHINA)
                .build();

        accessTokenManager = new AccessTokenManager(this);

        loginManager = new LoginManager(accessTokenManager, new SampleLoginCallback(), configuration, CUSTOM_BUTTON_REQUEST_CODE);
        customButton = (Button) findViewById(R.id.open_uber);
        customButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginManager.isAuthenticated()) {
                    getProducts();
                } else {
                    loginManager.login(SampleActivity.this);
                }
            }
        });


//        UberSdk.initialize(configuration);
        // Optional: to use the SDK in China, set the region property
        // See https://developer.uber.com/docs/china for more details.

        validateConfiguration(configuration);
        ServerTokenSession session = new ServerTokenSession(configuration);
        rideParametersForProduct = new RideParameters.Builder()
                .setProductId(UBERX_PRODUCT_ID)
                .setPickupLocation(PICKUP_LAT, PICKUP_LONG, PICKUP_NICK, PICKUP_ADDR)
                .setDropoffLocation(DROPOFF_LAT, DROPOFF_LONG, DROPOFF_NICK, DROPOFF_ADDR)
                .build();

        // This button demonstrates launching the RideRequestActivity (customized button behavior).
        // You can optionally setRideParameters for pre-filled pickup and dropoff locations.
        RideRequestButton uberButtonWhite = (RideRequestButton) findViewById(R.id.uber_button_white);
        RideRequestActivityBehavior rideRequestActivityBehavior = new RideRequestActivityBehavior(this, WIDGET_REQUEST_CODE, configuration);
        uberButtonWhite.setRequestBehavior(rideRequestActivityBehavior);
        uberButtonWhite.setRideParameters(rideParametersForProduct);
        uberButtonWhite.setSession(session);
        uberButtonWhite.loadRideInformation();

//        Session session = rideRequestActivityBehavior.;
//        RidesService service = UberRidesApi.with(session).build().createService();
    }

    private class SampleLoginCallback implements LoginCallback {

        @Override
        public void onLoginCancel() {
        }

        @Override
        public void onLoginError(@NonNull AuthenticationError error) {
        }

        @Override
        public void onLoginSuccess(@NonNull AccessToken accessToken) {
            getProducts();
        }

        @Override
        public void onAuthorizationCodeReceived(@NonNull String authorizationCode) {
        }
    }

    private void getProducts() {
        Session session = loginManager.getSession();
        RidesService service = UberRidesApi.with(session).build().createService();
        service.getProducts(22.579968f,113.927403f ).enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                if (response != null) {
                    if (response.body() != null)
                        if (response.body().getProducts() != null) {
                            for (Product product : response.body().getProducts()) {
                                Log.e("---------getProduct----", "---------getProductId----" + product.getProductId());
                            }
                            UBERX_PRODUCT_ID = response.body().getProducts().get(0).getProductId();
                            RideRequestActivityBehavior rideRequestActivityBehavior = new RideRequestActivityBehavior(SampleActivity.this, WIDGET_REQUEST_CODE, configuration);
                            rideRequestActivityBehavior.requestRide(SampleActivity.this, rideParametersForProduct);
                        }
                }
                Log.e("---------getProducts----", "---------getProductId----" + UBERX_PRODUCT_ID);
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {

            }
        });
    }

    @Override
    public void onRideInformationLoaded() {
        Toast.makeText(this, "Estimates have been refreshed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(ApiError apiError) {
        Toast.makeText(this, apiError.getClientErrors().get(0).getTitle(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e("SampleActivity", "Error obtaining Metadata", throwable);
        Toast.makeText(this, "Connection error", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        loginManager.onActivityResult(this, requestCode, resultCode, data);
        if (requestCode == WIDGET_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED && data != null) {
            if (data.getSerializableExtra(RideRequestActivity.AUTHENTICATION_ERROR) != null) {
                AuthenticationError error = (AuthenticationError) data.getSerializableExtra(RideRequestActivity
                        .AUTHENTICATION_ERROR);
                Toast.makeText(SampleActivity.this, "Auth error " + error.name(), Toast.LENGTH_SHORT).show();
                Log.d(ERROR_LOG_TAG, "Error occurred during authentication: " + error.toString
                        ().toLowerCase());
            } else if (data.getSerializableExtra(RideRequestActivity.RIDE_REQUEST_ERROR) != null) {
                RideRequestViewError error = (RideRequestViewError) data.getSerializableExtra(RideRequestActivity
                        .RIDE_REQUEST_ERROR);
                Toast.makeText(SampleActivity.this, "RideRequest error " + error.name(), Toast.LENGTH_SHORT).show();
                Log.d(ERROR_LOG_TAG, "Error occurred in the Ride Request Widget: " + error.toString().toLowerCase());
            }
        }
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

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);

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
        } else if (id == R.id.action_refresh_meta_data) {
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
//        checkNotNull(configuration.getServerToken(), String.format(nullError, "Server Token"));
//        checkState(!configuration.getClientId().equals(CLIENT_ID),
//                String.format(sampleError, "Client ID"));
//        checkState(!configuration.getRedirectUri().equals(REDIRECT_URI),
//                String.format(sampleError, "Redirect URI"));
//        checkState(!configuration.getRedirectUri().equals(SERVER_TOKEN),
//                String.format(sampleError, "Server Token"));
    }
}
