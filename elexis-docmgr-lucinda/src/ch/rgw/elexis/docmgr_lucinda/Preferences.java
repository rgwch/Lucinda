/*******************************************************************************
 * Copyright (c) 2016 by G. Weirich
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *
 * Contributors:
 * G. Weirich - initial implementation
 *********************************************************************************/

package ch.rgw.elexis.docmgr_lucinda;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.core.data.activator.CoreHub;
import ch.rgw.io.Settings;
import ch.rgw.tools.net.NetTool;

public class Preferences {
	static final Settings cfg = CoreHub.localCfg;
	private static final String BASE = "ch.rgw.docmgr-lucinda.";
	public static final String NETWORK = BASE + "network";
	public static final String INCLUDE_KONS = BASE + "withKons";
	public static final String INCLUDE_OMNI = BASE + "withOmni";
	public static final String LASTSCAN_OMNI = BASE + "omniLast";
	public static final String LASTSCAN_KONS = BASE + "konsLast";
	public static final String MSG = "ch.rgw.lucinda";

	public List<String> getNetworks() {
		ArrayList<String> ret = new ArrayList<String>();
		for (String ip : NetTool.IPs) {
			if (ip.split(".").length == 4) {
				ret.add(ip);
			}
		}
		return ret;
	}

	public static String get(final String key, final String def) {
		return cfg.get(key, def);
	}
	
	public static void set(final String key, final String value){
		cfg.set(key, value);
	}
}
