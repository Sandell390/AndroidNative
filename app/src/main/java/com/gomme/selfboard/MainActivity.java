package com.gomme.selfboard;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpEntity;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.ClientProtocolException;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.HttpPost;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Button button;
    Button btn_ShowImage;
    RelativeLayout relativeLayout;
    Uri photoPath;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.captureImg);
        button.setOnClickListener(this::OnClickButton);
        //textView = findViewById(R.id.textView);
        relativeLayout = findViewById(R.id.layout1);
        btn_ShowImage = findViewById(R.id.btn_show_image);
        btn_ShowImage.setOnClickListener(this::GetData);
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        x = new HashMap<>();
        y = new HashMap<>();
        dx = new HashMap<>();
        dy = new HashMap<>();
    }
    ArrayList<ImageView> imageViews = new ArrayList<>();
    public Map<Integer, Float> x, y = new HashMap<Integer,Float>();

    public Map<Integer, Float> dx, dy = new HashMap<Integer,Float>();





    public void openSomeActivityForResult() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {

        }
        Uri photoURI = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        photoPath = Uri.parse(photoFile.getPath());
        someActivityResultLauncher.launch(intent);
    }

    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    File image = new File(photoPath.getPath());
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos); // bm is the bitmap object
                    byte[] b = baos.toByteArray();
                    String imageString = Base64.encodeToString(b, Base64.DEFAULT);
                    Log.i("TAG", "Image: "+ imageString);
                    //imageView.setImageBitmap(bitmap);
                    //textView.setText(imageString);
                    try {
                        UploadFile(imageString);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    DeleteFile(photoPath.getPath());

                }
            });

    public void OnClickButton(View view) {
        openSomeActivityForResult();
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private boolean DeleteFile(String filePath){

        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("file Deleted :" + photoPath.getPath());
                return true;
            } else {
                System.out.println("file not Deleted :" + photoPath.getPath());
                return false;
            }
        }
        return false;
    }

    private void GetData(View view){
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        JsonArrayRequest postRequest = new JsonArrayRequest(Request.Method.GET, "http://192.168.0.27:5177/Picture/Get",
                new JSONArray(),
                new Response.Listener<JSONArray>() {
                    @SuppressLint("ResourceType")
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("INFo", "DATA SENDT");
                        Log.i("INfo", response.toString());

                        if(response.length() > 0){
                            try {
                                imageViews.clear();
                                Log.i("out","out");
                                for (int i = 0; i < response.length(); i++){
                                    Log.i("in","in");

                                    byte[] bytes = Base64.decode(response.getString(i), Base64.DEFAULT);
                                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                                    ImageView temp_imageView = new ImageView(MainActivity.this);
                                    temp_imageView.setLayoutParams(new RelativeLayout.LayoutParams(200,200));
                                    temp_imageView.setImageBitmap(bitmap);
                                    temp_imageView.setId(10420000 + i);
                                    x.put(temp_imageView.getId(), 0f);
                                    y.put(temp_imageView.getId(), 0f);
                                    dx.put(temp_imageView.getId(), 0f);
                                    dy.put(temp_imageView.getId(), 0f);
                                    imageViews.add(temp_imageView);
                                    int finalI = i;
                                    temp_imageView.setOnTouchListener(new View.OnTouchListener() {
                                        @Override
                                        public boolean onTouch(View view, MotionEvent motionEvent) {
                                            int id = view.getId();
                                            Log.i("info", String.valueOf(x.get(id)));
                                            if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                                                x.replace(id,motionEvent.getX());
                                                y.replace(id,motionEvent.getY());
                                            }

                                            if(motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                                                //dx = motionEvent.getX() -x;
                                                //dy = motionEvent.getY() -y;

                                                dx.replace(id,motionEvent.getX() - x.get(id));
                                                dy.replace(id,motionEvent.getY() - y.get(id));

                                                imageViews.get(finalI).setX(imageViews.get(finalI).getX() + dx.get(id));
                                                imageViews.get(finalI).setY(imageViews.get(finalI).getY() + dy.get(id));
                                                //imageView.setX(imageView.getX() + dx);
                                                //imageView.setY(imageView.getY() + dy);

                                                x.replace(id,motionEvent.getX());
                                                y.replace(id,motionEvent.getY());
                                                //x = motionEvent.getX();
                                                //y = motionEvent.getY();
                                            }

                                            return true;
                                        }
                                    });
                                    relativeLayout.addView(temp_imageView);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //   Handle Error
                        Log.e("ERR",error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("User-agent", System.getProperty("http.agent"));
                return headers;
            }
        };
        requestQueue.add(postRequest);
        //requestQueue.start();
    }
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });
    private void UploadFile(String imagedata) throws JSONException {

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, "http://192.168.0.27:5177/Picture/Post",
                new JSONObject("{ \"ImageData\": \"" + imagedata+"\"}"),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("INFo", "DATA SENDT");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //   Handle Error
                        Log.e("ERR",error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                headers.put("User-agent", System.getProperty("http.agent"));
                return headers;
            }
        };
        requestQueue.add(postRequest);
    }

}