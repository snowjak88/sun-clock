/**
 * 
 */
package org.snowjak.sunclock;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.snowjak.sunclock.Options.DefinedOption;
import org.snowjak.sunclock.projection.Projection;

/**
 * @author snowjak88
 *
 */
public class MenuBar extends JMenuBar {
	
	private static final long serialVersionUID = -3337450323034480745L;
	
	public MenuBar() {
		
		super();
		
		final JMenu fileMenu, projectionsMenu;
		final ActionListener menuActionListener = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (e.getActionCommand().equals("quit"))
					System.exit(0);
				
				if (e.getActionCommand().startsWith("projection-")) {
					final String projectionName = e.getActionCommand().replaceFirst("projection-", "");
					try {
						final Projection selectedProjection = Projection.valueOf(projectionName);
						Options.setValue(DefinedOption.PROJECTION, selectedProjection);
					} catch (IllegalArgumentException ex) {
						
					}
				}
			}
		};
		
		fileMenu = new JMenu("File");
		final JMenuItem quitItem = fileMenu.add("Quit");
		quitItem.setActionCommand("quit");
		quitItem.addActionListener(menuActionListener);
		
		projectionsMenu = new JMenu("Projections");
		for (Projection projection : Projection.values()) {
			final JMenuItem projectionItem = projectionsMenu.add(projection.getName());
			projectionItem.setActionCommand("projection-" + projection.name());
			projectionItem.addActionListener(menuActionListener);
		}
		
		add(fileMenu);
		add(projectionsMenu);
	}
	
}
