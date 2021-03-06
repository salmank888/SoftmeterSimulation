package com.example.skhalid.softmetersimulation;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FloatingImage extends RelativeLayout implements OnTouchListener, View.OnClickListener {
    private Handler connectionStatusHandler;
    private ScheduledExecutorService connectionStatusScheduler;
    WindowManager windowManager; // to hold our image on screen
	Context ctx; // context so in case i use it somewhere.
	GestureDetector gestureDetector; // to detect some listener on the image.
	WindowManager.LayoutParams params; // layoutParams where i set the image height/width and other.
	WindowManager.LayoutParams paramsF;
    View softmeterGUI;
	int initialX;
	int initialY;
	float initialTouchX;
	float initialTouchY;
	private LayoutInflater mInflater;
    private TextView distanceTimeValue;
    private TextView gpsVal;
    private ImageView connectionStatusVal, floatBtn;
    private TextView fareVal;
    private TextView extrasVal;
    private TextView meterStatus;
    private BootstrapButton meterOnBtn;
    private BootstrapButton timeOffBtn;
    private ImageView addimage;
    private ImageView subimage;
    private RelativeLayout RA2, softmeterUI;
    protected ArrayList<LatiLongi> coordinatesList;
    public static TextView currencyValue;
    public static TextView currencyValue1;

    double deltaTime = 1.0/6.0; //Seconds
    double deltaDistance;

    double totalDistance = 0.0; //M
    double totalTime = 0.0; // Minutes
    int K=0; //Iterator
    double accumDeltaD = 0.0;  //M
    double acccumDeltaT = 0.0; //Minutes
    double tempLat, tempLong;

    double distanceCost, timeCost;
    double meterValue;
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    final DecimalFormat dFormat = new DecimalFormat("0.00");
    Future<?> future;
    Handler mainHandler;
    private boolean timeOffPressed = false;
    private String tempDir;	// Binder given to clients
    private OutputStreamWriter myOutWriter = null;
    private FileOutputStream fOut = null;
    private File myFile = null;

    /**
	 * @param context
	 */
	public FloatingImage(Context context) {
		super(context);
		this.ctx = context;
		this.setOnTouchListener(this); // setting touchListener to the imageView
		
		 LayoutInflater.from(context).inflate(R.layout.softmeternew, this, true);
		 
		 Typeface tf = Typeface.createFromAsset(context.getAssets(), "digital-7.ttf");

         softmeterUI = (RelativeLayout) findViewById(R.id.floatSoftmeter);
        softmeterUI.setVisibility(GONE);
         floatBtn = (ImageView) findViewById(R.id.floatImage);
        floatBtn.setVisibility(VISIBLE);

         distanceTimeValue = (TextView) findViewById(R.id.distTimeValue);
         gpsVal = (TextView) findViewById(R.id.gpsValue);
         connectionStatusVal = (ImageView) findViewById(R.id.gpsImage);
		 fareVal = (TextView) findViewById(R.id.fareValue);
		 extrasVal = (TextView) findViewById(R.id.extrasValue);
         currencyValue = (TextView) findViewById(R.id.currencyValue);
         currencyValue1 = (TextView) findViewById(R.id.currencyValue1);

         currencyValue.setText(" "+MainActivity.unitCurrency);
         currencyValue1.setText(" "+MainActivity.unitCurrency);

	     fareVal.setTypeface(tf);
	     extrasVal.setTypeface(tf);



        meterOnBtn = (BootstrapButton) findViewById(R.id.hiredButton);
        timeOffBtn = (BootstrapButton) findViewById(R.id.timeOffButton);

        RA2 = (RelativeLayout) findViewById(R.id.RA2);
        meterStatus = (TextView) findViewById(R.id.meterState);

        addimage = (ImageView) findViewById(R.id.add);
        subimage = (ImageView) findViewById(R.id.sub);

        timeOffBtn.setBootstrapButtonEnabled(false);


        meterOnBtn.setOnClickListener(this);
        timeOffBtn.setOnClickListener(this);

        addimage.setOnClickListener(this);
        subimage.setOnClickListener(this);


        gestureDetector = new GestureDetector(ctx, new GestureListener(this));
		windowManager = (WindowManager) ctx.getSystemService("window"); // ini the windowManager
		params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				PixelFormat.TRANSLUCENT); // assigning height/width to the imageView
		params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
				| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM; // assigning some flags to the layout
		params.gravity = Gravity.TOP | Gravity.LEFT; // setting the gravity of the imageView
		params.windowAnimations = android.R.style.Animation_Toast; // adding an animation to it.
		params.x = 0; // horizontal location of imageView
		params.y = 100; // vertical location of imageView
		params.height = LayoutParams.WRAP_CONTENT; // given it a fixed height in case of large image
		params.width = LayoutParams.WRAP_CONTENT; // given it a fixed width in case of large image
		params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
		windowManager.addView(this, params); // adding the imageView & the  params to the WindowsManger.

        coordinatesList = new ArrayList<LatiLongi>();
        mainHandler = new Handler(context.getMainLooper());
        connectionStatusHandler = new Handler(context.getMainLooper());
        connectionStatusScheduler = Executors.newScheduledThreadPool(1);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                connectionStatusHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(MainActivity.isConnectedWithDriverApp)
                            connectionStatusVal.setImageDrawable(getResources().getDrawable(R.drawable.connected));
                        else
                            connectionStatusVal.setImageDrawable(getResources().getDrawable(R.drawable.notconnected));
                    }
                });

            }
        };

        connectionStatusScheduler.scheduleAtFixedRate(r, 5, 2, TimeUnit.SECONDS);

    }

	/**
	 * @param context
	 * @param attrs
	 */
	public FloatingImage(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public FloatingImage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param v
	 * @param event
	 * @return true/false
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		paramsF = params; // getting the layout params from the current one and assigning new one.
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			paramsF = params;
			initialX = paramsF.x; // Horizontal location of the ImageView
			initialY = paramsF.y; // Vertical location of the ImageView
			initialTouchX = event.getRawX(); //X coordinate  location of the ImageView
			initialTouchY = event.getRawY(); //Y coordinate  location of the ImageView
			break;
		case MotionEvent.ACTION_UP: // this called when we actually leave the Imageview
			break;
		case MotionEvent.ACTION_MOVE:
			paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
			paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
			windowManager.updateViewLayout(this, paramsF);
			break;
		}
		return false; // returning false otherwise any touch event on the imageView wont work.
	}

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.hiredButton:

                if(MainActivity.isConnectedWithDriverApp) {

                    if (meterStatus.getText().toString().trim().equalsIgnoreCase("For Hire")) {
                        if (MainActivity.cosAdapter != null) {
                            Message message = new Message();
                            message.what = Constants.MSG_MON;
                            message.arg1 = 1;
                            FloatingService.sendMessageToLauncherActivity(message);
                            MainActivity.pref.edit().putString("FARE", "0.00").putString("EXTRAS", "0.00").putString("DISTANCE", "0.00").putString("TIME", "0.00").commit();

                        } else
                            Toast.makeText(ctx, "No Class of Service Table Found", Toast.LENGTH_LONG).show();

                    } else if (meterStatus.getText().toString().trim().equalsIgnoreCase("Time Off")) {
                        if (timeOffPressed) {

                            // Send Time Off Message
                            Message message = new Message();
                            message.what = Constants.MSG_MOFF;
                            FloatingService.sendMessageToLauncherActivity(message);

                            future.cancel(true);
                            totalDistance = 0.0; //M
                            totalTime = 0.0; // Minutes
                            K = 0; //Iterator
                            accumDeltaD = 0.0;  //M
                            acccumDeltaT = 0.0; //Minutes

                            extrasVal.setText(dFormat.format(0.0));

                            fareVal.setText(dFormat.format(0.0));
                            distanceTimeValue.setText("0.0M | 0.00min");

                            timeOffBtn.setBootstrapButtonEnabled(false);
                            meterOnBtn.setBootstrapButtonEnabled(true);
                            meterOnBtn.setBootstrapType("success");
                            meterOnBtn.setText("MeterOn");
                            meterOnBtn.setRightIcon("fa-play");
                            meterStatus.setText("For Hire");
//                        FloatingService.sendMessageToLauncherActivity(Constants.MSG_ENABLE_FIELDS);
                            timeOffPressed = false;
                            try {
                                myOutWriter.close();
                                fOut.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Crashlytics.logException(e);
                            }
                        } else
                            Toast.makeText(ctx, "Please Stop First", Toast.LENGTH_LONG).show();

                    }
                }
                else
                    Toast.makeText(ctx, "Please Connect Meter to Driver App. \nThen Retry.", Toast.LENGTH_LONG).show();


                break;
            case R.id.timeOffButton:
                if(MainActivity.isConnectedWithDriverApp) {

                    if (meterStatus.getText().toString().trim().equalsIgnoreCase("Hired")) {
                        // Send Time Off Message
                        Message message = new Message();
                        message.what = Constants.MSG_TOFF;
                        FloatingService.sendMessageToLauncherActivity(message);
                        //
                        timeOffBtn.setText("TimeOn");
                        meterOnBtn.setBootstrapButtonEnabled(true);
                        meterStatus.setText("Time Off");
                        timeOffPressed = true;
                    } else if (meterStatus.getText().toString().trim().equalsIgnoreCase("Time Off")) {
                        // Send Time Off Message
                        Message message = new Message();
                        message.what = Constants.MSG_TON;
                        FloatingService.sendMessageToLauncherActivity(message);
                        //
                        timeOffBtn.setText("TimeOff");
                        meterOnBtn.setBootstrapButtonEnabled(false);
                        meterStatus.setText("Hired");
//                    meterOnBtn.performClick();
                        timeOffPressed = false;
                    }
                }
                else
                    Toast.makeText(ctx, "Please Connect Meter to Driver App. \nThen Retry.", Toast.LENGTH_LONG).show();
                break;
            case R.id.add:
                extrasVal.setText(String.format("%.2f", Float.parseFloat(extrasVal.getText().toString().trim()) + 0.50));
                break;
            case R.id.sub:
                extrasVal.setText(String.format("%.2f", Float.parseFloat(extrasVal.getText().toString().trim()) - 0.50));
                if((Float.parseFloat(extrasVal.getText().toString().trim()) + Float.parseFloat(fareVal.getText().toString().trim())) < 0)
                    extrasVal.setText(String.format("%.2f", Float.parseFloat(extrasVal.getText().toString().trim()) + 0.50));
                break;
            default:
                break;
        }
    }

    private class GestureListener extends
			GestureDetector.SimpleOnGestureListener {

        public GestureListener(View view){
            softmeterGUI = view;
        }
		@Override
		public boolean onDown(MotionEvent e) {// When there is a touch event on the imageView
			return true;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) { // perform Double tap on the ImageView

                if(meterStatus.isEnabled()) {
                    meterStatus.setEnabled(false);
                    addimage.setVisibility(View.GONE);
                    meterStatus.setVisibility(View.GONE);
                    meterOnBtn.setVisibility(View.GONE);
                    timeOffBtn.setVisibility(View.GONE);
                    subimage.setVisibility(View.GONE);


                    RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams) RA2.getLayoutParams();
                    relativeParams.setMargins(0, 5, 0, 0);  // left, top, right, bottom
                    RA2.setLayoutParams(relativeParams);
                }
            else {
                    meterStatus.setEnabled(true);
                    distanceTimeValue.setVisibility(View.VISIBLE);
                    meterStatus.setVisibility(View.VISIBLE);
                    meterOnBtn.setVisibility(View.VISIBLE);
                    timeOffBtn.setVisibility(View.VISIBLE);
                    connectionStatusVal.setVisibility(View.VISIBLE);
                    gpsVal.setVisibility(View.VISIBLE);
                    addimage.setVisibility(View.VISIBLE);
                    subimage.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams) RA2.getLayoutParams();
                    relativeParams.setMargins(0, 15, 0, 0);  // left, top, right, bottom
                    RA2.setLayoutParams(relativeParams);
                }
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) { // perform single tap on the ImageView
			if(floatBtn.getVisibility() == VISIBLE) {
                floatBtn.setVisibility(GONE);
                softmeterUI.setVisibility(VISIBLE);
                params.width = LayoutParams.MATCH_PARENT; // given it a fixed width in case of large image
                windowManager.updateViewLayout(softmeterGUI, params); // adding the imageView & the  params to the WindowsManger.
            }
            else{
                softmeterUI.setVisibility(GONE);
                floatBtn.setVisibility(VISIBLE);
                params.width = LayoutParams.WRAP_CONTENT; // given it a fixed width in case of large image
                windowManager.updateViewLayout(softmeterGUI, params); // adding the imageView & the  params to the WindowsManger.
            }

			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) { // perform long press on the ImageView
            Intent i = new Intent(ctx, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
		}
	}
	/**
	 * return [Remove the FloatingImage from the windowManager]
	 */
	public void destroy(){
		if(windowManager != null){ // if the image still exists on the WindowManager release it.
			windowManager.removeView(this); // remove the ImageView
		}
        if(scheduler != null)
        scheduler.shutdownNow();

        if(connectionStatusScheduler != null)
            connectionStatusScheduler.shutdownNow();
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
            Toast.makeText(ctx, e.toString(), Toast.LENGTH_LONG).show();
            Crashlytics.logException(e);
        }
    }

    private void openTxtFileforWriting(){
        try {
            tempDir = Environment.getExternalStorageDirectory() + "/SoftMeter/";
            ContextWrapper cw = new ContextWrapper(ctx);
            File directory = cw.getDir("SoftMeter", Context.MODE_PRIVATE);

            prepareDirectory();
            myFile = new File(tempDir, "lastTripData"+ System.currentTimeMillis() +".txt");
            myFile.createNewFile();
            fOut = new FileOutputStream(myFile);
            myOutWriter = 	new OutputStreamWriter(fOut);


        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }

    private boolean prepareDirectory() {
        try {
            if (makedirs()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            return false;
        }
    }

    private boolean makedirs() {
        File tempdir = new File(tempDir);
        if (!tempdir.exists())
            tempdir.mkdirs();


        return (tempdir.isDirectory());
    }

//    private void calculateAndShowFarefromFile(){
//        future = scheduler.scheduleWithFixedDelay(new Runnable() {
//            @Override
//            public void run() {
//                if(K <= coordinatesList.size()) {
//                    if (K == 0) {
//                        meterValue = MainActivity.ambPickupFee;
//                    } else {
//                        deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, coordinatesList.get(K).lat, coordinatesList.get(K).lon)/1609.34;
//                        totalDistance = totalDistance + deltaDistance;
//                        totalTime = totalTime + deltaTime;
//                        if(totalTime > MainActivity.puTime || totalDistance > MainActivity.puMiles){
//
//                            if(MainActivity.additionalDistanceUnit > 0) {
//                                accumDeltaD = accumDeltaD + deltaDistance;
//                                distanceCost = (int) (round(accumDeltaD, 2) / MainActivity.additionalDistanceUnit) * MainActivity.additionalDistanceUnitCost;
//                            }
//
//                            if(!timeOffPressed)
//                            if(MainActivity.additionalTimeUnit > 0) {
//                                acccumDeltaT = acccumDeltaT + deltaTime;
//                                timeCost = (int) (round(acccumDeltaT, 2) / MainActivity.additionalTimeUnit) * MainActivity.additionalTimeUnitCost;
//                            }
//
//                            if(MainActivity.additionalDistanceUnit > 0) {
//                                if(round(accumDeltaD, 2) >= MainActivity.additionalDistanceUnit)
//                                    acccumDeltaT = 0.0;
//                                accumDeltaD = round(accumDeltaD, 2) % MainActivity.additionalDistanceUnit;
//                            }
//
//                            if(!timeOffPressed)
//                            if(MainActivity.additionalTimeUnit > 0) {
//                                if(round(acccumDeltaT, 2) > MainActivity.additionalTimeUnit)
//                                    accumDeltaD = 0.0;
//                                acccumDeltaT = round(acccumDeltaT, 2) % MainActivity.additionalTimeUnit;
//                            }
//
//                            if(timeOffPressed) {
//                                meterValue = meterValue + distanceCost;
//                                        try {
//                                            myOutWriter.append(dFormat.format(totalDistance)+ "M" + "\t" + dFormat.format(totalTime)+ "min" + "\t" + "DistanceTic" + "\t" + dFormat.format(meterValue)+ "\n" );
//                                        } catch (Exception e) {
//                                            // TODO Auto-generated catch block
//                                            e.printStackTrace();
//                                        }
//                            }
//                            else {
//                                meterValue = meterValue + Math.max(distanceCost, timeCost);
//                                        try {
//                                            String strToPrint;
//                                            if(distanceCost > timeCost)
//                                                strToPrint ="DistanceTic";
//                                            else if(distanceCost < timeCost)
//                                                strToPrint ="TimeTic";
//                                            else
//                                                strToPrint =" ";
//
//                                            myOutWriter.append(dFormat.format(totalDistance)+ "M\t" + dFormat.format(totalTime)+ "min\t" + strToPrint + "\t" + dFormat.format(meterValue) + "\n" );
//                                        } catch (IOException e) {
//                                            // TODO Auto-generated catch block
//                                            e.printStackTrace();
//                                        }
//
//                            }
//
//                        }
//                    }
//
//                    mainHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            fareVal.setText(dFormat.format(meterValue));
//                            distanceTimeValue.setText(dFormat.format(totalDistance) +"M | " + dFormat.format(totalTime) + "min");
//                        }
//                    });
//
//
//                    tempLat = coordinatesList.get(K).lat;
//                    tempLong = coordinatesList.get(K).lon;
//                    K = K + 1;
//                }
//            }
//        }, 0, 10, TimeUnit.SECONDS);
//    }

    private void calculateAndShowFare(){
        future = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                String strToPrint = "";
                if(MainActivity.pref != null) {
                    if (K == 0) {
                        meterValue = MainActivity.ambPickupFee;
                    } else {
                        strToPrint = " ";
                        if(MainActivity.unitDistance.equalsIgnoreCase("miles") || MainActivity.unitDistance.equalsIgnoreCase("mile")) {
                            deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, Double.parseDouble(MainActivity.pref.getString("Lattitude", "0.0")), Double.parseDouble(MainActivity.pref.getString("Longitude", "0.0"))) / 1609.34;
                        } else {
                            deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, Double.parseDouble(MainActivity.pref.getString("Lattitude", "0.0")), Double.parseDouble(MainActivity.pref.getString("Longitude", "0.0"))) / 1000.00;
                        }
                        totalDistance = totalDistance + deltaDistance;
                        totalTime = totalTime + deltaTime;
                        if(totalTime > MainActivity.puTime || totalDistance > MainActivity.puMiles){

                            if(MainActivity.additionalDistanceUnit > 0) {
                                accumDeltaD = accumDeltaD + deltaDistance;
                                distanceCost = (int) (round(accumDeltaD, 2) / MainActivity.additionalDistanceUnit) * MainActivity.additionalDistanceUnitCost;
                            }

                            if(!timeOffPressed)
                                if(MainActivity.additionalTimeUnit > 0) {
                                    acccumDeltaT = acccumDeltaT + deltaTime;
                                    timeCost = (int) (round(acccumDeltaT, 2) / MainActivity.additionalTimeUnit) * MainActivity.additionalTimeUnitCost;
                                }

                            if(MainActivity.additionalDistanceUnit > 0) {
                                if(round(accumDeltaD, 2) >= MainActivity.additionalDistanceUnit) {
                                    acccumDeltaT = 0.0;
                                    strToPrint ="DistanceTic";
                                }
                                accumDeltaD = round(accumDeltaD, 2) % MainActivity.additionalDistanceUnit;
                            }

                            if(!timeOffPressed)
                                if(MainActivity.additionalTimeUnit > 0) {
                                    if(round(acccumDeltaT, 2) > MainActivity.additionalTimeUnit) {
                                        accumDeltaD = 0.0;
                                        strToPrint ="TimeTic";
                                    }
                                    acccumDeltaT = round(acccumDeltaT, 2) % MainActivity.additionalTimeUnit;
                                }

                            if(timeOffPressed)
                                meterValue = meterValue + distanceCost;
                            else
                                meterValue = meterValue + Math.max(distanceCost, timeCost);

                                try {
                                    myOutWriter.append(dFormat.format(totalDistance)+ MainActivity.unitDistance +"\t" + dFormat.format(totalTime)+ "min\t" + strToPrint + "\t" + dFormat.format(meterValue) + "\n" );
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    Crashlytics.logException(e);
                                }



                        }
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            fareVal.setText(dFormat.format(meterValue));
                            currencyValue.setText(" "+MainActivity.unitCurrency);
                            currencyValue1.setText(" "+MainActivity.unitCurrency);
                            if(MainActivity.unitDistance.equalsIgnoreCase("miles") || MainActivity.unitDistance.equalsIgnoreCase("mile")) {
                                distanceTimeValue.setText(dFormat.format(totalDistance) + "Mi | " + dFormat.format(totalTime) + "min");
                            } else {
                                distanceTimeValue.setText(dFormat.format(totalDistance) + "Km | " + dFormat.format(totalTime) + "min");
                            }
                        }
                    });
                    MainActivity.pref.edit().putString("FARE", dFormat.format(meterValue)).putString("EXTRAS", extrasVal.getText().toString()).putString("DISTANCE", dFormat.format(totalDistance)).putString("TIME", dFormat.format(totalTime)).commit();

                    tempLat = Double.parseDouble(MainActivity.pref.getString("Lattitude","0.0"));
                    tempLong = Double.parseDouble(MainActivity.pref.getString("Longitude","0.0"));
                    K = K + 1;
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public void hired(){
        if(K == 0) {
//                        readTextFile();
            openTxtFileforWriting();

        }

        meterOnBtn.setBootstrapButtonEnabled(false);
        timeOffBtn.setBootstrapButtonEnabled(true);
        meterOnBtn.setBootstrapType("primary");
        meterOnBtn.setText("MeterOff");
        meterOnBtn.setRightIcon("fa-stop");
        meterStatus.setText("Hired");
        timeOffBtn.setText("TimeOff");
        calculateAndShowFare();
    }

    public void timeOff(){
        timeOffBtn.performClick();
    }
    public void timeOn(){
        timeOffBtn.performClick();
    }
    public void meterOff(){
        meterOnBtn.performClick();
    }

}