package cn.zhoujia.wificonnectiondemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.socks.library.KLog;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.button1)
    Button button1;
    @Bind(R.id.button2)
    Button button2;
    @Bind(R.id.button3)
    Button button3;
    @Bind(R.id.button4)
    Button button4;
    @Bind(R.id.content)
    TextView content;
    @Bind(R.id.reciver)
    TextView reciver;

    Activity activity = MainActivity.this;
    WifiAdmin mWifiAdmin;
    WifiApAdmin wifiAp;
    @Bind(R.id.button5)
    Button button5;
    @Bind(R.id.button6)
    Button button6;

    private WifiP2pInfo info;
    private FileServerAsyncTask mServerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

    }

    @OnClick({R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5, R.id.button6})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1:
                wifiAp = new WifiApAdmin(activity);
                wifiAp.startWifiAp(Constant.HOST_SPOT_SSID, Constant.HOST_SPOT_PASS_WORD);
                break;
            case R.id.button5:
                wifiAp.closeWifiAp(activity);
                openWifi();
                break;

            case R.id.button2:
                mWifiAdmin = new WifiAdmin(activity) {

                    @Override
                    public void myUnregisterReceiver(BroadcastReceiver receiver) {
                        // TODO Auto-generated method stub
                        unregisterReceiver(receiver);
                    }

                    @Override
                    public Intent myRegisterReceiver(BroadcastReceiver receiver, IntentFilter filter) {
                        // TODO Auto-generated method stub
                        registerReceiver(receiver, filter);
                        return null;
                    }

                    @Override
                    public void onNotifyWifiConnected() {
                        // TODO Auto-generated method stub
                        KLog.e("have connected success!");
                        KLog.e("###############################");
                    }

                    @Override
                    public void onNotifyWifiConnectFailed() {
                        // TODO Auto-generated method stub
                        KLog.e("have connected failed!");
                        KLog.e("###############################");
                    }
                };
                mWifiAdmin.openWifi();
                // 连的WIFI热点是用WPA方式保护
                mWifiAdmin.addNetwork(mWifiAdmin.createWifiInfo(Constant.HOST_SPOT_SSID, Constant.HOST_SPOT_PASS_WORD,
                        WifiAdmin.TYPE_WPA));

                break;
            case R.id.button3:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image");
                startActivityForResult(intent, 20);
                break;
            case R.id.button4:
                if (service != null) {
                    service.close();
                }
                service = new Receiver();
                service.start();

                break;
            case R.id.button6:
                closeWifi();
                MyTimerCheck timerCheck = new MyTimerCheck() {

                    @Override
                    public void doTimerCheckWork() {
                        openWifi();
                    }

                    @Override
                    public void doTimeOutWork() {
                        this.exit();
                    }
                };
                timerCheck.start(15, 1000);

                break;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (service != null) {
            service.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /* 服务器 接收数据 */
    Receiver service;

    class Receiver extends Thread {
        public boolean flag = true;

        public void run() {
            if (flag) {
                mServerTask = new FileServerAsyncTask(MainActivity.this, reciver);
                mServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        public void close() {
            flag = false;
        }
    }

    /**
     * 将获取的int转为真正的ip地址,参考的网上的，修改了下
     */
    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    WifiManager mWifiManager;

    // 打开WIFI
    public void openWifi() {
        if (mWifiAdmin != null) {
            mWifiAdmin.openWifi();
            return;
        }
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        }
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }


    // 关闭WIFI
    public void closeWifi() {
        if (mWifiAdmin != null) {
            mWifiAdmin.closeWifi();
            return;
        }
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        }
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 20) {
            super.onActivityResult(requestCode, resultCode, data);
            WifiManager wifiManage = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            DhcpInfo info = wifiManage.getDhcpInfo();
            WifiInfo wifiinfo = wifiManage.getConnectionInfo();
            String ip = intToIp(wifiinfo.getIpAddress());
            String serverAddress = intToIp(info.serverAddress);

            KLog.e("ip:" + ip + "  serverAddress:" + serverAddress + "  " + info);

            Uri uri = data.getData();

            new Sender(serverAddress, uri).start();
        }
    }


    /* 客户端发送数据 */
    class Sender extends Thread {
        String serverIp;
        String message;

        Sender(String serverAddress, Uri uri) {
            super();
            serverIp = serverAddress;
            this.message = uri.toString();
        }

        public void run() {
            int length = 0;
            byte[] sendBytes = null;
            Socket socket = null;
            DataOutputStream dos = null;
            FileInputStream fis = null;

            try {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(serverIp, 8988),
                            10 * 1000);
                    dos = new DataOutputStream(socket.getOutputStream());
                    File file = new File(message);
                    fis = new FileInputStream(file);
                    sendBytes = new byte[1024];
                    while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                        dos.write(sendBytes, 0, length);
                        dos.flush();
                    }
                } finally {
                    if (dos != null)
                        dos.close();
                    if (fis != null)
                        fis.close();
                    if (socket != null)
                        socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
