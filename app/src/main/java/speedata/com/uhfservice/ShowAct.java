package speedata.com.uhfservice;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.speedata.libuhf.R2K;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.List;

/**
 * Created by 张明_ on 2017/5/27.
 */

public class ShowAct extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_ip;
    private ToggleButton btn_serviceStatus;
    private TextView tv_status;
    private ToggleButton btn_changeStatus;
    private EditText tv_tcp_status;
    private EditText tv_tcp_receive;
    private EditText tv_tcp_send;
    private TextView tv_config;
    private TextView tv_uhf_status;
    private MyReceiver myReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_show);
        initView();
        EventBus.getDefault().register(this);

        //初始化超高频
        R2K r2K=new R2K(this);
        boolean serviceWork = isServiceWork(this, "speedata.com.uhfservice.UHFService");
        int antenna_power = 0;
        if (serviceWork) {
            antenna_power = r2K.get_antenna_power();
        } else {
            r2K.OpenDev();
            SystemClock.sleep(200);
            antenna_power = r2K.get_antenna_power();
            r2K.CloseDev();
        }
        if (antenna_power >= 10 && antenna_power <= 30) {
            tv_uhf_status.setText("正常");
            tv_uhf_status.setTextColor(getResources().getColor(R.color.green));
        }else {
            tv_uhf_status.setText("异常");
            tv_uhf_status.setTextColor(getResources().getColor(R.color.red));
        }

        if (myReceiver!=null){
            unregisterReceiver(myReceiver);
        }
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.geo.warn.msg");
        registerReceiver(myReceiver, intentFilter);
    }


    private void initView() {
        tv_ip = (TextView) findViewById(R.id.tv_ip);
        btn_serviceStatus = (ToggleButton) findViewById(R.id.btn_serviceStatus);
        btn_serviceStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private Intent intent;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    intent = new Intent(ShowAct.this, UHFService.class);
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });
        tv_status = (TextView) findViewById(R.id.tv_status);

        btn_changeStatus = (ToggleButton) findViewById(R.id.btn_changeStatus);
        btn_changeStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent intent = new Intent();
                    intent.setAction("com.geo.warn.msg");
                    intent.putExtra("status", "high");
                    sendBroadcast(intent);
                } else {
                    Intent intent = new Intent();
                    intent.setAction("com.geo.warn.msg");
                    intent.putExtra("status", "low");
                    sendBroadcast(intent);
                }
            }
        });
        tv_tcp_status = (EditText) findViewById(R.id.tv_tcp_status);
        tv_tcp_receive = (EditText) findViewById(R.id.tv_tcp_receive);
        tv_tcp_send = (EditText) findViewById(R.id.tv_tcp_send);
        tv_config = (TextView) findViewById(R.id.tv_config);
        tv_uhf_status = (TextView) findViewById(R.id.tv_uhf_status);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MsgEvent mEvent) {
        String type = mEvent.getType();
        String msg = (String) mEvent.getMsg();
        if ("TCPConnect".equals(type)) {
            tv_tcp_status.append(msg);
        } else if ("status".equals(type)) {
            tv_status.setText(msg);
        } else if ("tcp_receiver".equals(type)) {
            tv_tcp_receive.append(msg + "\n");
        } else if ("tcp_send".equals(type)) {
            tv_tcp_send.append(msg);
        } else if ("config".equals(type)) {
            tv_config.setText(msg);
        }
    }

    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getExtras().getString("status");
            if (status != null) {
                EventBus.getDefault().post(new MsgEvent("status", status));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tv_ip.setText("本机IP：" + getLocalIpAddress());

        boolean serviceWork = isServiceWork(this, "speedata.com.uhfservice.UHFService");
        btn_serviceStatus.setChecked(serviceWork);

        ReadINIThread readINIThread = new ReadINIThread();
        readINIThread.start();
    }

    private class ReadINIThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                IniReader iniReader = new IniReader("/storage/emulated/0/config.txt");
                String AntPower = iniReader.getValue("StatusSet", "AntPower");
                String TagMaxCnt = iniReader.getValue("StatusSet", "TagMaxCnt");
                String RSSIFilter = iniReader.getValue("StatusSet", "RSSIFilter");
                String ReadCntFilter = iniReader.getValue("StatusSet", "ReadCntFilter");
                String result = "AntPower=" + AntPower + "\nTagMaxCnt=" + TagMaxCnt + "\nRSSIFilter=" + RSSIFilter
                        + "\nReadCntFilter=" + ReadCntFilter;
                EventBus.getDefault().post(new MsgEvent("config", result));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(myReceiver);
    }

    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext
     * @param serviceName 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    /**
     * 获取WIFI下ip地址
     */
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }
}
