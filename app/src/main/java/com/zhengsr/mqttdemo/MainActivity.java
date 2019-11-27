package com.zhengsr.mqttdemo;



import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;



import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    /**
     * 使用自己的服务器测试
     */
    private static final String HOST = "tcp://192.168.49.41:61613";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";
    private static final String SUBSCRIBE_TOPIC = "dev/status";    //订阅主题
    private static final String PUBLISH_TOPIC = "dev/push";       //发布主题
    private static final String CLIENT_ID = "ANDROID_ID"; //客户端ID,避免与 MQTT.fx 冲突
    private MqttAndroidClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMQTT();
    }

    private void initMQTT() {
        /**
         * 新建一个客户端，HOST 为 MQTT 地址，CLIENT_ID 为唯一标识
         */
        mClient = new MqttAndroidClient(this, HOST, CLIENT_ID);
        mClient.setCallback(new MQTTMessageListener());

        /**
         * 通过 options 配置客户端的信息，比如账号，密码，和最后的遗嘱等
         */
        MqttConnectOptions options = new MqttConnectOptions();

        //清除缓存
        options.setCleanSession(true);
        //超时
        options.setConnectionTimeout(10);
        //心跳包
        options.setKeepAliveInterval(20);
        //用户名和密码
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());


        /**
         * 设置最后的遗嘱，该方式可以让设备在掉线时，发送状态给服务器
         */
        String topic = PUBLISH_TOPIC;
        String msg = "disconnect";
        /**
         * qos 表示传递的服务质量，它的参数有 0,1,2三种如下：
         * 0：至多一次，消息根据底层因特网协议网络尽最大努力进行传递。 可能会丢失消息
         * 1：至少一次，保证消息抵达，但可能会出现重复
         * 2：刚好一次，确保只收到一次消息
         * retained：是否在服务器保留断开连接后的最后一条消息
         */

        options.setWill(topic,msg.getBytes(),2,false);
        if (!mClient.isConnected()) {
            try {
                //连接服务器和监听消息
                mClient.connect(options, null, IMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }



    }


    /**
     * 检测到 MQTT 是否连接服务器
     */
    private IMqttActionListener IMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            //当连接上服务器，则可以订阅哪些主题了,这里也用 dev/status 这个主题
            Log.d(TAG, "zsr - 连接成功 ");
            try {
                // 订阅Topic话题,然后用 mqtt.fx 发消息过来
                mClient.subscribe(SUBSCRIBE_TOPIC,2);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            // 连接失败，重连
            Log.d(TAG, "zsr - onFailure: "+arg1.getMessage());
        }
    };



    /**
     * 监听是否收到信的消息
     */
    class MQTTMessageListener implements MqttCallback{

        @Override
        public void connectionLost(Throwable cause) {
            //连接失败
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //新信息到达
            Log.d(TAG, "zsr - 收到新消息了: "+message);
            //当收到信息之后，我们回一条消息给 mqtt.fx
            publish(PUBLISH_TOPIC,"msg from android");
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //发送成功
        }
    }


    /**
     * 发布不同主题的消息
     * @param topic
     * @param msg
     */
    public void publish(String topic,String msg){
        try {
            if ( mClient!= null){
                mClient.publish(topic, msg.getBytes(),2, false);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (mClient != null) {
                mClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();

    }
}
