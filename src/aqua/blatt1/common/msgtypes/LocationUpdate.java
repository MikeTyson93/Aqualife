package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class LocationUpdate implements Serializable {
	private final String fish_id;
	private InetSocketAddress location;

	public LocationUpdate(String fish_id) {
		this.fish_id = fish_id;
	}

	public String getId() {
		return fish_id;
	}

}
