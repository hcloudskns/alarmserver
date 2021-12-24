
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class AlarmClient {
	public static void main(String args[]) {
		try {
			
            // 소켓을 생성하여 연결을 요청한다.
			Socket socket;
			
			if(DBConst.mode.equals("TEST")) {
				socket = new Socket(DBConst.testAlarmServer, DBConst.testAlarmServerPort);
				System.out.println("테스트 알람 서버에 연결되었습니다.");
			}else {
				socket = new Socket(DBConst.alarmServer, DBConst.alarmServerPort);
				System.out.println("실제 알람 서버에 연결되었습니다.");
			}
			
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
		String strVmUuid ="",  strVmName="" ; 
		
		Statement stmt = null; 
		try {
			Class.forName("org.mariadb.jdbc.Driver");
			Connection conn = DriverManager.getConnection(DBConst.DBConnectionStr,DBConst.userName, DBConst.userPwd);
			
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(" SELECT TV.VM_UUID as vmUuid, TV.VM_NAME as vmName "  + 
					                         " FROM TB_VM TV  INNER JOIN TB_VM_DETAIL TVD " + 
					                         " ON TV.VM_UUID = TVD.VM_UUID " + 
					                         " WHERE TVD.SYSTEM_VM_YN='N' " + 
					                         " LIMIT 1");
			
			while(rs.next()) {
				strVmUuid = rs.getString("vmUuid");
				strVmName = rs.getString("vmName"); 
			}
		} catch (ClassNotFoundException e1) {
			
			
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(out!=null) {
			try {
				byte[] buffer ;
				
				
				/*temp="0000006STATUS";
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);
				//byte[] buffer = new byte[354];
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);
				*/
				
				/* TTA 3번 패턴 */
				
				/*SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar cal1 = Calendar.getInstance();
				String strDateTTA3 = sdf1.format(cal1.getTime());

				temp = "0000346{\"vm_pattern_event_info\":[{\"event_type\":\"CR\", \"ip_address\":\"192.168.1.5\", \"device_type\":\"VM\", \"uuid\":\"" + strVmUuid + "\", \"device_name\":\"" + strVmName +"\", \"location\":\"PATTERN\", \"usage\":\"-1\", \"event_time\":\"2020-08-03 16:04:44\", \"event_code\":\"PATTERN0001\", \"event_msg\":\"/tmp/error.log 파일에서 ERROR 패턴이 발생하였습니다.\",  \"analysis_log_time\":\"" + strDateTTA3 + "\"}]}";	
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);
				//byte[] buffer = new byte[354];
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);*/
				
				/* TTA 3번 패턴 */
				
				
				
				/*TTA 4번 자원 사용량 상위*/
				
				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar cal2 = Calendar.getInstance();
				String strDateTTA4 = sdf2.format(cal2.getTime());
				
				temp = "0000346{\"vm_cpu_event_info\":[{\"event_type\":\"CA\", \"ip_address\":\"192.168.101.13\", \"device_type\":\"VM\", \"uuid\":\"" + strVmUuid+ "\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"usage\":\"50.38\", \"event_time\":\"2020-11-07 16:24:13\", \"event_code\":\"CPU0001\", \"event_msg\":\"임계치 40,000이상 3회 연속 초과하였습니다.\",  \"analysis_log_time\":\"" + strDateTTA4  + "\"}]}";	
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);			
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				//sleep(5000);
				
				/*
				temp = "0000346{\"vm_cpu_report\":["
						+ "{\"pid\":\"5780\", \"process\":\"loop_100\", \"cpu_usage\":\"93.8\", \"mem_usage\":\"0.8\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid +"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"1235\", \"process\":\"cloud_age\", \"cpu_usage\":\"6.2\", \"mem_usage\":\"0.5\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid +"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"1\",	 \"process\":\"systemd\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.2\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid +"\", \"device_name\":\""+strVmName +"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"2\", \"process\":\"kthread\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"4\", \"process\":\"kworker\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"5\", \"process\":\"kworkeru+\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"6\", \"process\":\"ksoftirqd\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"7\", \"process\":\"migration+\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"8\", \"process\":\"rcu_bh\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "{\"pid\":\"9\", \"process\":\"rcu_sched\", \"cpu_usage\":\"0.0\", \"mem_usage\":\"0.0\",\"event_type\":\"IM\",\"ip_address\":\"192.168.101.13\", \"device_type\":\"XS\",  \"uuid\":\""+strVmUuid+"\", \"device_name\":\""+strVmName+"\", \"location\":\"CPU\", \"event_level\":\"CA\"}"
						+ "]}";	
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);
				//byte[] buffer = new byte[354];
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);
				*/
				/*TTA 4번 자원 사용량 상위 */
				
				/*TTA 7번 자원 사용량 상위*/	
				/*SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar cal3 = Calendar.getInstance();
				String strDateTTA7 = sdf3.format(cal3.getTime());
				
				//temp = "0000346{\"vm_power_event_info\":[{\"event_type\":\"CA\", \"ip_address\":\"192.168.101.12\", \"device_type\":\"VM\", \"uuid\":\""+strVmUuid+"\", \"device_name\":\"TEST_20210831_001\", \"location\":\"POWERSTATE\", \"usage\":\"50.38\", \"event_time\":\"2020-11-07 16:24:13\", \"event_code\":\"POWER0001\", \"event_msg\":\"VM_TEST_001이  종료 되었습니다.\",  \"analysis_log_time\":\"" + strDateTTA7  + "\"}]}";
				//temp = "0000346{\"vm_power_event_info\":[{\"event_type\":\"CA\", \"ip_address\":\"192.168.101.13\", \"device_type\":\"VM\", \"device_name\":\""+strVmName+"\", \"location\":\"POWERSTATE\", \"usage\":\"50.38\", \"event_time\":\"2020-11-07 16:24:13\", \"event_code\":\"POWER0001\", \"event_msg\":\"VM_TEST_001이  종료 되었습니다.\"}]}";
				 
				temp = "0000346{\"nw_xs_agent_report\":[{\"event_type\":\"CA\", \"ip_address\":\"192.168.101.12\", \"device_type\":\"VM\", \"uuid\":\""+strVmUuid+"\", \"device_name\":\"TEST_20210831_001\", \"location\":\"POWERSTATE\", \"usage\":\"50.38\", \"event_time\":\"2020-11-07 16:24:13\", \"event_code\":\"POWER0001\", \"event_msg\":\"VM_TEST_001이  종료 되었습니다.\",  \"analysis_log_time\":\"" + strDateTTA7  + "\"}]}";
				//0000353{"nw_xs_agent_report":[{"ip_address":"192.s168.101.32", "device_type":"XS", "device_name":"TEST_001", "agent_version":"1.12", "uuid":"3b330e71-7c8f-e2ee-bd40-3ad3bc384e74", "first_time":"2021-09-15 15:41:00.744", "last_time":"2021-09-15 15:43:00.746", "last_status":"Active", "total_check_count":"3", "success_check_count":"3", "fail_check_count":"0"}]}
				
				temp = "0000025{\"nw_xs_agent_report\":[]}";
				
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);			
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(5000);*/
				/*TTA 7번 자원 사용량 상위*/
				
				
				/* nw_status */
				
				//temp =  "0000185{\"nw_status\":[{\"ip_address\":\"192.168.101.118\", \"device_type\":\"VM\", \"device_name\":\"DANIEL_001\", \"last_time\":\"2021-11-22 20:48:16.060\", \"last_latency\":\"1.347\", \"last_status\":\"Active\"}]}";
				
				
				temp =  "0000351{\"nw_status\":[" +    
				"{\"ip_address\":\"192.168.101.118\", \"device_type\":\"VM\", \"device_name\":\"DANIEL_001\", \"last_time\":\"2021-11-22 21:47:31.729\", \"last_latency\":\"1.602\", \"last_status\":\"Active\"}"   +
				",{\"ip_address\":\"192.168.101.63\", \"device_type\":\"VM\", \"device_name\":\"TEST_003\", \"last_time\":\"2021-11-22 21:47:31.729\", \"last_latency\":\"2.365\", \"last_status\":\"Active\"}"+
				"]}";


				
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);			
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(500);
				/* nw_status */
				
				
				/*nw_event_info*/
				temp = "0000232{\"nw_event_info\":[{\"event_type\":\"CR\", \"ip_address\":\"192.168.1.20\", \"device_type\":\"Gateway\", \"device_name\":\"GW_019\", \"location\":\"Latency\", \"usage\":\"-1\", \"event_time\":\"2018-10-18 13:02:16\", \"event_code\":\"NWICMP909\", \"event_msg\":\"네트워크 3회 연속 장애 발생하였습니다\",  \"detail_msg\":\"네트워크 3회 연속 장애 발생하였습니다\"}]}"; 
						
				
				buffer = new byte[temp.getBytes().length];
				Arrays.fill(buffer,(byte)0);			
				buffer = temp.getBytes("UTF-8");
				len = temp.getBytes().length;
				
				out.write(buffer);
				sleep(500);
				/*nw_event_info*/
				
				
				
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


