package com.example.locationtutorial;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    int LOCATION_REQUEST_CODE = 10001;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;

    LocationCallback locationCallback = new LocationCallback() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {     // ?????????????????? ??????.
                Log.d(TAG, "onLocationResult: " + location.toString()); // ?????? ??????.
                JudgementInNOut();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);      //???????????? ?????? 4000ms
        locationRequest.setFastestInterval(2000);       //????????? 2000ms
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void moveMarkerList(View view) {
        startActivity(new Intent(this, MarkerListActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            getLastLocation();        //?????? ???????????? ?????? ??????.
            checkSettingsAndStartLocationUpdates();
        } else {
            askLocationPermission();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    private void checkSettingsAndStartLocationUpdates() {
        LocationSettingsRequest request = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();
        SettingsClient client = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> locationSettingsResponseTask = client.checkLocationSettings(request);
        locationSettingsResponseTask.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Settings of device are satisfied and we can start location updates
                startLocationUpdates();
            }
        });
        locationSettingsResponseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    ResolvableApiException apiException = (ResolvableApiException) e;
                    try {
                        apiException.startResolutionForResult(MainActivity.this, 1001);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    //We have a location
                    Log.d(TAG, "onSuccess: " + location.toString());
                    Log.d(TAG, "onSuccess: " + location.getLatitude());
                    Log.d(TAG, "onSuccess: " + location.getLongitude());
                } else  {
                    Log.d(TAG, "onSuccess: Location was null...");
                }
            }
        });
        locationTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: " + e.getLocalizedMessage() );
            }
        });
    }

    private void askLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "askLocationPermission: you should show an alert dialog...");
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
//                getLastLocation();
                checkSettingsAndStartLocationUpdates();
            } else {
                //Permission not granted
            }
        }
    }

    ///////////////
    static int N = 0;
    static Point[] POINTS = new Point[20];

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void JudgementInNOut() {
        //?????????????????? ???????????? ?????????.
        POINTS = new Point[20];
        N = 0;
        //?????? ?????? ????????? ????????????.
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    //????????? ????????? ????????? ????????????.
                    POINTS[N++] = new Point(location.getLatitude() * 10000, location.getLongitude()*10000);
                }
            }
        });


        //????????????????????? ????????? ????????????.
        MemoDbHelper dbHelper = MemoDbHelper.getInstance(this);
        //????????? ?????????
        Cursor cursor =  dbHelper.getReadableDatabase().query(MemoContract.MemoEntry.TABLE_NAME, null, null, null,null,null,null,null);
        //????????? ????????? ????????????.
        while(cursor.moveToNext()){
            String lat = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUMN_NAME_LAT));
            String lng = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUMN_NAME_LNG));
            POINTS[N++] = new Point(Double.parseDouble(lat) * 10000, Double.parseDouble(lng) * 10000);
        }

        //??? ????????? ?????? ???????????????.
        Point MY_POINTS = POINTS[0];
        //y??? ???????????? '/'?????? ???????????? ?????? ??? ????????? ?????????.
        Arrays.sort(POINTS,0 , N, new Comparator<Point>() {
            @Override
            public int compare(Point a, Point b) {
                if (a.y != b.y) {
                    if(a.y < b.y){
                        return -1;
                    }
                    else{
                        return 1;
                    }
                }
                if(a.x < b.x)
                    return -1;
                return 1;
            }
        });

        //?????? 0??? ???????????? ?????? ????????? ????????? ???????????????.(?????? ????????? ????????? ??????????????? ????????? ?????????.
        for (int i = 1; i < N; i++) {
            POINTS[i].p = POINTS[i].x - POINTS[0].x;
            POINTS[i].q = POINTS[i].y - POINTS[0].y;
        }

        //?????? ????????? 0 ??? ????????? ????????? ??????????????? ???????????? ??????
        Arrays.sort(POINTS,1 , N-1, new Comparator<Point>() {
            @Override
            public int compare(Point a, Point b) {
                if(a.q*b.p != a.p*b.q){
                    if(a.q*b.p < a.p*b.q)
                        return -1;
                    else
                        return 1;
                }
                if (a.y != b.y) {
                    if(a.y < b.y){
                        return -1;
                    }
                    else{
                        return 1;
                    }
                }
                if(a.x < b.x)
                    return -1;
                return 1;
            }
        });

        //???????????? ???????????? ???????????? ????????? index??? ????????????.
        Stack<Integer> stack = new Stack<>();
        stack.add(0);
        stack.add(1);

        //?????? ???????????? ????????? ????????? ??? ????????? ?????? ??????
        for (int i = 2; i < N; i++) {
            //???????????? 2?????? ??????
            while(stack.size() >= 2){
                int first = stack.pop();
                int second = stack.peek();
                //???????????? ????????? ??????????????? ?????? ????????? ???????????? ????????????.
                long ccw = find_ccw(POINTS[first], POINTS[second], POINTS[i]);
                if (ccw > 0) {
                    //????????? stack??? ????????????.
                    stack.add(first);
                    break;
                }
            }
            stack.add(i);
        }

        //?????? ?????? ????????? ????????? ???????????? ???????????? ????????? ?????????.
        boolean isInside = true;
        for(int i=0;i<stack.size();i++){
            if(POINTS[stack.get(i)].x == MY_POINTS.x && POINTS[stack.get(i)].y == MY_POINTS.y){
                isInside = false;
            }
        }


        //????????? ?????? bool ?????? ???????????? ????????? ?????? ????????? ????????????.
        if(isInside){
            final Context context = this;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

            alertDialogBuilder.setTitle("????????????");

            alertDialogBuilder
                    .setMessage("??????????????? ?????????????????????.")
                    .setCancelable(false)
                    .setPositiveButton("??????",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("??????",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    // ?????????????????? ????????????
                                    dialog.cancel();
                                }
                            });

            // ??????????????? ??????
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else{
            final Context context = this;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

            alertDialogBuilder.setTitle("?????? ??????");

            alertDialogBuilder
                    .setMessage("????????? ????????????")
                    .setCancelable(false)
                    .setPositiveButton("??????",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("??????",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    // ?????????????????? ????????????
                                    dialog.cancel();
                                }
                            });

            // ??????????????? ??????
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }


    protected static long find_dist(Point a, Point b) {

        return (long)(a.x - b.x) * (a.x - b.x) + (long)(a.y - b.y) * (a.y - b.y);

    }

    protected static long find_ccw(Point a, Point b, Point c) {

        return (long)(b.x - a.x) * (long)(c.y - a.y) - (long)(c.x - a.x) * (long)(b.y - a.y);
    }

    static class Point {
        long x, y;
        //???????????????????????? ?????? ??????
        long p,q;

        public Point(double x, double y) {
            this.x = (long) x;
            this.y = (long) y;
            p=1;
            q=0;
        }

        public Point(double x, double y, long p, long q){
            this.x = (long) x;
            this.y = (long) y;
            this.p=p;
            this.q=q;
        }

        public Point(long x, long y) {
            this.x = x;
            this.y = y;
            p=1;
            q=0;
        }
    }

    ///////////////
}