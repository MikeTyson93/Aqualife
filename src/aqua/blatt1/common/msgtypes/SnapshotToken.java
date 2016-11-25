package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

import aqua.blatt1.client.TankView;

@SuppressWarnings("serial")
public class SnapshotToken implements Serializable{
	int global_snapshot = 0;

	public void count_global_snapshot(int snapshot){
		global_snapshot += snapshot;
	}
	
	public int get_global_snapshot(){
		return global_snapshot;
	}
}



