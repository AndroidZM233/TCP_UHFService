package speedata.com.uhfservice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.Tag_Data;
import com.speedata.libuhf.UHFManager;
import com.speedata.libutils.SharedXmlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by 张明_ on 2017/5/12.
 */

public class UHFService extends Service {
    private ServerSocket serverSocket;//创建ServerSocket对象
    private Socket clicksSocket;//连接通道，创建Socket对象
    private InputStream inputstream;//创建输入数据流
    private OutputStream outputStream;//创建输出数据流
    private IUHFService uhfService;
    private SharedXmlUtil sharedXmlUtil;
    private String result = "";
    private long currentTimeMillis;
    public final String TAG = "UHFNotify";
    private List<ReadData> firm = new ArrayList<ReadData>();
    int seq = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedXmlUtil = SharedXmlUtil.getInstance(UHFService.this, "UHFService");
        //初始化超高频
        uhfService = UHFManager.getUHFService(UHFService.this);
        //打开uhf上电
        uhfService.OpenDev();

        //启动服务器监听线程
        ServerSocketThread mServerSocketThread = new ServerSocketThread();
        mServerSocketThread.start();

    }


    /**
     * 服务器监听线程
     */
    private class ServerSocketThread extends Thread {

        public void run()//重写Thread的run方法
        {
            try {
                serverSocket = new ServerSocket(6117);//监听port端口，这个程序的通信端口就是port了
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    //监听连接 ，如果无连接就会处于阻塞状态，一直在这等着
                    clicksSocket = serverSocket.accept();
                    inputstream = clicksSocket.getInputStream();
                    //启动接收线程
                    ReceiveThread mReceiveThread = new ReceiveThread();
                    mReceiveThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 接收线程
     */
    private class ReceiveThread extends Thread//继承Thread
    {
        public void run()//重写run方法
        {
            while (true) {
                try {
                    final byte[] buf = new byte[1024];
                    final int len = inputstream.read(buf);
                    String receiveStr = new String(buf, 0, len);
                    String[] split = receiveStr.split(":");
                    switch (split[0]) {
                        //读标签R（Read）
                        case "R":
                        case "r":
                            readCard();
                            break;
                        //设置开关天线S(Set)
                        case "S":
                        case "s":
                            break;
                        //设置天线功率P(Power)
                        case "P":
                        case "p":
                            setP(split[1]);
                            break;
                        //设置TAG数目和过滤器C(Continuous parameter)
                        case "C":
                        case "c":
                            setC(split[1]);
                            break;
                        //查询状态I（Inquire）
                        case "I":
                        case "i":
                            getI();
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //读标签R（Read）
    private void readCard() {
        currentTimeMillis = System.currentTimeMillis();
        //开始盘点
        uhfService.inventory_start(handler);

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    long nowTime = System.currentTimeMillis();
                    ArrayList<Tag_Data> ks = (ArrayList<Tag_Data>) msg.obj;
                    String tmp[] = new String[ks.size()];
                    for (int i = 0; i < ks.size(); i++) {
                        byte[] nq = ks.get(i).epc;
                        if (nq != null) {
                            tmp[i] = b2hexs(nq, nq.length);
                        }
                    }
                    int i, j;
                    for (i = 0; i < tmp.length; i++) {
                        for (j = 0; j < firm.size(); j++) {
                            if (tmp[i].equals(firm.get(j).getEPC())) {
                                int count = firm.get(j).getCount();
                                int countResult = count + 1;
                                firm.get(j).setCount(countResult);
                                int rssi = firm.get(j).getRSSI();
                                int rssi1 = Integer.parseInt(ks.get(i).rssi);
                                firm.get(j).setRSSI((rssi + rssi1) / 2);
                                break;
                            }
                        }
                        if (j == firm.size()) {
                            int rssi = Integer.parseInt(ks.get(i).rssi);
                            firm.add(new ReadData(tmp[i], rssi, 1));
                        }
                    }

                    if (nowTime - currentTimeMillis > 750) {
                        sendN();
                    }
                    break;
            }

        }
    };

    //通知TAG读取结果N(Notify)
    private void sendN() {
        uhfService.inventory_stop();
        if (seq<255){
            seq++;
        }else {
            seq=0;
        }
        String timeStyle2 = getTimeStyle2();
        String resultStr = "";
        if (firm.size() == 0) {
            resultStr = "N:" + TAG+"," + seq+",0," + timeStyle2 + "\r" + "\n";
            sendMsg(resultStr);
            return;
        }
        Collections.sort(firm, new Comparator<ReadData>() {
            @Override
            public int compare(ReadData o1, ReadData o2) {
                int o1RSSI = o1.getRSSI();
                int o2RSSI = o2.getRSSI();
                if (o1RSSI == o2RSSI) {
                    return 0;
                } else if (o1RSSI < o2RSSI) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        int bianli = 3;
        String tagMaxCnt = sharedXmlUtil.read("tagMaxCnt", "");
        String rssiFilter = sharedXmlUtil.read("rssiFilter", "");
        String readCntFilter = sharedXmlUtil.read("readCntFilter", "");
        if (!TextUtils.isEmpty(tagMaxCnt)) {
            bianli = Integer.parseInt(tagMaxCnt);
        }
        if (!TextUtils.isEmpty(rssiFilter)) {
            int rssi = Integer.parseInt(rssiFilter);
            for (int k = 0; k < firm.size(); k++) {
                if (firm.get(k).getRSSI() < rssi) {
                    firm.remove(k);
                    k--;
                }
            }
        }
        if (!TextUtils.isEmpty(readCntFilter)) {
            int readCnt = Integer.parseInt(readCntFilter);
            for (int k = 0; k < firm.size(); k++) {
                if (firm.get(k).getCount() < readCnt) {
                    firm.remove(k);
                    k--;
                }
            }
        }

        if (firm.size()<bianli){
            bianli=firm.size();
        }
        resultStr = "N:" + TAG+"," + seq+","+bianli+",";
        StringBuffer resultBuff=new StringBuffer();
        resultBuff.append(resultStr);
        for (int k = 0; k < bianli; k++) {
            resultBuff.append(firm.get(k).getEPC()+","
                    +firm.get(k).getRSSI()+","
                    +firm.get(k).getCount()+",");
        }
        resultBuff.append(timeStyle2+"\r"+"\n");
        sendMsg(String.valueOf(resultBuff));
    }

    //查询状态I（Inquire）
    //示例
    //输入：I:<CR><LF>
    //返回：1,AntPower =30,TagMaxCnt=3，RSSIFilter=-65,CntFilter=3<CR><LF>
    private void getI() {
        String AntPower = String.valueOf(uhfService.get_antenna_power());
        String tagMaxCnt = sharedXmlUtil.read("tagMaxCnt", "");
        String rssiFilter = sharedXmlUtil.read("rssiFilter", "");
        String readCntFilter = sharedXmlUtil.read("readCntFilter", "");
        if (AntPower.equals("-1")) {
            result = "0,AntPower=" + AntPower + ",TagMaxCnt=" + tagMaxCnt + ",RSSIFilter=" + rssiFilter
                    + ",CntFilter=" + readCntFilter + "\r" + "\n";
        } else {
            result = "1,AntPower=" + AntPower + ",TagMaxCnt=" + tagMaxCnt + ",RSSIFilter=" + rssiFilter
                    + ",CntFilter=" + readCntFilter + "\r" + "\n";
        }
        sendMsg(result);
    }


    //设置天线功率P(Power)
    private void setP(String s) {
        String str = s.replace("\r", "").replace("\n", "").replace("<CR><LF>","");
        int i = uhfService.set_antenna_power(Integer.parseInt(str));
        if (i == 0) {
            result = "1:success" + "\r" + "\n";
        } else {
            result = "0:failed" + "\r" + "\n";
        }
        sendMsg(result);
    }


    //设置TAG数目和过滤器C(Continuous parameter)
    public void setC(String s) {
        try {
            String[] splitResult = s
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("<CR><LF>","")
                    .split(",");

            sharedXmlUtil.write("tagMaxCnt", splitResult[0]);
            sharedXmlUtil.write("rssiFilter", splitResult[1]);
            sharedXmlUtil.write("readCntFilter", splitResult[2]);

            result = "1:success" + "\r" + "\n";
            sendMsg(result);
        } catch (Exception e) {
            e.printStackTrace();
            result = "0:failed" + "\r" + "\n";
            sendMsg(result);
        }
    }

    //发送信息
    private void sendMsg(String msg) {
        try {
            //获取输出流
            outputStream = clicksSocket.getOutputStream();
            //发送数据
            outputStream.write(msg.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String b2hexs(byte[] b, int length) {
        String ret = "";

        for (int i = 0; i < length; ++i) {
            String hex = Integer.toHexString(b[i] & 255);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }

            ret = ret + hex.toUpperCase();
        }

        return ret;
    }

    public static String getTimeStyle2() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        return str;
    }
}
