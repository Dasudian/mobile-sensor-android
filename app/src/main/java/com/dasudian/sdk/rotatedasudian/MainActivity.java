package com.dasudian.sdk.rotatedasudian;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.dasudian.iot.sdk.*;
import java.util.logging.Formatter;
import java.util.logging.*;


public class MainActivity extends AppCompatActivity {
    private static final Logger LOGGER = Logger.getLogger(MainActivity.class.getSimpleName());
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {
        DF.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    private static class MyCallback extends ActionCallback {
        //打印发布消息（主题和内容）
        @Override
        public void onMessageReceived(String topic, byte[] payload) {
            LOGGER.info("onMessageReceived,topic=" + topic + ",payload=" + new String(payload));
        }
        //打印服务器连接状态成功或者失败
        @Override
        public void onConnectionStatusChanged(boolean isConnected) {
            LOGGER.info("current connect status = " + isConnected);
        }
    }

    private SensorManager sm;

    //Gyroscope 陀螺仪传感器
    private Sensor gyroscopeSensor;

    //Accelerometer 含重力的加速度传感器
    private Sensor accelerometerSensor;

    //从监听器中获取值
    float[] gyroscopeValues = new float[3];
    float[] accelerometerValues = new float[3];
    long gyrTime = 0;
    long accTime = 0;

    private static final String TAG = "sensor";

    private DataHubClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //启动SDK客户端
        startSDK();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //这里的这个是陀螺仪传感器
        gyroscopeSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //包含重力
        accelerometerSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //监听事件??  第三个参数为监听的值
        sm.registerListener(myListener, gyroscopeSensor, 100000);
        sm.registerListener(myListener, accelerometerSensor,100000);


        //更新显示数据的方法
    //    calculateOrientation();

    }

    //监听赋值
    final SensorEventListener myListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                accelerometerValues = sensorEvent.values;
                accTime = sensorEvent.timestamp;
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE ) {
                gyroscopeValues = sensorEvent.values;
                gyrTime = sensorEvent.timestamp;
                calculateOrientation();
            }

        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    public void onPause(){
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        sm.unregisterListener(myListener);
        super.onPause();
    }

    //发送数据
    private  void calculateOrientation() {
        String topic = "behavior";
        int qos = 1;
       //陀螺仪数据
        float axisX = gyroscopeValues[0];
        float axisY = gyroscopeValues[1];
        float axisZ = gyroscopeValues[2];

        float x1 = accelerometerValues[0];
        float y1 = accelerometerValues[1];
        float z1 = accelerometerValues[2];

        try {
            //拼接JSON
            String dataJson = "{\"x\":" + x1 +
                    ",\"y\":" + y1 +
                    ",\"z\":" + z1 +
                    ",\"gyro_rotation_x\":" + axisX +
                    ",\"gyro_rotation_y\":" + axisY +
                    ",\"gyro_rotation_z\":" + axisZ +
                    ",\"SENSORID\":\"Android\",\"stime\":" + gyrTime +
                    "}";

            //下面的是陀螺仪的数据 ,上面的是加速度的数据
            String dataJson2 = "{\"x\":" + x1 +
                    ",\"y\":" + y1 +
                    ",\"z\":" + z1 +
                    ",\"gyro_rotation_x\":" + axisX +
                    ",\"gyro_rotation_y\":" + axisY +
                    ",\"gyro_rotation_z\":" + axisZ +
                    ",\"sensorid\":\"android\"" +
                    ",\"type\":\"\"" +
                    "}";


            Message msg = new Message(dataJson2.getBytes());
            client.sendRequest(topic, msg, qos, 10, Constants.JSON);
            TextView textView = (TextView) findViewById(R.id.dasudian1);
            textView.setText("sent request success");
        } catch (Exception e) {
            TextView textView = (TextView) findViewById(R.id.dasudian1);
            textView.setText("sent request fail");
            e.printStackTrace();
            LOGGER.info(e.getMessage());
        }

        System.out.println("axisX = "+axisX + ",axisY = "+axisY + ",axisZ = " + axisZ);
        System.out.println( x1 + "----" + "------"+ y1 +"------"+ z1);

    }

    public  void startSDK(){
        try {
            FileHandler fileHandler = new FileHandler("/storage/emulated/0/dasudian/RotateException.txt",true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(new MyLogHander());
            LOGGER.addHandler(fileHandler);
        }
        catch(Exception e ){
            e.printStackTrace();
            LOGGER.info("FileHandler exception");
        }

        //服务器地址
        String server_url = "tcp://119.23.147.127:1883";
        //your instanceId
        String instanceId = "dsd_9HsIJEZF9c76Nh6mEy_A";
        //your instanceKey
        String instanceKey = "3438f4e7f7d5268e";

        //客户端设备类型
        String clientType = "clientType";
        //客户端ID
        String clientId = UUID.randomUUID().toString();


        try {
            //建立客户端
            client = new DataHubClient.Builder(instanceId, instanceKey, clientType, clientId)
                    .setServerURL(server_url)
                    .setDebug(true)
                    .setCallback(new MyCallback()).build();

        } catch (Exception e) {

            TextView textView = (TextView) findViewById(R.id.dasudian1);
            textView.setText("建立SDK客户端失败");
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }


    public static void backHomeFinishSelf(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(intent);
    }

}

class MyLogHander extends Formatter {
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Override
    public String format(LogRecord logRecord)
    {
        String dateFormat = DF.format(logRecord.getMillis());
        return dateFormat + " " + logRecord.getLevel()+":"+logRecord.getMessage()+"\n";
    }
}
