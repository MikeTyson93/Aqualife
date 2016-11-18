package aqua.blatt1.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import aqua.blatt1.client.TankModel;

import javax.swing.JOptionPane;

public class SnapshotController implements ActionListener {
	private final Component parent;
	TankModel tankModel;

	public SnapshotController(Component parent, TankModel tankModel) {
		this.parent = parent;
		this.tankModel = tankModel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		JOptionPane.showMessageDialog(parent, "Make global Snapshot.");
		//tankModel.initiateSnapshot();
	}
}
