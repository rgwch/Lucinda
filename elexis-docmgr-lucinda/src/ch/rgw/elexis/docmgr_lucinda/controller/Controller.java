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

package ch.rgw.elexis.docmgr_lucinda.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.rgw.elexis.docmgr_lucinda.Activator;
import ch.rgw.elexis.docmgr_lucinda.model.Document;
import ch.rgw.elexis.docmgr_lucinda.view.GlobalViewPane;
import ch.rgw.io.FileTool;
import ch.rgw.lucinda.Client;
import ch.rgw.lucinda.Handler;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class Controller implements Handler, IProgressController {
	Client lucinda;
	GlobalViewPane view;
	ContentProvider cnt;
	TableViewer viewer;
	boolean bRestrictCurrentPatient=false;
	Map<Long,Integer> visibleProcesses=new HashMap<Long,Integer>();
	long actMax;
	int div;
	int actValue;
	
	public Controller(){
		lucinda = Activator.getDefault().getLucinda();
		cnt = new ContentProvider();
		Activator.getDefault().addHandler(this);
		Activator.getDefault().setProgressController(this);
	}
	
	public void reconnect(){
		clear();
		view.setConnected(false);
		Activator.getDefault().disconnect();
		Activator.getDefault().connect();
	}
	
	public Composite createView(Composite parent){
		view = new GlobalViewPane(parent, this);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel=(IStructuredSelection)event.getSelection();
				if(!sel.isEmpty()){
					Document doc=new Document(sel.getFirstElement());
					if(doc.get("lucinda_doctype").equals("Konsultation")){
						Konsultation kons=Konsultation.load(doc.get("_id"));
						if(kons.exists()){
							ElexisEventDispatcher.fireSelectionEvent(kons);
						}
					}
				}
				
			}
		});
		return view;
	}
	
	public IStructuredContentProvider getContentProvider(TableViewer tv){
		viewer = tv;
		return cnt;
		
	}
	
	public LabelProvider getLabelProvider(){
		return new LucindaLabelProvider();
	}
	
	public void clear(){
		viewer.setInput(new ArrayList());
	}
	
	public void restrictToCurrentPatient(boolean bRestrict){
		bRestrictCurrentPatient=bRestrict;
		runQuery(view.getText());
	}
	
	public void runQuery(String input){
		StringBuilder query=new StringBuilder(input);
		if(bRestrictCurrentPatient){
			Patient pat=ElexisEventDispatcher.getSelectedPatient();
			query.append(" +lastname:").append(pat.getName())
			.append(" +firstname:").append(pat.getVorname())
			.append(" +birthdate:").append(new TimeTool(pat.getGeburtsdatum()).toString(TimeTool.DATE_COMPACT));
		}
		lucinda.query(query.toString(), result -> {
			if (result.get("status").equals("ok")) {
				@SuppressWarnings("rawtypes")
				List queryResult = (List) result.get("result");
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run(){
						viewer.setInput(queryResult);
						
					}
					
				});
				
			}
		});
		
	}
	
	public void loadDocument(final Document doc){
		lucinda.get(doc.get("_id"), result -> {
			if (result.get("status").equals("ok")) {
				@SuppressWarnings("unused")
				byte[] contents = (byte[]) result.get("result");
				String ext = FileTool.getExtension(doc.get("url"));
				launchViewerForDocument(contents, ext);
			}
		});
	}
	
	@Override
	public void signal(Map<String, Object> message){
		switch ((String) message.get("status")) {
		case "connected":
			view.setConnected(true);
			break;
		case "disconnected":
			view.setConnected(false);
			break;
		}
	}
	
	public void launchViewerForDocument(byte[] cnt, String ext){
		try {
			File temp = File.createTempFile("_lucinda_", "_." + ext);
			temp.deleteOnExit();
			FileTool.writeFile(temp, cnt);
			Program proggie = Program.findProgram(ext);
			if (proggie != null) {
				proggie.execute(temp.getAbsolutePath());
			} else {
				if (Program.launch(temp.getAbsolutePath()) == false) {
					Runtime.getRuntime().exec(temp.getAbsolutePath());
				}
			}
			
		} catch (Exception ex) {
			ExHandler.handle(ex);
			SWTHelper.showError("Could not launch file", ex.getMessage());
		}
	}
	
	public Long initProgress(int maximum){
		Long proc=System.currentTimeMillis()+new Random().nextLong();
		visibleProcesses.put(proc, maximum);
		long val=0;
		for(Integer k:visibleProcesses.values()){
			val+=k;
		}
		if(val<Integer.MAX_VALUE){
			div=1;
		}
		int amount=(int)(val/div);
		view.initProgress(amount);
	
		return proc;
	}
	
	public void addProgress(Long handle, int amount){
		Integer val=visibleProcesses.get(handle);
		val-=amount;
		if(val<=0){
			visibleProcesses.remove(handle);
			amount+=val;
			if(visibleProcesses.isEmpty()){
				view.finishProgress();
			}
		}else{
			visibleProcesses.put(handle, val);
		}
		actValue+=amount;
		view.showProgress(actValue/div);
		
	}
		
	
}
