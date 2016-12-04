package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class NameResolutionRequest implements Serializable {
	String tankId;
	String requestId;
	public NameResolutionRequest(String tankId, String requestId){
		this.tankId = tankId;
		this.requestId = requestId;
	}
	
	public String getTankId(){
		return this.tankId;
	}
	
	public String getRequestId(){
		return this.requestId;
	}
}
