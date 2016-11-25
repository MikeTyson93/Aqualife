package aqua.blatt1.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import aqua.blatt1.client.TankModel.tokenRunner;
import aqua.blatt1.common.FishModel;

@SuppressWarnings("serial")
public class TankView extends JPanel implements Observer {
	private final TankModel tankModel;
	private final FishView fishView;
	private final Runnable repaintRunnable;
	int i = 0;
	private int globalSnapshot = -1;
	private boolean globalFlag = false;
	
	public TankView(final TankModel tankModel) {
		this.tankModel = tankModel;
		fishView = new FishView();

		repaintRunnable = new Runnable() {
			@Override
			public void run() {
				repaint();
			}
		};

		setPreferredSize(new Dimension(TankModel.WIDTH, TankModel.HEIGHT));
		setBackground(new Color(175, 200, 235));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				tankModel.newFish(e.getX(), e.getY());
			}
		});
	}

	@SuppressWarnings("unused")
	private void drawBorders(Graphics2D g2d) {
		g2d.drawLine(0, 0, 0, TankModel.HEIGHT);
		g2d.drawLine(TankModel.WIDTH - 1, 0, TankModel.WIDTH - 1, TankModel.HEIGHT);
	}

	private void doDrawing(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		if (tankModel.initiator && tankModel.getGlobalSnapshotFinish()){
			if (i == 0){
			i += 1;
			showGlobalSnapshot();
			}
		}
		for (FishModel fishModel : tankModel) {
			g2d.drawImage(fishView.getImage(fishModel), fishModel.getX(), fishModel.getY(), null);
			g2d.drawString(fishModel.getId(), fishModel.getX(), fishModel.getY());
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
		if (!tankModel.hasToken()){
			drawBorders((Graphics2D) g);
		}
		
	}

	@Override
	public void update(Observable o, Object arg) {
		SwingUtilities.invokeLater(repaintRunnable);
	}
	
	public void showGlobalSnapshot(){
			globalSnapshot = tankModel.snapToken.get_global_snapshot();
			JOptionPane.showMessageDialog(this, globalSnapshot);
	}
}
