package speedata.com.uhfservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

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
    public static final String TAG = "TAG";
    private List<ReadData> firm = new ArrayList<ReadData>();
    int seq = 0;
    private MyReceiver myReceiver;
    private ServerSocketThread mServerSocketThread;


    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ------------------------------");


//        File file=new File("/storage/emulated/0/config.ini");
//        if (!file.exists()){
//            IniFile file2 = new IniFile();
//            file2.set("Config", "AntPower", 30);
//            file2.set("Config", "TagMaxCnt", 3);
//            file2.set("Config", "RSSIFilter", -70);
//            file2.set("Config", "ReadCntFilter", 1);
//            file2.save(file);
//        }else {
//            IniFile iniFile = new IniFile(file);
//            Object AntPower = iniFile.get("Config", "AntPower");
//            Object TagMaxCnt = iniFile.get("Config", "TagMaxCnt");
//        }

        sharedXmlUtil = SharedXmlUtil.getInstance(UHFService.this, "UHFService");
        //初始化超高频
        sharedXmlUtil.write("modle", "r2k");
        uhfService = UHFManager.getUHFService(UHFService.this);
        uhfService.OpenDev();
        ReadINIThread readINIThread=new ReadINIThread();
        readINIThread.start();

        //启动服务器监听线程
        synchronized (this) {
            if (mServerSocketThread == null) {
                mServerSocketThread = new ServerSocketThread();
                mServerSocketThread.start();
            }
        }

        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.geo.warn.msg");
        registerReceiver(myReceiver, intentFilter);

        //        uhfService.setListener(new IUHFService.Listener() {
        //            @Override
        //            public void update(Tag_Data var1) {
        //                Log.d(TAG, "setListener: ------------------------");
        //                long nowTime = System.currentTimeMillis();
        //                byte[] nq = var1.epc;
        //                String epcStr = "";
        //                if (nq != null) {
        //                    epcStr = b2hexs(nq, nq.length);
        //                }
        //                Log.d(TAG, "setListener: epcStr" + epcStr);
        //                int j;
        //                if (firm.size() == 0) {
        //                    int rssi = Integer.parseInt(var1.rssi);
        //                    firm.add(new ReadData(epcStr, rssi, 1));
        //                }
        //                Log.d(TAG, "setListener: firm.size" + firm.size());
        //                for (j = 0; j < firm.size(); j++) {
        //                    if (epcStr.equals(firm.get(j).getEPC())) {
        //                        int count = firm.get(j).getCount();
        //                        int countResult = count + 1;
        //                        firm.get(j).setCount(countResult);
        //                        int rssi = firm.get(j).getRSSI();
        //                        int rssi1 = Integer.parseInt(var1.rssi);
        //                        firm.get(j).setRSSI((rssi + rssi1) / 2);
        //                        break;
        //                    }
        //                    if (j == firm.size() - 1) {
        //                        int rssi = Integer.parseInt(var1.rssi);
        //                        firm.add(new ReadData(epcStr, rssi, 1));
        //                    }
        //                }
        //
        //                if (nowTime - currentTimeMillis > 750) {
        //                    Log.d(TAG, "setListener: ------------------------sendN");
        //                    sendN();
        //                }
        //            }
        //        });
        return super.onStartCommand(intent, flags, startId);
    }

    //    @Override
    //    public void onCreate() {
    //        super.onCreate();
    //        sharedXmlUtil = SharedXmlUtil.getInstance(UHFService.this, "UHFService");
    //        //初始化超高频
    //        uhfService = UHFManager.getUHFService(UHFService.this);
    //
    ////        uhfService.OpenDev();
    //        //启动服务器监听线程
    //        ServerSocketThread mServerSocketThread = new ServerSocketThread();
    //        mServerSocketThread.start();
    //
    //        myReceiver = new MyReceiver();
    //        IntentFilter intentFilter = new IntentFilter();
    //        intentFilter.addAction("com.geo.warn.msg");
    //        registerReceiver(myReceiver, intentFilter);
    //
    //        uhfService.setListener(new IUHFService.Listener() {
    //            @Override
    //            public void update(Tag_Data var1) {
    //                long nowTime = System.currentTimeMillis();
    //                byte[] nq = var1.epc;
    //                String epcStr = "";
    //                if (nq != null) {
    //                    epcStr = b2hexs(nq, nq.length);
    //                }
    //                int j;
    //                if (firm.size() == 0) {
    //                    int rssi = Integer.parseInt(var1.rssi);
    //                    firm.add(new ReadData(epcStr, rssi, 1));
    //                }
    //                for (j = 0; j < firm.size(); j++) {
    //                    if (epcStr.equals(firm.get(j).getEPC())) {
    //                        int count = firm.get(j).getCount();
    //                        int countResult = count + 1;
    //                        firm.get(j).setCount(countResult);
    //                        int rssi = firm.get(j).getRSSI();
    //                        int rssi1 = Integer.parseInt(var1.rssi);
    //                        firm.get(j).setRSSI((rssi + rssi1) / 2);
    //                        break;
    //                    }
    //                    if (j == firm.size() - 1) {
    //                        int rssi = Integer.parseInt(var1.rssi);
    //                        firm.add(new ReadData(epcStr, rssi, 1));
    //                    }
    //                }
    //
    //                if (nowTime - currentTimeMillis > 750) {
    //                    sendN();
    //                }
    //            }
    //        });
    //    }

    private class ReadINIThread extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                IniReader iniReader = new IniReader("/storage/emulated/0/config.ini");
                String AntPower = iniReader.getValue("StatusSet", "AntPower");
                String TagMaxCnt = iniReader.getValue("StatusSet", "TagMaxCnt");
                String RSSIFilter = iniReader.getValue("StatusSet", "RSSIFilter");
                String ReadCntFilter = iniReader.getValue("StatusSet", "ReadCntFilter");
                sharedXmlUtil.write("antPower", AntPower);
                sharedXmlUtil.write("tagMaxCnt", TagMaxCnt);
                sharedXmlUtil.write("rssiFilter", RSSIFilter);
                sharedXmlUtil.write("readCntFilter", ReadCntFilter);
                uhfService.set_antenna_power(Integer.parseInt(AntPower));
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ReadINIThread: IOException");
            }
        }
    }

    private int broadcastCount=0;
    private int instructCount=0;
    private Handler broadcastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 10:
                    if (isend) {
                        isend = false;
                        broadcastCount=0;
                        Log.d(TAG, "broadcastHandler: ------------------------");
//                        uhfService.OpenDev();
                        readCard();
                    } else {
//                        uhfService.OpenDev();
//                        readCard();
                        result = "0:Operating too fast" + "\r" + "\n";
                        sendMsg(result);
                        broadcastCount++;
                        if (broadcastCount>=2){
                            uhfService.inventory_stop();
                            isend=true;
                        }
                    }

                    break;
            }

        }
    };

    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ------------------------");
            String status = intent.getExtras().getString("status");
            if (status != null) {
                Log.d(TAG, "onReceive: status is:" + status);
            } else {
                Log.d(TAG, "onReceive: status   is   null");
            }
            if ("high".equals(status)) {
                broadcastHandler.sendMessage(broadcastHandler.obtainMessage(10));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: --------------------------");
        unregisterReceiver(myReceiver);
//        uhfService.CloseDev();
        firm = null;
        try {
            serverSocket.close();
            if (clicksSocket != null) {
                clicksSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    ReceiveThread mReceiveThread;

    /**
     * 服务器监听线程
     */
    private class ServerSocketThread extends Thread {

        public void run()//重写Thread的run方法
        {
            try {
                if (serverSocket == null) {
                    serverSocket = new ServerSocket(6117);//监听port端口，这个程序的通信端口就是port了
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!interrupted()) {
                try {
                    SystemClock.sleep(10);
                    //监听连接 ，如果无连接就会处于阻塞状态，一直在这等着
                    clicksSocket = serverSocket.accept();
                    inputstream = clicksSocket.getInputStream();
                    //启动接收线程
                    Log.d(TAG, "服务器监听线程: ");
                    if (mReceiveThread == null) {
                        mReceiveThread = new ReceiveThread();
                        mReceiveThread.start();
                        Log.d(TAG, "mReceiveThreadStart ");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    result = "0:IOException" + "\r" + "\n";
                    sendMsg(result);
                }
            }
        }
    }


    byte[] buf = new byte[1024];

    /**
     * 接收线程
     */
    private class ReceiveThread extends Thread//继承Thread
    {
        public void run()//重写run方法
        {
            while (!isInterrupted()) {
                try {
                    SystemClock.sleep(10);
                    Log.d(TAG, "接收线程: new byte");
                    final int len = inputstream.read(buf);
                    if (len < 0) {
                        mReceiveThread.interrupt();
                        mReceiveThread = null;
                        return;
                    }
                    String receiveStr = new String(buf, 0, len);
                    Log.d(TAG, "接收线程: " + receiveStr);
                    String[] split = receiveStr.split(":");
                    //打开uhf上电
//                    uhfService.OpenDev();
                    switch (split[0]) {
                        //读标签R（Read）
                        case "R":
                        case "r":
                            Log.d(TAG, "run: ----------------------isend=false");
                            if (isend) {
                                Log.d(TAG, "run: ---------------------------");
                                instructCount=0;
                                isend = false;
                                readCard();
                            } else {
//                                uhfService.inventory_stop();
//                                readCard();
                                result = "0:Operating too fast" + "\r" + "\n";
                                sendMsg(result);
                                instructCount++;
                                if (instructCount>=2){
                                    uhfService.inventory_stop();
                                    isend=true;
                                }
                            }
                            break;
                        //设置开关天线S(Set)
                        case "S":
                        case "s":
                            setS();
                            break;
                        //设置天线功率P(Power)
                        case "P":
                        case "p":
                            if (split.length < 2) {
                                result = "0:Instruction format is wrong" + "\r" + "\n";
                                sendMsg(result);
                            } else {
                                setP(split[1]);
                            }
                            break;
                        //设置TAG数目和过滤器C(Continuous parameter)
                        case "C":
                        case "c":
                            if (split.length < 2) {
                                result = "0:Instruction format is wrong" + "\r" + "\n";
                                sendMsg(result);
                            } else {
                                setC(split[1]);
                            }

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

    //设置开关天线S(Set)
    private void setS() {
        result = "1:success" + "\r" + "\n";
        sendMsg(result);

//        uhfService.CloseDev();
    }

    private volatile boolean isend = true;

    //读标签R（Read）
    private void readCard() {
        currentTimeMillis = System.currentTimeMillis();

//        firm = new ArrayList<ReadData>();
        firm.clear();
        send = false;
        //开始盘点
        uhfService.inventoryStart(handler);

        //        uhfService.newInventoryStart();
        Log.d(TAG, "readCard: ------------------------");
    }

    private volatile boolean send = false;
    ArrayList<Tag_Data> ks = new ArrayList<>();
    @SuppressWarnings("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    Log.d(TAG, "handleMessage: -------------------------");
                    long nowTime = System.currentTimeMillis();
                    if (nowTime - currentTimeMillis > 750 && !send) {
                        Log.d(TAG, "handleMessage: sendN-----------------------");
                        send = true;
                        sendN();
                    } else {
                        Log.d(TAG, "handleMessage: NONOsendN-----------------------");
                        ks.clear();
                        ks.addAll((ArrayList<Tag_Data>) msg.obj);
                        if (ks.size() != 0) {
                            Log.d(TAG, "handleMessage: ks.size" + ks.size());
                            String tmp[] = new String[ks.size()];
                            try {
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
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(TAG, "handleMessage: setRSSI error");
                            }
                            tmp = null;
                        }
                    }
                    break;
            }

        }
    };

    //通知TAG读取结果N(Notify)
    private void sendN() {
        uhfService.inventory_stop();
        //        uhfService.newInventoryStop();
        try {
            if (seq < 255) {
                seq++;
            } else {
                seq = 0;
            }
            String timeStyle2 = getTimeStyle2();
            String resultStr = "";
            if (firm.size() == 0) {
                resultStr = "N:" + TAG + "," + seq + ",0," + timeStyle2 + "\r" + "\n";
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

            if (firm.size() < bianli) {
                bianli = firm.size();
            }
            resultStr = "N:" + TAG + "," + seq + "," + bianli + ",";
            StringBuffer resultBuff = new StringBuffer();
            resultBuff.append(resultStr);
            for (int k = 0; k < bianli; k++) {
                resultBuff.append(firm.get(k).getEPC() + ","
                        + firm.get(k).getRSSI() + ","
                        + firm.get(k).getCount() + ",");
            }
            resultBuff.append(timeStyle2 + "\r" + "\n");
            sendMsg(String.valueOf(resultBuff));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.d(TAG, "sendN: error");
        }

        isend = true;
        Log.d(TAG, "sendN: ————————————————————————————————————————————————");
//        uhfService.CloseDev();
    }

    //查询状态I（Inquire）
    //示例
    //输入：I:<CR><LF>
    //返回：1,AntPower =30,TagMaxCnt=3，RSSIFilter=-65,CntFilter=3<CR><LF>
    private void getI() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String AntPower = String.valueOf(uhfService.get_antenna_power());
        String tagMaxCnt = sharedXmlUtil.read("tagMaxCnt", "");
        String rssiFilter = sharedXmlUtil.read("rssiFilter", "");
        String readCntFilter = sharedXmlUtil.read("readCntFilter", "");
        if (AntPower.equals("-1")) {
            result = "0,AntPower=" + AntPower + ",TagMaxCnt=" + tagMaxCnt + ",RSSIFilter=" + rssiFilter
                    + ",ReadCntFilter=" + readCntFilter + "\r" + "\n";
        } else {
            result = "1,AntPower=" + AntPower + ",Ta" +
                    "gMaxCnt=" + tagMaxCnt + ",RSSIFilter=" + rssiFilter
                    + ",ReadCntFilter=" + readCntFilter + "\r" + "\n";
        }
        sendMsg(result);
//        uhfService.CloseDev();
    }


    //设置天线功率P(Power)
    private void setP(String s) {
        try {
            String str = s.replace("\r", "").replace("\n", "").replace("<CR><LF>", "");
            if (TextUtils.isEmpty(str)) {
                result = "0:Power can't be empty" + "\r" + "\n";
                sendMsg(result);
                uhfService.CloseDev();
                return;
            }
            int power = Integer.parseInt(str);
            if (power >= 10 && power <= 30) {
                int i = uhfService.set_antenna_power(power);
                if (i == 0) {
                    result = "1:AntPower=" + power + "\r" + "\n";
                } else {
                    result = "0:failed" + "\r" + "\n";
                }
                sendMsg(result);
            } else {
                result = "0:Power Range of 10-30" + "\r" + "\n";
                sendMsg(result);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            result = "0:NumberFormatException" + "\r" + "\n";
            sendMsg(result);
        }

        Log.d(TAG, "setP: ----------------------");
//        uhfService.CloseDev();
    }


    //设置TAG数目和过滤器C(Continuous parameter)
    public void setC(String s) {
        try {
            String[] splitResult = s
                    .replace("\r", "")
                    .replace("\n", "")
                    .replace("<CR><LF>", "")
                    .replace(" ", "")
                    .split(",");

            try {

                if (splitResult.length == 1) {
                    if (!TextUtils.isEmpty(splitResult[0])) {
                        Integer.parseInt(splitResult[0]);
                        sharedXmlUtil.write("tagMaxCnt", splitResult[0]);
                    }
                }

                if (splitResult.length == 2) {
                    if (!TextUtils.isEmpty(splitResult[0])) {
                        Integer.parseInt(splitResult[0]);
                        sharedXmlUtil.write("tagMaxCnt", splitResult[0]);
                    }
                    if (!TextUtils.isEmpty(splitResult[1])) {
                        Integer.parseInt(splitResult[1]);
                        sharedXmlUtil.write("rssiFilter", splitResult[1]);
                    }

                }

                if (splitResult.length == 3) {
                    if (!TextUtils.isEmpty(splitResult[0])) {
                        Integer.parseInt(splitResult[0]);
                        sharedXmlUtil.write("tagMaxCnt", splitResult[0]);
                    }
                    if (!TextUtils.isEmpty(splitResult[1])) {
                        Integer.parseInt(splitResult[1]);
                        sharedXmlUtil.write("rssiFilter", splitResult[1]);
                    }
                    if (!TextUtils.isEmpty(splitResult[2])) {
                        Integer.parseInt(splitResult[2]);
                        sharedXmlUtil.write("readCntFilter", splitResult[2]);
                    }

                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
                result = "0:NumberFormatException" + "\r" + "\n";
                sendMsg(result);
                return;
            }
            int anInt = 3;
            if (!TextUtils.isEmpty(splitResult[0])) {
                anInt = Integer.parseInt(splitResult[0]);
                if (anInt > 3 || anInt < 0) {
                    sharedXmlUtil.write("tagMaxCnt", "3");
                    result = "1:TagMaxCnt Range of 1-3 We help you set it to 3" + "\r" + "\n";
                    sendMsg(result);
                }
            }
            String tagMaxCnt = sharedXmlUtil.read("tagMaxCnt", "");
            String rssiFilter = sharedXmlUtil.read("rssiFilter", "");
            String readCntFilter = sharedXmlUtil.read("readCntFilter", "");
            result = "1:TagMaxCnt=" + tagMaxCnt + ",RSSIFilter=" + rssiFilter
                    + ",ReadCntFilter=" + readCntFilter + "\r" + "\n";
            sendMsg(result);


        } catch (Exception e) {
            e.printStackTrace();
            result = "0:failed" + "\r" + "\n";
            sendMsg(result);
        }

//        uhfService.CloseDev();
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
//            result = "0:An error occurred while trying to send a message" + "\r" + "\n";
//            sendMsg(result);
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
