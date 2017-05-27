package speedata.com.uhfservice;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

/**
 * 用于读取ini配置文件
 * @author USER
 *
 */
public class IniReader {

	//用于存放配置文件的属性值
	protected HashMap<String, Properties> sections = new HashMap<String, Properties>();
	private transient String currtionSecion;
	private transient Properties current;

	/**
	 * 读取文件
	 * @param name 文件名
	 * @throws IOException
	 */
	public IniReader(String name) throws IOException
	{
		File file=new File(name);
		FileInputStream fileInputStream = new FileInputStream(file);
		InputStream in= new BufferedInputStream(fileInputStream);
		InputStreamReader reader = new InputStreamReader(in, "GBK");
		BufferedReader read = null;
		try {
			if(reader != null)
			{
				read = new BufferedReader(reader);
				reader(read);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new FileNotFoundException("文件不存在或者文件读取失败");
		}
	}

	/**
	 * 设置每次读取文件一行
	 * @param reader 文件流
	 * @throws IOException
	 */
	private void reader(BufferedReader reader) throws IOException {
		// TODO Auto-generated method stub
		String line = null;
		try {
			while((line = reader.readLine()) != null)
			{
				parseLine(line);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IOException("文件内容读取失败");
		}
	}

	/**
	 * 获取ini文件的属性值
	 * @param line ini文件每行数据
	 */
	private void parseLine(String line) {
		// TODO Auto-generated method stub
		try {
			if (line != null) {
				line = line.trim();
				if (line.matches("\\[.*\\]")) {
					currtionSecion = line.replaceFirst("\\[(.*)\\]", "$1");
					current = new Properties();
					sections.put(currtionSecion, current);
				} else if (line.matches(".*=.*")) {
					if (current != null) {
						int i = line.indexOf('=');
						String name = line.substring(0, i);

						String value = line.substring(i+1);

						current.setProperty(name, value);

					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 用于获取属性值的值
	 * @param section 整体属性的值
	 * @param name 属性值名字
	 * @return 属性值的值
	 */
	public String getValue(String section, String name)
	{

		Properties p = (Properties)sections.get(section);

		if(p == null)
		{
			return null;
		}
		String value = p.getProperty(name);

		return value;
	}
}
