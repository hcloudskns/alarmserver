
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class TcpIpServer {
	public static void main(String args[]) {
		ServerSocket serverSocket = null;
		Socket socket = null;
		
		Map<String, Client> clients = new HashMap<String, Client> ();
		
		try {
				serverSocket = new ServerSocket(7777);
				System.out.println("서버가 준비되었습니다.");
				//socket = serverSocket.accept();
				
				while(true){
					
					socket = serverSocket.accept();
					
					Client c = new Client(socket.getInetAddress().toString(), Integer.toString(socket.getPort()),socket);
					clients.put(socket.getInetAddress().toString()+Integer.toString(socket.getPort()) , c);
					System.out.println("IP: " + socket.getInetAddress().toString()+ " , " + Integer.toString(socket.getPort())) ;
	
					ServerReceiver receiver = new ServerReceiver(socket.getInetAddress().toString()+Integer.toString(socket.getPort()),clients );
					//sender.start();
					receiver.start();
				}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	} // main
		
} // class


class ServerReceiver extends Thread {
	Socket socket;
	DataInputStream in;

	ServerReceiver(String key,Map<String, Client> clients) {
		
		Client c = clients.get(key);
		this.socket = c.getSocket();
		try {
			in = new DataInputStream(socket.getInputStream());
		} catch(IOException e) {}

	}

	public synchronized void run() {
		while(in!=null) {
			
			/*프로시저로 처리*/	
			Connection conn = null;
			CallableStatement cstmt = null;
			
				try {
					
					byte[] buffer = new byte[8096];
					int ret = in.read(buffer, 0, buffer.length);
					System.out.println(ret);
					String strData = new String((Arrays.copyOfRange(buffer, 0, ret)),"UTF-8");
					int event_type_flag = 0;
					
					JSONParser jParser = new JSONParser();
					JSONObject jObject;
									
					String strRealData = strData.substring(22,strData.length()-2);
					//String strRealData = strData.substring(7);
					System.out.println("클라이언트로부터 받은 메시지: " + strRealData);
					
					jObject = (JSONObject)jParser.parse(strRealData);
					
					String event_type = (String)jObject.get("event_type");
					
					switch(event_type) {
					
						case "CA":
							event_type_flag = 1;
							break;
							
						case "WA":	
							event_type_flag = 2;
							break;
						
						case "CR":	
							event_type_flag = 3;
							break;
					}
					
					String ip_address = (String)jObject.get("ip_address");
					String device_type = (String)jObject.get("device_type");
					String device_name = (String)jObject.get("device_name");
					String location = (String)jObject.get("location");
					String usage_data = (String)jObject.get("usage_data");
					String event_time = (String)jObject.get("event_time");
					String event_code = (String)jObject.get("event_code");					
					String uuid = (String)jObject.get("uuid");
					String analysis_log_time = (String)jObject.get("analysis_log_time");
					String event_msg = (String)jObject.get("event_msg");
					
										
					Class.forName("org.mariadb.jdbc.Driver");	
					conn = DriverManager.getConnection("jdbc:mariadb://192.168.101.110:3306/HCLOUD","root", "P@$$w0rd");
					 
					cstmt = conn.prepareCall("{call SP_ALARM_MESSAGE_I(?,?,?,?,?,?,?,?,?,?,?,?)}"); 
							
					cstmt.setString(1, event_type);
					cstmt.setInt(2, event_type_flag);
					cstmt.setString(3, ip_address);
					cstmt.setString(4, device_type);
					cstmt.setString(5, device_name);
					cstmt.setString(6, location);
					cstmt.setFloat(7, Float.parseFloat(usage_data));
					cstmt.setTimestamp(8,java.sql.Timestamp.valueOf(event_time));
					cstmt.setString(9, event_code);
					cstmt.setString(10, uuid);
					cstmt.setTimestamp(11, java.sql.Timestamp.valueOf(analysis_log_time));
					cstmt.setString(12, event_msg);
										
					cstmt.executeUpdate();
					
					SendMessage(socket,"클랑언트에 보낼 메시지: OK");
															
				}
				catch(SocketException e) {
					
					try {
						socket.close();
						break;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				/*}catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;*/
				}catch(Exception e) {
					e.printStackTrace();
					break;
				}finally {
					if(conn != null)
						try {
							conn.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					if(cstmt != null)
						try {
							cstmt.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
		}
	} // run
	
	public void SendMessage(java.net.Socket socket, String msg) throws IOException {
		
		DataOutputStream out = null;
		
		try {
			/*out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF(msg);*/
			//out.flush();
			//socket.close();
			out = new DataOutputStream(socket.getOutputStream());
			
			byte[] buffer = new byte[msg.getBytes().length];
			buffer = msg.getBytes("utf-8");
			//int ret = in.read(buffer, 0, buffer.length);
			//String strData = new String(Arrays.copyOfRange(buffer, 0, msg));
			out.write(buffer);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}



/*class ServerSender extends Thread {
Socket socket;
DataOutputStream out;
String name;

ServerSender(String key, Map<String, Client> clients) {
	//this.socket = socket;
    Client c = clients.get(key);
    this.socket = c.getSocket();
    
    try {
		
		out = new DataOutputStream(socket.getOutputStream());
		name = "["+socket.getInetAddress()+":"+socket.getPort()+"]";
	} catch(Exception e) {}
}

public void run() {
	Scanner scanner = new Scanner(System.in);
	while(out!=null) {
		try {
			
			out.writeUTF(name+scanner.nextLine());		
		} catch(IOException e) {}
	}
} // run()
}
*/

/*class Sender extends Thread {
Socket socket;
DataOutputStream out;
String name;

Sender(Socket socket) {
	this.socket = socket;
	try {
		out = new DataOutputStream(socket.getOutputStream());
		name = "["+socket.getInetAddress()+":"+socket.getPort()+"]";
	} catch(Exception e) {}
}

public void run() {
	Scanner scanner = new Scanner(System.in);
	while(out!=null) {
		try {
			out.writeUTF(name+scanner.nextLine());		
		} catch(IOException e) {}
	}
} // run()
}*/
