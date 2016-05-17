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
import ch.elexis.core.ui.actions.RestrictedAction;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.data.Patient;
import ch.rgw.elexis.docmgr_lucinda.Activator;
import ch.rgw.elexis.docmgr_lucinda.Preferences;
import ch.rgw.elexis.docmgr_lucinda.controller.Controller;
import ch.rgw.tools.TimeTool;

public class GlobalView extends ViewPart {

	private Controller controller;
	private Action doubleClickAction, filterCurrentPatAction, showInboxAction, showConsAction, showOmnivoreAction;
	private RestrictedAction indexOmnivoreAction, indexKonsAction;

	private final ElexisUiEventListenerImpl eeli_pat = new ElexisUiEventListenerImpl(Patient.class,
			ElexisEvent.EVENT_SELECTED) {

		@Override
		public void run(ElexisEvent ev) {
			Patient pat = (Patient) ev.getObject();
			StringBuilder qs = new StringBuilder().append("+lastname:").append(pat.getName()).append(" +firstname:")
					.append(pat.getVorname()).append(" +birthdate:")
					.append(new TimeTool(pat.getGeburtsdatum()).toString(TimeTool.DATE_COMPACT));
			controller.runQuery(qs.toString());
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

	private void fillLocalToolBar(IToolBarManager manager){
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
		if(Preferences.get(Preferences.INCLUDE_OMNI, "0").equals("1")){
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

		filterCurrentPatAction=new Action("Aktueller Patient",Action.AS_CHECK_BOX){
			{
				setToolTipText("Nur Treffer für den aktuellen patienten anzeigen");
				setImageDescriptor(Images.IMG_FILTER.getImageDescriptor());
			}

			@Override
			public void run() {
				controller.restrictToCurrentPatient(isChecked());
			}
		};
		showConsAction=new Action("Kons",Action.AS_CHECK_BOX){
			{
				setToolTipText("Konsultationstexte einbeziehen");
			}
		};
		showOmnivoreAction=new Action("Omni",Action.AS_CHECK_BOX){
			{
				setToolTipText("Omnivore Dokumente einbeziehen");
			}
		};
		showInboxAction=new Action("Inbox",Action.AS_CHECK_BOX){
			{
				setToolTipText("Inbox Dokumente einbeziehen");
			}
		};
	}

}
