package com.example.skhalid.softmetersimulation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
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

    private Button startBtn;
    private Button stopBtn;

    protected TextSwitcher addressVal;
    public static double ambPickupFee = 4.00; //APF
    public static double puMiles = 0; // PUM
    public static double puTime = 0;

    public static double additionalDistanceUnit = 0.2; // ADU - Fraction Of KM
    public static double additionalDistanceUnitCost = 0.4; // ADC

    public static double additionalTimeUnit = 0.25; // ATU - Fraction of Minute
    public static double additionalTimeUnitCost =  0.15; //ATC

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates;
    public static SharedPreferences pref;
    protected String mAddressOutput;
    private android.support.v7.app.ActionBar actionBar;
    /**
     * Receiver registered with this activity to get the response from FetchAddressIntentService.
     */
    private AddressResultReceiver mResultReceiver;
    private AlertDialogFragment gpsDialog;
    private LocationManager lm;
    private String locationProviders;
    private int locationMode;
    private Messenger mServiceMessenger;
    private DialogFragment mMenuDialogFragment;

    private ServiceConnection mCon = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mServiceMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            mServiceMessenger = null;
            unbindService(mCon);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.action_bar);


        MenuObject close = new MenuObject();
        close.setResource(R.drawable.icn_close);

        MenuObject send = new MenuObject("Send message");
        send.setResource(R.drawable.icn_1);

        List<MenuObject> menuObjects = new ArrayList<>();
        menuObjects.add(close);
        menuObjects.add(send);

        mMenuDialogFragment = ContextMenuDialogFragment.newInstance((int) getResources().getDimension(R.dimen.tool_bar_height), getMenuObjects());

        Typeface tf = Typeface.createFromAsset(getApplicationContext().getAssets(),
                "ManilaSansBld.otf");
        TextView title = (TextView) findViewById(R.id.actionbartextview);

        title.setTypeface(tf);
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

        apfVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    ambPickupFee = Double.parseDouble(s.toString()); //APF
            }
        });

        pumVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    puMiles = Double.parseDouble(s.toString()); //APF
            }
        });

        putVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    puTime = Double.parseDouble(s.toString()); //APF
            }
        });

        aduVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    additionalDistanceUnit = Double.parseDouble(s.toString()); //APF
            }
        });

        adcVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    additionalDistanceUnitCost = Double.parseDouble(s.toString()); //APF
            }
        });

        atuVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    additionalTimeUnit = Double.parseDouble(s.toString()); //APF
            }
        });

        atcVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0)
                    additionalTimeUnitCost = Double.parseDouble(s.toString()); //APF
            }
        });

        addressVal = (TextSwitcher) findViewById(R.id.textSwitcher);

        startBtn = (Button) findViewById(R.id.startButton);
        stopBtn = (Button) findViewById(R.id.stopButton);

        stopBtn.setEnabled(false);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);

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
        Messenger mActivityMessenger;
        mActivityMessenger = new Messenger(new IncomingHandler());

        Intent lIntent = new Intent(MainActivity.this, FloatingService.class);
        lIntent.putExtra("Messenger", mActivityMessenger);
        startService(lIntent);
        bindService(lIntent, mCon, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.context_menu:
                mMenuDialogFragment.show(getSupportFragmentManager(), "ContextMenuDialogFragment");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                sendMessage(Constants.MSG_SOFTMETER_ON);
                stopBtn.setEnabled(true);
                startBtn.setEnabled(false);
//                runTcpClientAsService();
                break;

            case R.id.stopButton:
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
                sendMessage(Constants.MSG_SOFTMETER_OFF);

                break;

            default:
                break;
        }
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

//        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
//            startLocationUpdates();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
//        if (mGoogleApiClient.isConnected()) {
//            stopLocationUpdates();
//        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        unbindService(mCon);
        mGoogleApiClient.disconnect();

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
//            sendMessage(Constants.MSG_LOCATION_CHANGED);
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

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.MSG_DISABLE_FIELDS:
                        apfVal.setEnabled(false);
                        pumVal.setEnabled(false);
                        putVal.setEnabled(false);

                        aduVal.setEnabled(false);
                        adcVal.setEnabled(false);
                        atuVal.setEnabled(false);
                        atcVal.setEnabled(false);

                    break;
                case Constants.MSG_ENABLE_FIELDS:
                    apfVal.setEnabled(true);
                    pumVal.setEnabled(true);
                    putVal.setEnabled(true);

                    aduVal.setEnabled(true);
                    adcVal.setEnabled(true);
                    atuVal.setEnabled(true);
                    atcVal.setEnabled(true);
                    break;
                default:
                    break;

            }
//            String str = (String)msg.obj;
//            Toast.makeText(getApplicationContext(),
//                    "From Service -> " + str, Toast.LENGTH_LONG).show();
        }
    }

    public void sendMessage(int msgType) {
        Message message = Message.obtain();
        switch (msgType) {
            case Constants.MSG_SOFTMETER_ON:
                message.what = msgType;
                break;
            case Constants.MSG_SOFTMETER_OFF:
                message.what = msgType;
                break;
            default :
                break;
        }
        try {
            mServiceMessenger.send(message);
        } catch (Exception e) {
//			FragmentTabsPager fTP = new FragmentTabsPager();
//			fTP.handleException("sendMessage: " + e.getLocalizedMessage());
        }

//        Message msg = new Message();
//        msg.obj = "Hi service..";
//        try {
//            mServiceMessenger.send(msg);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onBackPressed() {
        if(!startBtn.isEnabled())
            Toast.makeText(this, "PowerOff Meter First, then Retry", Toast.LENGTH_LONG).show();
        else
        super.onBackPressed();
    }

    private void runTcpClientAsService() {
        Intent lIntent = new Intent(MainActivity.this, TcpClientService.class);
        this.startService(lIntent);
    }

    private List<MenuObject> getMenuObjects() {
        // You can use any [resource, bitmap, drawable, color] as image:
        // item.setResource(...)
        // item.setBitmap(...)
        // item.setDrawable(...)
        // item.setColor(...)
        // You can set image ScaleType:
        // item.setScaleType(ScaleType.FIT_XY)
        // You can use any [resource, drawable, color] as background:
        // item.setBgResource(...)
        // item.setBgDrawable(...)
        // item.setBgColor(...)
        // You can use any [color] as text color:
        // item.setTextColor(...)
        // You can set any [color] as divider color:
        // item.setDividerColor(...)

        List<MenuObject> menuObjects = new ArrayList<>();

        MenuObject close = new MenuObject();
        close.setResource(R.drawable.icn_close);

        MenuObject send = new MenuObject("Send message");
        send.setResource(R.drawable.icn_1);

//        MenuObject like = new MenuObject("Like profile");
//        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.icn_2);
//        like.setBitmap(b);
//
//        MenuObject addFr = new MenuObject("Add to friends");
//        BitmapDrawable bd = new BitmapDrawable(getResources(),
//                BitmapFactory.decodeResource(getResources(), R.drawable.icn_3));
//        addFr.setDrawable(bd);
//
//        MenuObject addFav = new MenuObject("Add to favorites");
//        addFav.setResource(R.drawable.icn_4);
//
//        MenuObject block = new MenuObject("Block user");
//        block.setResource(R.drawable.icn_5);

        menuObjects.add(close);
        menuObjects.add(send);
//        menuObjects.add(like);
//        menuObjects.add(addFr);
//        menuObjects.add(addFav);
//        menuObjects.add(block);
        return menuObjects;
    }
}
