package aqua.blatt1.broker;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.Properties;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import aqua.blatt1.broker.ClientCollection;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt2.broker.PoisonPill;

public class Broker{
	
	public class ShowMessageDialog implements Runnable {
		@Override
		public void run() {

			JFrame frame = new JFrame("Message");
	    
			JOptionPane.showMessageDialog(frame,
					"Press OK Button to stop server");
			done = true;
		}
	}
	
	
	public class BrokerTask implements Runnable{
		Serializable payload;
		InetSocketAddress sender;
				
		public BrokerTask(Serializable payload, InetSocketAddress sender){
			this.payload = payload;
			this.sender = sender;
		}

		@Override
		public void run() {
			if (payload instanceof DeregisterRequest){
				deregister(sender);
			} else if (payload instanceof HandoffRequest){
				handoffFish(payload, sender);
			} else if (payload instanceof RegisterRequest){
				RegisterResponse response = register(sender);
			} else if (payload instanceof PoisonPill){
				Executor.shutdown();
				System.exit(0);
			}
		}
		
		public RegisterResponse register(InetSocketAddress sender){
			String client_id = "Tank" + id;
			id++;
			lock.writeLock().lock();
			clients.add(client_id, sender);
			lock.writeLock().unlock();
			RegisterResponse response = new RegisterResponse(client_id);
			return response;
		}
		
		public void deregister(InetSocketAddress sender){
			int index_of_client = clients.indexOf(sender);
			lock.writeLock().lock();
			clients.remove(index_of_client);
			lock.writeLock().unlock();
		}
		
		public void handoffFish(Serializable payload, InetSocketAddress sender) {
			int index_of_client = clients.indexOf(sender);
			FishModel fish = ((HandoffRequest) payload).getFish();
			Direction direction = fish.getDirection();
			lock.readLock().lock();
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
			lock.readLock().unlock();
		}
		
	}
	static Endpoint endpoint;
	static ClientCollection<InetSocketAddress> clients;
	static final int port = Properties.PORT;
	static int id = 1;
	ReadWriteLock lock = new ReentrantReadWriteLock();
	boolean done = false;
	ExecutorService Executor = Executors.newFixedThreadPool(4);
	
	public static void main(String [] args){
		
		endpoint = new Endpoint(port);
		clients = new ClientCollection<>();
		
		Broker broker = new Broker();
		broker.broker();
	}
	
	public void broker(){
		Executor.execute(new ShowMessageDialog());
		while(!done){
			Message message = endpoint.blockingReceive();
			Serializable payload = message.getPayload();
			InetSocketAddress sender = message.getSender();
			Executor.execute(new BrokerTask(payload, sender));
		}
		Executor.shutdown();
		System.exit(0);
	}
	
}
