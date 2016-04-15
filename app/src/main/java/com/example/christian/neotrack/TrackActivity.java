package com.example.christian.neotrack;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEventListener;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.example.christian.neotrack.fragments.MapTabFragment;
import com.example.christian.neotrack.fragments.TrackFragment;
import com.example.christian.neotrack.persistence.DataCapture;
import com.example.christian.neotrack.persistence.DataCaptureDAO;
import com.example.christian.neotrack.persistence.Itinerary;
import com.example.christian.neotrack.persistence.ItineraryDAO;
import com.example.christian.neotrack.persistence.Point;
import com.example.christian.neotrack.services.MyRecognitionListener;
import com.example.christian.neotrack.services.TabsAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 * Maps Activity with tabs
 */
public class TrackActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final static private String TAG = "TrackActivity";
    private final static int REQ_CODE_SPEECH_INPUT = 100;

    private final static String DIALOG_SAVE_FILE_TITLE = "Guardar archivo";
    private final static String B_OK = "Aceptar";
    private final static String B_CANCEL = "Cancelar";

    private static final int ZOOM = 20;

    private AlertDialog saveFileDialog;
    private DatePickerDialog dateInitDialog;

    private final static SimpleDateFormat DATE_FORMATTER_VIEW =
            new SimpleDateFormat("dd-MM-yyyy", new Locale("es", "ES"));
    private final static SimpleDateFormat DATE_FORMATTER_SAVE =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("es", "ES"));
    private EditText etDateStart;
    private EditText etDateEnd;
    private DatePickerDialog dateEndDialog;
    private EditText etNameSaveFile;
    private Calendar newDateStart;
    private Calendar newDateEnd;

    private GoogleApiClient mGoogleApiClient;
    private boolean mAddressRequested;

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    private Fragment mapFragment;
    private Fragment trackFragment;
    private boolean isFirstLocation = true;
    private String addressPattern = "ZZZZZZZZZZ"; // cadena imposible
    public TextToSpeech speakerOut;
    private boolean speakerOutReady = false;
    private Itinerary visitItinerary;
    private boolean runningTracking = false;

    public DataCaptureDAO dbDataCapture;
    private String SESSION_ID;
    final static private String TAG_SESSION_ID = "SESSION_ID";

    public SpeechRecognizer sr;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private TriggerEventListener mTriggerEventListener;
    public boolean speeching = false;
    public boolean waitToStart = true;
    public boolean runningSpeech = false;
    private boolean tStop = true;
    private double acceleration = 0.0;
    private double pressure = 0.0;
    private double light = 0.0;
    private double temperature = 0.0;
    private double humidity = 0.0;
    private double sumAcceleration = 0.0;
    private SensorEventListener mSensorListener;
    private double[] accelerationVector;
    private double[] preaccelerationVector;
    private long oldTime;
    private double[] speed;
    private double vel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Configure Interface
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_map);
        mViewPager = (ViewPager) findViewById(R.id.fragment_container);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        configureActionBar(savedInstanceState);

        buildGoogleApiClient();

        newSessionId();
        dbDataCapture = new DataCaptureDAO(this);

        // reset sensors variables
        acceleration = 0.0;
        pressure = 0.0;
        light = 0.0;
        temperature = 0.0;
        humidity = 0.0;
        sumAcceleration = 0.0;
        oldTime = System.currentTimeMillis();
        accelerationVector = new double[]{0,0,0};
        preaccelerationVector = new double[]{0,0,0};
        speed = new double[]{0,0,0};
        vel = 0;
    }

    private void newSessionId() {
        String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String timestamp = DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime());
        SESSION_ID = android_id + "::" + timestamp;
        Log.i(TAG, "new session ID: "+ SESSION_ID);
    }

    @Override
    public void onResume() {
        super.onResume();
        dbDataCapture.open();
        mGPSStatusListener = new GpsStatus.Listener() {
            public void onGpsStatusChanged(int event) {
                switch (event) {
                    case GpsStatus.GPS_EVENT_STARTED:
                        Log.i("GPS", "Searching...");
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        Log.i("GPS", "STOPPED");
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        // GPS_EVENT_FIRST_FIX Event is called when GPS is locked
                        Log.i("GPS", "Locked position");
                        setHiddenFragment(); // visual log
                        ((MapTabFragment) mapFragment).setZoom(ZOOM);
                        dialogWait.dismiss();
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        break;
                }
            }
        };

        configurePreference();
        configureDialogWait();
        configureLocation();

        speakerOut = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    speakerOut.setLanguage(new Locale("es", "ES"));
//                    speakerOut.speak( getResources().getString(R.string.speak_out_welcome), TextToSpeech.QUEUE_ADD, null);
                    speakerOutReady = true;
                }
            }
        });

//        Intent mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
//                this.getPackageName());
//        sr.startListening(mSpeechRecognizerIntent);

        sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        MyRecognitionListener listener = new MyRecognitionListener(this);
        sr.setRecognitionListener(listener);
//        sr.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                mSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor mySensor = event.sensor;

                if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//                    Log.i("Sensor", event.values[0] + " " + event.values[1] + " " + event.values[2]);
                    // alpha is calculated as t / (t + dT)
                    // with t, the low-pass filter's time-constant
                    // and dT, the event delivery rate
                    double x = accelerationVector[0];
                    double y = accelerationVector[1];
                    double z = accelerationVector[2];
//                    double a = Math.sqrt((x * x) + (y * y) + (z * z));
                    acceleration = z;
                    sumAcceleration = sumAcceleration - acceleration;
//                    Log.i("Accelerometer", "Sum: " + sumAcceleration);

                    long trackTime = System.currentTimeMillis();
                    if(trackTime - oldTime > 100) {
                        long diffTime = trackTime - oldTime;
                        oldTime = trackTime;

//                    final float alpha = 0.8f;
//                    float alpha = trackTime / (float) (trackTime + (trackTime - oldTime));

                        final float alpha = 0.99f;

                        double factor = 1000000000.0;
                        double preaccel = Math.sqrt((accelerationVector[0] * accelerationVector[0]) + (accelerationVector[1] * accelerationVector[1]) + (accelerationVector[2] * accelerationVector[2]));
                        preaccelerationVector[0] = accelerationVector[0];
                        preaccelerationVector[1] = accelerationVector[1];
                        preaccelerationVector[2] = accelerationVector[2];
                        // Isolate the force of gravity with the low-pass filter.
                        accelerationVector[0] = alpha * accelerationVector[0] + (1 - alpha) * event.values[0];
                        accelerationVector[1] = alpha * accelerationVector[1] + (1 - alpha) * event.values[1];
                        accelerationVector[2] = alpha * accelerationVector[2] + (1 - alpha) * event.values[2];

//                        if (accelerationVector[0] < 0.09) {
//                            accelerationVector[0] = 0.0;
//                        }
//                        if (accelerationVector[1] < 0.09) {
//                            accelerationVector[1] = 0.0;
//                        }
//                        if (accelerationVector[2] < 0.09) {
//                            accelerationVector[2] = 0.0;
//                        }
//                        accelerationVector[0] = (accelerationVector[0] / 0.078) -1.0;
//                        accelerationVector[1] = (accelerationVector[1] / 0.078) -1.0;
//                        accelerationVector[2] = (accelerationVector[2] / 0.078) -1.0;

                        // Integration
                        speed[0] = ((trackTime - oldTime) / 6.0) * (x + 4 * ((accelerationVector[0] - x) / 2.0) + accelerationVector[0]) / factor;
                        speed[1] = ((trackTime - oldTime) / 6.0) * (y + 4 * ((accelerationVector[1] - y) / 2.0) + accelerationVector[1]) / factor;
                        speed[2] = ((trackTime - oldTime) / 6.0) * (z + 4 * ((accelerationVector[2] - z) / 2.0) + accelerationVector[2]) / factor;

//                        speed[0] = (trackTime - oldTime) / factor * (accelerationVector[0]);
//                        speed[1] = (trackTime - oldTime) / factor * (accelerationVector[1]);
//                        speed[2] = (trackTime - oldTime) / factor * (accelerationVector[2]);

//                        if(preaccelerationVector[0] * accelerationVector[0]>0) {
//                            speed[0] = ((trackTime - oldTime) / (factor * 2.0)) * (preaccelerationVector[0] + accelerationVector[0]);
//                        } else {
//                            speed[0] = ((trackTime - oldTime) / (factor * 4.0)) * (preaccelerationVector[0] + accelerationVector[0]);
//                        }
//                        if(preaccelerationVector[1] * accelerationVector[1]>0) {
//                            speed[1] = ((trackTime - oldTime) / (factor * 2.0)) * preaccelerationVector[1] + (accelerationVector[1]);
//                        } else {
//                            speed[1] = ((trackTime - oldTime) / (factor * 4.0)) * preaccelerationVector[1] + (accelerationVector[1]);
//                        }
//                            if(preaccelerationVector[2] * accelerationVector[2]>0) {
//                                speed[2] = ((trackTime - oldTime) / (factor * 2.0)) * (preaccelerationVector[2] + accelerationVector[2]);
//                            } else {
//                                speed[2] = ((trackTime - oldTime) / (factor * 4.0)) * (preaccelerationVector[2] + accelerationVector[2]);
//                            }
                        double v = Math.abs(accelerationVector[0] + accelerationVector[1] + accelerationVector[2] - preaccelerationVector[0] - preaccelerationVector[1] - preaccelerationVector[2])/ diffTime * 10000;//Math.sqrt((speed[0] * speed[0]) + (speed[1] * speed[1]) + (speed[2] * speed[2]));

                        // nano to seconds
//                    speed[0] = speed[0] / factor;
//                    speed[1] = speed[1] / factor;
//                    speed[2] = speed[2] / factor;

                        double accel = Math.sqrt((accelerationVector[0] * accelerationVector[0]) + (accelerationVector[1] * accelerationVector[1]) + (accelerationVector[2] * accelerationVector[2]));
                        double dot = (preaccelerationVector[0] * accelerationVector[0] +
                                preaccelerationVector[1] * accelerationVector[1] +
                                preaccelerationVector[2] * accelerationVector[2]) /
                                (preaccel * accel);
                        vel += (trackTime - oldTime) * (preaccel - accel) / 2;
//                    Log.i("Speed", "[" + speed[0] + " " + speed[1] + " " + speed[2] + "]\t\t" + Math.sqrt(speed[0] * speed[0] + speed[1] * speed[1] + speed[2] * speed[2]) + "  " + alpha);
//                    Log.i("Speed", "[" + ((trackTime - oldTime) / 6.0) * (preaccel + (4.0 * ((accel - preaccel)/2.0))  + accel) + "]");
//                    Log.i("Speed", "[" + vel+ "]\t" + accel + "\t" + preaccel + "\t" + (trackTime - oldTime));
//                    Log.i("Accel", "[" + accelerationVector[0] + " " + accelerationVector[1] + " " + accelerationVector[2] + "]\t\t" + Math.sqrt(accelerationVector[0] * accelerationVector[0] + accelerationVector[1] * accelerationVector[1] + accelerationVector[2] * accelerationVector[2]) + "  " + alpha);
                        Log.i("Speed", "[" + dot + "]\t" + v + "\t" + accel + "\t" + preaccel + "\t[" + accelerationVector[0] + " " + accelerationVector[1] + " " + accelerationVector[2] + "]");
//                        if (dot < 0.5) {
//                            Log.i("dot", dot + "\t" + accel + "\t" + preaccel);
//                        }
                        acceleration = vel;
                        Location loc = new Location("");
                        temperature = v;
                        light = accelerationVector[0];
                        humidity = accelerationVector[1];
                        pressure = accelerationVector[2];
                    loc.setLongitude(-4.4);
                    loc.setLatitude(3.33);
                    myLocationChanged(loc);
                        if (tStop) {
                            if (v > 2) {
                                speakerOut.speak("Andando", TextToSpeech.QUEUE_ADD, null);
                                tStop = false;
                            }
                        } else {
                            if (v < 0.06) {
                                speakerOut.speak("no", TextToSpeech.QUEUE_ADD, null);
                                tStop = true;
                            }
                        }


//                    if(a < 2.3f && a > -2.3f) {
                        if (tStop) {
                            if (!waitToStart) {
                                waitToStart = true;

                                if (!runningSpeech) {
                                    runningSpeech = true;
                                    restartSpeech();
                                }
//                            speeching = true;
//                            getNewSpeechReady = false;
//                            Log.i("Sensor", Math.sqrt((x * x) + (y * y) + (z * z)) + "\t" + x + "\t" + y + "\t" + z);
//                            restartSpeech();
                            }
                        } else {
                            waitToStart = false;
//                        Log.i("Sensor", Math.sqrt((x * x) + (y * y) + (z * z)) + "\t" + x + "\t" + y + "\t" + z);
                        }
                    }

                }
//                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                    float x = event.values[0];
//                    float y = event.values[1];
//                    float z = event.values[2];
//                    if(Math.sqrt((x*x) + (y*y) + (z*z)) > 11.0f)
//                    Log.i("Sensor", Math.sqrt((x*x) + (y*y) + (z*z)) + "\t" + x + "\t" + y + "\t" + z);
//                }

                if (mySensor.getType() == Sensor.TYPE_PRESSURE) {
                    pressure = event.values[0];
                }
                if (mySensor.getType() == Sensor.TYPE_LIGHT) {
                    light = event.values[0];
                }
                if (mySensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    temperature = event.values[0];
                }
                if (mySensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                    humidity = event.values[0];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL);



        List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor sensor: list){
            Log.i("SensorList", sensor.getName() + " " + sensor.getType() + " " + sensor.toString());
        }
    }

    public void restartSpeech() {
//        sr.stopListening();
//        sr.cancel();
//        MyRecognitionListener listener = new MyRecognitionListener(this);
//        sr.setRecognitionListener(listener);
//        Log.i("test", "test");
//        MyRecognitionListener listener = new MyRecognitionListener(this);
//        sr.setRecognitionListener(listener);
//        sr.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
//        sr.stopListening();
//        sr.cancel();
//        sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
//        MyRecognitionListener listener = new MyRecognitionListener(this);
//        sr.setRecognitionListener(listener);
        speakerOut.speak("Parada", TextToSpeech.QUEUE_ADD, null);
        sr.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        SESSION_ID = savedInstanceState.getString(TAG_SESSION_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString(TAG_SESSION_ID, SESSION_ID);
    }

    @Override
    public void onPause() {
        super.onPause();
//        stopRepeatingTask();
        locationManager.removeUpdates(gpsLocationListener);
        locationManager.removeGpsStatusListener(mGPSStatusListener);
        dbDataCapture.close();

        sr.stopListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sr.stopListening();
        sr.cancel();
        sr.destroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                sendSettings();
                return true;
            case R.id.action_save_file:
                displaySaveFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void configureLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            // Register GPSStatus listener for events
            locationManager.addGpsStatusListener(mGPSStatusListener);
            gpsLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (!tStop)
                        myLocationChanged(location);
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // TODO Auto-generated method stub
                }
                @Override
                public void onProviderEnabled(String provider) {
                    Toast.makeText(TrackActivity.this,
                            getResources().getString(R.string.enabled_provider) + provider,
                            Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onProviderDisabled(String provider) {
                    Toast.makeText(TrackActivity.this,
                            getResources().getString(R.string.disabled_provider) + provider,
                            Toast.LENGTH_SHORT).show();
                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    intervalTimeGPS, minDistance, gpsLocationListener);
        } else {
            Toast.makeText(this, getResources().getString(R.string.gps_disabled),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void configurePreference() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        loadSettings();
        PreferenceChangeListener preferenceListener = new PreferenceChangeListener();
        pref.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void configureActionBar(Bundle savedInstanceState) {
        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

            mTabsAdapter = new TabsAdapter(this, mViewPager);
            mTabsAdapter.addTab(bar.newTab().setText(getResources().getString(R.string.map)),
                    MapTabFragment.class, null);
            mTabsAdapter.addTab(bar.newTab().setText(getResources().getString(R.string.data)),
                    TrackFragment.class, null);

            if (savedInstanceState != null) {
                bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
            }
        }
    }



    public void displayItineraries(View view) {
        ItineraryDAO db = new ItineraryDAO(TrackActivity.this);
        db.open();
        ArrayList<Itinerary> itineraryList = new ArrayList<>();
        itineraryList.addAll(db.getAll());
        db.close();

        List<String> list = new ArrayList<>();
        for(Itinerary i : itineraryList) {
            list.add(i.getName());
        }

        String title = getResources().getString(R.string.select_itinerary_title);
        CharSequence[] array = list.toArray(new CharSequence[list.size()]);
        Dialog dialog = onCreateDialogSingleChoice(title, array, itineraryList);
        dialog.show();
    }

    public void controlTracking(View view) {
        runningTracking = !runningTracking;
        Log.i(TAG, "capturing points: " + runningTracking);
//        Display stuff (change play icon to pause icon)
    }

    public void stopTracking(View view) {
        runningTracking = false;
        Log.i(TAG, "Stop capturing points");

        // Display summary
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tracking_summary)
                .setMessage(printSummaryTracking(SESSION_ID));
        AlertDialog dialog = builder.create();
        dialog.show();

        // Reset sessionId
        newSessionId();
    }

    private String printSummaryTracking(String tag) {
        long time = 0;
        float distance = 0;

        Log.i(TAG, "Search for sessionId: " + tag);
        List<DataCapture> results = dbDataCapture.get(tag);
        Log.i(TAG, "recover " + results.size() + " elements");
        int stops = 0;
        if (results.size() > 0) {
            // time
            Date dateStart = null;
            Date dateEnd = null;
            try {
                dateStart = DATE_FORMATTER_SAVE.parse(results.get(0).getDate());
                dateEnd = DATE_FORMATTER_SAVE.parse(results.get(results.size() - 1).getDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (dateStart != null && dateEnd != null) {
                time = (dateEnd.getTime() - dateStart.getTime()) / 1000;
            }
            // distance
            Location lastLoc = new Location("");
            lastLoc.setLatitude(results.get(0).getLatitude());
            lastLoc.setLongitude(results.get(0).getLongitude());
            for (DataCapture dc : results) {
                Location newLoc = new Location("");
                newLoc.setLatitude(dc.getLatitude());
                newLoc.setLongitude(dc.getLongitude());
                distance += lastLoc.distanceTo(newLoc);
                lastLoc = newLoc;
                // number of stops
                if (dc.getStopType() != null) {
                    stops++;
                }
            }

            // Save data of itinerary automatically
            saveTrack(results, time, distance);
        }

        float vel = time != 0 ? distance * 3.6f / time : 1;
        return "Tiempo del trayecto:\t" + time + "s.\n"
                + "Distancia recorrida:\t" + distance + "m.\n"
                + "Velocidad media:\t" + vel + "km/h\n"
                + "Número de paradas:\t" + stops + "\n";
    }

    public void sendSettings() {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void saveFile(String fileName) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");
        FileOutputStream out = null;
        FileOutputStream outST = null;
        DataCaptureDAO db = new DataCaptureDAO(this);
        //StreetTrackDAO dbST = new StreetTrackDAO(this);
        db.open();
        //dbST.open();
        List<DataCapture> data = db.get(newDateStart, newDateEnd);
        //List<StreetTrack> dataST = dbST.getAll();
        Log.i("DB", "Find " + data.size() + " DataCapture elements");
       // Log.i("DB", "Find " + dataST.size() + " StreetTrack elements");
        String extension = ".csv";
        String folderName = "/neoTrack";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                dir.mkdirs();
                File file = new File (dir, fileName + extension);
                File fileST = new File (dir, "ST" + fileName + extension);
                out = new FileOutputStream(file);
//                outST = new FileOutputStream(fileST);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
//                outST = openFileOutput("ST" + fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "_id,latitude,longitude,street,stoptype,comment,date,acceleration,pressure,light,temperature,humidity\n";
            out.write(head.getBytes());
            for(DataCapture dc : data) {
                out.write((String.valueOf(dc.getId()) + ",").getBytes());
                if(dc.getSession() != null) {
                    out.write(("\"" + dc.getSession() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write((String.valueOf(dc.getLatitude()) + ",").getBytes());
                out.write((String.valueOf(dc.getLongitude()) + ",").getBytes());
                if(dc.getStopType() != null) {
                    out.write(("\"" + dc.getStopType() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                if(dc.getComment() != null) {
                    out.write(("\"" + dc.getComment() + "\",").getBytes());
                } else {
                    out.write(("null,").getBytes());
                }
                out.write(("\"" + dc.getDate() + "\",").getBytes());
                out.write((String.valueOf(dc.getSensorAcceleration()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorPressure()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorLight()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorTemperature()) + ",").getBytes());
                out.write((String.valueOf(dc.getSensorHumidity()) + "\n").getBytes());
            }
            out.flush();
            out.close();
/*
            String headST = "_id,address,latitude start,longitude start," +
                    "latitude end,longitude end,datetime start,datetime end,distance\n";
            outST.write(headST.getBytes());
            for(StreetTrack st : dataST) {
                outST.write((String.valueOf(st.getId()) + ",").getBytes());
                outST.write(("\"" + st.getAddress() + "\",").getBytes());
                outST.write((String.valueOf(st.getStartLatitude()) + ",").getBytes());
                outST.write((String.valueOf(st.getStartLongitude()) + ",").getBytes());
                outST.write((String.valueOf(st.getEndLatitude()) + ",").getBytes());
                outST.write((String.valueOf(st.getEndLongitude()) + ",").getBytes());
                outST.write(("\"" + st.getStartDateTime() + "\",").getBytes());
                outST.write(("\"" + st.getEndDateTime() + "\",").getBytes());
                outST.write((String.valueOf(st.getDistance()) + "\n").getBytes());
            }
            outST.flush();
            outST.close();
*/
            Log.i("DB", "File saved");
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                db.close();
//                dbST.close();
                if (out != null) {
                    out.close();
                }
//                if(outST != null) {
//                    outST.close();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void saveTrack(List<DataCapture> results, float time, float distance) {
        Toast.makeText(this, "Saving file...", Toast.LENGTH_SHORT).show();
        Log.i("DB", "Saving file...");

        String fileName = "itinerary" + SESSION_ID;

        FileOutputStream out = null;
        String extension = ".csv";
        String folderName = "/neoTrack";
        try {
            if(isExternalStorageWritable()) {
                String path = Environment.getExternalStorageDirectory().toString();
                File dir = new File(path + folderName);
                dir.mkdirs();
                File file = new File (dir, fileName + extension);
                out = new FileOutputStream(file);
            } else {
                out = openFileOutput(fileName + extension, Context.MODE_PRIVATE);
            }
            String head = "ID\ttime\tdistance\n";
            out.write(head.getBytes());
            out.write((SESSION_ID + "\t" + time + "\t" + distance + "\n").getBytes());
            head = "_id\tsession\tlatitude\tlongitude\tstoptype\tcomment\tdate\tacceleration\tpressure\tlight\ttemperature\thumidity\n";
            out.write(head.getBytes());
            for(DataCapture dc : results) {
                out.write((String.valueOf(dc.getId()) + "\t").getBytes());
                if(dc.getSession() != null) {
                    out.write(("\"" + dc.getSession() + "\"\t").getBytes());
                } else {
                    out.write(("null\t").getBytes());
                }
                out.write((String.valueOf(dc.getLatitude()) + "\t").getBytes());
                out.write((String.valueOf(dc.getLongitude()) + "\t").getBytes());
                if(dc.getStopType() != null) {
                    out.write(("\"" + dc.getStopType() + "\"\t").getBytes());
                } else {
                    out.write(("null\t").getBytes());
                }
                if(dc.getComment() != null) {
                    out.write(("\"" + dc.getComment() + "\"\t").getBytes());
                } else {
                    out.write(("null\t").getBytes());
                }
                out.write(("\"" + dc.getDate() + "\"\t").getBytes());
                out.write((String.valueOf(dc.getSensorAcceleration()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorPressure()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorLight()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorTemperature()) + "\t").getBytes());
                out.write((String.valueOf(dc.getSensorHumidity()) + "\n").getBytes());
            }
            out.flush();
            out.close();

                       Log.i("DB", "File saved");
            Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
//                db.close();
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void displaySaveFile() {
        Calendar newCalendar = Calendar.getInstance();

        newDateStart = Calendar.getInstance();
        newDateStart.set(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH),0,0,0);
        newDateEnd = Calendar.getInstance();
        newDateEnd.set(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH),23,59,59);
        newDateEnd.set(Calendar.HOUR,23);
        dateInitDialog = new DatePickerDialog(this, new OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Log.i("Dialog", "Change datepicker");
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    etDateStart.setText(DATE_FORMATTER_VIEW.format(newDate.getTime()));
                }
            },
                newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dateInitDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, B_OK,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg2, int which) {
                        dlg2.cancel();
                        saveFileDialog.show();
                    }
        });
        dateInitDialog.getDatePicker().init(newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH),
                new DatePicker.OnDateChangedListener() {
                    @Override
                    public void onDateChanged(DatePicker datePicker, int year, int monthOfYear,
                                              int dayOfMonth) {
                        Log.i("Dialog", "Change datepicker");
                        newDateStart = Calendar.getInstance();
                        newDateStart.set(year, monthOfYear, dayOfMonth,0,0,0);
                        etDateStart.setText(DATE_FORMATTER_VIEW.format(newDateStart.getTime()));
                    }
                }
        );

        dateEndDialog = new DatePickerDialog(this,new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                etDateEnd.setText(DATE_FORMATTER_VIEW.format(newDate.getTime()));
            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
        dateEndDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg2, int which) {
                dlg2.cancel();
                saveFileDialog.show();
            }
        });
        dateEndDialog.getDatePicker().init(newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                Log.i("Dialog", "Change datepicker");
                newDateEnd = Calendar.getInstance();
                newDateEnd.set(year, monthOfYear, dayOfMonth,23,59,59);
                newDateEnd.set(Calendar.HOUR,23);
                etDateEnd.setText(DATE_FORMATTER_VIEW.format(newDateEnd.getTime()));
            }
        });

        AlertDialog.Builder saveFileDialogBuilder = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setMessage(DIALOG_SAVE_FILE_TITLE)
                .setPositiveButton(B_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        dateInitDialog.show();
                        saveFile(etNameSaveFile.getText().toString());
                    }
                })
                .setNegativeButton(B_CANCEL, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setView(R.layout.dialog_save_file);
        saveFileDialog = saveFileDialogBuilder.create();

        saveFileDialog.show();

        etNameSaveFile = (EditText) saveFileDialog.findViewById(R.id.d_save_file_name);
        etDateStart = (EditText) saveFileDialog.findViewById(R.id.d_save_file_date_start);
        etDateEnd = (EditText) saveFileDialog.findViewById(R.id.d_save_file_date_end);
        Button bStart = (Button) saveFileDialog.findViewById(R.id.db_save_file_date_start);
        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateInitDialog.show();
            }
        });
        Button bEnd = (Button) saveFileDialog.findViewById(R.id.db_save_file_date_end);
        bEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateEndDialog.show();
            }
        });
        etDateStart.setText(DATE_FORMATTER_VIEW.format(newCalendar.getTime()));
        etDateEnd.setText(DATE_FORMATTER_VIEW.format(newCalendar.getTime()));
        etNameSaveFile.setText("info_track_" + etDateStart.getText() + "_" + etDateEnd.getText());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
    }



    private void configureDialogWait() {
        dialogWait = new ProgressDialog(this);
        dialogWait.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialogWait.setMessage(getResources().getString(R.string.gps_loading));
        dialogWait.setIndeterminate(true);
        dialogWait.setCanceledOnTouchOutside(false);
        dialogWait.show();
    }

    /*****
     * GPS
     */

//    private static final String DATA_START = "Iniciando la recogida de los datos...";
//    private static final String DATA_END = "Recogida de datos terminada";
//    private static final String NEW_POSITION = "Guardando la siguiente posición: ";
//    private static final String NEW_GPS = "Nueva posición GPS: ";

    private long intervalTimeGPS; // milliseconds
    private float minDistance; // meters


    //GPS periodico
    public LocationManager locationManager;
    private ProgressDialog dialogWait;
    public DataCaptureDAO db;

    private SharedPreferences pref; // Settings listener

    public boolean runningCaptureData;
    private float trackDistance;
    private DataCapture currentTrackPoint;

    private DataCapture startTrackPoint;

    // Process to repeat
    private int intervalCapture;
    public Handler mHandler;
    public Handler addressHandler;
    private Location currentLocation;
    // Location
    private LocationListener gpsLocationListener;
    private GpsStatus.Listener mGPSStatusListener;


    private void loadSettings() {
        Log.i("MapActivity","Loading settings...");
        String syncConnPref = pref.getString("pref_key_interval_time", "0");
        int intervalTimeSetting = Integer.parseInt(syncConnPref);

        syncConnPref = pref.getString("pref_key_interval_capture", "0");
        int intervalCaptureSetting = Integer.parseInt(syncConnPref);

        syncConnPref = pref.getString("pref_key_min_distance", "0");
        int minDistanceSetting = Integer.parseInt(syncConnPref);

        intervalTimeGPS = intervalTimeSetting * 1000;
        intervalCapture = intervalCaptureSetting * 1000;
        minDistance = minDistanceSetting;
    }

    public void myLocationChanged(Location location) {
        currentLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        setHiddenFragment(); // visual log
        ((MapTabFragment) mapFragment).setCamera(latLng);
        // Print marker car position
        ((MapTabFragment) mapFragment)
                .addMarker(MapTabFragment.Marker_Type.POSITION, null, currentLocation);

        if (runningTracking) {
            // Print marker track point
            ((MapTabFragment) mapFragment)
                    .addMarker(MapTabFragment.Marker_Type.GPS, null,currentLocation);
            new SavePointTask().execute(new SavePointInput(visitItinerary, location));
        }
//        // save data
//        processTrackData(location); // Global process information
    }

    public void myLocationChanged(Location location, String cause) {
        currentLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        setHiddenFragment(); // visual log
        ((MapTabFragment) mapFragment).setCamera(latLng);
        // Print marker car position
        ((MapTabFragment) mapFragment)
                .addMarker(MapTabFragment.Marker_Type.POSITION, null, currentLocation);

        if (runningTracking) {
            // Print marker track point
            ((MapTabFragment) mapFragment)
                    .addMarker(MapTabFragment.Marker_Type.GPS, null,currentLocation);
            new SavePointTask().execute(new SavePointInput(visitItinerary, location, cause));
        }
//        // save data
//        processTrackData(location); // Global process information
    }

    public void runSaveData(DataCapture dc) {
        dc.setSensorAcceleration(acceleration);
        dc.setSensorPressure(pressure);
        dc.setSensorLight(light);
        dc.setSensorTemperature(temperature);
        dc.setSensorHumidity(humidity);
        new SavePointTask2().execute(new SavePointInput2(visitItinerary,dc));
    }



















/*
    // Repeat process for catch information
    public Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus(); //this function can change value of mInterval.
            mHandler.postDelayed(mStatusChecker, intervalCapture);
        }
    };

    public void startRepeatingTask() {
        mStatusChecker.run();
        runningCaptureData = true;
    }

    public void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
        runningCaptureData = false;
    }
    */
/*
    private void updateStatus() {
        if (currentLocation != null) {
            Log.i("Background", "Collecting data in: " + currentLocation.getLatitude() + ", "
                    + currentLocation.getLongitude());

            DataCapture dc = new DataCapture();
            dc.setLatitude(currentLocation.getLatitude());
            dc.setLongitude(currentLocation.getLongitude());
//            dc.setAddress(getStreet(currentLocation));
            dc.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));

            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
            receiver.setDataCapture(dc);
            receiver.setIsInserted(true);
            startIntentService(receiver);
//            db.create(dc);
            ((MapTabFragment) mapFragment)
                    .addMarker(MapTabFragment.Marker_Type.GPS, null, currentLocation);
        }
    }
*/
    public void setHiddenFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        Log.i("Activity","Num of fragments: " + fragments.size());
        for(Fragment fragment : fragments){
            if(fragment != null) {
                if (fragment instanceof MapTabFragment) {//!fragment.isVisible())
                    mapFragment = fragment;
//                    ((MapTabFragment) mapFragment).setZoom(10.0f);
                } else if (fragment instanceof TrackFragment)//!fragment.isVisible())
                    trackFragment = fragment;
            }
        }
    }

    /**
     * Handle preferences changes
     */
    private class PreferenceChangeListener implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Log.i("Settings", "Changed settings");
            loadSettings();
        }
    }
/*
    public void processTrackData(Location location) {
        if((startTrackPoint != null) && (startTrackPoint.getAddress() != null)) {
            DataCapture dc = new DataCapture();
            dc.setLatitude(location.getLatitude());
            dc.setLongitude(location.getLongitude());
            dc.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));

            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
            receiver.setDataCapture(dc);
            receiver.setIsInserted(false);
            startIntentService(receiver);
        } else {
            startTrackPoint = new DataCapture();
            startTrackPoint.setLatitude(location.getLatitude());
            startTrackPoint.setLongitude(location.getLongitude());
//            startTrackPoint.setAddress(getStreet(location));
            startTrackPoint.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));
            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
            receiver.setDataCapture(startTrackPoint);
            receiver.setIsInserted(true);
            startIntentService(receiver);
            currentTrackPoint = startTrackPoint;
            Log.i("Track","Set start track point in " + startTrackPoint.getLatitude() + " "
                    + startTrackPoint.getLongitude());

            trackDistance = 0;
        }
    }
*/

    /**
     * GEOCODER
     *
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        // Gets the best and most recent location currently available,
        // which may be null in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }

            if (mAddressRequested) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
//                AddressResultReceiver mResultReceiver = new AddressResultReceiver(mHandler);
//                startIntentService(mResultReceiver);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    protected Location mLastLocation;
/*
    protected void startIntentService(AddressResultReceiver mResultReceiver) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, currentLocation);
        startService(intent);
    }
*/
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
/*
    public class AddressResultReceiver extends ResultReceiver {
        private String mAddressOutput;
        private DataCapture dataCapture;
        private boolean isInserted;

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            if (mAddressOutput != null) {
                mAddressOutput.replace("\n", "");
            }
            dataCapture.setAddress(mAddressOutput);

            if(isInserted) {
                db = new DataCaptureDAO(TrackActivity.this);
                db.open();
                db.create(dataCapture);
                db.close();
            } else {
                callbackTrack(dataCapture);
            }
        }

        public DataCapture getDataCapture() {
            return dataCapture;
        }

        public void setDataCapture(DataCapture dataCapture) {
            this.dataCapture = dataCapture;
        }

        public void setIsInserted(boolean isInserted) {
            this.isInserted = isInserted;
        }
    }

    private void callbackTrack(DataCapture dataCapture) {
        if(dataCapture.getAddress() == null) {
            Log.e("Geocoder", "Address is null");
            startTrackPoint = dataCapture;
//            startTrackPoint.setLatitude(location.getLatitude());
//            startTrackPoint.setLongitude(location.getLongitude());
////            startTrackPoint.setAddress(getStreet(location));
//            startTrackPoint.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));
//            AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
//            receiver.setDataCapture(startTrackPoint);
//            receiver.setIsInserted(true);
//            startIntentService(receiver);
//            currentTrackPoint = startTrackPoint;
//            Log.i("Track","Set start track point in " +startTrackPoint.getLatitude() + " " + startTrackPoint.getLongitude());
            trackDistance = 0;
        } else {
//            if(startTrackPoint.getAddress().equals(dataCapture.getAddress())) {
            Log.i("--------", addressPattern + " " + dataCapture.getAddress());
            if(dataCapture.getAddress().contains(addressPattern)) {
                Location start = new Location("");
                start.setLatitude(currentTrackPoint.getLatitude());
                start.setLongitude(currentTrackPoint.getLongitude());
                Location end = new Location("");
                end.setLatitude(dataCapture.getLatitude());
                end.setLongitude(dataCapture.getLongitude());
                Log.i("-TT-------", trackDistance + " " + start.distanceTo(end));
                Log.i("-TT-------", start.getLatitude() + " " + start.getLongitude());
                Log.i("-TT-------", end.getLatitude() + " " + end.getLongitude());
                trackDistance += start.distanceTo(end);

                // set new current
                currentTrackPoint = dataCapture;
            } else {
                // Save track data
                AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
                receiver.setDataCapture(dataCapture);
                receiver.setIsInserted(true);
                startIntentService(receiver);

                StreetTrack st = new StreetTrack(startTrackPoint.getAddress(),
                        startTrackPoint.getLatitude(), startTrackPoint.getLongitude(),
                        currentTrackPoint.getLatitude(), currentTrackPoint.getLongitude(),
                        startTrackPoint.getDate(), currentTrackPoint.getDate(),
                        trackDistance);

                StreetTrackDAO dbLocalInstanceST = new StreetTrackDAO(this);
                dbLocalInstanceST.open();
                dbLocalInstanceST.create(st);
                dbLocalInstanceST.close();
                // Elapsed
                long time = 0;
                try {
                    Date dateStart = DATE_FORMATTER_SAVE.parse(st.getStartDateTime());
                    Date dateEnd = DATE_FORMATTER_SAVE.parse(st.getEndDateTime());
                    time = (dateEnd.getTime()-dateStart.getTime())/1000;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                // Display Information
                String line = "Dirección: " + st.getAddress() + "\n"
                        + "\t Distancia recorrida: " + st.getDistance()+ " m.\n"
                        + "\t Tiempo transcurrido: " + time + " s.\n"
                        + "\t Punto de entrada: " + st.getStartLatitude() + " "
                        + st.getStartLongitude() + "\n"
                        + "\t Punto de salida: " + st.getEndLatitude() + " "
                        + st.getEndLongitude() + "\n";

                ((TrackFragment) trackFragment).appendLog(line);

                startTrackPoint = dataCapture;
                int index = startTrackPoint.getAddress().indexOf(",");
                if(index == -1) {
                    index = startTrackPoint.getAddress().length();
                }
                addressPattern = startTrackPoint.getAddress().substring(0,index);
                Log.i("addressPattern",addressPattern);
                currentTrackPoint = dataCapture;
                trackDistance = 0;
            }
        }
    }
*/
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

/*
    public void saveData(DataCapture dc) {
        AddressResultReceiver receiver = new AddressResultReceiver(addressHandler);
        receiver.setDataCapture(dc);
        receiver.setIsInserted(true);
        startIntentService(receiver);
    }
*/






    /**
     * Receiving speech input
     * */
    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    // For debug
                    System.out.println(result);
                    String out = "";
                    for(String s : result) {
                        out += s + " ";
                    }
                    Log.i("Speak",out);
                    //// Para debug END
                    if(getStopType(result) == null) {
                        Log.e("Speak","Not match stop cause");
                    }
                    processStopChoice(getStopType(result),null);
                }
                break;
            }

        }
    }
*/
    private String getStopType(ArrayList<String> result) {
        final String[] stopChoices = {"Atasco", "Obras", "Accidente", "Otros", "Reanudar"};
        final String[] stopChoicesPattern = {"asco", "bra", "ente", "tro", "anudar"};

        for(String str : result) {
         System.out.println(str);
            str = Normalizer.normalize(str, Normalizer.Form.NFD);
            // remove accents
            str = str.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            for(int i = 0; i<stopChoicesPattern.length;i++) {
                if(str.toLowerCase().contains(stopChoicesPattern[i])) {
                    return stopChoices[i];
                }
            }
        }
        return null;
    }
/*
    public void processStopChoice(String title, String text) {
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        DataCapture dc = new DataCapture();
        dc.setLatitude(loc.getLatitude());
        dc.setLongitude(loc.getLongitude());
        dc.setStopType(title);
        dc.setComment(text);
        dc.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));
        saveData(dc);

        ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.STOP, title, loc);
    }
*/
    public Dialog onCreateDialogSingleChoice(String title, CharSequence[] array, final List data) {
        int index = -1;
        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title
        builder.setTitle(title)
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
                .setSingleChoiceItems(array, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
//                        index = which;
                    }
                })
                .setPositiveButton(B_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        displayItineraySelected((Itinerary) data.get(selectedPosition));
                    }
                })
                .setNegativeButton(B_CANCEL, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        return builder.create();
    }

    private void displayItineraySelected(Itinerary itinerary) {
        ((MapTabFragment) mapFragment).clearItineraryMarkers();
        if(speakerOutReady)
            speakerOut.speak(getResources().getString(R.string.speak_out_itinerary_added) + itinerary.getPoints().size() + " puntos", TextToSpeech.QUEUE_ADD, null);

        for(Object point : itinerary.getPoints()) {
            Location location = new Location("Test");
            location.setLatitude(((Point) point).getLatitude());
            location.setLongitude(((Point) point).getLongitude());
            ((MapTabFragment) mapFragment).addMarker(MapTabFragment.Marker_Type.ITINERARY,
                    ((Point) point).getAddress(), location);
        }
        visitItinerary = itinerary;
    }



    public class SavePointTask extends AsyncTask<SavePointInput, Void, Boolean> {
        private float MIN_DISTANCE = 0.00015f; // 15 meters

        @Override
        protected Boolean doInBackground(SavePointInput... params) {
            // Save point
            savePoint(params[0].getLocation(), params[0].getCause());
            // Check itinerary
            if ((params[0].getItinerary() != null) && (params[0].getItinerary().getPoints().size() > 0)) {
                double distance = distance(params[0].getLocation(), (Point) params[0].getItinerary().getPoints().get(0));
                if (distance < MIN_DISTANCE) {
                    params[0].getItinerary().getPoints().remove(0);
                    return true;
                }
            }
            return false;
        }

        private void savePoint(Location location, String cause) {
            DataCapture dc = new DataCapture();
            dc.setLatitude(location.getLatitude());
            dc.setLongitude(location.getLongitude());
            dc.setStopType(cause);
            dc.setDate(DATE_FORMATTER_SAVE.format(Calendar.getInstance().getTime()));
            dc.setSession(SESSION_ID);
            dc.setSensorAcceleration(acceleration);
            dc.setSensorPressure(pressure);
            dc.setSensorLight(light);
            dc.setSensorTemperature(temperature);
            dc.setSensorHumidity(humidity);
            dbDataCapture.create(dc);
        }

        protected void onPostExecute(Boolean wasItineraryPoint) {
            if (wasItineraryPoint && speakerOutReady) {
                speakerOut.speak( getResources().getString(R.string.speak_out_visit_itinerary_point), TextToSpeech.QUEUE_ADD, null);
            }
        }

    }

    public class SavePointTask2 extends AsyncTask<SavePointInput2, Void, Boolean> {
        private float MIN_DISTANCE = 0.00015f; // 15 meters

        @Override
        protected Boolean doInBackground(SavePointInput2... params) {
            // Save point
            savePoint(params[0].getLocation());
            // Check itinerary
            if (params[0].getItinerary() != null) {
                Location loc = new Location("");
                loc.setLatitude(params[0].getLocation().getLatitude());
                loc.setLongitude(params[0].getLocation().getLongitude());
                double distance = distance(loc, (Point) params[0].getItinerary().getPoints().get(0));
                if (distance < MIN_DISTANCE) {
                    params[0].getItinerary().getPoints().remove(0);
                    return true;
                }
            }
            return false;
        }

        private void savePoint(DataCapture location) {
            DataCapture dc = location;
            dc.setSession(SESSION_ID);
            dbDataCapture.create(dc);
        }

        protected void onPostExecute(Boolean wasItineraryPoint) {
//            if (wasItineraryPoint && speakerOutReady) {
                speakerOut.speak( getResources().getString(R.string.speak_out_stop_added), TextToSpeech.QUEUE_ADD, null);
//            }
        }

    }

    private static double distance(Location location, Point point) {
        return Math.sqrt(Math.pow(location.getLatitude() - point.getLatitude(), 2) + Math.pow(location.getLongitude() - point.getLongitude(), 2));
    }

    private static double distance(double x0, double y0, double x1, double y1) {
        return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2));
    }
}
