
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class AlarmServer {
	
	public static void main(String args[]) {
		ServerSocket serverSocket = null;
		Socket socket = null;
		
		Map<String, Client> clients = new HashMap<String, Client> ();
		
		try {
				
				if(DBConst.mode.equals("TEST")) {
					serverSocket = new ServerSocket(DBConst.testAlarmServerPort);
					System.out.println("테스트 Alarm 서버가 준비되었습니다.");
				
				}else {
					serverSocket = new ServerSocket(DBConst.alarmServerPort);
					System.out.println("Alarm 서버가 준비되었습니다.");
				}
				
				  /*System.out.println( new Timestamp(System.currentTimeMillis()) );
				  System.nanoTime()- this.start
				  
				System.out.println(Instant.now().truncatedTo(ChronoUnit.MICROS));*/
				
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
	
	public void insertLog(String deviceType, String uuid, String deviceName, String logMessage) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String strSQL ="";
		
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal1 = Calendar.getInstance();
		String strDate = sdf1.format(cal1.getTime());
		
		int r =0;

		try
		{
			Class.forName("org.mariadb.jdbc.Driver");
			conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
						
			strSQL = "insert into TB_TR_LOG(DEVICE_TYPE, UUID, DEVICE_NAME,LOG_MESSAGE,LOG_DATE) values(?, ?, ?, ?, ?)";
			pstmt = conn.prepareStatement(strSQL);
			
			pstmt.setString(1,deviceType );
			pstmt.setString(2, uuid);
			pstmt.setString(3, deviceName);
			pstmt.setString(4, logMessage);
			pstmt.setTimestamp(5,  java.sql.Timestamp.valueOf(strDate));
			
			r = pstmt.executeUpdate();
			
		} catch (SQLException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(conn != null)
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(pstmt != null)
				try {
					pstmt.close();
				} catch (SQLException e) {
				// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	@SuppressWarnings("resource")
	public synchronized void run() {
		if(in!=null) {
		//while(in!=null) {
			
			/*프로시저로 처리*/	
			Connection conn = null;
			CallableStatement cstmt = null;
			PreparedStatement stmt = null;
			String strData = "";
			String strRealData = "";
			String sql = "";
			
			try {
				
				byte[] buffer = new byte[30000];
				Arrays.fill(buffer,(byte)0);
				int ret=0;
				ret = in.read(buffer, 0, buffer.length);		
				
				if( ret > 0) {   // 이 처리가 굉장히 중요함
									
							strData = new String((Arrays.copyOfRange(buffer, 0, ret)),"UTF-8");
																
							insertLog("", "", "","[Alarm서버 수신]:" +  strData );
							System.out.println("클라이언트로부터 받은 메시지: " + strData);
						
							/*헬스 체크*/
							if(strData.toUpperCase().equals("0000006STATUS")){
								strData = "0000012{\"health_check\":[]}";
							}
								strRealData = strData.substring(7);
								
								int event_type_flag = 0;					
								String ip_address = "";
								String device_type = "";
								String device_name = "";
								String location = "";
								String usage = "";
								String event_time = "";
								String event_code = "";					
								String uuid = "";
								String analysis_log_time = "";
								String event_msg = "";
								String event_type = "";
								String last_time = "";
								float last_latency = 0.0f;
								String last_status="";
								
								String pid = "";
								String process ="";
								float cpu_usage = 0.0f;
								float mem_usage = 0.0f;
								String event_level = "";
													
								JSONParser jParser = new JSONParser();
								JSONObject jObjectRep;
								
								jObjectRep = (JSONObject)jParser.parse(strRealData);
								JSONArray jsonArray ; 
										
								if(jObjectRep.containsKey("health_check")) {
									SendMessage(socket, "0000007RUNNING");
								}else if(jObjectRep.containsKey("vm_pattern_event_info")) {
										/*TTA3*/	
									jsonArray = (JSONArray)jObjectRep.get("vm_pattern_event_info");
									
									for(int i=0; i< jsonArray.size(); i++) {
										
										JSONObject jObject = (JSONObject)jsonArray.get(i);
										
										ip_address = (String)jObject.get("ip_address");
										device_type = (String)jObject.get("device_type");
										device_name = (String)jObject.get("device_name");
										location = (String)jObject.get("location");
										usage = (String)jObject.get("usage");
										event_time = (String)jObject.get("event_time");															
										event_code = (String)jObject.get("event_code");
																
										uuid = jObject.containsKey("uuid")?(String)jObject.get("uuid"):"";
										analysis_log_time =	jObject.containsKey("analysis_log_time")?(String)jObject.get("analysis_log_time"):"0001-01-01 00:00:00.000000";
										
										event_msg = (String)jObject.get("event_msg");
										/*전문에는 PA로 오는데 CA로 바꾼다. */
										//event_type = (String)jObject.get("event_type");
										event_type = "CA";
										
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
										
										Class.forName("org.mariadb.jdbc.Driver");	
										conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
										 
										cstmt = conn.prepareCall("{call SP_ALARM_MESSAGE_I(?,?,?,?,?,?,?,?,?,?,?,?)}"); 
												
										cstmt.setString(1, event_type);
										cstmt.setInt(2, event_type_flag);
										cstmt.setString(3, ip_address);
										cstmt.setString(4, device_type);
										cstmt.setString(5, device_name);
										cstmt.setString(6, location);
										cstmt.setFloat(7, Float.parseFloat(usage));
										cstmt.setTimestamp(8,java.sql.Timestamp.valueOf(event_time));
										cstmt.setString(9, event_code);
										cstmt.setString(10, uuid);
										cstmt.setTimestamp(11, java.sql.Timestamp.valueOf(analysis_log_time));
										cstmt.setString(12, event_msg);											
										cstmt.executeUpdate();
										
										sql = " UPDATE TB_VM_CREATE_JOB_LOG " +
											  " SET PATTERN_FIND_DATE=? "+
											  " WHERE VM_UUID=? ";
										
										stmt = conn.prepareStatement(sql);
										stmt.setTimestamp(1, java.sql.Timestamp.valueOf(analysis_log_time));
										stmt.setString(2, uuid);
										stmt.executeUpdate();
										
										insertLog(device_type, uuid, device_name,"[Alarm서버  vm_pattern_event_info 프로시저 매개 변수] :" +  event_type + " / " + event_type_flag + " / " + 
												ip_address + " / " +	 device_type + " / " + device_name + " / " + location  + " / " + usage + " / " +
												event_time + " / " + event_code + " / " + uuid  + " / " +analysis_log_time + " / " +event_msg
										);
										
										//SendMessage(socket,"클랑언트에 보낼 메시지: OK");							
										//메일 발송
										SendMail(device_type, location, event_type, uuid, event_msg);
									}
								}else if(jObjectRep.containsKey("vm_cpu_event_info")) {/*TTA4*/
									
									jsonArray = (JSONArray)jObjectRep.get("vm_cpu_event_info");
									
									for(int i=0; i< jsonArray.size(); i++) {
										
										JSONObject jObject = (JSONObject)jsonArray.get(i);
										
										ip_address = (String)jObject.get("ip_address");
										device_type = (String)jObject.get("device_type");
										device_name = (String)jObject.get("device_name");
										location = (String)jObject.get("location");
										usage = (String)jObject.get("usage");
										event_time = (String)jObject.get("event_time");
										event_code = (String)jObject.get("event_code");					
																
										uuid = jObject.containsKey("uuid")?(String)jObject.get("uuid"):"";
										analysis_log_time =	jObject.containsKey("analysis_log_time")?(String)jObject.get("analysis_log_time"):"0001-01-01 00:00:00.000000";
										
										event_msg = (String)jObject.get("event_msg");
										event_type = (String)jObject.get("event_type");
										
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
										
										Class.forName("org.mariadb.jdbc.Driver");	
										conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
										 
										cstmt = conn.prepareCall("{call SP_ALARM_MESSAGE_I(?,?,?,?,?,?,?,?,?,?,?,?)}"); 
												
										cstmt.setString(1, event_type);
										cstmt.setInt(2, event_type_flag);
										cstmt.setString(3, ip_address);
										cstmt.setString(4, device_type);
										cstmt.setString(5, device_name);
										cstmt.setString(6, location);
										cstmt.setFloat(7, Float.parseFloat(usage));
										cstmt.setTimestamp(8,java.sql.Timestamp.valueOf(event_time));
										cstmt.setString(9, event_code);
										cstmt.setString(10, uuid);
										cstmt.setTimestamp(11, java.sql.Timestamp.valueOf(analysis_log_time));
										cstmt.setString(12, event_msg);											
										cstmt.executeUpdate();
										
										sql = " UPDATE TB_VM_CREATE_JOB_LOG " +
											  " SET CPU_LIMIT_HAPPEN_DATE=?, "+
											  "     CPU_LIMIT_SERVER_SAVE_DATE=? "+
											  " WHERE VM_UUID=? ";
										
										stmt = conn.prepareStatement(sql);
										stmt.setTimestamp(1,java.sql.Timestamp.valueOf(event_time));
										stmt.setTimestamp(2, java.sql.Timestamp.valueOf(analysis_log_time));
										stmt.setString(3, uuid);
										stmt.executeUpdate();
										
										insertLog(device_type, uuid, device_name,"[Alarm서버  vm_cpu_event_info 프로시저 매개 변수] :" +  event_type + " / " + event_type_flag + " / " + 
												ip_address + " / " +	 device_type + " / " + device_name + " / " + location  + " / " + usage + " / " +
												event_time + " / " + event_code + " / " + uuid  + " / " +analysis_log_time + " / " +event_msg
										);
				
										
										//SendMessage(socket,"클랑언트에 보낼 메시지: OK");							
										//메일 발송
										SendMail(device_type, location, event_type, uuid, event_msg);
									}
								}else if(jObjectRep.containsKey("vm_power_event_info")) {/*TTA7*/
									
									jsonArray = (JSONArray)jObjectRep.get("vm_power_event_info");
									
									for(int i=0; i< jsonArray.size(); i++) {
										
										JSONObject jObject = (JSONObject)jsonArray.get(i);
										
										ip_address = (String)jObject.get("ip_address");
										device_type = (String)jObject.get("device_type");
										device_name = (String)jObject.get("device_name");
										location = (String)jObject.get("location");
										usage = (String)jObject.get("usage");
										event_time = (String)jObject.get("event_time");
										event_code = (String)jObject.get("event_code");
										
										uuid = jObject.containsKey("uuid")?(String)jObject.get("uuid"):"";
										analysis_log_time =	jObject.containsKey("analysis_log_time")?(String)jObject.get("analysis_log_time"):"0001-01-01 00:00:00.000000";
										                                                                                                       
										event_msg = (String)jObject.get("event_msg");
										event_type = (String)jObject.get("event_type");
										
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
										
										Class.forName("org.mariadb.jdbc.Driver");	
										conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
										 
										cstmt = conn.prepareCall("{call SP_ALARM_MESSAGE_I(?,?,?,?,?,?,?,?,?,?,?,?)}"); 
												
										cstmt.setString(1, event_type);
										cstmt.setInt(2, event_type_flag);
										cstmt.setString(3, ip_address);
										cstmt.setString(4, device_type);
										cstmt.setString(5, device_name);
										cstmt.setString(6, location);
										cstmt.setFloat(7, Float.parseFloat(usage));
										cstmt.setTimestamp(8,java.sql.Timestamp.valueOf(event_time));
										cstmt.setString(9, event_code);
										cstmt.setString(10, uuid);
										cstmt.setTimestamp(11, java.sql.Timestamp.valueOf(analysis_log_time));
										cstmt.setString(12, event_msg);											
										cstmt.executeUpdate();
										
										sql = " UPDATE TB_VM_CREATE_JOB_LOG " +
											  " SET VM_EXIT_SERVER_SAVE_DATE=? "+							  
											  " WHERE VM_NAME=? AND IP_ADDRESS=? ";
										
										stmt = conn.prepareStatement(sql);
										stmt.setTimestamp(1,  java.sql.Timestamp.valueOf(analysis_log_time));						
										stmt.setString(2, device_name);
										stmt.setString(3, ip_address);
										stmt.executeUpdate();
										
										
										sql = " UPDATE TB_VM " +
											  " SET POWER_STATE='HALTED' "+							  
											  " WHERE VM_NAME=? AND IP_ADDRESS=? ";
											
										stmt = conn.prepareStatement(sql);
										stmt.setString(1, device_name);
										stmt.setString(2, ip_address);
										stmt.executeUpdate();						
											
										
										insertLog(device_type, uuid, device_name,"[Alarm서버  vm_power_event_info 프로시저 매개 변수] :" +  event_type + " / " + event_type_flag + " / " + 
												ip_address + " / " +	 device_type + " / " + device_name + " / " + location  + " / " + usage + " / " +
												event_time + " / " + event_code + " / " + uuid  + " / " +analysis_log_time + " / " +event_msg
										);
										
										//SendMessage(socket,"클랑언트에 보낼 메시지: OK");							
										//메일 발송
										SendMail(device_type, location, event_type, uuid, event_msg);
									}
								}else if(jObjectRep.containsKey("vm_cpu_report") ||jObjectRep.containsKey("vm_memory_report")){
									
									String p_device_type = "VM";
									String p_location;
									String p_event_code = "XSCPU0003";
									String p_uuid = "";
									String p_ip_address = "";
									int	detail_seq = 0;
									JSONObject jObject;
									
									if(jObjectRep.containsKey("vm_cpu_report")) {
										p_location = "CPU";
										jsonArray = (JSONArray)jObjectRep.get("vm_cpu_report");
										jObject = (JSONObject)jsonArray.get(0);										
									}else {
										p_location = "MEM";
										jsonArray =(JSONArray)jObjectRep.get("vm_memory_report");
										jObject = (JSONObject)jsonArray.get(0);
									}
										
									p_uuid = (String)jObject.get("uuid");
									p_ip_address = (String)jObject.get("ip_address");
									
									sql =" 	SELECT IFNULL(MAX(DETAIL_SEQ),0) AS DETAIL_SEQ " +
										 "	FROM TB_ALARM_MSG_DETAIL " + 
										 "	WHERE  1=1	" + 
										 "	AND	IP_ADDRESS = ? " + 
										 "	AND	DEVICE_TYPE= ? " + 
										 "	AND	LOCATION = ?	" + 
										 "	AND EVENT_CODE = ? " + 
										 "	AND	UUID = ? " ; 
									
									Class.forName("org.mariadb.jdbc.Driver");	
									conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
									
									stmt = conn.prepareStatement(sql);									
									stmt.setString(1, p_ip_address);
									stmt.setString(2, p_device_type);
									stmt.setString(3, p_location);
									stmt.setString(4, p_event_code);
									stmt.setString(5, p_uuid);
									
									ResultSet rs = stmt.executeQuery();
									
									while(rs.next()) {
										detail_seq = rs.getInt("DETAIL_SEQ");
									}
									
									for(int i=0; i< jsonArray.size(); i++) {
										
										jObject = (JSONObject)jsonArray.get(i);
										
										pid = (String)jObject.get("pid");
										process = (String)jObject.get("process");
										cpu_usage = Float.parseFloat((String)jObject.get("cpu_usage"));
										mem_usage = Float.parseFloat((String)jObject.get("mem_usage"));						
										event_type = (String)jObject.get("event_type");
										ip_address = (String)jObject.get("ip_address");
										device_type = (String)jObject.get("device_type");
										uuid = (String)jObject.get("uuid");
										device_name = (String)jObject.get("device_name");
										location = (String)jObject.get("location");
										event_level = (String)jObject.get("event_time");
																																				 
										cstmt = conn.prepareCall("{call SP_RESOURCE_USE_INFO_I(?,?,?,?,?,?,?,?,?,?,?,?)}"); 
												
										cstmt.setString(1, pid);
										cstmt.setString(2, process);
										cstmt.setFloat(3, cpu_usage);						
										cstmt.setFloat(4, mem_usage);
										cstmt.setString(5, event_type);
										cstmt.setString(6, ip_address);
										cstmt.setString(7, device_type);						
										cstmt.setString(8, uuid);
										cstmt.setString(9, device_name);
										cstmt.setString(10, location);
										cstmt.setString(11, event_level);
										cstmt.setInt(12, detail_seq);
										
										cstmt.executeUpdate();		
									}
								}else if(jObjectRep.containsKey("nw_status")){
									/* 알람서버에서 처리하지 않고 포털에서 직접 소켓으로 가져옴
									 * 이 부분은 사용안함 
									 */
									jsonArray = (JSONArray)jObjectRep.get("nw_status");
									
									for(int i=0; i< jsonArray.size(); i++) {
										
										JSONObject jObject = (JSONObject)jsonArray.get(i);
										
										ip_address = (String)jObject.get("ip_address");
										device_type = (String)jObject.get("device_type");
										device_name = (String)jObject.get("device_name");
										last_time = (String)jObject.get("last_time");
										last_latency = Float.parseFloat((String)jObject.get("last_latency"));
										last_status = (String)jObject.get("last_status");
																
										Class.forName("org.mariadb.jdbc.Driver");	
										conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
										 
										cstmt = conn.prepareCall("{call SP_NET_STATUS_INFO_I(?,?,?,?,?,?)}"); 
												
										cstmt.setString(1, ip_address);
										cstmt.setString(2, device_type);
										cstmt.setString(3, device_name);						
										cstmt.setTimestamp(4, java.sql.Timestamp.valueOf(last_time));
										cstmt.setFloat(5, last_latency);
										cstmt.setString(6, last_status);
										cstmt.executeUpdate();		
									}
								}else if(jObjectRep.containsKey("nw_event_info")){
									
									jsonArray = (JSONArray)jObjectRep.get("nw_event_info");
									
									for(int i=0; i< jsonArray.size(); i++) {
										
										JSONObject jObject = (JSONObject)jsonArray.get(i);
										
										ip_address = (String)jObject.get("ip_address");
										device_type = (String)jObject.get("device_type");
										device_name = (String)jObject.get("device_name");
										location = (String)jObject.get("location");
										usage = (String)jObject.get("usage");
										event_time = (String)jObject.get("event_time");
										event_code = (String)jObject.get("event_code");					
										event_msg = (String)jObject.get("event_msg");
										event_type = (String)jObject.get("event_type");
										
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
										
										Class.forName("org.mariadb.jdbc.Driver");	
										conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
										 
										cstmt = conn.prepareCall("{call SP_ALARM_MESSAGE_I(?,?,?,?,?,?,?,?,?,?,?,?)}"); 
												
										cstmt.setString(1, event_type);
										cstmt.setInt(2, event_type_flag);
										cstmt.setString(3, ip_address);
										cstmt.setString(4, device_type);
										cstmt.setString(5, device_name);
										cstmt.setString(6, location);
										cstmt.setFloat(7, Float.parseFloat(usage));
										cstmt.setTimestamp(8,java.sql.Timestamp.valueOf(event_time));
										cstmt.setString(9, event_code);
										cstmt.setString(10, "");
										cstmt.setTimestamp(11, java.sql.Timestamp.valueOf(event_time));
										cstmt.setString(12, event_msg);											
										cstmt.executeUpdate();										
												
									}
									SendMail(device_type, location, event_type, "", event_msg);
								}
						
					}//  ret >0
			}
			catch(SocketException e) {
				
				try {
					socket.close();
					//break;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//break;
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//break;
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
				//break;
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
				if(stmt != null)
					try {
						stmt.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				if(in != null)
					try {
						in.close();
					} catch (IOException e) {
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
		
	public void SendMail (String device_type, String location, String event_type, String uuid, String event_msg) throws SQLException, ClassNotFoundException{
			try{
					
					String sql = "";
					String user = DBConst.mailAddr; // gmail 계정
			        String password = DBConst.mailPwd;   // 패스워드	        
			        String mailTitle = "관리자용 장애 알람 메일";
			        String Body ="";
					sql = " SELECT MAIL_SEND_YN, DEVICE_TYPE, LOCATION, EVENT_TYPE, CODE_NM " +
						  " FROM TB_MAIL_POLICY TMP  LEFT OUTER JOIN LETTCCMMNDETAILCODE  LET " + 
						  " ON TMP.EVENT_TYPE = LET.CODE " + 
						  " WHERE LET.CODE_ID='COM031' AND DEVICE_TYPE=? AND LOCATION=? AND EVENT_TYPE=?";
					
					Connection dbCon = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
					PreparedStatement stmt = dbCon.prepareStatement(sql);
									
					stmt.setString(1, device_type);
					stmt.setString(2, location);
					stmt.setString(3, event_type);
					
					String adminInfo = getAdminMailAccount(device_type, uuid);
					String[] email = adminInfo.split("_");
						
					ResultSet rs = stmt.executeQuery();
						
					while(rs.next()) {
						
						System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
						System.setProperty("jsse.enableSNIExtension", "false");
						  
				        Body = String.join(
				        		System.getProperty("line.separator"),
				        		"<table style=\"border:1px solid #f5f5f5;margin:auto;width:576px;border-collapse:collapse;background-color:white;color:rgba(0,0,0,0.66)\">",
				        		"<tbody><tr style=\"background-color:#e21837;height:56px;\">",
				        		"<td style=\"padding:0 36px\">",
				        		"<img src=\"http://192.168.102.111:8080/style/images/common/header_logo.png\" alt=\"NCC-HCloud\" style=\"height:20px;margin-right:10px;vertical-align:middle\" />",
				        		"<span style=\"font:21px;color:white;vertical-align:middle;\">통합 클라우드 플랫폼 서비스</span>",
				        		"</td>",
				        		"</tr>",
				        		"<tr style=\"height:250px;vertical-align:top\">",
				        		"<td style=\"padding:18px 36px;\">",
				        		"<p></p>",
				        		"<h2 style=\"font-size:21px;font-weight:normal;margin-bottom:25px;\">" + mailTitle + "</h2>",
				        		"<p></p>",
				        		"<p style=\"margin-top:14px\"></p>",
				        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">장치 타입</h3>",
				        		"<span style=\"font-size:18px;\">"+ device_type +"</span>",
				        		"<p></p>",
				        		"<p style=\"margin-top:14px\"></p>",
				        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">발생 위치</h3>",
				        		"<span style=\"font-size:18px;\">" + location + "</span>",
				        		"<p></p>",
				        		"<p style=\"margin-top:14px\"></p>",
				        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">장애 등급</h3>",
				        		"<span style=\"font-size:18px;\">" + rs.getString("CODE_NM") + "</span>",
				        		"<p></p>",
				        		"<p style=\"margin-top:14px\"></p>",
				        		"<h3 style=\"font-size:13px;font-weight:normal;margin-bottom:4px;\">메시지</h3>",
				        		"<span style=\"font-size:18px;\">" + event_msg + "</span>",
				        		"<p></p>",
				        		/*"<p style=\"margin-top:24px;margin-bottom:16px;\">",
				        		"<a href=\"#\" style=\"background-color:#e21837;text-decoration:none;color:white;font-size:13px;padding:0.5em 1em;\" target=\"_blank\">버튼명</a>",
				        		"</p>",*/
				        		"</td>",
				        		"</tr>",
				        		"<tr style=\"background-color:#fafafa;height:57px;\">",
				        		"<td style=\"padding:0 36px;font-size:12px;\">\r\n" + 
				        		"          COPYRIGHT (C) 2020 나무기술 CO., LTD. ALL RIGHT RESERVED.<br />\r\n" + 
				        		"		  서울특별시 강남구 삼성로 531 고운빌딩 T 02-3288-7900 | F 02-3288-8110<br />\r\n" + 
				        		/*"		  If you don't want this type of information or e-mail, please <a href=\"https://www.bizmailer.co.kr/bizsmart/action/openCheck.do?method=deny&amp;sn=27&amp;mk=14810069414996T7uhA2&amp;lk=1623658900746&amp;sk=27_1623658900746_16141306730300000098_D8BA4951916A8F00F281&amp;dk=D8BA4951916A8F00F281&amp;ck=16141306730300000098&amp;ce=hyeongsoo.kim@namutech.co.kr&amp;la=kr\" target=\"_blank\" data-saferedirecturl=\"https://www.google.com/url?q=https://www.bizmailer.co.kr/bizsmart/action/openCheck.do?method%3Ddeny%26sn%3D27%26mk%3D14810069414996T7uhA2%26lk%3D1623658900746%26sk%3D27_1623658900746_16141306730300000098_D8BA4951916A8F00F281%26dk%3DD8BA4951916A8F00F281%26ck%3D16141306730300000098%26ce%3Dhyeongsoo.kim@namutech.co.kr%26la%3Dkr&amp;source=gmail&amp;ust=1623888208881000&amp;usg=AFQjCNH1uBinB5F7zWR9kHSqUpw6w3Q-WA\" style=\"color:#e21837;\">[click here]</a>\r\n" +*/ 
				        		"      </td>",
				        		"</tr>\r\n" + 
				        		"  </tbody>\r\n" + 
				        		"</table>\r\n" + 
				        		"</html>"
				        );
				        
						if(rs.getString("MAIL_SEND_YN").toUpperCase().equals("Y")) {
								
								// SMTP 서버 정보를 설정한다.
						        Properties prop = new Properties();
						        prop.put("mail.transport.protocol", "smtp");
						        prop.put("mail.smtp.starttls.enable", "true");
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
						            message.setSubject("관리자용 장애 알람 메일입니다."); //메일 제목을 입력
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
							
						java.util.Date utilDate = new java.util.Date();
						java.sql.Timestamp sqlDate = new java.sql.Timestamp(utilDate.getTime());
						
						if(rs.getString("MAIL_SEND_YN").toUpperCase().equals("Y"))
							sqlDate = new java.sql.Timestamp(utilDate.getTime());
						else
							sqlDate = null;
						
						sql = "INSERT INTO TB_MAIL_SEND_LOG (USER_NAME, EMAIL_ADDR, MAIL_TITLE, MAIL_MESSAGE, REGISTER_DATE ,SEND_DATE, SEND_YN) VALUES(?, ?, ?, ?, NOW(), ?, ?)";
						stmt = dbCon.prepareStatement(sql);
						stmt.setString(1, email[0]);
						stmt.setString(2, email[1]);
						stmt.setString(3, mailTitle);
						stmt.setString(4, Body);
						stmt.setTimestamp(5, sqlDate);
						stmt.setString(6, rs.getString("MAIL_SEND_YN").toUpperCase());							
						stmt.executeUpdate();
					}
					
					rs.close();
					stmt.close();
					dbCon.close();
				
			}catch (SQLException ex){
		        throw new RuntimeException(ex);
			}	
		}

	public String getAdminMailAccount(String device_type, String uuid) throws SQLException, ClassNotFoundException {
		
		String sql = " SELECT  USER_NM, EMAIL_ADRES " +
		             " FROM  LETTNEMPLYRINFO " +
				     " WHERE AUTHOR_CD='ROLE_HCLOUD'" + 
					 " AND CALL_ORDER > 0 " + 
				     " ORDER BY CALL_ORDER ASC " + 
				     " LIMIT 1 ";
		
        Connection con = null;
        PreparedStatement pstmt = null;
        
        String ret = ""; // problem here
		try {
			
			Class.forName("org.mariadb.jdbc.Driver");
        	con = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
        	
        	pstmt = con.prepareStatement(sql);
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
