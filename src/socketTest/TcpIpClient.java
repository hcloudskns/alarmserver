
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
			String serverIp = "192.168.101.113";
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
		while(out!=null) {
			try {
				//0000352{"nw_event_info":[{"event_type":"WA", "ip_address":"192.168.101.101", "device_type":"NW", "device_name":"VM_TEST3_001", "location":"PowerState", "usage":"-1", "event_time":"2020-08-24 13:53:00", "event_code":"NWXEN0005", "event_msg":"NCC-Hcloud Agent 접속이 종료되었습니다",  "detail_msg":"NCC-Hcloud Agent 접속이 종료되었습니다"}]}
				String temp = "0000346{\"event_info\":[{\"event_type\":\"CA\", \"ip_address\":\"192.168.1.5\", \"device_type\":\"NW\", \"device_name\":\"NCC-AaaS\", \"location\":\"PowerState\", \"usage_data\":\"-1\", \"event_time\":\"2020-08-03 16:04:44\", \"event_code\":\"NWXEN0005\", \"event_msg\":\"NCC-Monitor Agent >접속이 종료되었습니다\",  \"analysis_log_time\":\"2020-08-03 16:04:44\"}]}";	
				byte[] buffer = new byte[temp.getBytes().length];	
				//byte[] buffer = new byte[354];
				buffer = temp.getBytes("UTF-8");
				int len = temp.getBytes().length;
				
			/*	for(int i=0; i <buffer.length;i++) {
					System.out.print(buffer[i]);
				}
			*/	
				//JSONObject jsonObject = new JSONObject();
				//jsonObject.put("MSG",temp);
				//System.out.println(jsonObject.toString());
				
				//out.writeUTF(temp);
				
				out.write(buffer);
				
				sleep(5000);
				temp = "";
				
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


