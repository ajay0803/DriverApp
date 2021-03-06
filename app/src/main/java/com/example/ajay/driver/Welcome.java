package com.example.ajay.driver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ajay.driver.Common.Common;
import com.example.ajay.driver.Model.Token;
import com.example.ajay.driver.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;

public class Welcome extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener{

    private GoogleMap mMap;

    private static final int MY_PERMISSION_REQUEST_CODE=7000;
    private static final int PLAY_SERVICES_RES_REQUEST=7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private  static int UPDATE_INTERVAL=5000;
    private  static int FATEST_INTERVAL=3000;
    private  static int DISPLACEMENT=10;

    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mCurrent;
    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;

    private IGoogleAPI mService;

    //Integrated presence system

    DatabaseReference onlineRef,currentUserRef;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        onlineRef=FirebaseDatabase.getInstance().getReference().child(".info/connected");
        currentUserRef=FirebaseDatabase.getInstance().getReference(Common.driver_tb1)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        location_switch=(MaterialAnimatedSwitch)findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {

                if(isOnline){

                    FirebaseDatabase.getInstance().goOnline();
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(),"You are Online!",Snackbar.LENGTH_SHORT).show();
                }
                else {
                    FirebaseDatabase.getInstance().goOffline();
                    stopLocationUpdates();
                    mCurrent.remove();
                    Snackbar.make(mapFragment.getView(),"You are Offline!",Snackbar.LENGTH_SHORT).show();
                }

            }
        });


        //geofire

        drivers= FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
        geoFire=new GeoFire(drivers);

        setUpLocation();

        mService= Common.getGoogleAPI();

        updateFirebaseToken();
    }

    private void updateFirebaseToken() {
        FirebaseDatabase db=FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference(Common.token_tb1);

        Token token=new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);

    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if(checkPlayServices())
                    {
                        buildGoogleApiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
        }

    }

    private void setUpLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //request runtime permission
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);

        }
        else
        {
            if(checkPlayServices())
            {
                buildGoogleApiClient();
                createLocationRequest();
                if(location_switch.isChecked())
                    displayLocation();
            }
        }

    }

    private void createLocationRequest() {

        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    private void buildGoogleApiClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    private boolean checkPlayServices() {
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode!= ConnectionResult.SUCCESS)
        {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICES_RES_REQUEST).show();
            else{
                Toast.makeText(this,"This device is not supported",Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void stopLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        Common.mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        if(Common.mLastLocation!=null){
            if(location_switch.isChecked()){
                final double latitude=Common.mLastLocation.getLatitude();
                final double longitude=Common.mLastLocation.getLongitude();
                //update to firebase

                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        //Add marker
                        if(mCurrent!=null)
                            mCurrent.remove();
                        mCurrent=mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude,longitude))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                .title("Your Location"));
                        //Move View to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));
                        rotateMarker(mCurrent,-360,mMap);

                    }
                });
            }
        }
        else
        {

            Log.d("ERROR","Cannot get your Location");
        }

    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        final Handler handler=new Handler();
        final long start= SystemClock.uptimeMillis();
        final float startRotation=mCurrent.getRotation();
        final long duration=1500;

        final Interpolator interpolator=new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed=SystemClock.uptimeMillis()  - start;
                float t=interpolator.getInterpolation((float)elapsed/duration);
                float rot=t*i+(1-t)*startRotation;
                mCurrent.setRotation(-rot >180?rot/2:rot);
                if(t<0.5)
                {
                    handler.postDelayed(this,16);
                }

            }
        });
    }

    private void startLocationUpdates() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, (com.google.android.gms.location.LocationListener) this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);



    }


    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
            displayLocation();
            startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation=location;
        displayLocation();

    }
}
