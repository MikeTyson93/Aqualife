package aqua.blatt1.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotToken;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected InetSocketAddress rightNeighbor;
	protected InetSocketAddress leftNeighbor;
	InetSocketAddress myself;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected boolean globalSnapshot = false;
	protected boolean token;
	protected Timer timer;
	public SnapshotToken snapToken;
	public boolean completed_snapshot = false;
	protected boolean initiator = false;
	int local_state = -1;
	public enum State {
		IDLE, LEFT, RIGHT, BOTH;
	}
	State recordState = State.IDLE;
	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		Direction direction = fish.getDirection();
		//Fisch schwimmt nach links, kommt vom rechten Nachbarn
		if (direction.getVector() == -1){
			if (recordState == State.RIGHT || recordState == State.BOTH){
				local_state++;
			}
		}
		// Fisch schwimmt nach rechts, kommt von linken Nachbarn
		if (direction.getVector() == +1){
			if (recordState == State.LEFT || recordState == State.BOTH){
				local_state++;
			}
		}
		fish.setToStart();
		fishies.add(fish);
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()){
				if (this.token == true){
					if (fish.getDirection().getVector() == -1){
						forwarder.handOff(fish, leftNeighbor);
					} else {
						forwarder.handOff(fish, rightNeighbor);
					}
				} else {
					fish.reverse();
					
				}
			}
			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public void receiveToken(){
		this.token = true;
		timer = new Timer();
		TimerTask task = new TimerTask(){
			@Override
			public void run() {
				token = false;
				forwarder.sendToken(leftNeighbor);
			}
		};
		timer.schedule(task, 2000);
	}
	
	public boolean hasToken(){
		return this.token;
	}
	
	public synchronized void finish() {
		forwarder.deregister(id);
	}
	
	public void setLeftNeighbor(InetSocketAddress leftNeighbor){
		this.leftNeighbor = leftNeighbor;
	}

	public void setRightNeighbor(InetSocketAddress rightNeighbor){
		this.rightNeighbor = rightNeighbor;
	}
	
	public void setMySelf(InetSocketAddress myself){
		this.myself = myself;
	}
	
	public void initiateSnapshot(boolean init, SnapshotToken snapToken){
		local_state = fishies.size();
		recordState = State.BOTH;
		forwarder.sendMarker(leftNeighbor, rightNeighbor);
		this.initiator = init;
		this.snapToken = snapToken;
		forwarder.sendSnapToken(leftNeighbor, snapToken);
		snapToken = null;
	}
	
	public void receiveSnapToken(SnapshotToken snapToken){
		this.snapToken = snapToken;
		if (!initiator){
			ExecutorService Executor = Executors.newFixedThreadPool(1);
			Executor.execute(new tokenRunner());
			while (true){
				if (this.snapToken == null){
					Executor.shutdown();
				}
			}
			
		} else {
			snapToken.count_global_snapshot(local_state);
			globalSnapshot = true;
		}
	}
	
	public void getLocalSnapshot(InetSocketAddress neighbor){
		if (recordState == State.BOTH){
			this.recordState = State.IDLE;
			completed_snapshot = true;
			//forwarder.sendMarker(this.leftNeighbor, this.rightNeighbor);
			return;
		}
		if (!completed_snapshot){
			if (neighbor == leftNeighbor){
				if (recordState == State.IDLE){
					this.local_state = fishies.size();
					this.recordState = State.RIGHT;
					forwarder.sendMarker(leftNeighbor, rightNeighbor);
				} else {
					this.recordState = State.BOTH;
				}
			}
		
			if (neighbor == rightNeighbor){
				if (recordState == State.IDLE){
					this.local_state = fishies.size();
					this.recordState = State.LEFT;
					forwarder.sendMarker(leftNeighbor, rightNeighbor);
				} else {
					this.recordState = State.BOTH;
				}	
			}
		}
	}
	
	public State get_record_state(){
		return this.recordState;
	}
	
	public class tokenRunner implements Runnable{
		@Override
		public void run() {
			while(true){
				if (completed_snapshot){
					snapToken.count_global_snapshot(local_state);
					forwarder.sendSnapToken(leftNeighbor, snapToken);
					snapToken = null;
					break;
				} else {
					continue;
				}
				
			}
			
		}
		
	}
}