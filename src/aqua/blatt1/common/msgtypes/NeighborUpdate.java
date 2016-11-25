package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public class NeighborUpdate implements Serializable{
	private InetSocketAddress left;
	private InetSocketAddress right;
	private InetSocketAddress myself;
	public NeighborUpdate(InetSocketAddress myself, InetSocketAddress left, InetSocketAddress right){
		this.myself = myself;
		this.left = left;
		this.right = right;
	}
	
	public InetSocketAddress getMySelf(){
		return this.myself;
	}
	
	public InetSocketAddress getLeftNeighbor(){
		return this.left;
	}
	
	public InetSocketAddress getRightNeighbor(){
		return this.right;
	}
	
}
