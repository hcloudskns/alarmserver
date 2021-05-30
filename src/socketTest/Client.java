

public class Client {
    private String ipAddress;
    private String port;
    private java.net.Socket socket = null;

    public Client ( String ipAddress,String port, java.net.Socket socket)
    {
       this.port = port;
       this.ipAddress = ipAddress;
       this.socket = socket;
    }
    
    public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public java.net.Socket getSocket()
    {
        return this.socket;
    }
}
