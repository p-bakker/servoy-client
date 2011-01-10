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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.ServoyException;

/**
 * Proxy class around a {@link IDataServer} instance for switchServer support.
 *  
 * @author jblok, rgansevles
 */
public class DataServerProxy implements IDataServer
{
	private final IDataServer ds;
	private final Map<String, String> mappedServers = new HashMap<String, String>();

	public DataServerProxy(IDataServer ds)
	{
		this.ds = ds;
	}

	public void switchServer(String sourceName, String destinationName)
	{
		if (sourceName.equals(mappedServers.get(destinationName)))
		{
			mappedServers.remove(destinationName);
		}
		mappedServers.put(sourceName, destinationName);
	}

	private String getMappedServerName(String sourceName)
	{
		String retval = mappedServers.get(sourceName);
		if (retval == null) retval = sourceName;
		return retval;
	}

	public String getReverseMappedServerName(String destName)
	{
		Iterator<Map.Entry<String, String>> entries = mappedServers.entrySet().iterator();
		while (entries.hasNext())
		{
			Map.Entry<String, String> entry = entries.next();
			if (destName != null && destName.equals(entry.getValue()))
			{
				return entry.getKey();
			}
		}
		return destName;
	}

	public ISQLStatement createSQLStatement(int action, String serverName, String tableName, Object[] pkColumnData, String tid, String sql,
		Object[] questiondata) throws RemoteException
	{
		return ds.createSQLStatement(action, getMappedServerName(serverName), tableName, pkColumnData, tid, sql, questiondata);
	}

	public IDataSet executeStoredProcedure(String clientId, String serverName, String transaction_id, String procedureDeclaration, Object[] args,
		int[] inOutType, int startRow, int maxNumberOfRowsToRetrieve) throws RepositoryException, RemoteException
	{
		return ds.executeStoredProcedure(clientId, getMappedServerName(serverName), transaction_id, procedureDeclaration, args, inOutType, startRow,
			maxNumberOfRowsToRetrieve);
	}

	public Blob getBlob(String clientId, String serverName, ISQLSelect blobSelect, ArrayList<TableFilter> filters, String tid) throws RepositoryException,
		RemoteException
	{
		return ds.getBlob(clientId, getMappedServerName(serverName), blobSelect, filters, tid);
	}

	public Object getNextSequence(String serverName, String tableName, String columnName, int columnInfoID) throws RepositoryException, RemoteException
	{
		return ds.getNextSequence(getMappedServerName(serverName), tableName, columnName, columnInfoID);
	}

	public IDataSet performCustomQuery(String client_id, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return ds.performCustomQuery(client_id, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve);
	}

	public IDataSet performQuery(String client_id, String serverName, String transaction_id, ISQLSelect sqlSelect, ArrayList filters, boolean distinctInMemory,
		int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		return ds.performQuery(client_id, getMappedServerName(serverName), transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve,
			updateIdleTimestamp);
	}

	public IDataSet performQuery(String client_id, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, boolean updateIdleTimestamp) throws ServoyException, RemoteException
	{
		return ds.performQuery(client_id, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve,
			updateIdleTimestamp);
	}

	public IDataSet performQuery(String client_id, String serverName, String transaction_id, ISQLSelect sqlSelect, ArrayList filters, boolean distinctInMemory,
		int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return ds.performQuery(client_id, getMappedServerName(serverName), transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, true);
	}

	public IDataSet performQuery(String client_id, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve) throws ServoyException, RemoteException
	{
		return ds.performQuery(client_id, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve, true);
	}

	public IDataSet performQuery(String client_id, String serverName, String transaction_id, ISQLSelect sqlSelect, ArrayList filters, boolean distinctInMemory,
		int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		return ds.performQuery(client_id, getMappedServerName(serverName), transaction_id, sqlSelect, filters, distinctInMemory, startRow, rowsToRetrieve, type);
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IDataServer#performQuery(com.servoy.j2db.dataprocessing.QueryData[])
	 */
	public IDataSet[] performQuery(String client_id, QueryData[] array) throws ServoyException, RemoteException
	{
		if (array != null)
		{
			for (QueryData element : array)
			{
				element.setServer_name(getMappedServerName(element.getServer_name()));
			}
		}
		return ds.performQuery(client_id, array);
	}


	public IDataSet performQuery(String client_id, String serverName, String driverTableName, String transaction_id, String sql, Object[] questiondata,
		int startRow, int rowsToRetrieve, int type) throws ServoyException, RemoteException
	{
		return ds.performQuery(client_id, getMappedServerName(serverName), driverTableName, transaction_id, sql, questiondata, startRow, rowsToRetrieve, type);
	}


	public Object[] performUpdates(String clientId, ISQLStatement[] statements) throws ServoyException, RemoteException
	{
		if (statements != null)
		{
			for (ISQLStatement element : statements)
			{
				String sname = element.getServerName();
				sname = getMappedServerName(sname);
				element.setServerName(sname);
			}
		}
		return ds.performUpdates(clientId, statements);
	}

	public String startTransaction(String clientId, String serverName) throws RepositoryException, RemoteException
	{
		return ds.startTransaction(clientId, getMappedServerName(serverName));
	}

	public boolean endTransactions(String client_id, String[] transaction_id, boolean commit) throws RepositoryException, RemoteException
	{
		return ds.endTransactions(client_id, transaction_id, commit);
	}

	public IDataSet acquireLocks(String client_id, String serverName, String table_name, Set<Object> pkhashkeys, QuerySelect lockSelect, String transaction_id,
		ArrayList<TableFilter> filters, int chunkSize) throws RemoteException
	{
		return ds.acquireLocks(client_id, getMappedServerName(serverName), table_name, pkhashkeys, lockSelect, transaction_id, filters, chunkSize);
	}

	public boolean releaseLocks(String client_id, String serverName, String table_name, Set<Object> pkhashkeys) throws RemoteException
	{
		return ds.releaseLocks(client_id, getMappedServerName(serverName), table_name, pkhashkeys);
	}

	public void addClientAsTableUser(String client_id, String serverName, String table_name) throws RemoteException
	{
		ds.addClientAsTableUser(client_id, getMappedServerName(serverName), table_name);
	}

	public IDataServer getEnclosingDataServer()
	{
		return ds;
	}

	public boolean notifyDataChange(String client_id, String server_name, String tableName, IDataSet pks, int action, String transaction_id)
		throws RemoteException
	{
		// Note: do not use getMappedServerName() here, this call is for the rawSQL plugin, which is NOT transparent for switch-server
		return ds.notifyDataChange(client_id, server_name, tableName, pks, action, transaction_id);
	}

	/**
	 * Log a message on the server
	 * 
	 * @param msg
	 */
	public void logMessage(String msg) throws RemoteException
	{
		ds.logMessage(msg);
	}

	public ITable insertDataSet(String client_id, IDataSet set, String dataSource, String serverName, String tableName, int[] types) throws ServoyException,
		RemoteException
	{
		return ds.insertDataSet(client_id, set, dataSource, getMappedServerName(serverName), tableName, types);
	}

	public void dropTemporaryTable(String client_id, String serverName, String tableName) throws RemoteException
	{
		ds.dropTemporaryTable(client_id, getMappedServerName(serverName), tableName);
	}

	public boolean isInGlobalMaintenanceMode() throws RemoteException
	{
		return ds.isInGlobalMaintenanceMode();
	}

	public void setGlobalMaintenanceMode(boolean maintenanceMode) throws RemoteException
	{
		ds.setGlobalMaintenanceMode(maintenanceMode);
	}

	public boolean isInServerMaintenanceMode() throws RemoteException
	{
		return ds.isInServerMaintenanceMode();
	}

	public void setServerMaintenanceMode(boolean maintenanceMode) throws RemoteException
	{
		ds.setServerMaintenanceMode(maintenanceMode);
	}

	public QuerySet getSQLQuerySet(String serverName, ISQLQuery sqlQuery, ArrayList filters, int startRow, int rowsToRetrieve, boolean forceQualifyColumns)
		throws RepositoryException, RemoteException
	{
		return ds.getSQLQuerySet(serverName, sqlQuery, filters, startRow, rowsToRetrieve, forceQualifyColumns);
	}
}
