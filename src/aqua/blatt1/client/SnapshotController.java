package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.client.TankModel;
import aqua.blatt1.client.TankView;
import aqua.blatt1.common.msgtypes.SnapshotToken;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
	private final Component parent;
	TankModel tankModel;
	TankView tankView;

	public SnapshotController(Component parent, TankModel tankModel, TankView tankView) {
		this.parent = parent;
		this.tankView = tankView;
		this.tankModel = tankModel;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		
		JOptionPane.showMessageDialog(parent, "Make global Snapshot.");
		tankModel.initiateSnapshot(true, new SnapshotToken());
	}
}
