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

package ch.rgw.elexis.docmgr_lucinda.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.elexis.data.PersistentObject;
import ch.rgw.elexis.docmgr_lucinda.Activator;
import ch.rgw.lucinda.Handler;

public class Sender implements Handler {
	private List<? extends PersistentObject> toDo;
	private Customer customer;
	private List<Document> answers = new ArrayList<Document>();
	private List<String> onTheWay = new ArrayList<String>();

	public Sender(Customer customer, List<? extends PersistentObject> list) {
		toDo = list;
		this.customer = customer;
		sendNext();
	}

	@Override
	public void signal(Map<String, Object> message) {
		answers.add(new Document(message));
		onTheWay.remove(message.get("_id"));
		if (toDo.isEmpty() && onTheWay.isEmpty()) {
			customer.finished(answers);
		}
		sendNext();
	}

	private void sendNext() {
		if (!toDo.isEmpty()) {
			PersistentObject po = toDo.remove(0);
			if (po.exists()) {
				Document order = customer.specify(po);
				if (order == null) {
					toDo.clear();
				} else {
					byte[] contents = (byte[]) order.toMap().get("payload");
					onTheWay.add(po.getId());
					Activator.getDefault().getLucinda().addToIndex(po.getId(), order.get("title"),
							order.get("type"), order.toMap(), contents, this);
				}
			}
		}
	}
}

interface Customer {
	public Document specify(PersistentObject po);

	public void finished(List<Document> messages);
}