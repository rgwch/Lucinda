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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.ui.preferences.SettingsPreferenceStore;
import ch.rgw.elexis.docmgr_lucinda.Activator;
import ch.rgw.elexis.docmgr_lucinda.Preferences;

public class LucindaPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public LucindaPrefs(){
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(CoreHub.localCfg));
		setDescription("Lucinda Client");
	}
	
	@Override
	public void init(IWorkbench workbench){
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void createFieldEditors(){
		addField(new StringFieldEditor(Preferences.NETWORK, "Netmask", getFieldEditorParent()));
		addField(new StringFieldEditor(Preferences.MSG, "Message Prefix", getFieldEditorParent()));
		addField(new BooleanFieldEditor(Preferences.INCLUDE_OMNI, "index Omnivore documents", getFieldEditorParent()));
		addField(new BooleanFieldEditor(Preferences.INCLUDE_KONS, "index Consultation texts", getFieldEditorParent()));
	}
	
	@Override
	public boolean performOk(){
		if (super.performOk()) {
			Activator.getDefault().disconnect();
			Activator.getDefault().connect();
			return true;
		}
		return false;
	}
	
}
