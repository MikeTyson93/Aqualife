package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.GlobalSnapshotToken;
import aqua.blatt1.common.msgtypes.Token;

public class TankModel extends Observable implements Iterable<FishModel> {
	boolean initiator = false;
	int result = 0;
	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	ExecutorService Executor = Executors.newSingleThreadExecutor();
	public boolean snapshotFinish = false;
	protected InetSocketAddress rightNeighbor;
	protected InetSocketAddress leftNeighbor;
	boolean local_snapshot_completed = false;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected boolean token;
	protected Timer timer;
	int local_state = 0;
	private enum states{
		BOTH, IDLE, RIGHT, LEFT
	}
	private states record_state = states.IDLE;
	
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
		if (direction.getVector() == -1){
			if (record_state == states.RIGHT || record_state == states.BOTH)
				local_state++;
		} else {
			if (record_state == states.LEFT || record_state == states.BOTH)
				local_state++;
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
	
	public void initiate_snapshot(){
		this.local_state = fishies.size();
		this.record_state = states.BOTH;
		forwarder.sendSnapshotMarker(leftNeighbor);
		forwarder.sendSnapshotMarker(rightNeighbor);
		forwarder.sendGlobalSnapshotToken(leftNeighbor, new GlobalSnapshotToken());
		initiator = true;
		snapshotFinish = false;
		local_snapshot_completed = false;
	}
	
	public void receiveSnapshotMarker(InetSocketAddress sender){
		switch(record_state){
			case IDLE :
				local_state = fishies.size();
				if (compare(sender, leftNeighbor) == 0)
					record_state = states.RIGHT;
				if (compare(sender, rightNeighbor) == 0)
					record_state = states.LEFT;
				this.local_state = fishies.size();
				forwarder.sendSnapshotMarker(leftNeighbor);
				forwarder.sendSnapshotMarker(rightNeighbor);
				break;
			case LEFT:
				if (compare(sender, leftNeighbor) == 0)
					record_state = states.IDLE;
				if (compare(sender, rightNeighbor) == 0)
					record_state = states.BOTH;
					local_snapshot_completed = true;
				break;
			case RIGHT:
				if (compare(sender, rightNeighbor) == 0)
					record_state = states.IDLE;
				if (compare(sender, leftNeighbor) == 0)
					record_state = states.BOTH;
					local_snapshot_completed = true;
				break;
			case BOTH:
				local_snapshot_completed = true;
				break;
		default:
			break;
		}
	}
	
	public void receiveGlobalSnapshotToken(GlobalSnapshotToken gst){
		Executor.execute(new wait_for_local_snapshot(gst));
		if (initiator){
			snapshotFinish = true;
			result = gst.get_global_state();
		}
	}
	
	public int compare(InetSocketAddress o1, InetSocketAddress o2)
    {
        return o1.toString().compareTo(o2.toString());
    }
	
	public class wait_for_local_snapshot implements Runnable {
		GlobalSnapshotToken gst;
		public wait_for_local_snapshot(GlobalSnapshotToken gst) {
			this.gst = gst;
		}
		@Override
		public void run() {
			while(!local_snapshot_completed){
				//wait for local snapshot
			}
			this.gst.add_local_state(local_state);
			if (!initiator)
				forwarder.sendGlobalSnapshotToken(leftNeighbor, gst);
			return;
		}
	}
}