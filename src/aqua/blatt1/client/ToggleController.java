package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
	private TankModel tankModel;
	public ToggleController(TankModel tankModel){
		this.tankModel = tankModel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String source = e.getActionCommand();
		
		this.tankModel.locateFishGlobally(source);
		
	}

}
