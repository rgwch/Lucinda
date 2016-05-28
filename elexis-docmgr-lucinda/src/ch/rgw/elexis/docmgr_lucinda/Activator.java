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
import java.util.LinkedList;
import java.util.List;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ch.elexis.core.ui.util.SWTHelper;
import ch.rgw.elexis.docmgr_lucinda.controller.IProgressController;
import ch.rgw.elexis.docmgr_lucinda.model.ConsultationIndexer;
import ch.rgw.elexis.docmgr_lucinda.model.Document;
import ch.rgw.elexis.docmgr_lucinda.model.OmnivoreIndexer;
import ch.rgw.lucinda.Client;
import ch.rgw.lucinda.Handler;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ch.rgw.elexis.docmgr-lucinda"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private Client lucinda;
	private boolean connected;
	private boolean RestAPI = false;
	private boolean BusApi=false;
	private List<Handler> handlers = new ArrayList<>();
	private ConsultationIndexer consultationIndexer = new ConsultationIndexer();
	private OmnivoreIndexer omnivoreIndexer = new OmnivoreIndexer();
	private List<Document> messages = new LinkedList<>();

	private IProgressController progressController;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	public Client getLucinda() {
		return lucinda;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		lucinda = new Client();
		connect();
	}

	public void connect() {
		if(Preferences.get(Preferences.SERVER_ADDR, "")!=""){
			connectRest();
		}else{
			connectBus();
		}
	}

	public void addHandler(Handler handler) {
		handlers.add(handler);
	}

	public void removeHandler(Handler handler) {
		handlers.remove(handler);
	}

	public void connectRest() {
		if (!connected) {
			String server = Preferences.get(Preferences.SERVER_ADDR, "127.0.0.1");
			int port = Integer.parseInt(Preferences.get(Preferences.SERVER_PORT, "2016"));
			lucinda.connect(server, port, result -> {
				switch ((String) result.get("status")) {

				case "connected":
					connected = true;
					RestAPI = true;
					if (Preferences.get(Preferences.INCLUDE_KONS, "0").equals("1")) {
						syncKons(true);
					}
					if (Preferences.get(Preferences.INCLUDE_OMNI, "0").equals("1")) {
						syncOmnivore(true);
					}
			
					break;
				case "disconnected":
					connected=false;
					RestAPI=false;
					break;
				case "failure":
					SWTHelper.showInfo("Lucinda", (String) result.get("message"));
					break;
				case "error":
					SWTHelper.showError("Lucinda", "Lucinda Fehler", "Meldung vom Server: " + result.get("message"));
					break;
			
				default:
					SWTHelper.showError("Lucinda", "Lucinda",
							"Unerwartete Antwort von Lucinda: " + result.get("status") + ", " + result.get("message"));
				}
				for (Handler handler : handlers) {
					handler.signal(result);
				}

			});
		}
	}

	public void connectBus() {
		if (!connected) {
			String prefix = Preferences.get(Preferences.MSG, "ch.rgw.lucinda");
			String network = Preferences.get(Preferences.NETWORK, "");
			lucinda.connect(prefix, network, result -> {
				addMessage(new Document(result));
				switch ((String) result.get("status")) {
				case "connected":
					connected = true;
					BusApi=true;
					if (Preferences.get(Preferences.INCLUDE_KONS, "0").equals("1")) {
						syncKons(true);
					}
					if (Preferences.get(Preferences.INCLUDE_OMNI, "0").equals("1")) {
						syncOmnivore(true);
					}
					break;
				case "REST ok":
					connected = true;
					RestAPI = true;
					break;
				case "disconnected":
					connected = false;
					break;
				case "failure":
					SWTHelper.showInfo("Lucinda", (String) result.get("message"));
					break;
				case "error":
					SWTHelper.showError("Lucinda", "Lucinda Fehler", "Meldung vom Server: " + result.get("message"));
					break;
				default:
					SWTHelper.showError("Lucinda", "Lucinda",
							"Unerwartete Antwort von Lucinda: " + result.get("status") + ", " + result.get("message"));
				}
				for (Handler handler : handlers) {
					handler.signal(result);
				}

			});
		}
	}

	public void disconnect() {
		if (connected) {
			connected = false;
			if (lucinda != null) {
				lucinda.shutDown();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		disconnect();
		plugin = null;
		super.stop(context);
	}

	public void syncKons(boolean doSync) {
		consultationIndexer.setActive(doSync);
		if (doSync) {
			consultationIndexer.start(progressController);
		}
	}

	public void syncOmnivore(boolean doSync) {
		omnivoreIndexer.setActive(doSync);
		if (doSync) {
			omnivoreIndexer.start(progressController);
		}
	}

	public boolean isRestAPI() {
		return RestAPI;
	}

	public boolean isBusAPI(){
		return BusApi;
	}
	public void addMessage(Document message) {
		messages.add(message);
	}

	public void addMessages(List<Document> messages) {
		this.messages.addAll(messages);
	}

	public List<Document> getMessages() {
		return messages;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public void setProgressController(IProgressController controller) {
		progressController = controller;
	}

}
