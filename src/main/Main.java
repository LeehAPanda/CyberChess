package main;

import javax.swing.JFrame;

public class Main {
	public static void main (String[] args) {
		JFrame window = new JFrame("CyberChess");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
		
		// adding gamepanel to window
		GamePanel gp = new GamePanel();
		window.add(gp);
		window.pack();
		
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		
		gp.initGame();
	}
}
