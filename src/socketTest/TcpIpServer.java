
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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
			Statement stmt = null;
			ResultSet rs = null;
			PreparedStatement pstmt = null;
			
			String driver = "org.mariadb.jdbc.Driver";
			
			String url = "jdbc:mariadb://192.168.101.110:3306/HCLOUD";
			String user = "root";
			String pw = "P@$$W0rd";
			
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
					
					//메일 발송
					FindType(device_type, location, event_type, uuid);
				
					
					
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
		
	public void FindType (String device_type, String location, String event_type, String uuid) throws SQLException, ClassNotFoundException{
			
			String sql = "SELECT MAIL_SEND_YN, DEVICE_TYPE, LOCATION, EVENT_TYPE FROM TB_MAIL_POLICY WHERE DEVICE_TYPE=? AND LOCATION=? AND EVENT_TYPE=? ;";
			
			String sql1 = "INSERT INTO TB_MAIL_SEND_LOG (USER_NAME, EMAIL_ADDR, MAIL_TITLE, MAIL_MESSAGE, SEND_DATE, SEND_YN) VALUES(?, ?, ?, ?, ?, ?)";
			
			String user = "hyunjo.hwang@namutech.co.kr"; // gmail 계정
	        String password = "Gksksla0702!";   // 패스워드
	        
	        String mailTitle = "문제 알람 보내드립니다.";
	        
	        
	        String Body = String.join(
	        		System.getProperty("line.separator"),
	        		"<table style=\"border:1px solid #f5f5f5;margin:auto;width:576px;border-collapse:collapse;background-color:white;color:rgba(0,0,0,0.66)\">",
	        		"<tbody><tr style=\"background-color:#e21837;height:56px;\">",
	        		"<td style=\"padding:0 36px\">",
	        		"<img src=\"/src/main/image/header_logo.png\" alt=\"NCC-HCloud\" style=\"height:20px;margin-right:10px;vertical-align:middle\" />",
	        		"<span style=\"font:21px;color:white;vertical-align:middle;\">통합 클라우드 플랫폼 서비스</span>",
	        		"</td>",
	        		"</tr>",
	        		"<tr style=\"height:250px;vertical-align:top\">",
	        		"<td style=\"padding:18px 36px;\">",
	        		"<p></p>",
	        		"<h2 style=\"font-size:21px;font-weight:normal;margin-bottom:25px;\">" + mailTitle + "</h2>",
	        		"<p></p>",
	        		"<p style=\"margin-top:14px\"></p>",
	        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">DEVICE TYPE</h3>",
	        		"<span style=\"font-size:18px;\">"+ device_type +"</span>",
	        		"<p></p>",
	        		"<p style=\"margin-top:14px\"></p>",
	        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">LOCATION</h3>",
	        		"<span style=\"font-size:18px;\">" + location + "</span>",
	        		"<p></p>",
	        		"<p style=\"margin-top:14px\"></p>",
	        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">장애등급</h3>",
	        		"<span style=\"font-size:18px;\">" + event_type + "</span>",
	        		"<p></p>",
	        		"<p style=\"margin-top:24px;margin-bottom:16px;\">",
	        		"<a href=\"#\" style=\"background-color:#e21837;text-decoration:none;color:white;font-size:13px;padding:0.5em 1em;\" target=\"_blank\">버튼명</a>",
	        		"</p>",
	        		"</td>",
	        		"</tr>",
	        		"<tr style=\"background-color:#fafafa;height:57px;\">",
	        		"<td style=\"padding:0 36px;font-size:12px;\">\r\n" + 
	        		"          COPYRIGHT (C) 2020 나무기술 CO., LTD. ALL RIGHT RESERVED.<br />\r\n" + 
	        		"		  서울특별시 강남구 삼성로 531 고운빌딩 T 02-3288-7900 | F 02-3288-8110<br />\r\n" + 
	        		"		  If you don't want this type of information or e-mail, please <a href=\"https://www.bizmailer.co.kr/bizsmart/action/openCheck.do?method=deny&amp;sn=27&amp;mk=14810069414996T7uhA2&amp;lk=1623658900746&amp;sk=27_1623658900746_16141306730300000098_D8BA4951916A8F00F281&amp;dk=D8BA4951916A8F00F281&amp;ck=16141306730300000098&amp;ce=hyeongsoo.kim@namutech.co.kr&amp;la=kr\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?q=https://www.bizmailer.co.kr/bizsmart/action/openCheck.do?method%3Ddeny%26sn%3D27%26mk%3D14810069414996T7uhA2%26lk%3D1623658900746%26sk%3D27_1623658900746_16141306730300000098_D8BA4951916A8F00F281%26dk%3DD8BA4951916A8F00F281%26ck%3D16141306730300000098%26ce%3Dhyeongsoo.kim@namutech.co.kr%26la%3Dkr&amp;source=gmail&amp;ust=1623888208881000&amp;usg=AFQjCNH1uBinB5F7zWR9kHSqUpw6w3Q-WA\" style=\"color:#e21837;\">[click here]</a>\r\n" + 
	        		"      </td>",
	        		"</tr>\r\n" + 
	        		"  </tbody>\r\n" + 
	        		"</table>\r\n" + 
	        		"</html>"
	        );
			
			try(Connection dbCon = DriverManager.getConnection("jdbc:mariadb://192.168.101.110:3306/HCLOUD","root", "P@$$w0rd");
				PreparedStatement stmt = dbCon.prepareStatement(sql);
				){
					stmt.setString(1, device_type);
					stmt.setString(2, location);
					stmt.setString(3, event_type);
					
					String bring = updateMailLog(device_type, uuid);
					String[] email = bring.split("_");
						
					ResultSet rs = stmt.executeQuery();
						
					while(rs.next()) {
						
							java.util.Date utilDate = new java.util.Date();
							java.sql.Timestamp sqlDate = new java.sql.Timestamp(utilDate.getTime());
							
							String send_yn = "";
							
							PreparedStatement stmt1 = dbCon.prepareStatement(sql1);
							stmt1.setString(1, email[0]);
							stmt1.setString(2, email[1]);
							stmt1.setString(3, mailTitle);
							stmt1.setString(4, Body);
							stmt1.setTimestamp(5, sqlDate);
							stmt1.setString(6, send_yn);
							
							stmt1.executeUpdate();

						if(rs.getString("MAIL_SEND_YN").toUpperCase().equals("Y")) {
							
							// SMTP 서버 정보를 설정한다.
					        Properties prop = new Properties();
					        prop.put("mail.smtp.host", "smtp.gmail.com"); 
					        prop.put("mail.smtp.port", 465); 
					        prop.put("mail.smtp.auth", "true"); 
					        prop.put("mail.smtp.ssl.enable", "true"); 
					        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
					        
					        Session session = Session.getDefaultInstance(prop, new javax.mail.Authenticator() {
					            protected PasswordAuthentication getPasswordAuthentication() {
					                return new PasswordAuthentication(user, password);
					            }
					        });
					       
					        try {
					            MimeMessage message = new MimeMessage(session);
					            message.setFrom(new InternetAddress(user));
	
					            //수신자메일주소
					            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email[1])); 
	
					            // Subject
					            message.setSubject("이것은 테스트입니다"); //메일 제목을 입력
					            
					            // Email 발송
					            message.setContent(Body, "text/html;charset=euc-kr");
	
					            // send the message
					            Transport.send(message); //전송
					            System.out.println("message sent successfully...");
					            
					            
					        } catch (AddressException e) {
					         
					            e.printStackTrace();
					        } catch (MessagingException e) {
					          
					            e.printStackTrace();
					        }
						}
				}
					
				rs.close();
				stmt.close();
				dbCon.close();
				
				
				
			}catch (SQLException ex){
		        throw new RuntimeException(ex);
			}	
		}

	public String updateMailLog(String device_type, String uuid) throws SQLException, ClassNotFoundException {
		
		String driver = "org.mariadb.jdbc.Driver";
		String sql1 = "SELECT USER_NAME, EMAIL_ADDR FROM TB_CLUSTER WHERE  CLUSTER_UUID IN (SELECT A.CLUSTER_UUID FROM ( SELECT CLUSTER_UUID, VM_UUID AS UUID,  'VM' AS DEVICE_TYPE   FROM TB_VM UNION  SELECT CLUSTER_UUID, NODE_UUID AS UUID, 'NODE' AS DEVICE_TYPE    FROM TB_NODE    ) A WHERE A.UUID=?  AND A.DEVICE_TYPE=?) ";
		
        Connection con = null;
        PreparedStatement pstmt = null;
        
        String user = "root"; // gmail 계정
        String password = "P@$$w0rd";   // 패스워드
        String url = "jdbc:mariadb://192.168.101.110:3306/HCLOUD";
        String ret = ""; // problem here
		try {
			
			Class.forName(driver);
        	con = DriverManager.getConnection(url, user, password);
        	
        	pstmt = con.prepareStatement(sql1);
        	pstmt.setString(1, uuid);
        	pstmt.setString(2, device_type);
        	
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()) {
				System.out.println(rs.getString("USER_NAME")+ "_" + rs.getString("EMAIL_ADDR"));
				ret = rs.getString("USER_NAME")+ "_" + rs.getString("EMAIL_ADDR");
			}
			
        } catch (SQLException e) { 
        	System.out.println("[SQL Error : " + e.getMessage() + "]"); 
        } catch (ClassNotFoundException e1) {
        	System.out.println("[JDBC Connector Driver 오류 : \" + e1.getMessage()");
        }
        pstmt.close();
        con.close();
        
		return ret;
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
