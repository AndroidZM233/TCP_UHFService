package speedata.com.uhfservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.speedata.libuhf.IUHFService;
import com.speedata.libuhf.UHFManager;
import com.speedata.libutils.SharedXmlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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
    private String result="";

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
                        //通知TAG读取结果N(Notify)
                        case "N":
                            uhfService.inventory_start();
                            break;
                        //读标签R（Read）
                        case "R":
                            break;
                        //设置开关天线S(Set)
                        case "S":
                            break;
                        //设置天线功率P(Power)
                        case "P":
                            setP(split[1]);
                            break;
                        //设置TAG数目和过滤器C(Continuous parameter)
                        case "C":
                            setC(split[1]);
                            break;
                        //查询状态I（Inquire）
                        case "I":
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


    //查询状态I（Inquire）
    //示例
    //输入：I:<CR><LF>
    //返回：1,AntPower =30,TagMaxCnt=3，RSSIFilter=-65,CntFilter=3<CR><LF>
    private void getI() {
        String AntPower = String.valueOf(uhfService.get_antenna_power());
        String tagMaxCnt=sharedXmlUtil.read("tagMaxCnt","");
        String rssiFilter=sharedXmlUtil.read("rssiFilter","");
        String readCntFilter=sharedXmlUtil.read("readCntFilter","");
        if (AntPower.equals("-1")){
            result="0,AntPower ="+AntPower+",TagMaxCnt="+tagMaxCnt+",RSSIFilter="+rssiFilter
                    +",CntFilter="+readCntFilter+"\r"+"\n";
        }else {
            result="1,AntPower ="+AntPower+",TagMaxCnt="+tagMaxCnt+",RSSIFilter="+rssiFilter
                    +",CntFilter="+readCntFilter+"\r"+"\n";
        }
        sendMsg(result);
    }



    //设置天线功率P(Power)
    private void setP(String s) {
        String str = s.replace("\r", "").replace("\n", "");
        int i = uhfService.set_antenna_power(Integer.parseInt(str));
        if (i==0){
            result="1:成功"+"\r"+"\n";
        }else {
            result="0:失败"+"\r"+"\n";
        }
        sendMsg(result);
    }


    //设置TAG数目和过滤器C(Continuous parameter)
    public void setC(String s) {
        try {
            String[] splitResult = s
                    .replace("\r", "")
                    .replace("\n", "")
                    .split(",");

            sharedXmlUtil.write("tagMaxCnt", splitResult[0]);
            sharedXmlUtil.write("rssiFilter", splitResult[1]);
            sharedXmlUtil.write("readCntFilter", splitResult[2]);

            result="1:成功"+"\r"+"\n";
            sendMsg(result);
        } catch (Exception e) {
            e.printStackTrace();
            result="0:失败"+"\r"+"\n";
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
}
