/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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

import com.sun.jna.Native;
import com.sun.jna.WString;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

/**
 * Contains the Windows specific native functionality. Do not try to instantiate on Linux/MacOSX !
 * @author zsombor
 *
 */
public class NativeWindowsUtils extends GenericSystemUtils implements SystemUtils {
	private static final Logger logger = LoggerFactory.getLogger(NativeWindowsUtils.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	private static final int KEY_READ = 0x20019;
	private static boolean isKerioFirewallInstalled = false;

	private static final AtomicLong disableOSSleepModeCallTime = new AtomicLong(0);
	private static final AtomicLong enableOSSleepModeCallTime = new AtomicLong(0);
	private static final long OSSleepAPICallsTimeout = 40000; // 40 seconds.

	@Override
	public void disableOSSleepMode() {
		// Skip if last method call was less than OSSleepAPICallsTimeout milliseconds ago.
		if (System.currentTimeMillis() - disableOSSleepModeCallTime.get() > OSSleepAPICallsTimeout) {
			disableOSSleepModeCallTime.set(System.currentTimeMillis());
			logger.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_CONTINUOUS);
		}
	}

	@Override
	public void enableOSSleepMode() {
		if (configuration.isPreventsSleep() && System.currentTimeMillis() - enableOSSleepModeCallTime.get() > OSSleepAPICallsTimeout) {
			enableOSSleepModeCallTime.set(System.currentTimeMillis());
			logger.trace("Calling SetThreadExecutionState ES_CONTINUOUS");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
		}
	}

	@Override
	public String getShortPathName(String longPathName) {
		boolean unicodeChars;
		try {
			byte b1[] = longPathName.getBytes("UTF-8");
			byte b2[] = longPathName.getBytes("cp1252");
			unicodeChars = b1.length != b2.length;
		} catch (Exception e) {
			return longPathName;
		}

		if (unicodeChars) {
			try {
				final WString pathName = new WString(longPathName);

				char test[] = new char[2 + pathName.length() * 2];
				int r = Kernel32.INSTANCE.GetShortPathNameW(pathName, test, test.length);
				if (r > 0) {
					logger.debug("Forcing short path name on " + pathName);
					return Native.toString(test);
				} else {
					logger.info("File does not exist? " + pathName);
					return longPathName;
				}

			} catch (Exception e) {
				return longPathName;
			}
		}
		return longPathName;
	}

	public NativeWindowsUtils() {
		super();
		start();
	}

	private void start() {
		final Preferences userRoot = Preferences.userRoot();
		final Preferences systemRoot = Preferences.systemRoot();
		final Class<? extends Preferences> clz = userRoot.getClass();
		try {
			if (clz.getName().endsWith("WindowsPreferences")) {
				final Method openKey = clz.getDeclaredMethod("WindowsRegOpenKey", int.class, byte[].class, int.class);
				openKey.setAccessible(true);

				final Method closeKey = clz.getDeclaredMethod("WindowsRegCloseKey", int.class);
				closeKey.setAccessible(true);

				final Method winRegQueryValue = clz.getDeclaredMethod("WindowsRegQueryValueEx", int.class, byte[].class);
				winRegQueryValue.setAccessible(true);

				byte[] valb;
				String key;

				// Check if VLC is installed
				key = "SOFTWARE\\VideoLAN\\VLC";
				int handles[] = (int[]) openKey.invoke(systemRoot, -2147483646, toCstr(key), KEY_READ);
				if (!(handles.length == 2 && handles[0] != 0 && handles[1] == 0)) {
					key = "SOFTWARE\\Wow6432Node\\VideoLAN\\VLC";
					handles = (int[]) openKey.invoke(systemRoot, -2147483646, toCstr(key), KEY_READ);
				}
				if (handles.length == 2 && handles[0] != 0 && handles[1] == 0) {
					valb = (byte[]) winRegQueryValue.invoke(systemRoot, handles[0], toCstr(""));
					vlcp = (valb != null ? new String(valb).trim() : null);
					valb = (byte[]) winRegQueryValue.invoke(systemRoot, handles[0], toCstr("Version"));
					vlcv = (valb != null ? new String(valb).trim() : null);
					closeKey.invoke(systemRoot, handles[0]);
				}

				// Check if Kerio is installed
				key = "SOFTWARE\\Kerio";
				handles = (int[]) openKey.invoke(systemRoot, -2147483646, toCstr(key), KEY_READ);
				if (handles.length == 2 && handles[0] != 0 && handles[1] == 0) {
					isKerioFirewallInstalled = true;
				}
			}
		} catch (Exception e) {
			logger.debug("Caught exception", e);
		}
	}

	@Override
	public boolean isKerioFirewallInstalled() {
		return isKerioFirewallInstalled;
	}

	private static byte[] toCstr(String str) {
		byte[] result = new byte[str.length() + 1];
		for (int i = 0; i < str.length(); i++) {
			result[i] = (byte) str.charAt(i);
		}
		result[str.length()] = 0;
		return result;
	}

	@Override
	public String[] getPingCommand(String hostAddress, int count, int packetSize) {
		return new String[] { "ping", /* count */ "-n" , Integer.toString(count), /* size */ "-l", Integer.toString(packetSize), hostAddress };
	}
}