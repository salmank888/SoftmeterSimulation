package com.example.skhalid.softmetersimulation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private static final String TAG = "SOFTMETER";
    private EditText apfVal;
    private EditText pumVal;
    private EditText putVal;

    private EditText aduVal;
    private EditText adcVal;
    private EditText atuVal;
    private EditText atcVal;


    private TextView totalTimeVal;
    private TextView totalDistanceVal;
    private TextView fareVal;

    private BootstrapButton startBtn;
    private BootstrapButton stopBtn;
    private BootstrapButton resetBtn;

    protected TextSwitcher addressVal;
    protected ArrayList<LatiLongi> coordinatesList;
    double ambPickupFee = 4.00; //APF
    double puMiles = 0; // PUM
    double puTime = 0;

    double additionalDistanceUnit = 0.2; // ADU - Fraction Of KM
    double additionalDistanceUnitCost = 0.4; // ADC

    double additionalTimeUnit = 0.25; // ATU - Fraction of Minute
    double additionalTimeUnitCost =  0.15; //ATC

    double deltaTime = 1.0/6.0; //Seconds
    double deltaDistance;

    double totalDistance = 0.0; //KM
    double totalTime = 0.0; // Minutes
    int K=0; //Iterator
    double accumDeltaD = 0.0;  //KM
    double acccumDeltaT = 0.0; //Minutes
    double tempLat, tempLong;

    double distanceCost, timeCost;
    double meterValue;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    final DecimalFormat dFormat = new DecimalFormat("0.00");
    Future<?> future;
    boolean isTestMode = true;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates;
    private SharedPreferences pref;
    protected String mAddressOutput;

    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;
    private AlertDialogFragment gpsDialog;
    private LocationManager lm;
    private String locationProviders;
    private int locationMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        apfVal = (EditText) findViewById(R.id.apfVal);
        pumVal = (EditText) findViewById(R.id.pumVal);
        putVal = (EditText) findViewById(R.id.putVal);

         aduVal  = (EditText) findViewById(R.id.aduVal);
         adcVal  = (EditText) findViewById(R.id.adcVal);
         atuVal  = (EditText) findViewById(R.id.atuVal);
         atcVal  = (EditText) findViewById(R.id.atcVal);

        totalTimeVal = (TextView) findViewById(R.id.timeVal);
        totalDistanceVal = (TextView) findViewById(R.id.distanceVal);
        fareVal = (TextView) findViewById(R.id.fareVal);

        addressVal = (TextSwitcher) findViewById(R.id.textSwitcher);
        Typeface tf = Typeface.createFromAsset(getAssets(),
                "digital-7.ttf");
        fareVal.setTypeface(tf);

        startBtn = (BootstrapButton) findViewById(R.id.startButton);
        stopBtn = (BootstrapButton) findViewById(R.id.stopButton);
        resetBtn = (BootstrapButton) findViewById(R.id.resetButton);

        stopBtn.setBootstrapButtonEnabled(false);
        resetBtn.setBootstrapButtonEnabled(false);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        resetBtn.setOnClickListener(this);

        coordinatesList = new ArrayList<LatiLongi>();

        mRequestingLocationUpdates = true;

        addressVal.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                // TODO Auto-generated method stub
                // create new textView and set the properties like clolr, size etc
                TextView myText = new TextView(MainActivity.this);
                myText.setGravity(Gravity.CENTER);
                myText.setTextSize(15);
                myText.setTextColor(Color.BLUE);
                return myText;
            }
        });

        // Declare the in and out animations and initialize them
        Animation in = AnimationUtils.loadAnimation(this,android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);

        // set the animation type of textSwitcher
        addressVal.setInAnimation(in);
        addressVal.setOutAnimation(out);

        mResultReceiver = new AddressResultReceiver(new Handler());

        pref = getSharedPreferences("Softmeter", MODE_PRIVATE);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        buildGoogleApiClient();

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.item1) {
                item.setChecked(true);
                isTestMode = true;

        }else if(id == R.id.item2){
                item.setChecked(true);
                isTestMode = false;

        }

        Toast.makeText(getApplicationContext(), "Fare Calculation According to " + item.getTitle(), Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                mRequestingLocationUpdates = true;
                if(apfVal.getText().toString().length() > 0 && pumVal.getText().toString().length() > 0 && putVal.getText().toString().length() > 0 && aduVal.getText().toString().length() > 0 && adcVal.getText().toString().length() > 0 && atuVal.getText().toString().length() > 0 && atcVal.getText().toString().length() > 0) {
                    if(K == 0) {
                        readTextFile();
                         ambPickupFee = Double.parseDouble(apfVal.getText().toString()); //APF
                         puMiles = Double.parseDouble(pumVal.getText().toString()); // PUM
                         puTime = Double.parseDouble(putVal.getText().toString());

                         additionalDistanceUnit = Double.parseDouble(aduVal.getText().toString()); // ADU - Fraction Of KM
                         additionalDistanceUnitCost = Double.parseDouble(adcVal.getText().toString()); // ADC

                         additionalTimeUnit = Double.parseDouble(atuVal.getText().toString()); // ATU - Fraction of Minute
                         additionalTimeUnitCost =  Double.parseDouble(atcVal.getText().toString()); //ATC

                        apfVal.setEnabled(false);
                        pumVal.setEnabled(false);
                        putVal.setEnabled(false);

                        aduVal.setEnabled(false);
                        adcVal.setEnabled(false);
                        atuVal.setEnabled(false);
                        atcVal.setEnabled(false);
                    }

                    startBtn.setBootstrapButtonEnabled(false);
                    stopBtn.setBootstrapButtonEnabled(true);
                    resetBtn.setBootstrapButtonEnabled(true);
                    if(isTestMode)
                    calculateAndShowFarefromFile();
                    else
                        calculateAndShowFare();
                }else
                Toast.makeText(getApplicationContext(), "Provide Proper Values of Fields", Toast.LENGTH_LONG).show();
                break;
            case R.id.stopButton:
                stopBtn.setBootstrapButtonEnabled(false);
                startBtn.setBootstrapButtonEnabled(true);
                future.cancel(true);
                break;
            case R.id.resetButton:
                if(future.isCancelled()) {
                    totalDistance = 0.0; //KM
                    totalTime = 0.0; // Minutes
                    K = 0; //Iterator
                    accumDeltaD = 0.0;  //KM
                    acccumDeltaT = 0.0; //Minutes
                    totalDistanceVal.setText(dFormat.format(totalDistance));
                    totalTimeVal.setText(dFormat.format(totalTime));
                    fareVal.setText(dFormat.format(0.0));

                    apfVal.setEnabled(true);
                    pumVal.setEnabled(true);
                    putVal.setEnabled(true);

                    aduVal.setEnabled(true);
                    adcVal.setEnabled(true);
                    atuVal.setEnabled(true);
                    atcVal.setEnabled(true);

                    stopBtn.setBootstrapButtonEnabled(true);
                    startBtn.setBootstrapButtonEnabled(true);

                }
                else
                    Toast.makeText(getApplicationContext(), "Please Stop First", Toast.LENGTH_LONG).show();

                break;
            default:
                break;
        }
    }

    private void readTextFile(){
        //Find the directory for the SD Card using the API
//*Don't* hardcode "/sdcard"
        File sdcard = Environment.getExternalStorageDirectory();

//Get the text file
        File file = new File(sdcard,"10.txt");

//Read text from file
        StringBuilder text = new StringBuilder();
        coordinatesList.clear();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
                coordinatesList.add(new LatiLongi(Double.parseDouble(line.split("\t")[1]), Double.parseDouble(line.split("\t")[2])));
            }
            br.close();
        }
        catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void calculateAndShowFarefromFile(){
        future = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if(K <= coordinatesList.size()) {
                    if (K == 0) {
                        meterValue = ambPickupFee;
                    } else {
                        deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, coordinatesList.get(K).lat, coordinatesList.get(K).lon)/1000.00;
                        totalDistance = totalDistance + deltaDistance;
                        totalTime = totalTime + deltaTime;
                        if(totalTime > puTime || totalDistance > puMiles){
                            acccumDeltaT = acccumDeltaT + deltaTime;
                            timeCost = (int)(acccumDeltaT/additionalTimeUnit)*additionalTimeUnitCost;
                            acccumDeltaT = acccumDeltaT % additionalTimeUnit;

                            accumDeltaD = accumDeltaD + deltaDistance;
                            distanceCost = (int)(accumDeltaD/additionalDistanceUnit)*additionalDistanceUnitCost;
                            accumDeltaD = accumDeltaD % additionalDistanceUnit;
//                            if(!isTestMode) {
                                if (timeCost > distanceCost)
                                    accumDeltaD = 0.0;
                                else
                                    acccumDeltaT = 0.0;
//                            }
                            meterValue = meterValue + Math.max(distanceCost, timeCost);

                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            totalDistanceVal.setText(dFormat.format(totalDistance));
                            totalTimeVal.setText(dFormat.format(totalTime));
                            fareVal.setText(dFormat.format(meterValue));
                        }
                    });

                    tempLat = coordinatesList.get(K).lat;
                    tempLong = coordinatesList.get(K).lon;
                    K = K + 1;
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void calculateAndShowFare(){
        future = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if(pref != null) {
                    if (K == 0) {
                        meterValue = ambPickupFee;
                    } else {
                        deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, Double.parseDouble(pref.getString("Lattitude","0.0")), Double.parseDouble(pref.getString("Longitude","0.0")))/1000.00;
                        totalDistance = totalDistance + deltaDistance;
                        totalTime = totalTime + deltaTime;
                        if(totalTime > puTime || totalDistance > puMiles){
                            acccumDeltaT = acccumDeltaT + deltaTime;
                            timeCost = (int)(acccumDeltaT/additionalTimeUnit)*additionalTimeUnitCost;
                            acccumDeltaT = acccumDeltaT % additionalTimeUnit;

                            accumDeltaD = accumDeltaD + deltaDistance;
                            distanceCost = (int)(accumDeltaD/additionalDistanceUnit)*additionalDistanceUnitCost;
                            accumDeltaD = accumDeltaD % additionalDistanceUnit;
//                            if(!isTestMode) {
                            if (timeCost >= distanceCost)
                                accumDeltaD = 0.0;
                            else
                                acccumDeltaT = 0.0;
//                            }
                            meterValue = meterValue + Math.max(distanceCost, timeCost);

                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            totalDistanceVal.setText(dFormat.format(totalDistance));
                            totalTimeVal.setText(dFormat.format(totalTime));
                            fareVal.setText(dFormat.format(meterValue));
                        }
                    });

                    tempLat = Double.parseDouble(pref.getString("Lattitude","0.0"));
                    tempLong = Double.parseDouble(pref.getString("Longitude","0.0"));
                    K = K + 1;
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
//        changeModetoHighAccuracy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
    @Override
    protected void onDestroy() {
        scheduler.shutdownNow();
        super.onDestroy();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();

    }

    @Override
    public void onConnected(Bundle bundle) {

        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            changeModetoHighAccuracy();

        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//            updateUI();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {

        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && location.getAccuracy() > 30)
        changeModetoHighAccuracy();
       else if(location.getAccuracy() < 100) {
            mCurrentLocation = location;
            pref.edit().putString("Lattitude", String.valueOf(mCurrentLocation.getLatitude())).putString("Longitude", String.valueOf(mCurrentLocation.getLongitude())).commit();
            Toast.makeText(this, getResources().getString(R.string.location_updated_message) + mCurrentLocation.getAccuracy() + " meters",
                    Toast.LENGTH_SHORT).show();
            if (!isTestMode)
                if (future != null)
                    if (future.isCancelled()) {
                        tempLat = Double.parseDouble(pref.getString("Lattitude", "0.0"));
                        tempLong = Double.parseDouble(pref.getString("Longitude", "0.0"));
                    }
            startIntentService();
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    protected void displayAddressOutput() {
        addressVal.setText(mAddressOutput);
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mCurrentLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }
    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
//                Toast.makeText(getApplicationContext(), getString(R.string.address_found), Toast.LENGTH_LONG).show();
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
//            mAddressRequested = false;
//            updateUIWidgets();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }
    public void changeModetoHighAccuracy()
    {
        gpsDialog = AlertDialogFragment.newInstance("Inaccurate Location", "", "Location is not up to mark. \nPlease change Location Mode to High Accuracy.", "OK", Constants.GPS);
        gpsDialog.show(getSupportFragmentManager(), "dialog");

    }
}
