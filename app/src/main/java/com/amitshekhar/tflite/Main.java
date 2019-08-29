package com.amitshekhar.tflite;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Main extends AppCompatActivity implements SurfaceHolder.Callback {

    private String TAG = "SurfaceView";

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private WebView mWebView;
    private Button btnControl, btnFish, btnGPS,btn_emergency;

    private TextView lblObjectName;
    private TextView lblAddress;

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    private static final String MODEL_PATH = "model.tflite";
    private static final boolean QUANT = true;
    private static final String LABEL_PATH = "dict.txt";
    private static final int INPUT_SIZE = 224;

    private boolean isDetectionStarted = false;

    private Thread captureThread;
    private Handler handler;
    private boolean isThreadStopped = false;

    private String currentAddress = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_camera);

        handler = new Handler();

        //cameraView = findViewById(R.id.cameraView);
        btnControl = findViewById(R.id.btn_control);
        btnFish = findViewById(R.id.btn_fish);
        btnGPS = findViewById(R.id.btn_gps);
        mWebView = findViewById(R.id.mWebView);
        btn_emergency = findViewById(R.id.btn_emergency);

        lblObjectName = findViewById(R.id.lbl_object_name);
        lblAddress = findViewById(R.id.lbl_address);


        surfaceView =  findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        0);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
        }


        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // GPS 프로바이더 사용가능여부
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 네트워크 프로바이더 사용가능여부
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.d("Main", "isGPSEnabled="+ isGPSEnabled);
        Log.d("Main", "isNetworkEnabled="+ isNetworkEnabled);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                final double lat = location.getLatitude();
                final double lng = location.getLongitude();

                Log.d("LOCATION","latitude: "+ lat +", longitude: "+ lng);



                final Geocoder geocoder = new Geocoder(getApplicationContext());

                // 위도,경도 입력 후 변환 버튼 클릭
                List<Address> list = null;
                try {
                    double d1 = lat;
                    double d2 = lng;

                    list = geocoder.getFromLocation(
                            d1, // 위도
                            d2, // 경도
                            10); // 얻어올 값의 개수
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
                }
                if (list != null) {
                    if (list.size()==0) {
                        Log.d("ERROR", "해당되는 주소 정보는 없습니다");
                    } else {
                        for(int i = 0; i <list.size(); i ++) {
                            Log.d("ADDRESS", list.get(i).toString());
                            if(list.get(i).getAddressLine(0).contains("-")) {
                                currentAddress = list.get(i).getAddressLine(0);
                                break;
                            }
                        }
                    }
                }

                btn_emergency.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {


                        String latit = Double.toString(lat);
                        String longit = Double.toString(lng);

                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("01073326255", null, "위급상황입니다. \n" +"위도 : " + latit + "\n" +"경도 : " + longit , null, null);




                    }

                });




            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
               // logView.setText("onStatusChanged");
            }

            public void onProviderEnabled(String provider) {
               // logView.setText("onProviderEnabled");
            }

            public void onProviderDisabled(String provider) {
              //  logView.setText("onProviderDisabled");
            }
        };

        // Register the listener with the Location Manager to receive location updates
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch(SecurityException e) {
            e.printStackTrace();
        }



        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWebView.getVisibility() == View.VISIBLE) {
                    mWebView.setVisibility(View.GONE);
                } else {
                    mWebView.setVisibility(View.VISIBLE);
                    String myUrl = "http:192.168.43.105";
                    mWebView.loadUrl(myUrl);
                }
            }
        });

        btnFish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isDetectionStarted) {
                    if(captureThread.isAlive()) {
                        captureThread.interrupt();
                    }
                    isDetectionStarted = false;
                    isThreadStopped = true;
                } else {
                    captureThread = new Thread() {
                        @Override
                        public void run() {
                            isThreadStopped = false;
                            try {
                                while(true) {

                                    camera.takePicture(null, null, new Camera.PictureCallback() {
                                        @Override
                                        public void onPictureTaken(byte[] data, Camera camera) {
                                            BitmapFactory.Options opt = new BitmapFactory.Options();
                                            opt.inMutable = true;
                                            Bitmap mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
                                            mBitmap = Bitmap.createScaledBitmap(mBitmap, INPUT_SIZE, INPUT_SIZE, false);

                                            final List<Classifier.Recognition> results = classifier.recognizeImage(mBitmap);
                                            handler.post(new Runnable() {
                                                public void run() {
                                                    lblObjectName.setText("");

                                                }


                                            });

                                            if(!isThreadStopped) {
                                                final StringBuffer sb = new StringBuffer(20);
                                                for(int i = 0; i < 1; i ++) {
                                                    Log.d("Result" , results.get(i).getTitle());
                                                    if(i != 0) sb.append(", ");
                                                    sb.append(results.get(i).getTitle());
                                                }

                                                handler.post(new Runnable() {
                                                    public void run() {
                                                        lblObjectName.setText(sb.toString());
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    Thread.sleep(1000);
                                }
                            } catch(InterruptedException e) {
                                Log.d("THREAD", "중지 버튼 누름");
                                handler.post(new Runnable( ) {
                                    @Override
                                    public void run() {
                                        lblObjectName.setText("");
                                    }
                                });
                            } catch(RuntimeException e) {
                                Log.d("THREAD", "앱 종료");
                            }
                        }
                    };
                    captureThread.start();
                    isDetectionStarted = true;
                }
            }
        });


        btnGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(lblAddress.getText().toString().equals("")) {
                    lblAddress.setText(currentAddress);
                } else {
                    lblAddress.setText("");
                }
            }
        });

        initTensorFlowAndLoadModel();
    }

    @Override
    public void onResume() {
        super.onResume();
      //  cameraView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
     //   cameraView.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                  //  makeButtonVisible();
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "카메라 기능 해제");

        try {
            if(captureThread.isAlive()) {
                captureThread.interrupt();
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
        }

        camera.release();
        camera = null;

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "카메라 미리보기 활성");
        camera = Camera.open();

        try {
            camera.setPreviewDisplay(holder);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
        Log.i(TAG,"카메라 미리보기 활성, width:" + width + ", height:" + height);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(width, height);
    //    parameters.set("orientation", "landscape");
        parameters.set("orientation", "portrait");
        if(this.getResources().getConfiguration().orientation == 2) {

        } else {
            camera.setDisplayOrientation(90);
        }
        Log.d("orientation" , String.valueOf(this.getResources().getConfiguration().orientation));

        camera.startPreview();

    }
}
