package com.example.lenovo.mygraduate8;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
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
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import org.bytedeco.opencv.global.opencv_core;
//import org.bytedeco.opencv.global.opencv_imgproc;
import com.dewarder.holdinglibrary.HoldingButtonLayout;
import com.dewarder.holdinglibrary.HoldingButtonLayoutListener;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import mbanje.kurt.fabbutton.FabButton;


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


public class MainActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener,HoldingButtonLayoutListener {
    private CameraBridgeViewBase cameraView;
    private CascadeClassifier classifier;
    private Mat mGray;
    private Mat mRgba;
    private Mat mImage;
    private int mAbsoluteFaceSize = 0;
    private boolean isFrontCamera;
    private String faceDir;

    private SQLiteDatabase sqLiteDatabase;
    private String userName;
    private EditText userText;
    private TextView textView;

    //声音录制控件
    private static final DateFormat mFormatter = new SimpleDateFormat("mm:ss:SS");
    private static final float SLIDE_TO_CANCEL_ALPHA_MULTIPLIER = 2.5f;
    private static final long TIME_INVALIDATION_FREQUENCY = 50L;
    private HoldingButtonLayout mHoldingButtonLayout;
    private TextView mTime;
    private TextView mExp;
    private TextView mInput;
    private View mSlideToCancel;
    private int mAnimationDuration;
    private ViewPropertyAnimator mTimeAnimator;
    private ViewPropertyAnimator mSlideToCancelAnimator;
    private long mStartTime;
    private Runnable mTimerRunnable;
    private AudioRecord ar = null;
    private String recordPath = null;

    private static final int SAMPLE_RATE = 44100;//Hz，采样频率
    //    private static final double FREQUENCY = 500; //Hz，标准频率（这里分析的是500Hz）
    // private static final double RESOLUTION = 10; //Hz，误差
    private int bufferSize=0;

    private TextView textUser;
    private TextView textHelp;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        Button buttonReg = findViewById(R.id.btnReg);
        buttonReg.setOnClickListener(this);
        faceDir = getExternalFilesDir(null)+"/faces";
        File fdir = new File(faceDir);
        if(!fdir.exists())
        {
            fdir.mkdir();
        }
        userText = findViewById(R.id.userID);
        textView = findViewById(R.id.textAllUser);
        DatabaseHelper databaseHelper = new DatabaseHelper(MainActivity.this,"userInfo");
        sqLiteDatabase = databaseHelper.getWritableDatabase();
        updateUserList();

        textUser = findViewById(R.id.textUser);
        textUser.setText(getClickabelSpanAdmin());
        textUser.setMovementMethod(LinkMovementMethod.getInstance());

        mHoldingButtonLayout = findViewById(R.id.input_holder);
        mHoldingButtonLayout.addListener(this);
        mTime = findViewById(R.id.time);

        mExp = findViewById(R.id.explain);
        mExp.setText("请录音");
        mExp.setVisibility(View.VISIBLE);
        mTime.setVisibility(View.INVISIBLE);
        mInput = findViewById(R.id.input);
        mSlideToCancel = findViewById(R.id.slide_to_cancel);
        mAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        textHelp = findViewById(R.id.textViewHelp);
        textHelp.setText(getClickableSpan());
        textHelp.setMovementMethod(LinkMovementMethod.getInstance());
    }
    //使用说明
    private SpannableString getClickableSpan()
    {
        SpannableString spStr = new SpannableString("使用说明");
        spStr.setSpan(new UnderlineSpan(),0,4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spStr.setSpan(new ForegroundColorSpan(Color.RED),0,4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spStr.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                showNormalDialog();
            }

        },0,4,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spStr;
    }
    private void showNormalDialog(){
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        //normalDialog.setIcon(R.drawable.icon_dialog);
        normalDialog.setTitle("使用说明");
        normalDialog.setMessage("使用者请依次按照以下步骤进行注册操作：\n" +
                "1.首先点击右下角话筒，长按录制音频,在5s内读出“芝麻开门”，可左右滑动," +
                "左滑按钮变红时松手则取消录制，右滑按钮变蓝时松手则录制完成。\n \n" +
                "2.然后，输入用户名，长度不可超过10个字 \n\n"+
                "3.之后，将面部正对相机，显示清晰的五官，在面部被检测出时点击注册按钮\n \n" +
                "4.系统会在2s内给出响应\n \n" +
                "注：已注册用户可多次注册；未注册用户不能打开门锁。");
        normalDialog.setPositiveButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do

                    }
                });
        // 显示
        normalDialog.show();
    }

    //用户界面跳转
    private SpannableString getClickabelSpanAdmin()
    {
        SpannableString spStr = new SpannableString("点此进入用户界面");
        spStr.setSpan(new UnderlineSpan(),0,8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spStr.setSpan(new ForegroundColorSpan(Color.RED),0,8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spStr.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,RMainActivity.class);
                startActivity(intent);
                finish();
            }

        },0,8,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spStr;
    }

    private void updateUserList()
    {
        final Handler handler = new Handler()
        {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        searchUser();
                        break;
                }
                super.handleMessage(msg);
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
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
            case R.id.btnReg:
                userName = userText.getText().toString();
                Log.i("userName",userName);
                if(userName.length()==0)
                {
                    Toast.makeText(this,"请输入用户名",Toast.LENGTH_LONG).show();
                }
                else if(recordPath==null)
                {
                    Toast.makeText(this,"请录音",Toast.LENGTH_LONG).show();
                }
                else if(mRgba!=null)
                {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String filepath = faceDir+File.separator+"IMG_" + timeStamp + ".jpg";
                    Imgcodecs.imwrite(filepath,mImage);
                    Toast.makeText(this,userName+"注册成功",Toast.LENGTH_SHORT).show();
                    insertUser(userName,filepath,recordPath);
                    mExp.setText("请录音");
                    recordPath = null;
                    userText.setText("");
                    showNewDialog(filepath);
                }
                else
                {
                    Toast.makeText(this,"人脸获取失败，请重试",Toast.LENGTH_SHORT).show();
                }

                break;
            default:
        }
    }
    private void showNewDialog(String filep)
    {
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        final View successView = getLayoutInflater().inflate(R.layout.activity_reg_success,null);
        alertDialog.setView(successView);
        final Button btn_success = successView.findViewById(R.id.btn_ok);
        final ImageView imageView = successView.findViewById(R.id.imageViewReg);
        final TextView textViewSuccess = successView.findViewById(R.id.textView_Reg_info);
        btn_success.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.cancel();
            }
        });

        imageView.setImageBitmap(BitmapFactory.decodeFile(filep));
        textViewSuccess.setText("恭喜"+userName+",注册成功");
        alertDialog.show();
    }


    private void insertUser(String username,String facePath,String voicePath)
    {
        ContentValues values = new ContentValues();
        values.put("name",username);
        values.put("facePath",facePath);
        values.put("voicePath",voicePath);
        sqLiteDatabase.insert("user",null,values);
    }

    private void searchUser()
    {
        Cursor cursor = sqLiteDatabase.query("user", new String[]{"name"}, null, null, null, null, null);
        //利用游标遍历所有数据对象
        //为了显示全部，把所有对象连接起来，放到TextView中
        String textview_data = "";
        while(cursor.moveToNext()){
            String name = cursor.getString(cursor.getColumnIndex("name"));
            textview_data = textview_data + "\n" + name;
        }
        if(textview_data.length()!=0)
        {
            textView.setText(textview_data);
        }
        else
        {
            textView.setText("无");
        }

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
        Size size = mGray.size();
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
            Imgproc.resize(sub,mImage,size);
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
    @Override
    public void onBeforeExpand() {
        cancelAllAnimations();

        mSlideToCancel.setTranslationX(0f);
        mSlideToCancel.setAlpha(0f);
        mSlideToCancel.setVisibility(View.VISIBLE);
        mSlideToCancelAnimator = mSlideToCancel.animate().alpha(1f).setDuration(mAnimationDuration);
        mSlideToCancelAnimator.start();

        mInput.setVisibility(View.INVISIBLE);
        mTime.setTranslationY(mTime.getHeight());
        mTime.setAlpha(0f);
        mExp.setVisibility(View.INVISIBLE);
        mTime.setVisibility(View.VISIBLE);
        mTime.setText("00:00:00");
        mTimeAnimator = mTime.animate().translationY(0f).alpha(1f).setDuration(mAnimationDuration);
        mTimeAnimator.start();
    }

    @Override
    public void onExpand() {
        mStartTime = System.currentTimeMillis();
        startRecord();
        invalidateTimer();
    }

    @Override
    public void onBeforeCollapse() {
        cancelAllAnimations();

        mSlideToCancelAnimator = mSlideToCancel.animate().alpha(0f).setDuration(mAnimationDuration);
        mSlideToCancelAnimator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSlideToCancel.setVisibility(View.INVISIBLE);
                mSlideToCancelAnimator.setListener(null);
            }
        });
        mSlideToCancelAnimator.start();

        mInput.setVisibility(View.VISIBLE);

        mTimeAnimator = mTime.animate().translationY(mTime.getHeight()).alpha(0f).setDuration(mAnimationDuration);
        mTimeAnimator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mTime.setVisibility(View.INVISIBLE);
                mExp.setVisibility(View.VISIBLE);
                mTimeAnimator.setListener(null);
            }
        });
        mTimeAnimator.start();
    }

    @Override
    public void onCollapse(boolean isCancel) {
        stopTimer();
        stopRecord();
        cancelAllAnimations();
        mExp.setVisibility(View.VISIBLE);
        File soundF = new File(recordPath);
        if (isCancel) {
            mExp.setText("请录音");
            Toast.makeText(this, "Action canceled! Time " + getFormattedTime(), Toast.LENGTH_SHORT).show();
            if(soundF.exists()) {
                soundF.delete();
            }
            recordPath = null;
        } else {
            mExp.setText("录音完成");
            Toast.makeText(this, "Action submitted! Time " + getFormattedTime(), Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "Action submitted! Time " + timeDur, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onOffsetChanged(float offset, boolean isCancel) {
        mSlideToCancel.setTranslationX(-mHoldingButtonLayout.getWidth() * offset);
        mSlideToCancel.setAlpha(1 - SLIDE_TO_CANCEL_ALPHA_MULTIPLIER * offset);
    }

    private void invalidateTimer() {
        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTime.setText(getFormattedTime());
                invalidateTimer();
            }
        };

        mTime.postDelayed(mTimerRunnable, TIME_INVALIDATION_FREQUENCY);
    }

    private void stopTimer() {
        if (mTimerRunnable != null) {
            mTime.getHandler().removeCallbacks(mTimerRunnable);
        }
    }

    private void cancelAllAnimations() {

        if (mSlideToCancelAnimator != null) {
            mSlideToCancelAnimator.cancel();
        }

        if (mTimeAnimator != null) {
            mTimeAnimator.cancel();
        }
    }

    private String getFormattedTime() {
        return mFormatter.format(new Date(System.currentTimeMillis() - mStartTime));
    }

}
