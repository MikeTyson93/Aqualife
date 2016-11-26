package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
	private final Component parent;
	private TankModel tankModel;
	private TankView tankView;
	private ExecutorService Executor = Executors.newSingleThreadExecutor();
	public SnapshotController(Component parent, TankModel tankModel, TankView tankView) {
		this.parent = parent;
		this.tankModel = tankModel;
		this.tankView = tankView;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JOptionPane.showMessageDialog(parent, "Make a global Snapshot.");
		tankModel.initiate_snapshot();
		Executor.execute(new wait_for_global_snapshot(tankModel));
	}
	
	public class wait_for_global_snapshot implements Runnable {
		TankModel tank;
		public wait_for_global_snapshot(TankModel tankModel) {
			this.tank = tankModel;
		}
		@Override
		public void run() {
			while(!this.tank.snapshotFinish){
				try {
					TimeUnit.MILLISECONDS.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			int result = this.tank.result;
			tankView.show_snapshot(result);
			tankModel.initiator = false;
		}
	}
}
