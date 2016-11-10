package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NeighborUpdate implements Serializable{
	private InetSocketAddress left;
	private InetSocketAddress right;
	
	public NeighborUpdate(InetSocketAddress left, InetSocketAddress right){
		this.left = left;
		this.right = right;
	}
	
	public InetSocketAddress getLeftNeighbor(){
		return this.left;
	}
	
	public InetSocketAddress getRightNeighbor(){
		return this.right;
	}
	
}
