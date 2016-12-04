package aqua.blatt1.common.msgtypes;

import java.net.InetSocketAddress;
import java.io.Serializable;

@SuppressWarnings("serial")
public class NameResolutionResponse implements Serializable{
	InetSocketAddress tank;
	String requestId;
	public NameResolutionResponse(InetSocketAddress tank, String requestId){
		this.tank = tank;
		this.requestId = requestId;
	}
	
	public InetSocketAddress getRequestAddress(){
		return this.tank;
	}
	
	public String getRequestId(){
		return this.requestId;
	}
}
