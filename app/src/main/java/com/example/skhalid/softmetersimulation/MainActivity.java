package com.example.skhalid.softmetersimulation;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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


public class MainActivity extends ActionBarActivity implements View.OnClickListener {


    EditText apfVal;
    EditText pumVal;
    EditText putVal;

    EditText aduVal;
    EditText adcVal;
    EditText atuVal;
    EditText atcVal;


    TextView totalTimeVal;
    TextView totalDistanceVal;
    TextView fareVal;

    Button startBtn;
    Button stopBtn;
    Button resetBtn;

    ArrayList<LatiLongi> coordinatesList;
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
    boolean isModeA = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        Typeface tf = Typeface.createFromAsset(getAssets(),
                "digital-7.ttf");
        fareVal.setTypeface(tf);
        
        startBtn = (Button) findViewById(R.id.startButton);
        stopBtn = (Button) findViewById(R.id.stopButton);
        resetBtn = (Button) findViewById(R.id.resetButton);

        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        resetBtn.setOnClickListener(this);

        coordinatesList = new ArrayList<LatiLongi>();
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
                isModeA = true;

        }else if(id == R.id.item2){
                item.setChecked(true);
                isModeA = false;

        }

        Toast.makeText(getApplicationContext(), "Fare Calculation According to " + item.getTitle(), Toast.LENGTH_LONG).show();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
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

                    startBtn.setEnabled(false);
                    stopBtn.setEnabled(true);
                    calculateAndShowFare();
                }else
                Toast.makeText(getApplicationContext(), "Provide Proper Values of Fields", Toast.LENGTH_LONG).show();
                break;
            case R.id.stopButton:
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
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

                    stopBtn.setEnabled(true);
                    startBtn.setEnabled(true);

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

    private void calculateAndShowFare(){
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
                            if(!isModeA) {
                                if (timeCost > distanceCost)
                                    accumDeltaD = 0.0;
                                else
                                    acccumDeltaT = 0.0;
                            }
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

    @Override
    protected void onDestroy() {
        scheduler.shutdownNow();
        super.onDestroy();
    }
}
