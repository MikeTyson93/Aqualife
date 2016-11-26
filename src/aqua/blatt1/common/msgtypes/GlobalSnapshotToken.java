package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class GlobalSnapshotToken implements Serializable {
	int global_state = 0;
	
	public void add_local_state(int localstate){
		this.global_state = this.global_state + localstate;
	}
	
	public int get_global_state(){
		return global_state;
	}
}
