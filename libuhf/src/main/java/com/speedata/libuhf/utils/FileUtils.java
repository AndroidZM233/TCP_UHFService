package com.speedata.libuhf.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ----------Dragon be here!----------/
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛
 * 　　　　┃　　　┃神兽保佑
 * 　　　　┃　　　┃代码无BUG！
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * ━━━━━━神兽出没━━━━━━
 *
 * @author :Reginer in  2017/2/22 10:33.
 *         联系方式:QQ:282921012
 *         功能描述:文件流
 */
public class FileUtils {

    private static final String TAG = "Reginer";

    /**
     * 读取文本文件中的内容 .
     *
     * @param strFilePath 文件路径
     * @return 文件内容
     */
    public static String readTxtFile(String strFilePath) {
        String content = "";
        File file = new File(strFilePath);

        try {
            InputStream inputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            //分行读取
            while ((line = bufferedReader.readLine()) != null) {
                content += line + "\n";
            }
            inputStream.close();
        }
        catch (java.io.FileNotFoundException e) {
            Log.d(TAG, "readTxtFile: The File doesn't not exist.");
        }
        catch (IOException e) {
            Log.d(TAG, "readTxtFile: " + e.toString());
        }

        return content;
    }
}
