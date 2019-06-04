package com.example.lenovo.mygraduate8b;
/**
 * 此页面是主界面，大多功能和管理员界面类似，负责验证登录，通过该界面可以跳转至管理员界面，但需要密码验证。
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import org.bytedeco.opencv.global.opencv_core;
//import org.bytedeco.opencv.global.opencv_imgproc;

//import org.bytedeco.javacv.OpenCVFrameConverter;
//import org.bytedeco.opencv.opencv_core.CvHistogram;
//import org.bytedeco.opencv.opencv_core.IplImage;
//import org.bytedeco.javacv.OpenCVFrameConverter;
//import org.bytedeco.opencv.opencv_core.IplImage;
import com.friendlyarm.FriendlyThings.GPIOEnum;
import com.friendlyarm.FriendlyThings.HardwareControler;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//import static org.bytedeco.opencv.global.opencv_core.CV_HIST_ARRAY;
//import static org.bytedeco.opencv.global.opencv_imgproc.CV_COMP_CORREL;
//import static org.bytedeco.opencv.global.opencv_imgproc.cvCompareHist;
//import static org.bytedeco.opencv.global.opencv_imgproc.cvNormalizeHist;
//import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
//import static org.bytedeco.opencv.helper.opencv_imgproc.cvCalcHist;


//import org.bytedeco.javacv.*;
//import org.bytedeco.javacpp.*;
//import org.bytedeco.javacpp.indexer.*;
//import org.bytedeco.opencv.opencv_core.*;
//import org.bytedeco.opencv.opencv_imgproc.*;
//import org.bytedeco.opencv.opencv_calib3d.*;
//import org.bytedeco.opencv.opencv_objdetect.*;
//import static org.bytedeco.opencv.global.opencv_core.*;
//import static org.bytedeco.opencv.global.opencv_imgproc.*;
//import static org.bytedeco.opencv.global.opencv_calib3d.*;
//import static org.bytedeco.opencv.global.opencv_objdetect.*;
//import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage;
//import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;


public class RMainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {
    private CameraBridgeViewBase cameraView;
    private CascadeClassifier classifier;
    private Mat mGray;
    private Mat mRgba;
    private Mat mImage;
    private int mAbsoluteFaceSize = 0;
    private boolean isFrontCamera;
    private String faceDir;

    private SQLiteDatabase sqLiteDatabase;
    private String userName="";
    private String curRecord ="";

    //声音录制控件
    private static final DateFormat mFormatter = new SimpleDateFormat("mm:ss:SS");
    private AudioRecord ar = null;
    private String recordPath = null;

    private TextView textAdmin;

    private boolean faceOk = false;
    private ImageView imageViewCur;
    // Used to load the 'native-lib' library on application startup

    public Facenet facenet;

    private static final int SAMPLE_RATE = 44100;//Hz，采样频率
//    private static final double FREQUENCY = 500; //Hz，标准频率（这里分析的是500Hz）
    private static final double RESOLUTION = 200; //Hz，误差
    private int bufferSize=0;

    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rmain);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO}, 1);
        }
        cameraView = (CameraBridgeViewBase)findViewById(R.id.camera_view);
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        isFrontCamera = true;
        initClassifier();
        cameraView.enableView();
        Button button = findViewById(R.id.switch_camera);
        button.setOnClickListener(this);
        Button buttonLog = findViewById(R.id.btnLog);
        buttonLog.setOnClickListener(this);
        faceDir = getExternalFilesDir(null)+"/faces";
        File fdir = new File(faceDir);
        if(!fdir.exists())
        {
            fdir.mkdir();
        }

        DatabaseHelper databaseHelper = new DatabaseHelper(RMainActivity.this,"userInfo");
        sqLiteDatabase = databaseHelper.getWritableDatabase();

        textAdmin = findViewById(R.id.textAdmin);
        textAdmin.setText(getClickabelSpanAdmin());
        textAdmin.setMovementMethod(LinkMovementMethod.getInstance());


        faceOk = false;
        imageViewCur = findViewById(R.id.imageViewCur);
        facenet=new Facenet(getAssets());
        //monitorFace();
        HardwareControler.exportGPIOPin(1033);
        HardwareControler.setGPIODirection(1033,GPIOEnum.OUT);
        HardwareControler.exportGPIOPin(1050);
        HardwareControler.setGPIODirection(1050,GPIOEnum.OUT);
    }
    /*
    * demoGPIOPins.put("Pin11", 1033);
      demoGPIOPins.put("Pin12", 1050);
      demoGPIOPins.put("Pin15", 1036);
      demoGPIOPins.put("Pin16", 1054);
      demoGPIOPins.put("Pin18", 1055);
      demoGPIOPins.put("Pin22", 1056);
      demoGPIOPins.put("Pin7", 1032);
      HardwareControler.setGPIOValue(1033,GPIOEnum.HIGH);
      HardwareControler.setGPIOValue(1033,GPIOEnum.LOW);*/


//    private void monitorFace()//用于实时对比人脸，但帧率太低，遂弃用
//    {
//        final Handler handler = new Handler()
//        {
//            public void handleMessage(Message msg) {
//                switch (msg.what) {
//                    case 1:
//                        //searchUser();
//                        checkFace();
//                        break;
//                }
//                super.handleMessage(msg);
//            }
//        };
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (!Thread.currentThread().isInterrupted()) {
//                    Message message = new Message();
//                    message.what = 1;
//                    handler.sendMessage(message);
//                    try {
//                        Thread.sleep(2000);
//                       // Log.v("sleep","2000");
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
////                    try {
////                        Thread.sleep(2000);
////                    } catch (InterruptedException e) {
////                        Thread.currentThread().interrupt();
////                    }
//                }
//            }
//        }).start();
//    }

    //管理员界面跳转
    private SpannableString getClickabelSpanAdmin()
    {
        SpannableString spStr = new SpannableString("点此进入管理员界面");
        spStr.setSpan(new UnderlineSpan(),0,9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spStr.setSpan(new ForegroundColorSpan(Color.RED),0,9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spStr.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                final AlertDialog addDialog = new AlertDialog.Builder(RMainActivity.this).create();

                final View checkview = getLayoutInflater().inflate(R.layout.admin_check, null);
                addDialog.setView(checkview);
                final EditText editText = checkview.findViewById(R.id.textCheck);
                final TextView textView = checkview.findViewById(R.id.textTrick);
                Button okBtn = checkview.findViewById(R.id.buttonCheck);
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Do your coding here

                        //如果要刷新页面则可调用onResume()方法
                        //onResume();
                        String pwd = editText.getText().toString();
                        if(pwd.equals("8888"))
                        {
                            addDialog.cancel();
                            Intent intent = new Intent(RMainActivity.this,MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else
                        {
                            textView.setVisibility(View.VISIBLE);

                            editText.setText("");
                        }
                    }
                });

                addDialog.show();
            }

        },0,9,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spStr;
    }





    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.switch_camera:
                cameraView.disableView();
                if (isFrontCamera) {
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                    isFrontCamera = false;
                } else {
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                    isFrontCamera = true;
                }
                cameraView.enableView();
                break;
            case R.id.btnLog:
                checkFace();
                //Toast.makeText(this,"别点我",Toast.LENGTH_LONG).show();
                break;
            default:
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showNewDialogToRecord()
    {
        final AlertDialog addDialog = new AlertDialog.Builder(RMainActivity.this).create();

        final View recordview = getLayoutInflater().inflate(R.layout.activity_record, null);
        addDialog.setView(recordview);
        final Button btnRecord = recordview.findViewById(R.id.btn_Record_record);
        final TextView textTime = recordview.findViewById(R.id.Record_time);
        final TextView textExp = recordview.findViewById(R.id.Record_explain);
        textExp.setText(userName+",请录音");
        btnRecord.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        btnRecord.setBackgroundColor(R.color.timeColor);
                        btnRecord.setText("松开结束");
                        final long RstartTime = System.currentTimeMillis();
                        startRecord();

                        textExp.setVisibility(View.INVISIBLE);
                        textTime.setVisibility(View.VISIBLE);
                        @SuppressLint("HandlerLeak")
                        final Handler inhandler = new Handler()
                        {
                            public void handleMessage(Message msg) {
                                switch (msg.what) {
                                    case 1:
                                        //searchUser();
                                        String timeF = mFormatter.format(new Date(System.currentTimeMillis() - RstartTime));
                                        textTime.setText(timeF);
                                        break;
                                }
                                super.handleMessage(msg);
                            }
                        };
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while(!Thread.currentThread().isInterrupted())
                                {
                                    Message message = new Message();
                                    message.what = 1;
                                    inhandler.sendMessage(message);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }

                            }
                        }).start();
                        break;

                    case MotionEvent.ACTION_UP:
                        btnRecord.setBackgroundColor(R.color.label_round_blue);
                        btnRecord.setText("按住录音");
                        stopRecord();
                        textExp.setVisibility(View.VISIBLE);
                        textTime.setVisibility(View.INVISIBLE);
                        boolean res = cmpRecord(recordPath,curRecord);
                        if(res)
                        {
                            HardwareControler.setGPIOValue(1033,GPIOEnum.HIGH);
                            HardwareControler.setGPIOValue(1050,GPIOEnum.HIGH);
                            showLogSuccessDialog();

                            //Toast.makeText(RMainActivity.this,"恭喜，登录成功",Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            showLogFailDialog();
                            //Toast.makeText(RMainActivity.this,"登录失败，请重试",Toast.LENGTH_LONG).show();
                        }
                        addDialog.cancel();

                        break;
                }
                return true;
            }
        });

        addDialog.show();
    }

    private void showLogSuccessDialog()
    {
        final AlertDialog alertDialog = new AlertDialog.Builder(RMainActivity.this).create();
        final View view = getLayoutInflater().inflate(R.layout.activity_login_success,null);
        alertDialog.setView(view);
        final Button btnOK = view.findViewById(R.id.buttonLoginOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HardwareControler.setGPIOValue(1033,GPIOEnum.LOW);
                HardwareControler.setGPIOValue(1050,GPIOEnum.LOW);
                faceOk = false;
                alertDialog.cancel();
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                HardwareControler.setGPIOValue(1033,GPIOEnum.LOW);
                HardwareControler.setGPIOValue(1050,GPIOEnum.LOW);
                faceOk = false;
            }
        });
        final TextView textView = view.findViewById(R.id.textViewLoginSuccess);
        textView.setText("恭喜"+userName+",登录成功");
        alertDialog.show();
    }
    private void showLogFailDialog()
    {
        final AlertDialog alertDialog = new AlertDialog.Builder(RMainActivity.this).create();
        final View view = getLayoutInflater().inflate(R.layout.activity_login_fail,null);
        alertDialog.setView(view);
        final Button btnWrong = view.findViewById(R.id.buttonLoginWrong);
        btnWrong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                faceOk = false;
                alertDialog.cancel();
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                faceOk = false;
            }
        });
        alertDialog.show();
    }

    //cmp two record by filepath
    //通过音频的文件路径，对比音频的相似度，但为了对比方便，音频存储的格式不可播放
    private boolean cmpRecord(String ap,String bp)
    {
        if(ap==null||bp==null)return false;
        double fre1 = getFreq(ap);
        double fre2 = getFreq(bp);
        Log.v("freq1",String.valueOf(fre1));
        Log.v("freq2",String.valueOf(fre2));
        if(Math.abs(fre1-fre2)<RESOLUTION)return true;
        return false;
    }
    private double getFreq(String filep)
    {
        try {
            Log.v("in",filep);
            DataInputStream inputStream1 = new DataInputStream(new FileInputStream(filep));
            //16bit采样，因此用short[]
            //如果是8bit采样，这里直接用byte[]
            //从文件中读出一段数据，这里长度是SAMPLE_RATE，也就是1s采样的数据
            short[] buffer1 = new short[SAMPLE_RATE];
            for (int i = 0; i < buffer1.length; i++) {
                buffer1[i] = inputStream1.readShort();
            }
            short[] data1 = new short[FFT.FFT_N];
//            byte[] buffer1 = new byte[SAMPLE_RATE];
//            for (int i = 0; i < buffer1.length; i++) {
//                buffer1[i] = inputStream1.readByte();
//            }
//            byte[] data1 = new byte[FFT.FFT_N];
            Log.v("inTest","1");
            //为了数据稳定，在这里FFT分析只取最后的FFT_N个数据
            System.arraycopy(buffer1, buffer1.length - FFT.FFT_N,
                    data1, 0, FFT.FFT_N);
            Log.v("inTest","2");
            //FFT分析得到频率
            double frequence1 = FFT.GetFrequency(data1);
            Log.v("inTest",String.valueOf(frequence1));
            return frequence1;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    //进行人脸对比，即当前人脸和数据库中已注册人脸进行对比
    private void checkFace()
    {
        if(!faceOk&&mImage!=null&&mImage.cols()>0&&mImage.rows()>0)
        {
            Bitmap bitmapface1 = Bitmap.createBitmap(mImage.cols(),mImage.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mImage,bitmapface1);
            imageViewCur.setImageBitmap(bitmapface1);


            Cursor cursor = sqLiteDatabase.query("user", new String[]{"name","facePath","voicePath"}, null, null, null, null, null);
            //利用游标遍历所有数据对象
            double min_cmp_value = 2.0;
            String final_user_name = "";
            String final_user_record = "";
            while(cursor.moveToNext()){
                String faceP = cursor.getString(cursor.getColumnIndex("facePath"));
                String cur_name = cursor.getString(cursor.getColumnIndex("name"));
                String cur_record = cursor.getString(cursor.getColumnIndex("voicePath"));
                Bitmap bitmapface2 = BitmapFactory.decodeFile(faceP);
                double cur_cmp_value = 0;
                try
                {
                    cur_cmp_value = cmpPic(bitmapface1,bitmapface2);
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                Log.v("value","in "+String.valueOf(cur_cmp_value));
                if(cur_cmp_value<min_cmp_value)
                {
                    min_cmp_value = cur_cmp_value;
                    final_user_name = cur_name;
                    final_user_record = cur_record;
                }
            }
            Log.v("value",String.valueOf(min_cmp_value));
            if(min_cmp_value<0.8)
            {
                userName = final_user_name;
                faceOk = true;
                curRecord = final_user_record;
                mImage = null;
                showNewDialogToRecord();
            }
            else
            {
                Toast.makeText(this,"人脸识别失败，请重试",Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Toast.makeText(this,"人脸检测失败，请重试",Toast.LENGTH_LONG).show();
            Log.v("wrong","wrong");
        }
    }

    private double cmpPic(Bitmap bitmapface1,Bitmap bitmapface2)
    {
        FaceFeature ff1=facenet.recognizeImage(bitmapface1);
        FaceFeature ff2=facenet.recognizeImage(bitmapface2);
        //(4)比较
        return ff1.compare(ff2);
    }





    private void initClassifier() {
        try {
            InputStream is = getResources()
                    .openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mImage = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mImage.release();
    }




    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Size size = mRgba.size();
        // 翻转矩阵以适配前后置摄像头
        if (isFrontCamera) {
            Core.flip(mRgba, mRgba, 1);
            Core.flip(mGray, mGray, 1);
        } else {
            Core.flip(mRgba, mRgba, -1);
            Core.flip(mGray, mGray, -1);
        }
        float mRelativeFaceSize = 0.2f;
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        MatOfRect faces = new MatOfRect();
        if (classifier != null)
            classifier.detectMultiScale(mGray, faces, 1.1, 2, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        Rect[] facesArray = faces.toArray();
        Scalar faceRectColor = new Scalar(0, 255, 0, 255);
        for (Rect faceRect : facesArray)
        {
            Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), faceRectColor, 3);
            Mat sub = mRgba.submat(faceRect);
            if(mImage!=null)
            {
                Imgproc.resize(sub,mImage,size);
            }
            else
            {
                mImage = new Mat();
            }

        }
        return mRgba;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }


    //声音录制
    //声音录制所需函数
    /*开始录制*/
    private void startRecord(){
        if(ar == null){
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            ar = new AudioRecord(MediaRecorder.AudioSource.MIC,SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            ar.startRecording();
            new Thread(new AudioRecordThread()).start();
//            mr.setAudioSource(MediaRecorder.AudioSource.MIC);  //音频输入源
//            mr.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);   //设置输出格式
//            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);   //设置编码格式
//            mr.setOutputFile(soundFile.getAbsolutePath());

//            try {
//                mr.prepare();
//                mr.start();  //开始录制
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        }
    }

    //停止录制，资源释放
    private void stopRecord(){
        ar.stop();
    }
    private class  AudioRecordThread implements Runnable{
        @Override
        public void run() {
            //将录音数据写入文件
            short[] audiodata = new short[bufferSize/2];
            DataOutputStream fos = null;
            File dir = new File(getExternalFilesDir(null),"sounds");
            if(!dir.exists()){
                dir.mkdirs();
            }
            if(recordPath!=null)recordPath=null;
            recordPath = getExternalFilesDir(null)+"/sounds/"+System.currentTimeMillis();
            File soundFile = new File(recordPath);
            if(!soundFile.exists()){
                try {
                    soundFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            Log.i("outFile",soundFile.getAbsolutePath());
            try {
                fos = new DataOutputStream( new FileOutputStream(soundFile));
                int readSize;
                while (ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
                    readSize = ar.read(audiodata,0,audiodata.length);
                    if(AudioRecord.ERROR_INVALID_OPERATION != readSize){
                        for(int i = 0;i<readSize;i++){
                            fos.writeShort(audiodata[i]);
                            fos.flush();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(fos!=null){
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //在这里release
                ar.release();
                ar = null;
            }
        }
    };

}
