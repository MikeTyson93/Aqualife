package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.Properties;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import aqua.blatt1.broker.ClientCollection;
import aqua.blatt1.client.TankModel;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;

public class Broker {
	public class BrokerTask implements Runnable{
		Serializable payload;
		InetSocketAddress sender;
		
		
		public BrokerTask(Serializable payload, InetSocketAddress sender){
			this.payload = payload;
			this.sender = sender;
			this.run();
		}

		@Override
		public void run() {
			if (payload instanceof DeregisterRequest){
				deregister(sender);
			} else if (payload instanceof HandoffRequest){
				handoffFish(sender, payload);
			} else if (payload instanceof RegisterRequest){
				RegisterResponse response = register(sender);
			}
		}
	}
	static Endpoint endpoint;
	static ClientCollection clients;
	static final int port = Properties.PORT;
	static int id = 1;
	
	public static void main(String [] args){
		
		endpoint = new Endpoint(port);
		clients = new ClientCollection();
		
		Broker broker = new Broker();
		broker.broker();
	}
	
	public void broker(){
		while(true){
			Message message = endpoint.blockingReceive();
			Serializable payload = message.getPayload();
			InetSocketAddress sender = message.getSender();
			Thread thread = new Thread(new BrokerTask(payload, sender));
			ExecutorService Executor = Executors.newFixedThreadPool(4);
			while (true){
				Executor.execute(new BrokerTask(payload, sender));
			}

		}
	}
	
	public RegisterResponse register(InetSocketAddress sender){
		String client_id = "Tank" + id;
		id++;
		clients.add(client_id, sender);
		RegisterResponse response = new RegisterResponse(client_id);
		return response;
	}
	
	public void deregister(InetSocketAddress sender){
		int index_of_client = clients.indexOf(sender);
		clients.remove(index_of_client);
	}
	
	public void handoffFish(InetSocketAddress sender, Serializable payload){
		int index_of_client = clients.indexOf(sender);
		
		
		FishModel fish = ((HandoffRequest) payload).getFish();
		Direction direction = fish.getDirection();
		if (direction.getVector() == -1){
			InetSocketAddress left_neighbour_of_client = (InetSocketAddress) clients.getLeftNeighorOf(index_of_client);
			if (clients.indexOf(sender) == 0){
				fish.reverse();
				endpoint.send(sender, payload);
			} else {
				endpoint.send(left_neighbour_of_client, payload);
			}
		} else if (direction.getVector() == +1){
			InetSocketAddress right_neighbour_of_client = (InetSocketAddress) clients.getRightNeighorOf(index_of_client);
			if (clients.indexOf(sender) == clients.size() -1){
				fish.reverse();
				endpoint.send(sender, payload);
			} else {
				endpoint.send(right_neighbour_of_client, payload);
			}
		}
	}
}
