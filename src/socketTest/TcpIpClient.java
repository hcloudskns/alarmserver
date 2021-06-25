
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;

public class TcpIpClient {
	public static void main(String args[]) {
		try {
			//String serverIp = "192.168.101.110";
			//String serverIp = "192.168.50.243";
			//String serverIp = "10.10.0.116";
			String serverIp = "127.0.0.1";
            // 소켓을 생성하여 연결을 요청한다.
			Socket socket = new Socket(serverIp, 7777); 

			System.out.println("서버에 연결되었습니다.");
			ClientSender sender = new ClientSender(socket);
			ClientReceiver receiver = new ClientReceiver(socket);

			sender.start();
			receiver.start();
			
		} catch(ConnectException ce) {
			System.out.println(ce.getMessage());
			ce.printStackTrace();
		} catch(IOException ie) {  
			ie.printStackTrace();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			
			e.printStackTrace();  
		}  
	} // main
}// class

class ClientSender extends Thread {
	Socket socket;
	DataOutputStream out;
	String name;

	ClientSender(Socket socket) {
		this.socket = socket;
		try {
			out = new DataOutputStream(socket.getOutputStream());
			name = "["+socket.getInetAddress()+":"+socket.getPort()+"]";
		} catch(Exception e) {
			System.out.print(e.getMessage());
		}
	}

	public void run() {
		//Scanner scanner = new Scanner(System.in);
		String temp = "";
		int len;
		while(out!=null) {
			try {
				byte[] buffer ;
				//0000352{"nw_event_info":[{"event_type":"WA", "ip_address":"192.168.101.101", "device_type":"NW", "device_name":"VM_TEST3_001", "location":"PowerState", "usage":"-1", "event_time":"2020-08-24 13:53:00", "event_code":"NWXEN0005", "event_msg":"NCC-Hcloud Agent 접속이 종료되었습니다",  "detail_msg":"NCC-Hcloud Agent 접속이 종료되었습니다"}]}
				temp = "0000346{\"event_info\":[{\"event_type\":\"CR\", \"ip_address\":\"192.168.1.5\", \"device_type\":\"VM\", \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"NCC-AaaS\", \"location\":\"PATTERN\", \"usage\":\"-1\", \"event_time\":\"2020-08-03 16:04:44\", \"event_code\":\"NWXEN0005\", \"event_msg\":\"NCC-Monitor Agent >접속이 종료되었습니다\",  \"analysis_log_time\":\"2020-08-03 16:04:44\"}]}";	
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);
				//byte[] buffer = new byte[354];
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);
				
				
				
				temp = "0000346{\"vm_event_info\":[{\"event_type\":\"CA\", \"ip_address\":\"192.168.101.13\", \"device_type\":\"VM\", \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK_20201107_001\", \"location\":\"CPU\", \"usage\":\"50.38\", \"event_time\":\"2020-11-07 16:24:13\", \"event_code\":\"XSCPU0003\", \"event_msg\":\"임계치 40,000이상 3회 연속 초과하였습니다.\",  \"analysis_log_time\":\"2020-08-03 16:04:44\"}]}";	
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);			
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);
				
				
				temp = "0000346{\"vm_cpu_report\":["
						+ "{\"pid\":\"5780\", \"process\":\"loop_100\", \"cpu_usage\":\"93.8\", \"mem_usage\":\"0.8\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"1235\", \"process\":\"cloud_age\", \"cpu_usage\":\"6.2\", \"mem_usage\":\"0.5\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"1\",	 \"process\":\"systemd\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.2\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"2\", \"process\":\"kthread\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"4\", \"process\":\"kworker\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"5\", \"process\":\"kworkeru+\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"6\", \"process\":\"ksoftirqd\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"7\", \"process\":\"migration+\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"8\", \"process\":\"rcu_bh\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"9\", \"process\":\"rcu_sched\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\"07b852d6-1534-79f8-f940-e8b1546612b7\", \"device_name\":\"TEST-SK-20170001\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "]}";	
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);
				//byte[] buffer = new byte[354];
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);
				
			} catch(IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	} // run()
}// class

class ClientReceiver extends Thread {
	Socket socket;
	DataInputStream in;

	ClientReceiver(Socket socket) {
		this.socket = socket;
		try {
			in = new DataInputStream(socket.getInputStream());
		} catch(IOException e) {
			e.printStackTrace();
		}

	}

	public void run() {
		while(in !=null) {
			
			try {
				byte[] buffer = new byte[16384];
				int ret = in.read(buffer, 0, buffer.length);
				String strData = new String(Arrays.copyOfRange(buffer, 0, ret));
				//String strData = new String(Arrays.copyOf(buffer, 8096));
				
				System.out.println("서버로부터 받은메시지:" + strData);
				
			//	System.out.println("서버로부터의 메시지: " + in.readUTF());
			} catch(IOException e) {
				e.printStackTrace();
				
			}
			
		}
	} // run
}//class


