/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2011 G.Zsombor
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.util.platform;

import com.sun.jna.Platform;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.newgui.LooksFrame;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base implementation for the SystemUtils class for the generic cases.
 * @author zsombor
 *
 */
public class GenericSystemUtils implements SystemUtils {
	private final static Logger logger = LoggerFactory.getLogger(GenericSystemUtils.class);

	protected String vlcp;
	protected String vlcv;

	@Override
	public void disableOSSleepMode() {

	}

	@Override
	public void enableOSSleepMode() {

	}

	@Override
	public String getShortPathName(String longPathName) {
		return longPathName;
	}

	@Override
	public String getDiskLabel(File f) {
		return null;
	}

	@Override
	public boolean isKerioFirewallInstalled() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.pms.util.platform.SystemUtils#getVlcp()
	 */
	@Override
	public String getVlcPath() {
		return vlcp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.pms.util.platform.SystemUtils#getVlcv()
	 */
	@Override
	public String getVlcVersion() {
		return vlcv;
	}

	@Override
	public void browseURI(String uri) {
		try {
			Desktop.getDesktop().browse(new URI(uri));
		} catch (IOException e) {
			logger.trace("Unable to open the given URI: " + uri + ".");
		} catch (URISyntaxException e) {
			logger.trace("Unable to open the given URI: " + uri + ".");
		}
	}

	@Override
	public void addSystemTray(final LooksFrame frame) {

		if (SystemTray.isSupported()) {
			SystemTray tray = SystemTray.getSystemTray();

			Image trayIconImage = resolveTrayIcon();

			PopupMenu popup = new PopupMenu();
			MenuItem defaultItem = new MenuItem(Messages.getString("LooksFrame.5"));
			MenuItem traceItem = new MenuItem(Messages.getString("LooksFrame.6"));

			defaultItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.quit();
				}
			});

			traceItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.setVisible(true);
				}
			});

			popup.add(traceItem);
			popup.add(defaultItem);

			final TrayIcon trayIcon = new TrayIcon(trayIconImage, PropertiesUtil.getProjectProperties().get("project.name") + " " + PMS.getVersion(), popup);

			trayIcon.setImageAutoSize(true);
			trayIcon.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					frame.setVisible(true);
					frame.setFocusable(true);
				}
			});
			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				logger.debug("Caught exception", e);
			}
		}
	}

	/**
	 * Return the platform specific ping command for the given host address,
	 * ping count and packet size.
	 *
	 * @param hostAddress The host address.
	 * @param count The ping count.
	 * @param packetSize The packet size.
	 * @return The ping command.
	 */
	@Override
	public String[] getPingCommand(String hostAddress, int count, int packetSize) {
		return new String[] { "ping", /* count */"-c", Integer.toString(count), /* size */
				"-s", Integer.toString(packetSize), hostAddress };
	}

	/**
	 * Return the proper tray icon for the operating system.
	 * 
	 * @return The tray icon.
	 */
	private Image resolveTrayIcon() {
		String icon = "icon-16.png";

		if (Platform.isMac()) {
			icon = "icon-22.png";
		}
		return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/images/" + icon));
	}
}
