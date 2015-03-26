package com.example.skhalid.softmetersimulation;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;

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

public class FloatingImage extends RelativeLayout implements OnTouchListener, View.OnClickListener {
	WindowManager windowManager; // to hold our image on screen
	Context ctx; // context so in case i use it somewhere.
	GestureDetector gestureDetector; // to detect some listener on the image.
	WindowManager.LayoutParams params; // layoutParams where i set the image height/width and other.
	WindowManager.LayoutParams paramsF;
	int initialX;
	int initialY;
	float initialTouchX;
	float initialTouchY;
	private LayoutInflater mInflater;
    private TextView distanceTimeValue;
    private TextView fareVal;
    private TextView bookingVal;
    private TextView meterStatus;
    private BootstrapButton meterOnBtn;
    private BootstrapButton timeOffBtn;
    protected ArrayList<LatiLongi> coordinatesList;

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

    /**
	 * @param context
	 */
	public FloatingImage(Context context) {
		super(context);
		this.ctx = context;
		this.setOnTouchListener(this); // setting touchListener to the imageView
		
		 LayoutInflater.from(context).inflate(R.layout.softmeternew, this, true);
		 
		 Typeface tf = Typeface.createFromAsset(context.getAssets(),
                 "digital-7.ttf");
         distanceTimeValue = (TextView) findViewById(R.id.distTimeValue);
		 fareVal = (TextView) findViewById(R.id.fareValue);
		 bookingVal = (TextView) findViewById(R.id.extrasValue);
	     fareVal.setTypeface(tf);
	     bookingVal.setTypeface(tf);

        meterOnBtn = (BootstrapButton) findViewById(R.id.hiredButton);
        timeOffBtn = (BootstrapButton) findViewById(R.id.timeOffButton);


        meterStatus = (TextView) findViewById(R.id.meterState);

        timeOffBtn.setBootstrapButtonEnabled(false);


        meterOnBtn.setOnClickListener(this);
        timeOffBtn.setOnClickListener(this);


        gestureDetector = new GestureDetector(ctx, new GestureListener());
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
                if (meterStatus.getText().toString().trim().equalsIgnoreCase("For Hire")){
                    if(K == 0) {
                        readTextFile();

                        FloatingService.sendMessageToLauncherActivity(Constants.MSG_DISABLE_FIELDS);

                    }

                    meterOnBtn.setBootstrapButtonEnabled(false);
                    timeOffBtn.setBootstrapButtonEnabled(true);
                    meterOnBtn.setBootstrapType("primary");
                    meterOnBtn.setText("MeterOff");
                    meterOnBtn.setRightIcon("fa-stop");
                    meterStatus.setText("Hired");
                    if(MainActivity.isTestMode)
                        calculateAndShowFarefromFile();
                    else
                        calculateAndShowFare();
                } else if(meterStatus.getText().toString().trim().equalsIgnoreCase("Time Off")){
                    if(future.isCancelled()) {
                        totalDistance = 0.0; //M
                        totalTime = 0.0; // Minutes
                        K = 0; //Iterator
                        accumDeltaD = 0.0;  //M
                        acccumDeltaT = 0.0; //Minutes

                        fareVal.setText(dFormat.format(0.0));
                        distanceTimeValue.setText("0.0M | 0.00min");

                        timeOffBtn.setBootstrapButtonEnabled(false);
                        meterOnBtn.setBootstrapButtonEnabled(true);
                        meterOnBtn.setBootstrapType("success");
                        meterOnBtn.setText("MeterOn");
                        meterOnBtn.setRightIcon("fa-play");
                        meterStatus.setText("For Hire");
                        FloatingService.sendMessageToLauncherActivity(Constants.MSG_ENABLE_FIELDS);

                    }
                    else
                        Toast.makeText(ctx, "Please Stop First", Toast.LENGTH_LONG).show();

                }

                break;
            case R.id.timeOffButton:
                if(meterStatus.getText().toString().trim().equalsIgnoreCase("Hired")) {
                    timeOffBtn.setText("TimeOn");
                    meterOnBtn.setBootstrapButtonEnabled(true);
                    meterStatus.setText("Time Off");
                    future.cancel(true);
                } else if(meterStatus.getText().toString().trim().equalsIgnoreCase("Time Off")){
                    timeOffBtn.setText("TimeOff");
                    meterOnBtn.setBootstrapButtonEnabled(false);
                    meterStatus.setText("Hired");
                    meterOnBtn.performClick();
                }

                break;
            default:
                break;
        }
    }

    private class GestureListener extends
			GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {// When there is a touch event on the imageView
			return true;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) { // perform Double tap on the ImageView
			Toast.makeText(ctx, "Hi You Double Tap Me", Toast.LENGTH_SHORT).show();
			return false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) { // perform single tap on the ImageView
			Toast.makeText(ctx, "Hi You Single Tap Me", Toast.LENGTH_SHORT).show();
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) { // perform long press on the ImageView
			Toast.makeText(ctx, "Hi You Long Tap Me", Toast.LENGTH_SHORT).show();
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
    }

    public void onLocationChanged(){
              if (!MainActivity.isTestMode)
                if (future != null)
                if (future.isCancelled()) {
                    tempLat = Double.parseDouble(MainActivity.pref.getString("Lattitude", "0.0"));
                    tempLong = Double.parseDouble(MainActivity.pref.getString("Longitude", "0.0"));
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
            Toast.makeText(ctx, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void calculateAndShowFarefromFile(){
        future = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if(K <= coordinatesList.size()) {
                    if (K == 0) {
                        meterValue = MainActivity.ambPickupFee;
                    } else {
                        deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, coordinatesList.get(K).lat, coordinatesList.get(K).lon)/1609.34;
                        totalDistance = totalDistance + deltaDistance;
                        totalTime = totalTime + deltaTime;
                        if(totalTime > MainActivity.puTime || totalDistance > MainActivity.puMiles){
                            if(MainActivity.additionalTimeUnit > 0) {
                                acccumDeltaT = acccumDeltaT + deltaTime;
                                timeCost = (int) (acccumDeltaT / MainActivity.additionalTimeUnit) * MainActivity.additionalTimeUnitCost;
                                acccumDeltaT = acccumDeltaT % MainActivity.additionalTimeUnit;
                            }
                            if(MainActivity.additionalDistanceUnit > 0) {
                                accumDeltaD = accumDeltaD + deltaDistance;
                                distanceCost = (int) (accumDeltaD / MainActivity.additionalDistanceUnit) * MainActivity.additionalDistanceUnitCost;
                                accumDeltaD = accumDeltaD % MainActivity.additionalDistanceUnit;
                            }
//                            if(!isTestMode) {
                            if (timeCost > distanceCost)
                                accumDeltaD = 0.0;
                            else
                                acccumDeltaT = 0.0;
//                            }
                            meterValue = meterValue + Math.max(distanceCost, timeCost);

                        }
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            fareVal.setText(dFormat.format(meterValue));
                            distanceTimeValue.setText(dFormat.format(totalDistance) +"M | " + dFormat.format(totalTime) + "min");
                        }
                    });
//                    ((Activity)ctx).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            totalDistanceVal.setText(dFormat.format(totalDistance));
////                            totalTimeVal.setText(dFormat.format(totalTime));
//                            fareVal.setText(dFormat.format(meterValue));
//                        }
//                    });

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
                if(MainActivity.pref != null) {
                    if (K == 0) {
                        meterValue = MainActivity.ambPickupFee;
                    } else {
                        deltaDistance = DistanceCalculator.CalculateDistance(tempLat, tempLong, Double.parseDouble(MainActivity.pref.getString("Lattitude","0.0")), Double.parseDouble(MainActivity.pref.getString("Longitude","0.0")))/1609.34;
                        totalDistance = totalDistance + deltaDistance;
                        totalTime = totalTime + deltaTime;
                        if(totalTime > MainActivity.puTime || totalDistance > MainActivity.puMiles){
                            if(MainActivity.additionalTimeUnit > 0) {
                                acccumDeltaT = acccumDeltaT + deltaTime;
                                timeCost = (int) (acccumDeltaT / MainActivity.additionalTimeUnit) * MainActivity.additionalTimeUnitCost;
                                acccumDeltaT = acccumDeltaT % MainActivity.additionalTimeUnit;
                            }
                            if(MainActivity.additionalDistanceUnit > 0) {
                                accumDeltaD = accumDeltaD + deltaDistance;
                                distanceCost = (int) (accumDeltaD / MainActivity.additionalDistanceUnit) * MainActivity.additionalDistanceUnitCost;
                                accumDeltaD = accumDeltaD % MainActivity.additionalDistanceUnit;
                            }

//                            if(!isTestMode) {
                            if (timeCost >= distanceCost)
                                accumDeltaD = 0.0;
                            else
                                acccumDeltaT = 0.0;
//                            }
                            meterValue = meterValue + Math.max(distanceCost, timeCost);

                        }
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            fareVal.setText(dFormat.format(meterValue));
                            distanceTimeValue.setText(dFormat.format(totalDistance) +"M | " + dFormat.format(totalTime) + "min");
                        }
                    });
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            totalDistanceVal.setText(dFormat.format(totalDistance));
////                            totalTimeVal.setText(dFormat.format(totalTime));
//                            fareVal.setText(dFormat.format(meterValue));
//                        }
//                    });

                    tempLat = Double.parseDouble(MainActivity.pref.getString("Lattitude","0.0"));
                    tempLong = Double.parseDouble(MainActivity.pref.getString("Longitude","0.0"));
                    K = K + 1;
                }
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

}