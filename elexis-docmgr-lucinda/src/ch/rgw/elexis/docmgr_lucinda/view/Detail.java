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

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import ch.elexis.core.ui.util.viewers.TableLabelProvider;

public class Detail extends Composite {
	TableViewer tv;
	
	public Detail(Composite parent){
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		tv = new TableViewer(this, SWT.NONE);
		TableViewerColumn tvc1 = new TableViewerColumn(tv, SWT.NONE);
		TableColumn tc1 = tvc1.getColumn();
		tv.getTable().setHeaderVisible(true);
		tv.getTable().setLinesVisible(true);
		tc1.setWidth(200);
		tc1.setText("Key");
		
		TableViewerColumn tvc2 = new TableViewerColumn(tv, SWT.NONE);
		TableColumn tc2 = tvc2.getColumn();
		tc2.setWidth(100);
		tc2.setText("Value");
		
		tv.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){}
			
			@Override
			public void dispose(){}
			
			@SuppressWarnings("unchecked")
			@Override
			public Object[] getElements(Object inputElement){
				Map<String, Object> el = (Map<String, Object>) inputElement;
				return el.entrySet().toArray();
			}
		});
		tv.setLabelProvider(new TableLabelProvider() {
			
			@Override
			public String getColumnText(Object element, int columnIndex){
				Entry<String, Object> en = (Entry<String, Object>) element;
				if (columnIndex == 0) {
					return en.getKey();
				} else {
					return en.getValue().toString();
				}
			}
			
		});
		
	}
	
	public void setInput(Object input){
		tv.setInput(input);
	}
}
