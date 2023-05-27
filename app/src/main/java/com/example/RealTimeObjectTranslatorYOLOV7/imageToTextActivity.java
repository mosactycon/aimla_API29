package com.example.RealTimeObjectTranslatorYOLOV7;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Locale;

public class imageToTextActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView translate_button;
    private ImageView take_picture_button;
    private ImageView storage_image_button;
    private ImageView current_image;
    private TextView textView;
    private LinearLayout read_text_bar;
    private TextRecognizer textRecognizer;
    private String Camera_or_recognizeText="camera;";
    private Bitmap bitmap=null;
    private TextToSpeech textToSpeech;
    private Button read_text_button;
    private Context context;


    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public imageToTextActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(imageToTextActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(imageToTextActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_image_to_text);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        textView=findViewById(R.id.textView);
        textView.setVisibility(View.GONE);
        read_text_bar=findViewById(R.id.read_text_bar);
        current_image=findViewById(R.id.current_image);
        current_image.setVisibility(View.GONE);
        read_text_button=findViewById(R.id.read_text_button);
        read_text_button.setVisibility(View.GONE);


        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(new Locale("id","ID"));

                }
            }
        });

        read_text_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = textView.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        textRecognizer= TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
        take_picture_button=findViewById(R.id.take_picture_button);
        take_picture_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP){
                    if(Camera_or_recognizeText=="camera"){
                        take_picture_button.setColorFilter(Color.BLUE);
                        Mat a=mRgba.t();
                        Core.flip(a,mRgba,1);
                        a.release();
                        bitmap= Bitmap.createBitmap(mRgba.cols(),mRgba.rows(),Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mRgba,bitmap);
                        mOpenCvCameraView.disableView();
                        Camera_or_recognizeText="recognizeText";

                    }
                    else{
                        take_picture_button.setColorFilter(Color.WHITE);
                        textView.setVisibility(View.GONE);
                        read_text_button.setVisibility(View.GONE);
                        current_image.setVisibility(View.GONE);
                        mOpenCvCameraView.enableView();
                        textView.setText("");
                        Camera_or_recognizeText="camera";
                    }
                    return true;
                }
                return false;
            }
        });

        translate_button=findViewById(R.id.translate_button);
        translate_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    translate_button.setColorFilter(Color.BLUE);
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP){
                    translate_button.setColorFilter(Color.WHITE);
                    if(Camera_or_recognizeText=="recognizeText"){
                        textView.setVisibility(View.VISIBLE);
//                        read_text_bar.setVisibility(View.VISIBLE);
                        read_text_button.setVisibility(View.VISIBLE);
                        InputImage image=InputImage.fromBitmap(bitmap,0);
                        Task<Text> result=textRecognizer.process(image)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text text) {

                                        textView.setText(text.getText());
                                        Log.d("imageToTextActivity", "Out"+text.getText());
                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("text:",text.getText());
                                        clipboard.setPrimaryClip(clip);

                                        Toast.makeText(imageToTextActivity.this,"Copied to clipboard",
                                                Toast.LENGTH_LONG).show();

                                    }
                                })

                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });

                    }
                    else{

                        Toast.makeText(imageToTextActivity.this,"Please take a picture",
                                Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });

        storage_image_button=findViewById(R.id.storage_image_button);
        storage_image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(imageToTextActivity.this, storageRecognitionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){

            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{

            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();

        return mRgba;

    }

}