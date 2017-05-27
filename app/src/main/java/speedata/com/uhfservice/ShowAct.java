package speedata.com.uhfservice;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;

/**
 * Created by 张明_ on 2017/5/27.
 */

public class ShowAct extends AppCompatActivity  {
    private TextView tv_ip;
    private ToggleButton btn_serviceStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_show);
        initView();
    }


    private void initView() {
        tv_ip = (TextView) findViewById(R.id.tv_ip);
        btn_serviceStatus = (ToggleButton) findViewById(R.id.btn_serviceStatus);
        btn_serviceStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            private Intent intent;
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    intent = new Intent(ShowAct.this, UHFService.class);
                    startService(intent);
                }else {
                    stopService(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tv_ip.setText("本机IP：" + getLocalIpAddress());

        boolean serviceWork = isServiceWork(this, "speedata.com.uhfservice.UHFService");
        btn_serviceStatus.setChecked(serviceWork);
        Toast.makeText(this, serviceWork + "", Toast.LENGTH_SHORT).show();
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

}
