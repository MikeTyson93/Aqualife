package aqua.blatt1.client;

import java.net.InetSocketAddress;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.GlobalSnapshotToken;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.LocationRequest;
import aqua.blatt1.common.msgtypes.LocationUpdate;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.Token;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress neighbor) {
			endpoint.send(neighbor, new HandoffRequest(fish));
		}
		
		public void sendToken(InetSocketAddress left){
			endpoint.send(left, new Token());
		}
		
		public void sendSnapshotMarker(InetSocketAddress socket){
			endpoint.send(socket, new SnapshotMarker());
		}
		
		public void sendGlobalSnapshotToken(InetSocketAddress socket, GlobalSnapshotToken gst){
			endpoint.send(socket, gst);
		}
		
		public void sendLocationRequest(InetSocketAddress socket, String fish_id){
			endpoint.send(socket, new LocationRequest(fish_id));
		}
		
		public void sendNameResolutionRequest(String tankId, String fishId){
			endpoint.send(this.broker, new NameResolutionRequest(tankId, fishId));
		}
	
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());
				
				if (msg.getPayload() instanceof NeighborUpdate){
					InetSocketAddress left = ((NeighborUpdate) msg.getPayload()).getLeftNeighbor();
					InetSocketAddress right = ((NeighborUpdate) msg.getPayload()).getRightNeighbor();
					if (left != null){
						tankModel.setLeftNeighbor(left);
					}
					if (right != null){
						tankModel.setRightNeighbor(right);
					}
				}
				
				if (msg.getPayload() instanceof Token){
					tankModel.receiveToken();
				}
				
				if (msg.getPayload() instanceof SnapshotMarker){
					tankModel.receiveSnapshotMarker(msg.getSender());
				}
				if (msg.getPayload() instanceof GlobalSnapshotToken){
					tankModel.receiveGlobalSnapshotToken((GlobalSnapshotToken) msg.getPayload());
				}
				if (msg.getPayload() instanceof LocationRequest){
					tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getId());
				}
				if (msg.getPayload() instanceof NameResolutionResponse){
					InetSocketAddress homeTank = ((NameResolutionResponse) msg.getPayload()).getRequestAddress();
					String fishId = ((NameResolutionResponse) msg.getPayload()).getRequestId();
					endpoint.send(homeTank, new LocationUpdate(fishId));
				}
				if (msg.getPayload() instanceof LocationUpdate){
					InetSocketAddress sender = msg.getSender();
					String fishId = ((LocationUpdate) msg.getPayload()).getId();
					tankModel.updateLocation(sender, fishId);
				}
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
