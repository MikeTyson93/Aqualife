package aqua.blatt1.client;

import java.net.InetSocketAddress;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.GlobalSnapshotReceived;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.SnapshotToken;
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
		
		public void sendMarker(InetSocketAddress left, InetSocketAddress right){
			endpoint.send(left, new SnapshotMarker());
			endpoint.send(right, new SnapshotMarker());
		}
		
		public void sendSnapToken(InetSocketAddress left, SnapshotToken snap){
			endpoint.send(left, snap);
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
					InetSocketAddress myself = ((NeighborUpdate) msg.getPayload()).getMySelf();
					
					if (myself != null){
						tankModel.setMySelf(myself);
					}
					
					if (left != null){
						tankModel.setLeftNeighbor(left);
					}
					if (right != null){
						tankModel.setRightNeighbor(right);
					}
				}
				
				if (msg.getPayload() instanceof SnapshotMarker){
					tankModel.getLocalSnapshot(msg.getSender());
				}
				
				if (msg.getPayload() instanceof Token){
					tankModel.receiveToken();
				}
				
				if (msg.getPayload() instanceof SnapshotToken){
					tankModel.receiveSnapToken((SnapshotToken) msg.getPayload());
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
