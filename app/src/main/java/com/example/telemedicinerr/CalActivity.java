package com.example.telemedicinerr;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import static java.lang.Math.sqrt;

public class CalActivity extends AppCompatActivity implements SensorEventListener {
    private static int SMOOTHING_WINDOW_SIZE = 10;
    SensorManager mSensorManager;
    public static Sensor mAccelerometer, mSensorCount;
    private float[] mValues= new float[3];

    // smoothing accelerometer signal variables
    private float[][] mHistory = new float[3][SMOOTHING_WINDOW_SIZE];
    private float[] mRunningTotal = new float[3];
    private float[] mCurrentAverage = new float[3];
    private int mCurrentReadIndex = 0;

    public static float mRRCounter = 0;
    public static float mRRCounterAndroid = 0;
    public static float mInitialRRCount = 0;
    private double mGraph1LastXValue = 0d;
    private double mGraph2LastXValue = 0d;

    private LineGraphSeries<DataPoint> mSeries1;
    private LineGraphSeries<DataPoint> mSeries2;

    //peak detection variables
    private double lastXPoint = 1d;
    double RRThreshold = 0.45d;
    double noiseThreshold = 2d;
    int windowSize = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cal);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSensorCount = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensorCount, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        GraphView graph = this.findViewById(R.id.graph1);
        mSeries1 = new LineGraphSeries<>();
        graph.addSeries(mSeries1);
        graph.setTitle("Respiration Rate");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Signal Value");
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(60);

        //Graph for showing smoothed acceleration magnitude signal
        GraphView graph2 = this.findViewById(R.id.graph2);
        mSeries2 = new LineGraphSeries<>();
        graph2.setTitle("Signal Smoothing");
        graph2.addSeries(mSeries2);
        graph2.getGridLabelRenderer().setVerticalAxisTitle("Signal Value");
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(0);
        Timer timer=new Timer();
        timer.schedule(new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                Intent intentdata=new Intent(CalActivity.this,Mainactivity.class);
                startActivity(intentdata);
                finish();

            }
        },60000);
    }

    public void onClick(View v){
        Intent i = new Intent(this, Mainactivity.class);
        this.startActivity(i);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_COUNTER:
                if (mInitialRRCount == 0.0) {
                    mInitialRRCount = event.values[0];
                }
                mRRCounterAndroid = event.values[0];
                break;
            case Sensor.TYPE_ACCELEROMETER:
                mValues[0] = event.values[0];
                mValues[1] = event.values[1];
                mValues[2] = event.values[2];

                double lastMag = sqrt(Math.pow(mValues[0], 2) + Math.pow(mValues[1], 2) + Math.pow(mValues[2], 2));
                for (int i = 0; i < 3; i++) {
                    mRunningTotal[i] = mRunningTotal[i] - mHistory[i][mCurrentReadIndex];
                    mHistory[i][mCurrentReadIndex] = mValues[i];
                    mRunningTotal[i] = mRunningTotal[i] + mHistory[i][mCurrentReadIndex];
                    mCurrentAverage[i] = mRunningTotal[i] / SMOOTHING_WINDOW_SIZE;
                }
                mCurrentReadIndex++;
                if (mCurrentReadIndex >= SMOOTHING_WINDOW_SIZE) {
                    mCurrentReadIndex = 0;
                }
                double avgMag = sqrt(Math.pow(mCurrentAverage[0], 2) + Math.pow(mCurrentAverage[1], 2) + Math.pow(mCurrentAverage[2], 2));

                double netMag = lastMag - avgMag; //removes gravity effect

                //update graph data points
                mGraph1LastXValue += 1d;
                mSeries1.appendData(new DataPoint(mGraph1LastXValue, lastMag), true, 100);
                mGraph2LastXValue += 1d;
                mSeries2.appendData(new DataPoint(mGraph2LastXValue, netMag), true, 100);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.sensor.getType());
        }
        TextView calculatedRR = this.findViewById(R.id.tv1);
        TextView androidRR = this.findViewById(R.id.tv2);
        peakDetection();

        calculatedRR.setText(String.format("RR: %d", (int) mRRCounter));
        androidRR.setText(String.format("RR Android: %d", (int) (mRRCounterAndroid - mInitialRRCount)));


    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void peakDetection() {
        double highestValX = mSeries2.getHighestValueX();

        if(highestValX - lastXPoint < windowSize){
            return;
        }

        Iterator<DataPoint> valuesInWindow = mSeries2.getValues(lastXPoint,highestValX);

        lastXPoint = highestValX;

        double forwardSlope ;
        double downwardSlope;
        List<DataPoint> dataPointList = new ArrayList<>();
        valuesInWindow.forEachRemaining(dataPointList::add); //This requires API 24 or higher
        for(int i = 0; i<dataPointList.size(); i++){
            if (i==0){
                //continue
            }

             else if(i < dataPointList.size() - 1){
                forwardSlope = dataPointList.get(i+1).getY() - dataPointList.get(i).getY();
                downwardSlope = dataPointList.get(i).getY() - dataPointList.get(i - 1).getY();


                if(forwardSlope < 0 && downwardSlope > 0 && dataPointList.get(i).getY() > RRThreshold && dataPointList.get(i).getY() < noiseThreshold){
                    mRRCounter+=1;
                }
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
