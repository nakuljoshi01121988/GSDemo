package com.cloudmotiv;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {
    private Button locate, add, clear;
    private Button config, upload, start, stop;
    protected static final String TAG = "MainActivity";
    private GoogleMap gMap;
    private double droneLocationLat = 39, droneLocationLng = -84;
    private Marker droneMarker = null;
    private FlightController mFlightController;
    private boolean isAdd = false;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };
    public static boolean checkGpsCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(mReceiver);
        removeListener();
    }
    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };
    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }
    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    public void onReturn(View view) {
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void initUI() {
        locate = findViewById(R.id.locate);
        add = findViewById(R.id.add);
        clear = findViewById(R.id.clear);
        config = findViewById(R.id.config);
        upload = findViewById(R.id.upload);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.VIBRATE,
                            android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.CHANGE_WIFI_STATE, android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                            android.Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);
        //Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        addListener();
    }

    private void onProductConnectionChange() {
        initFlightController();
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    updateDroneLocation();
                }
            });
        }
    }

    private void updateDroneLocation() {

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void showSettingDialog() {
        LinearLayout wayPointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = wayPointSettings.findViewById(R.id.altitude);
        RadioGroup speed_RG = wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
                //Log.d(TAG, "Select Speed finish");
            }
        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub
//                Log.d(TAG, "Select heading finish");
                Log.d(TAG, "Select heading");
                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }
    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            if (DJISDKManager.getInstance().getMissionControl() != null){
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }

    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }
    private String nulltoIntegerDefalt(String altitudeString) {

        if(!isIntValue(altitudeString)) altitudeString="0";
        return altitudeString;
    }


    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            }
            case R.id.locate: {
                updateDroneLocation();
                cameraUpdate();
                break;
            }
            case R.id.config: {
                showSettingDialog();
                break;
            }
            case R.id.upload:{
                uploadWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            default:
                break;
        }
    }
    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }
    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }
    private void cameraUpdate() {
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);
    }
    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object
    }
    @Override
    public void onMapClick(LatLng latLng) {
        if (isAdd == true){
            markWaypoint(latLng);
            Waypoint mWaypoint = new Waypoint(latLng.latitude, latLng.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }
    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(error -> {

            if (error == null) {
                setResultToToast("Mission upload successfully!");
            } else {
                setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                getWaypointMissionOperator().retryUploadMission(null);
            }
        });

    }
    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        // Now that the map is ready, you can interact with it safely
        gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(0, 0)));
        gMap.setOnMapClickListener(this);
        gMap.setOnMapClickListener(this);
    }
}