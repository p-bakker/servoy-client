/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.dataprocessing;


import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * Transaction object, keeping track of db transctions (ids) 
 * @author jblok
 */
public class GlobalTransaction
{
	private WeakHashMap<IRecordInternal, GlobalTransaction> records;//all rows to prevent they are gc'ed
	private WeakHashMap<IRecordInternal, GlobalTransaction> deletedRecords;//all rows to prevent they are gc'ed
	private Map<String, String> serverTransactions;//serverName->transactionID
	private final IDataServer dataServer;
	private final String clientID;

	GlobalTransaction(IDataServer ds, String cid)
	{
		serverTransactions = new HashMap<String, String>();
		records = new WeakHashMap<IRecordInternal, GlobalTransaction>();
		deletedRecords = new WeakHashMap<IRecordInternal, GlobalTransaction>();
		dataServer = ds;
		clientID = cid;
	}

	Collection<String> rollback(boolean queryForNewData, boolean revertSavedRecords)
	{
		if (serverTransactions != null)
		{
			String[] tids = new String[serverTransactions.size()];
			serverTransactions.values().toArray(tids);
			try
			{
				dataServer.endTransactions(clientID, tids, false);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		if (queryForNewData && (!records.isEmpty() || !deletedRecords.isEmpty()))
		{
			Set<String> set = new HashSet<String>();
			try
			{
				Iterator<IRecordInternal> it2 = records.keySet().iterator();
				while (it2.hasNext())
				{
					// TODO this can be optimized.
					// Search for all the rows with the same table and do a in query (per 200 rows..)
					IRecordInternal record = it2.next();
					Row row = record.getRawData();
					// call startEdit with false so that start edit on a formcontroller (of the current visible one so only for that record)
					// if a deleted record was first updated then just overwrite with the database. 
					if (!revertSavedRecords && !deletedRecords.containsKey(record) &&
						row.getRowManager().getFoundsetManager().getEditRecordList().startEditing(record, false))
					{
						// if we shouldn't revert to database values,
						// quickly create the old values (edit values) array
						row.createOldValuesIfNeeded();
						// then revert by keeping the current column data values.
						row.rollbackFromDB(Row.ROLLBACK_MODE.KEEP_CHANGES);
					}
					else
					{
						row.rollbackFromDB(Row.ROLLBACK_MODE.OVERWRITE_CHANGES);
					}
					set.add(record.getParentFoundSet().getDataSource());
				}

				Iterator<IRecordInternal> it3 = deletedRecords.keySet().iterator();
				while (it3.hasNext())
				{
					set.add(it3.next().getParentFoundSet().getDataSource());
				}
				records = null;
				deletedRecords = null;
			}
			catch (Exception e)
			{
				Debug.error(e); //TODO: add to application
			}
			return set;
		}
		return Collections.<String> emptyList();
	}

	Collection<String> commit(boolean revertSavedRecords)
	{
		String[] tids = new String[serverTransactions.size()];
		serverTransactions.values().toArray(tids);
		try
		{
			dataServer.endTransactions(clientID, tids, true);
			records = null;
		}
		catch (Exception e)
		{
			Debug.error(e);//TODO: add to application
		}
		if (records != null)
		{
			serverTransactions = new HashMap<String, String>();//clear, so they are not processes on IDataService
			return rollback(true, revertSavedRecords);//all others
		}
		return null;
	}

	String addRecord(String serverName, IRecordInternal r) throws RepositoryException
	{
		if (!records.containsKey(r))
		{
			records.put(r, this);
		}
		return getTransactionID(serverName);
	}

	void addDeletedRecord(IRecordInternal r)
	{
		if (!deletedRecords.containsKey(r))
		{
			deletedRecords.put(r, this);
		}
	}


	public synchronized String getTransactionID(String serverName) throws RepositoryException
	{
		try
		{
			//check if transaction exist for a server
			String tid = serverTransactions.get(serverName);
			if (tid == null)
			{
				//create
				tid = dataServer.startTransaction(clientID, serverName);
				//store
				serverTransactions.put(serverName, tid);
			}
			return tid;
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}
}
