package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class LocationRequest implements Serializable {
	private final String fish_id;

	public LocationRequest(String fish_id) {
		this.fish_id = fish_id;
	}

	public String getId() {
		return fish_id;
	}

}
