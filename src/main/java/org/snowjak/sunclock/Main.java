/**
 * 
 */
package org.snowjak.sunclock;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.FileNotFoundException;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * @author snowjak88
 *
 */
public class Main extends JFrame {
	
	private static final long serialVersionUID = 6084664053217422813L;
	
	public static void main(final String[] args) {
		
		new Main();
		
	}
	
	public Main() throws HeadlessException {
		
		super();
		
		setJMenuBar(new MenuBar());
		
		final MapDisplay mapDisplay = new MapDisplay();
		add(mapDisplay);
		
		setTitle("Sun Clock");
		setMinimumSize(new Dimension(400, 300));
		setExtendedState(MAXIMIZED_BOTH);
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		try {
			Options.readFromProperties();
		} catch (FileNotFoundException e) {
			
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> mapDisplay.dispose()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> Options.writeToProperties()));
	}
}
