package com.example.skhalid.softmetersimulation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
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

import com.github.espiandev.showcaseview.ShowcaseView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.softmeter.utils.COS;
import com.softmeter.utils.COSAdapter;
import com.softmeter.utils.IOMessage;
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.graphics.Color.LTGRAY;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMenuItemClickListener {


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
    private AlertDialogFragment infoDialog;
    private AlertDialogFragment forceCloseDialog;

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
    private SwipeRefreshLayout swipeContainer;
    private String versionName;
    private static final int TCP_SERVER_PORT = 21111;

    private Socket s;
    private BufferedReader in;
    private BufferedWriter out;
    private SimpleDateFormat dateFormat;
    private String deviceID;

    private static TelephonyManager tm;
    private static WifiManager wifiMan;
    public static COSAdapter cosAdapter;
    private CircleProgressBar cpr;
    private ShowcaseView sv;
    private ShowcaseView.ConfigOptions co;
    private TextView ins;

    Display mdisp;
    Point mdispSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionBar = getSupportActionBar();

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.action_bar);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                sendMessageToCabDispatch(Constants.MSG_RCF, "");
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        dateFormat = new SimpleDateFormat("MMddyyyyHHmmssSSS", Locale.US);


        MenuObject close = new MenuObject();
        close.setResource(android.R.drawable.ic_menu_close_clear_cancel);

        MenuObject send = new MenuObject("Settings");
        send.setResource(android.R.drawable.ic_menu_manage);

        List<MenuObject> menuObjects = new ArrayList<>();
        menuObjects.add(close);
        menuObjects.add(send);

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

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

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        cpr = (CircleProgressBar) findViewById(R.id.progressBar);

        co = new ShowcaseView.ConfigOptions();

        ins = (TextView) findViewById(R.id.Instruction);
         mdisp = getWindowManager().getDefaultDisplay();
         mdispSize = new Point();
        mdisp.getSize(mdispSize);
//        int maxX = mdispSize.x;
//        int maxY = mdispSize.y;
        runTcpClient();




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
                sendMessage(Constants.MSG_SOFTMETER_POWER_ON);
                stopBtn.setEnabled(true);
                startBtn.setEnabled(false);
                break;

            case R.id.stopButton:
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
                sendMessage(Constants.MSG_SOFTMETER_POWER_OFF);

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
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        stopService(intent);
        Intent lIntent = new Intent(MainActivity.this, FloatingService.class);
        stopService(lIntent);
        try {
            if(s != null)
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public void onMenuItemClick(View view, int position) {
        switch (position){
            case 0:
                break;
            case 1:
                infoDialog = AlertDialogFragment.newInstance("Info", "", "Version: " + versionName, "OK", Constants.INFO);
                infoDialog.show(getSupportFragmentManager(), "dialog");
                break;
            case 2:
                break;
            default:
                break;
        }
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

                case Constants.MSG_TON:
                    sendMessageToCabDispatch(Constants.MSG_TON, pref.getString("FARE", "0.0") + Constants.COLSEPARATOR + pref.getString("EXTRAS", "0.0") + Constants.COLSEPARATOR + pref.getString("DISTANCE", "0.0") +Constants.COLSEPARATOR + pref.getString("TIME", "0.0"));
                    break;

                case Constants.MSG_MOFF:
                    sendMessageToCabDispatch(Constants.MSG_MOFF, pref.getString("FARE", "0.0") + Constants.COLSEPARATOR + pref.getString("EXTRAS", "0.0") + Constants.COLSEPARATOR + pref.getString("DISTANCE", "0.0") +Constants.COLSEPARATOR + pref.getString("TIME", "0.0"));
                    break;

                case Constants.MSG_TOFF:
                      sendMessageToCabDispatch(Constants.MSG_TOFF, pref.getString("FARE", "0.0") + Constants.COLSEPARATOR + pref.getString("EXTRAS", "0.0") + Constants.COLSEPARATOR + pref.getString("DISTANCE", "0.0") +Constants.COLSEPARATOR + pref.getString("TIME", "0.0"));
                    break;

                case Constants.MSG_MON:
                      sendMessageToCabDispatch(Constants.MSG_MON, String.valueOf(msg.arg1));
                    break;

                default:
                    break;

            }
        }
    }

    public void sendMessage(int msgType) {
        Message message = Message.obtain();
        switch (msgType) {
            case Constants.MSG_SOFTMETER_POWER_ON:
                message.what = msgType;
                break;
            case Constants.MSG_SOFTMETER_POWER_OFF:
                message.what = msgType;
                break;
            case Constants.MSG_MON_RSP:
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

    private void runTcpClient() {

        cpr.setVisibility(View.VISIBLE);
        cpr.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);


        new Thread(new Runnable() {
            public boolean socketStatus;
            @Override
            public void run() {
                try {

                    s = new Socket("127.0.0.1", TCP_SERVER_PORT);
//                    s.setSoTimeout(3000);
                    in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    //send output msg
//                    String outMsg = "TCP connecting to " + TCP_SERVER_PORT + System.getProperty("line.separator");
//                    out.write(outMsg);
//                    out.flush();
//                    Log.i("TcpClient", "sent: " + outMsg);

                    //accept server response
                    String inMsg = in.readLine() + System.getProperty("line.separator");
                    if(inMsg.contains("OPEN"))
                        socketStatus = true;
                    else
                    socketStatus = false;
                    Log.i("TcpClient", "received: " + inMsg);
                    //close connection


                } catch (Exception e) {
                    socketStatus = false;
                } finally {
                    if(socketStatus)
                        tcpReceiverThread.start();

                        else {
                        forceCloseDialog = AlertDialogFragment.newInstance("Warning", "", "Please Login Dispatch App First.\nThen Try Again", "OK", Constants.WARNING);
                        forceCloseDialog.setCancelable(false);
                        forceCloseDialog.show(getSupportFragmentManager(), "dialog");
                    }

                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                        cpr.setVisibility(View.GONE);
                            showShowcase();

                        }
                    });
                }
           }
        }).start();

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
        close.setBgColor(getResources().getColor(R.color.soft_blue));
        close.setResource(android.R.drawable.ic_menu_close_clear_cancel);
        close.setTextColor(getResources().getColor(R.color.soft_orange));
        close.setDividerColor(getResources().getColor(R.color.soft_green));

        MenuObject about = new MenuObject("Info");
        about.setBgColor(getResources().getColor(R.color.soft_blue));
        about.setResource(android.R.drawable.ic_menu_info_details);
        about.setTextColor(getResources().getColor(R.color.soft_orange));
        about.setDividerColor(getResources().getColor(R.color.soft_green));

        MenuObject settings = new MenuObject("Settings");
        settings.setBgColor(getResources().getColor(R.color.soft_blue));
        settings.setResource(android.R.drawable.ic_menu_manage);
        settings.setTextColor(getResources().getColor(R.color.soft_orange));
        settings.setDividerColor(getResources().getColor(R.color.soft_green));





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
        menuObjects.add(about);
        menuObjects.add(settings);
//        menuObjects.add(like);
//        menuObjects.add(addFr);
//        menuObjects.add(addFav);
//        menuObjects.add(block);
        return menuObjects;
    }

    private void sendMessageToCabDispatch(int messageType, String msg){
        try {

            String msgTag = getDateTime();
            int msgType = messageType;
            deviceID = getDeviceID();

            String header = msgType + "^" + msgTag + "^" + deviceID;

            String body = String.valueOf(msg);

            String msgToSend = header + Constants.BODYSEPARATOR + body + Constants.EOT;
            //send output msg
            String outMsg = msgToSend + System.getProperty("line.separator");
            out.write(outMsg);
            out.flush();

            Log.i("TcpClient", "sent: " + outMsg);
            //accept server response
//            String inMsg = in.readLine() + System.getProperty("line.separator");
//            Log.i("TcpClient", "received: " + inMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDateTime() {

        Date date = Calendar.getInstance(Locale.US).getTime();
        return dateFormat.format(date);
    }

    public String getDeviceID() {
        try {
            deviceID = tm.getDeviceId();
        } catch (Exception e) {
            try {
                if (wifiMan.isWifiEnabled())
                    deviceID = wifiMan.getConnectionInfo().getMacAddress();
                else
                    deviceID = "000000000000000";
            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, ex.toString(), Toast.LENGTH_LONG);
                deviceID = "000000000000000";
            }
        }
        return deviceID;
    }

    Thread tcpReceiverThread = new Thread(new Runnable() {
        @Override
        public void run() {

            while (!Thread.interrupted()){

                //accept server response
                String inMsg = null;
                try {
                    in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    inMsg = in.readLine() + System.getProperty("line.separator");
                    IOMessage msgRcvd = new IOMessage(inMsg);
                        if(msgRcvd != null){
                            switch (msgRcvd.getType()) {
                                case Constants.MSG_CF_RCV:
                                    cosAdapter = new COSAdapter(msgRcvd.getBody());
                                 new Handler(getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                    swipeContainer.setRefreshing(false);
                                           }
                                        }, 2000);
                                    break;
                                case Constants.MSG_QTD_RCV:
                                        for (COS cos : cosAdapter.values()){
                                            if(msgRcvd.getBody().equalsIgnoreCase("-1")){
                                                if(Boolean.valueOf(cos.get_DefaultClassOfService())) {
                                                    ambPickupFee = Double.parseDouble(cos.get_APF());
                                                    puMiles = Double.parseDouble(cos.get_PUM());
                                                    puTime = Double.parseDouble(cos.get_PUT());

                                                    additionalDistanceUnit = Double.parseDouble(cos.get_ADU());
                                                    additionalDistanceUnitCost = Double.parseDouble(cos.get_ADC());

                                                    additionalTimeUnit = Double.parseDouble(cos.get_ATU());
                                                    additionalTimeUnitCost = Double.parseDouble(cos.get_ATC());
//                                                    new Handler(getMainLooper()).post(new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            sendMessage(Constants.MSG_MON_RSP);
//                                                            sendMessageToCabDispatch(Constants.MSG_RTD, pref.getString("FARE", "0.0") + Constants.COLSEPARATOR + pref.getString("EXTRAS", "0.0") + Constants.COLSEPARATOR + pref.getString("DISTANCE", "0.0") + Constants.COLSEPARATOR + pref.getString("TIME", "0.0"));
//                                                        }
//                                                    });

                                                    break;
                                                }
                                            }
                                            else if(cos.get_ClassOfServiceID().equalsIgnoreCase(msgRcvd.getBody())){
                                                ambPickupFee = Double.parseDouble(cos.get_APF());
                                                puMiles = Double.parseDouble(cos.get_PUM());
                                                puTime = Double.parseDouble(cos.get_PUT());

                                                additionalDistanceUnit = Double.parseDouble(cos.get_ADU());
                                                additionalDistanceUnitCost = Double.parseDouble(cos.get_ADC());

                                                additionalTimeUnit = Double.parseDouble(cos.get_ATU());
                                                additionalTimeUnitCost = Double.parseDouble(cos.get_ATC());


                                                break;
                                            }
                                        }
                                    new Handler(getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendMessage(Constants.MSG_MON_RSP);
                                            sendMessageToCabDispatch(Constants.MSG_RTD, pref.getString("FARE", "0.0") + Constants.COLSEPARATOR + pref.getString("EXTRAS", "0.0") + Constants.COLSEPARATOR + pref.getString("DISTANCE", "0.0") +Constants.COLSEPARATOR + pref.getString("TIME", "0.0"));
                                        }
                                    });
                                    break;
                                default:
                                    break;
                            }
                        }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i("TcpClient", "received: " + inMsg);
            }
        }
    });


    public void showShowcase() {
        // TODO Auto-generated method stub
        try{
            co.hideOnClickOutside = false;
            co.showcaseId = 1;
            sv = ShowcaseView.insertShowcaseView(ins, MainActivity.this, "", "", co);
            sv.setTextColors(getResources().getColor(R.color.soft_orange), getResources().getColor(R.color.soft_orange));
            sv.setShowcasePosition(0, 0);
            sv.setAlpha(0.5f);
            sv.animateGesture(mdispSize.x/2, 200, mdispSize.x/2, 750).start();

        }
        catch(Exception e){
            e.printStackTrace();
        }


    }
}
