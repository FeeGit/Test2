package com.example.locationtutorial;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

    static int N = 0;
    static Point[] POINT = new Point[20];

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;

    LocationCallback locationCallback = new LocationCallback() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {     // 받아올때마다 출력.
                Log.d(TAG, "onLocationResult: " + location.toString()); // 로그 출력.
                //jugementClicked();

                POINTS[N++] = new Point(location.getLatitude() * 10000, location.getLongitude()*10000);
                //데이터베이스의 위치를 가져오자.
                MemoDbHelper dbHelper = MemoDbHelper.getInstance(MainActivity.this);
                //커서에 담아서
                Cursor cursor =  dbHelper.getReadableDatabase().query(MemoContract.MemoEntry.TABLE_NAME, null, null, null,null,null,null,null);
                //전체를 배열에 담아준다.
                cursor.moveToNext();
                String lat = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUMN_NAME_LAT));
                String lng = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUMN_NAME_LNG));
                POINTS[++N] = new Point(Double.parseDouble(lat) * 10000, Double.parseDouble(lng) * 10000);

                //내 위치는 따로 저장해준다.
                Point MY_POINTS = POINTS[0];
                //y를 우선으로 '/'처럼 아래에서 위로 쭉 정렬을 해준다.
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

                //이제 0을 기준으로 상대 위치를 새롭게 정의해준다.(이를 위해서 아래를 우선적으로 정렬한 것이다.
                for (int i = 1; i < N; i++) {
                    POINTS[i].p = POINTS[i].x - POINTS[0].x;
                    POINTS[i].q = POINTS[i].y - POINTS[0].y;
                }

                //이제 기준점 0 을 제외한 것들을 상대위치를 활용하여 정렬
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

                //스택에는 데이터를 절약하기 위해서 index만 담아준다.
                Stack<Integer> stack = new Stack<>();
                stack.add(0);
                stack.add(1);

                //모든 데이터가 스택에 들어갈 수 있도록 전체 반복
                for (int i = 2; i < N; i++) {
                    //사이즈가 2보다 크면
                    while(stack.size() >= 2){
                        int first = stack.pop();
                        int second = stack.peek();
                        //들어있는 점들을 확인하여서 가장 외부의 점인지를 확인한다.
                        long ccw = find_ccw(POINTS[first], POINTS[second], POINTS[i]);
                        if (ccw > 0) {
                            //맞으면 stack에 담아준다.
                            stack.add(first);
                            break;
                        }
                    }
                    stack.add(i);
                }

                //이제 나의 위치가 영역의 내부인지 외부인지 확인을 해주자.
                boolean isInside = true;
                for(int i=0;i<stack.size();i++){
                    if(POINTS[stack.get(i)].x == MY_POINTS.x && POINTS[stack.get(i)].y == MY_POINTS.y){
                        isInside = false;
                    }
                }

                //아래는 위의 bool 값을 활용해서 메시지 창을 띄우는 부분이다.
                if(isInside){
                    final Context context = this;
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                    alertDialogBuilder.setTitle("위험경보");

                    alertDialogBuilder
                            .setMessage("위험지역에 위치하였습니다.")
                            .setCancelable(false)
                            .setPositiveButton("삭제",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .setNegativeButton("취소",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            // 다이얼로그를 취소한다
                                            dialog.cancel();
                                        }
                                    });

                    // 다이얼로그 생성
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                else{
                    final Context context = this;
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                    alertDialogBuilder.setTitle("경보 아님");

                    alertDialogBuilder
                            .setMessage("경보가 아니에요")
                            .setCancelable(false)
                            .setPositiveButton("삭제",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .setNegativeButton("취소",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            // 다이얼로그를 취소한다
                                            dialog.cancel();
                                        }
                                    });

                    // 다이얼로그 생성
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(4000);      //업데이트 간격 4000ms
        locationRequest.setFastestInterval(2000);       //빠르면 2000ms
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void moveMarkerList(View view) {
        startActivity(new Intent(this, MarkerListActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            getLastLocation();        //가장 마지막의 위치 체크.
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

    public void PopMessage(boolean isInside){

    }

    //이제 마지막이다. convex hull 알고리즘을 활용해보았다.
    //http://woowabros.github.io/experience/2018/03/31/hello-geofence.html(이 부분을 참고하면, 비슷한 방식의 문제해결 방법을 볼 수 있다.)
//    static int N = 0;
    static Point[] POINTS = new Point[20];

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void jugementClicked() {
        //클릭할때마다 초기화를 해주자.
        POINTS = new Point[20];
        N = 0;
//        //나의 현재 위치를 가져오자.
//        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
//            return;
//        }
//        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
//            @Override
//            public void onSuccess(Location location) {
//                if(location != null){
//                    //가져온 위치를 배열에 저장한다.
//                    POINTS[N++] = new Point(location.getLatitude() * 10000, location.getLongitude()*10000);
//                }
//            }
//        });


//        //데이터베이스의 위치를 가져오자.
//        MemoDbHelper dbHelper = MemoDbHelper.getInstance(this);
//        //커서에 담아서
//        Cursor cursor =  dbHelper.getReadableDatabase().query(MemoContract.MemoEntry.TABLE_NAME, null, null, null,null,null,null,null);
//        //전체를 배열에 담아준다.
//        if(cursor.moveToNext()){
//            String lat = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUMN_NAME_LAT));
//            String lng = cursor.getString(cursor.getColumnIndexOrThrow(MemoContract.MemoEntry.COLUMN_NAME_LNG));
//            POINTS[N++] = new Point(Double.parseDouble(lat) * 10000, Double.parseDouble(lng) * 10000);
//        }

        //내 위치는 따로 저장해준다.
        Point MY_POINTS = POINTS[0];
        //y를 우선으로 '/'처럼 아래에서 위로 쭉 정렬을 해준다.
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

        //이제 0을 기준으로 상대 위치를 새롭게 정의해준다.(이를 위해서 아래를 우선적으로 정렬한 것이다.
        for (int i = 1; i < N; i++) {
            POINTS[i].p = POINTS[i].x - POINTS[0].x;
            POINTS[i].q = POINTS[i].y - POINTS[0].y;
        }

        //이제 기준점 0 을 제외한 것들을 상대위치를 활용하여 정렬
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

        //스택에는 데이터를 절약하기 위해서 index만 담아준다.
        Stack<Integer> stack = new Stack<>();
        stack.add(0);
        stack.add(1);

        //모든 데이터가 스택에 들어갈 수 있도록 전체 반복
        for (int i = 2; i < N; i++) {
            //사이즈가 2보다 크면
            while(stack.size() >= 2){
                int first = stack.pop();
                int second = stack.peek();
                //들어있는 점들을 확인하여서 가장 외부의 점인지를 확인한다.
                long ccw = find_ccw(POINTS[first], POINTS[second], POINTS[i]);
                if (ccw > 0) {
                    //맞으면 stack에 담아준다.
                    stack.add(first);
                    break;
                }
            }
            stack.add(i);
        }

        //이제 나의 위치가 영역의 내부인지 외부인지 확인을 해주자.
        boolean isInside = true;
        for(int i=0;i<stack.size();i++){
            if(POINTS[stack.get(i)].x == MY_POINTS.x && POINTS[stack.get(i)].y == MY_POINTS.y){
                isInside = false;
            }
        }


        //아래는 위의 bool 값을 활용해서 메시지 창을 띄우는 부분이다.
        if(isInside){
            final Context context = this;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

            alertDialogBuilder.setTitle("위험경보");

            alertDialogBuilder
                    .setMessage("위험지역에 위치하였습니다.")
                    .setCancelable(false)
                    .setPositiveButton("삭제",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("취소",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    // 다이얼로그를 취소한다
                                    dialog.cancel();
                                }
                            });

            // 다이얼로그 생성
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
        else{
            final Context context = this;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

            alertDialogBuilder.setTitle("경보 아님");

            alertDialogBuilder
                    .setMessage("경보가 아니에요")
                    .setCancelable(false)
                    .setPositiveButton("삭제",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("취소",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    // 다이얼로그를 취소한다
                                    dialog.cancel();
                                }
                            });

            // 다이얼로그 생성
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    protected static long find_ccw(Point a, Point b, Point c) {
        return (long)(b.x - a.x) * (long)(c.y - a.y) - (long)(c.x - a.x) * (long)(b.y - a.y);
    }

    static class Point {
        long x, y;
        //기준점으로부터의 상대 위치
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
}