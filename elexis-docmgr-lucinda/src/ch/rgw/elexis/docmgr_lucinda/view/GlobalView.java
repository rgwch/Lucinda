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

package ch.rgw.elexis.docmgr_lucinda.view;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.ui.actions.GlobalEventDispatcher;
import ch.elexis.core.ui.actions.IActivationListener;
import ch.elexis.core.ui.actions.RestrictedAction;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.data.Patient;
import ch.rgw.elexis.docmgr_lucinda.Activator;
import ch.rgw.elexis.docmgr_lucinda.Preferences;
import ch.rgw.elexis.docmgr_lucinda.controller.Controller;

public class GlobalView extends ViewPart implements IActivationListener {

	private Controller controller;
	private Action doubleClickAction, filterCurrentPatAction, showInboxAction, showConsAction, showOmnivoreAction;
	private RestrictedAction indexOmnivoreAction, indexKonsAction;

	private final ElexisUiEventListenerImpl eeli_pat = new ElexisUiEventListenerImpl(Patient.class,
			ElexisEvent.EVENT_SELECTED) {

		@Override
		public void run(ElexisEvent ev) {
			controller.changePatient((Patient) ev.getObject());
		}

	};

	public GlobalView() {
		controller = new Controller();
	}

	@Override
	public void createPartControl(Composite parent) {
		makeActions();
		controller.createView(parent);
		contributeToActionBars();
		GlobalEventDispatcher.addActivationListener(this, this);
	}

	public void visible(final boolean mode) {
		controller.reload();
		if (mode) {
			ElexisEventDispatcher.getInstance().addListeners(eeli_pat);
		} else {
			ElexisEventDispatcher.getInstance().removeListeners(eeli_pat);
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(filterCurrentPatAction);
		manager.add(showInboxAction);
		manager.add(showConsAction);
		manager.add(showOmnivoreAction);
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(indexOmnivoreAction);
		manager.add(indexKonsAction);
		manager.add(filterCurrentPatAction);

	}

	private void makeActions() {
		indexOmnivoreAction = new RestrictedAction(AccessControlDefaults.DOCUMENT_CREATE, "Omnivore import",
				Action.AS_CHECK_BOX) {
			{
				setToolTipText("Index all documents from omnivore (this may take a long time!)");
				setImageDescriptor(Images.IMG_DATABASE.getImageDescriptor());
			}

			@Override
			public void doRun() {
				Activator.getDefault().syncOmnivore(this.isChecked());
				Preferences.set(Preferences.INCLUDE_OMNI, isChecked() ? "1" : "0");
			}
		};
		if (Preferences.get(Preferences.INCLUDE_OMNI, "0").equals("1")) {
			indexOmnivoreAction.setChecked(true);
		}

		indexKonsAction = new RestrictedAction(AccessControlDefaults.DOCUMENT_CREATE, "Synchronisiere Kons",
				Action.AS_CHECK_BOX) {
			{
				setToolTipText("Indexiere Konsultationen für Lucinda");
				setImageDescriptor(Images.IMG_GEAR.getImageDescriptor());
			}

			@Override
			public void doRun() {
				Activator.getDefault().syncKons(this.isChecked());
				Preferences.set(Preferences.INCLUDE_KONS, isChecked() ? "1" : "0");
			}
		};
		if (Preferences.get(Preferences.INCLUDE_KONS, "0").equals("1")) {
			indexKonsAction.setChecked(true);
		}

		filterCurrentPatAction = new Action("Aktueller Patient", Action.AS_CHECK_BOX) {
			{
				setToolTipText("Nur Treffer für den aktuellen patienten anzeigen");
				setImageDescriptor(Images.IMG_PERSON.getImageDescriptor());
			}

			@Override
			public void run() {
				controller.restrictToCurrentPatient(isChecked());
			}
		};
		showConsAction = new Action("Kons", Action.AS_CHECK_BOX) {
			{
				setToolTipText("Konsultationstexte weglassen");
			}

			@Override
			public void run() {
				controller.toggleDoctypeFilter(isChecked(), "Konsultation");
			}
		};
		showOmnivoreAction = new Action("Omni", Action.AS_CHECK_BOX) {
			{
				setToolTipText("Omnivore Dokumente weglassen");
			}

			@Override
			public void run() {
				controller.toggleDoctypeFilter(isChecked(), "Omnivore");
			}

		};
		showInboxAction = new Action("Inbox", Action.AS_CHECK_BOX) {
			{
				setToolTipText("Inbox Dokumente weglassen");
			}

			@Override
			public void run() {
				controller.toggleDoctypeFilter(isChecked(), "Inbox");
			}

		};
	}

	@Override
	public void activation(boolean mode) {
		// TODO Auto-generated method stub

	}

}
