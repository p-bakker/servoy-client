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


import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.QueryString;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.info.COLUMNTYPE;
import com.servoy.j2db.scripting.info.SQL_ACTION_TYPES;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Scriptable database manager object
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Database Manager", scriptingName = "databaseManager")
public class JSDatabaseManager
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSDatabaseManager.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { COLUMNTYPE.class, SQL_ACTION_TYPES.class, JSColumn.class, JSDataSet.class, JSFoundSetUpdater.class, JSTable.class };
			}
		});
	}

	private volatile IApplication application;

	public JSDatabaseManager(IApplication application)
	{
		this.application = application;
	}

	private void checkAuthorized() throws ServoyException
	{
		if (!application.haveRepositoryAccess())
		{
			// no access to repository yet, have to log in first
			throw new ServoyException(ServoyException.CLIENT_NOT_AUTHORIZED);
		}
	}

	/*
	 * _____________________________________________________________ locking methods
	 */

	/**
	 * Request lock(s) for a foundset, can be a normal or related foundset.
	 * The record_index can be -1 to lock all rows, 0 to lock the current row, or a specific row of > 0 
	 * Optionally name the lock(s) so that it can be referenced it in releaseAllLocks()
	 * 
	 * returns true if the lock could be acquired.
	 * 
	 * @sample
	 * //locks the complete foundset
	 * databaseManager.acquireLock(foundset,-1);
	 * 
	 * //locks the current row
	 * databaseManager.acquireLock(foundset,0);
	 * 
	 * //locks all related orders for the current Customer
	 * var success = databaseManager.acquireLock(Cust_to_Orders,-1);
	 * if(!success)
	 * {
	 *   plugins.dialogs.showWarningDialog('Alert','Failed to get a lock','OK');
	 * }
	 *
	 * @param foundset The JSFoundset to get the lock for
	 * @param record_index The record index which should be locked.
	 * @param lock_name optional The name of the lock.
	 * 
	 * @return true if the lock could be acquired.
	 */
	public boolean js_acquireLock(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		if (vargs == null || vargs.length < 2)
		{
			return false;
		}
		int n = 0;
		Object arg = vargs[n++];
		int index = Utils.getAsInteger(vargs[n++]);
		String lockName = null;
		if (vargs.length > n)
		{
			Object o = vargs[n++];
			if (o != null) lockName = o.toString();
		}
		return ((FoundSetManager)application.getFoundSetManager()).acquireLock(arg, index - 1, lockName);
	}

	/**
	 * Adds a filter to all the foundsets based on a table.
	 * Note: if null is provided as the tablename the filter will be applied on all tables with the dataprovider name
	 * returns true if the tablefilter could be applied.
	 *
	 * @sample
	 * //best way to call this in a global solution startup method
	 * var success = databaseManager.addTableFilterParam('admin', 'messages', 'messagesid', '>', 10, 'higNumberedMessagesRule')
	 *
	 * @param server_name The name of the database server connection for the specified table name.
	 * @param table_name The name of the specified table. 
	 * @param dataprovider A specified dataprovider column name.  
	 * @param operator One of "=, <, >, >=, <=, !=, LIKE, or IN". 
	 * @param value The specified filter value. 
	 * @param filter_name optional The specified name of the database table filter. 
	 * 
	 * @return true if the tablefilter could be applied.
	 */
	public boolean js_addTableFilterParam(Object[] args) throws ServoyException
	{
		checkAuthorized();
		if (args.length < 5) return false;
		String serverName = args[0] instanceof String ? (String)args[0] : null;
		String tableName = args[1] instanceof String ? (String)args[1] : null;
		String dataprovider = args[2] instanceof String ? (String)args[2] : null;
		String operator = args[3] instanceof String ? (String)args[3] : null;
		Object value = args[4];
		String filterName = args.length >= 6 && args[5] instanceof String ? (String)args[5] : null;

		try
		{
			if (value instanceof Wrapper)
			{
				value = ((Wrapper)value).unwrap();
			}
			IServer server = application.getSolution().getServer(serverName);
			if (server != null)
			{
				if (tableName == null)
				{
					boolean retval = false;
					Iterator<String> it = server.getTableAndViewNames(false).iterator();
					while (it.hasNext())
					{
						String tname = it.next();
						Table t = (Table)server.getTable(tname);
						if (t != null && t.getColumn(dataprovider) != null)
						{
							retval = (((FoundSetManager)application.getFoundSetManager()).addTableFilterParam(filterName, t, dataprovider, operator, value) || retval);
						}
					}
					return retval;
				}
				else
				{
					ITable t = server.getTable(tableName);
					if (t != null)
					{
						return ((FoundSetManager)application.getFoundSetManager()).addTableFilterParam(filterName, (Table)t, dataprovider, operator, value);
					}
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return false;
	}

	/**
	 * Removes a previously defined table filter.
	 *
	 * @sample var success = databaseManager.removeTableFilterParam('admin', 'higNumberedMessagesRule')
	 *
	 * @param serverName The name of the database server connection.
	 * @param filterName The name of the filter that should be removed.
	 * 
	 * @return true if the filter could be removed.
	 */
	public boolean js_removeTableFilterParam(String serverName, String filterName)
	{
		if (serverName == null || filterName == null) return false;

		try
		{
			return (((FoundSetManager)application.getFoundSetManager()).removeTableFilterParam(serverName, filterName));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return false;
	}

	/**
	 * Returns a two dimensional array object containing the table filter information currently applied to the servers tables.
	 * The "columns" of a row from this array are: tablename,dataprovider,operator,value,tablefilername
	 *
	 * @sample
	 * var params = databaseManager.getTableFilterParams(databaseManager.getDataSourceServerName(controller.getDataSource()))
	 * for (var i = 0; params != null && i < params.length; i++)
	 * {
	 * 	application.output('Table filter on table ' + params[i][0]+ ': '+ params[i][1]+ ' '+params[i][2]+ ' '+params[i][3] +(params[i][4] == null ? ' [no name]' : ' ['+params[i][4]+']'))
	 * }
	 *
	 * @param server_name The name of the database server connection.
	 * @param filter_name optional The filter name for which to get the array.
	 * 
	 * @return Two dimensional array.
	 */
	public Object[][] js_getTableFilterParams(Object[] args)
	{
		if (args == null || args.length < 1 || args[0] == null) return null;
		String serverName = args[0].toString();
		String filterName = args.length > 1 && args[1] != null ? args[1].toString() : null;

		try
		{
			return (((FoundSetManager)application.getFoundSetManager()).getTableFilterParams(serverName, filterName));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return null;
	}

	/**
	 * Creates a foundset that combines all the records of the specified one-to-many relation seen from the given parent/primary foundset.
	 *
	 * @sample
	 * // Convert in the order form a orders foundset into a orderdetails foundset, 
	 * // that has all the orderdetails from all the orders in the foundset.
	 * var convertedFoundSet = databaseManager.convertFoundSet(foundset,order_to_orderdetails);
	 * // or var convertedFoundSet = databaseManager.convertFoundSet(foundset,"order_to_orderdetails");
	 * forms.orderdetails.controller.showRecords(convertedFoundSet);
	 *
	 * @param foundset The JSFoundset to convert.
	 * @param relation can be a one-to-many relation object or the name of a one-to-many relation
	 * 
	 * @return The converted JSFoundset. 
	 */
	public FoundSet js_convertFoundSet(Object foundset, Object relation) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet && ((FoundSet)foundset).getTable() != null)
		{
			FoundSet fs_old = (FoundSet)foundset;
			try
			{
				Relation r = null;
				if (relation instanceof RelatedFoundSet)
				{
					r = application.getFlattenedSolution().getRelation(((RelatedFoundSet)relation).getRelationName());
				}
				else if (relation instanceof String)
				{
					r = application.getFlattenedSolution().getRelation((String)relation);
				}

				if (r != null && !r.isMultiServer() && fs_old.getTable() != null && fs_old.getTable().equals(r.getPrimaryTable()))
				{
					Table ft = r.getForeignTable();
					FoundSet fs_new = (FoundSet)application.getFoundSetManager().getNewFoundSet(ft, null);

					QuerySelect sql = fs_old.getPksAndRecords().getQuerySelectForModification();
					SQLSheet sheet_new = fs_old.getSQLSheet().getRelatedSheet(r.getName(),
						((FoundSetManager)application.getFoundSetManager()).getSQLGenerator());
					if (sheet_new != null)
					{
						QueryTable oldTable = sql.getTable();
						QueryJoin join = (QueryJoin)sql.getJoin(oldTable, r.getName());
						if (join == null)
						{
							join = SQLGenerator.createJoin(application.getFlattenedSolution(), r, oldTable, new QueryTable(ft.getSQLName(), ft.getCatalog(),
								ft.getSchema()), fs_old);
							sql.addJoin(join);
						}

						QueryTable mainTable = join.getForeignTable();

						// invert the join
						sql.setTable(mainTable);
						join.invert("INVERTED." + join.getName()); //$NON-NLS-1$

						// set the columns to be the PKs from the related table
						ArrayList<IQuerySelectValue> pkColumns = new ArrayList<IQuerySelectValue>();
						Iterator<Column> pks = sheet_new.getTable().getRowIdentColumns().iterator();
						while (pks.hasNext())
						{
							Column column = pks.next();
							pkColumns.add(new QueryColumn(mainTable, column.getID(), column.getSQLName(), column.getType(), column.getLength(),
								column.getScale()));
						}
						sql.setColumns(pkColumns);

						// sorting will be on the original columns, when distinct is set, this will conflict with the related pk columns
						sql.setDistinct(false);

						fs_new.setSQLSelect(sql);
						return fs_new;
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	/**
	 * Converts the argument to a JSDataSet, possible use in controller.loadRecords(dataset)
	 *
	 * @sample
	 * // converts a foundset pks to a dataset
	 * var dataset = databaseManager.convertToDataSet(foundset);
	 * // converts a foundset to a dataset
	 * //var dataset = databaseManager.convertToDataSet(foundset,['product_id','product_name']);
	 * // converts an object array to a dataset
	 * //var dataset = databaseManager.convertToDataSet(files,['name','path']);
	 * // converts an array to a dataset
	 * //var dataset = databaseManager.convertToDataSet(new Array(1,2,3,4,5,6));
	 * // converts an string list to a dataset
	 * //var dataset = databaseManager.convertToDataSet('4,5,6');
	 *
	 * @param array/ids_string/foundset The data that should go into the JSDataSet.
	 * @param array_with_dataprovider_names optional Array with column names.
	 * 
	 * @return JSDataSet with the data. 
	 */
	public JSDataSet js_convertToDataSet(Object[] args) throws RepositoryException
	{
		if (args.length == 0) return null;
		Object object = args[0];
		if (object == null) return null;
		if (object instanceof JSDataSet) return (JSDataSet)object;

		String[] dpnames = { "id" }; //$NON-NLS-1$
		int[] dptypes = { IColumnTypes.INTEGER };
		List<Object[]> lst = new ArrayList<Object[]>();

		if (object instanceof FoundSet)
		{
			FoundSet fs = (FoundSet)object;
			if (fs.getTable() != null)
			{
				if (args.length > 1 && args[1] != null && args[1].getClass().isArray())
				{
					Object[] odp = (Object[])args[1];
					dpnames = new String[odp.length];
					for (int i = 0; i < odp.length; i++)
					{
						dpnames[i] = String.valueOf(odp[i]);
					}
				}
				else
				{
					dpnames = fs.getSQLSheet().getPKColumnDataProvidersAsArray();
				}
				dptypes = new int[dpnames.length];
				Table table = fs.getSQLSheet().getTable();
				for (int i = 0; i < dpnames.length; i++)
				{
					IDataProvider dp = application.getFlattenedSolution().getDataProviderForTable(table, dpnames[i]);
					dptypes[i] = dp == null ? 0 : dp.getDataProviderType();
				}
				for (int i = 0; i < fs.getSize(); i++)
				{
					IRecordInternal record = fs.getRecord(i);
					Object[] pk = new Object[dpnames.length];
					for (int j = 0; j < dpnames.length; j++)
					{
						pk[j] = record.getValue(dpnames[j]);
					}
					lst.add(pk);
				}
			}
		}
		else if (object instanceof String && args.length == 1)
		{
			StringTokenizer st = new StringTokenizer(object.toString(), ",;\n\r\t "); //$NON-NLS-1$
			while (st.hasMoreElements())
			{
				Object o = st.nextElement();
				if (o instanceof Double && ((Double)o).doubleValue() == ((Double)o).intValue())
				{
					o = new Integer(((Double)o).intValue());
				}
				lst.add(new Object[] { o });
			}
		}
		else
		{
			// 2 possibilities: args is the list of values or args[0] is the list of values and args[1] is the list of dpnames
			Object[] array;
			if (args.length > 1 && (args[0] instanceof Object[]) && (args[1] instanceof Object[]))
			{
				array = (Object[])args[0];
				dpnames = new String[((Object[])args[1]).length];
				for (int i = 0; i < dpnames.length; i++)
				{
					dpnames[i] = String.valueOf(((Object[])args[1])[i]);
				}
			}
			else
			{
				array = args;
			}

			Map<String, Method> getters = new HashMap<String, Method>();

			for (Object o : array)
			{
				if (o instanceof Number || o instanceof String || o instanceof UUID || o instanceof Date)
				{
					if (o instanceof Double && ((Double)o).doubleValue() == ((Double)o).intValue())
					{
						o = new Integer(((Double)o).intValue());
					}
					lst.add(new Object[] { o });
				}
				else if (o instanceof Scriptable)
				{
					List<Object> row = new ArrayList<Object>();
					for (String dpname : dpnames)
					{
						if (((Scriptable)o).has(dpname, (Scriptable)o)) row.add(((Scriptable)o).get(dpname, (Scriptable)o));
					}
					if (dpnames.length != row.size() || dpnames.length == 0)
					{
						// for backward compatibility 
						lst.add(new Object[] { o });
					}
					else
					{
						lst.add(row.toArray());
					}
				}
				else if (o != null)
				{
					//try reflection
					List<Object> row = new ArrayList<Object>();
					for (String dpname : dpnames)
					{
						Method m = getMethod(o, dpname, getters);
						if (m != null)
						{
							try
							{
								row.add(m.invoke(o, (Object[])null));
							}
							catch (Exception e)
							{
								Debug.error(e);
							}
						}
					}
					if (dpnames.length != row.size() || dpnames.length == 0)
					{
						// for backward compatibility 
						lst.add(new Object[] { o });
					}
					else
					{
						lst.add(row.toArray());
					}
				}
			}
		}
		return new JSDataSet(application, new BufferedDataSet(dpnames, dptypes, lst));
	}

	private Method getMethod(Object o, String pname, Map<String, Method> getters)
	{
		Method retval = getters.get(pname);
		if (retval == null && !getters.containsKey(pname))
		{
			Method[] methods = o.getClass().getMethods();
			for (Method m : methods)
			{
				String name = m.getName();
				if (m.getParameterTypes().length == 0 && name.startsWith("get") && name.substring(3).equalsIgnoreCase(pname)) //$NON-NLS-1$
				{
					retval = m;
					break;
				}
			}
			getters.put(pname, retval);
		}
		return retval;
	}

	/**
	 * Returns an empty dataset object. 
	 *
	 * @sample
	 * // gets an empty dataset with a specifed row and column count
	 * var dataset = databaseManager.createEmptyDataSet(10,10)
	 * // gets an empty dataset with a specifed row count and column array
	 * var dataset2 = databaseManager.createEmptyDataSet(10,new Array ('a','b','c','d'))
	 *
	 * @param row_count The number of rows in the DataSet object.
	 * @param columnCount/array_with_column_names Number of columns or the column names.
	 * 
	 * @return An empty JSDataSet with the initial sizes. 
	 */
	public JSDataSet js_createEmptyDataSet(Object[] args)
	{
		if (args != null && args.length >= 2 && args[1] != null && args[1].getClass().isArray())
		{
			Object[] array = (Object[])args[1];
			String[] cols = new String[array.length];
			for (int i = 0; i < cols.length; i++)
			{
				cols[i] = (array[i] != null ? array[i].toString() : null);
			}
			return new JSDataSet(application, Utils.getAsInteger(args[0]), cols);
		}
		else if (args != null && args.length >= 2 && args[1] instanceof Number)
		{
			return new JSDataSet(application, Utils.getAsInteger(args[0]), new String[((Number)args[1]).intValue()]);
		}
		else
		{
			return new JSDataSet(application);
		}
	}

	/**
	 * Performs a sql query on the specified server, returns the result in a dataset.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * @sample
	 * //finds duplicate records in a specified foundset
	 * var vQuery =" SELECT companiesid from companies where company_name IN (SELECT company_name from companies group bycompany_name having count(company_name)>1 )";
	 * var vDataset =databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), vQuery, null, 1000);
	 * controller.loadRecords(vDataset);
	 * 
	 * var maxReturnedRows = 10;//useful to limit number of rows
	 * var query = 'select c1,c2,c3 from test_table where start_date = ?';//do not use '.' or special chars in names or aliases if you want to access data by name
	 * var args = new Array();
	 * args[0] = order_date //or  new Date()
	 * var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), query, args, maxReturnedRows);
	 * 
	 * // place in label: 
	 * // elements.myLabel.text = '<html>'+dataset.getAsHTML()+'</html>';
	 * 
	 * //example to calc a strange total
	 * global_total = 0;
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	dataset.rowIndex = i;
	 * 	global_total = global_total + dataset.c1 + dataset.getValue(i,3);
	 * }
	 * //example to assign to dataprovider
	 * //employee_salary = dataset.getValue(row,column)
	 *
	 * @param server_name The name of the server where the query should be executed.
	 * @param sql_query The custom sql.
	 * @param arguments Specified arguments or null if there are no arguments.
	 * @param max_returned_rows The maximum number of rows returned by the query.  
	 * 
	 * @return The JSDataSet containing the results of the query.
	 */
	public JSDataSet js_getDataSetByQuery(String server_name, String sql_query, Object[] arguments, int max_returned_rows) throws ServoyException
	{
		checkAuthorized();
		if (server_name == null) throw new RuntimeException(new ServoyException(ServoyException.InternalCodes.SERVER_NOT_FOUND, new Object[] { "<null>" })); //$NON-NLS-1$
		if (sql_query == null || sql_query.trim().length() == 0) throw new RuntimeException(new DataException(ServoyException.BAD_SQL_SYNTAX,
			new SQLException(), sql_query));

		// TODO HOW TO HANDLE ARGS WITH NULL?? sHOULD BE CONVERTED TO NullValue?????
		if (arguments != null)
		{
			for (int i = 0; i < arguments.length; i++)
			{
				if (arguments[i] instanceof java.util.Date)
				{
					arguments[i] = new Timestamp(((java.util.Date)arguments[i]).getTime());
				}
				else if (arguments[i] instanceof DbIdentValue && ((DbIdentValue)arguments[i]).getPkValue() == null)
				{
					Debug.log("Custom query: " + sql_query + //$NON-NLS-1$
						" not executed because the arguments have a database ident value that is null, from a not yet saved record"); //$NON-NLS-1$
					return new JSDataSet(application);
				}
			}
		}
		try
		{
			return new JSDataSet(application, ((FoundSetManager)application.getFoundSetManager()).getDataSetByQuery(server_name, sql_query, arguments,
				max_returned_rows));
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
//			Debug.error(e);
//			if (application != null) application.handleException(null, e);
//			return new JSDataSet(e);
		}
	}

	/**
	 * @see com.servoy.extensions.plugins.rawSQL.RawSQLProvider#js_executeStoredProcedure(String, String, Object[], int[], int)
	 */
	@Deprecated
	public Object js_executeStoredProcedure(String serverName, String procedureDeclaration, Object[] args, int[] inOutType, int maxNumberOfRowsToRetrieve)
		throws ServoyException
	{
		checkAuthorized();
		IClientPlugin cp = application.getPluginManager().getPlugin(IClientPlugin.class, "rawSQL"); //$NON-NLS-1$
		if (cp != null)
		{
			IScriptObject so = cp.getScriptObject();
			if (so != null)
			{
				try
				{
					Method m = so.getClass().getMethod(
						"js_executeStoredProcedure", new Class[] { String.class, String.class, Object[].class, int[].class, int.class }); //$NON-NLS-1$
					return m.invoke(so, new Object[] { serverName, procedureDeclaration, args, inOutType, new Integer(maxNumberOfRowsToRetrieve) });
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
		}
		application.reportError(
			"Writing to file failed", "For this operation the file plugin is needed\nNote this method is deprecated, use the plugin directly in your code"); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public String js_getLastDatabaseMessage()//for last unspecified db warning and errors
	{
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the total number of records in a foundset.
	 * 
	 * NOTE: This can be an expensive operation (time-wise) if your resultset is large.
	 *
	 * @sample
	 * //return the total number of records in a foundset.
	 * databaseManager.getFoundSetCount(foundset);
	 *
	 * @param foundset The JSFoundset to get the count for.
	 * 
	 * @return the foundset count
	 */
	public int js_getFoundSetCount(Object foundset) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof IFoundSetInternal)
		{
			return application.getFoundSetManager().getFoundSetCount((IFoundSetInternal)foundset);
		}
		return 0;
	}

	/**
	 * Can be used to recalculate a specified record or all rows in the specified foundset.
	 * May be necessary when records are inserted in a program external to Servoy.
	 *
	 * @sample
	 * // recalculate one record from a foundset.
	 * databaseManager.recalculate(foundset.getRecord(1));
	 * // recalculate all records from the foundset.
	 * // please use with care, this can be expensive!
	 * //databaseManager.recalculate(foundset);
	 *
	 * @param foundsetOrRecord JSFoundset or JSRecord to recalculate.
	 */
	public void js_recalculate(Object foundsetOrRecord) throws ServoyException
	{
		checkAuthorized();
		if (foundsetOrRecord instanceof IRecordInternal)
		{
			recalculateRecord((IRecordInternal)foundsetOrRecord);
			((FoundSet)((IRecordInternal)foundsetOrRecord).getParentFoundSet()).fireFoundSetChanged();
		}
		else if (foundsetOrRecord instanceof FoundSet)
		{
			FoundSet fs = (FoundSet)foundsetOrRecord;
			for (int i = 0; i < fs.getSize(); i++)
			{
				recalculateRecord(fs.getRecord(i));
			}
			fs.fireFoundSetChanged();
		}
	}

	private void recalculateRecord(IRecordInternal record)
	{
		record.startEditing();
		record.getRawData().getRowManager().flagAllRowCalcsForRecalculation(record.getPKHashKey());
		SQLSheet sheet = record.getParentFoundSet().getSQLSheet();
		//recalc all stored calcs (required due to use of plugin methods in calc)
		Iterator<String> it = sheet.getAllCalculationNames();
		while (it.hasNext())
		{
			String calc = it.next();
			record.getValue(calc);
		}
		try
		{
			record.stopEditing();
		}
		catch (Exception e)
		{
			Debug.error("error when recalculation record: " + record, e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a JSFoundsetUpdater object that can be used to update all or a specific number of rows in the specified foundset.
	 *
	 * @sampleas com.servoy.j2db.dataprocessing.JSFoundSetUpdater#js_performUpdate()
	 *
	 * @param foundset The foundset to update.
	 * 
	 * @return The JSFoundsetUpdater for the specified JSFoundset.
	 */
	public JSFoundSetUpdater js_getFoundSetUpdater(Object foundset) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet)
		{
			return new JSFoundSetUpdater(application, (FoundSet)foundset);
		}
		return null;
	}

	/**
	 * Returns an array of records that fail after a save. 
	 *
	 * @sample
	 * var array = databaseManager.getFailedRecords()
	 * for( var i = 0 ; i < array.length ; i++ )
	 * {
	 * 	var record = array[i];
	 * 	application.output(record.exception);
	 * 	if (record.exception.getErrorCode() == ServoyException.RECORD_VALIDATION_FAILED)
	 * 	{
	 * 		// exception thrown in pre-insert/update/delete event method
	 * 		var thrown = record.exception.getValue()
	 * 		application.output("Record validation failed: "+thrown)
	 * 	}
	 *  // find out the table of the record (similar to getEditedRecords)
	 *  var jstable = databaseManager.getTable(record);
	 *  var tableSQLName = jstable.getSQLName();
	 *  application.output('Table:'+tableSQLName+' in server:'+jstable.getServerName()+' failed to save.')
	 * }
	 * 
	 * @return Array of failed JSRecords
	 */
	public IRecordInternal[] js_getFailedRecords()
	{
		return application.getFoundSetManager().getEditRecordList().getFailedRecords();
	}

	/**
	 * Returns an array of edited records with outstanding (unsaved) data. 
	 * 
	 * NOTE: To return a dataset of outstanding (unsaved) edited data for each record, see JSRecord.getChangedData();
	 * NOTE2: The fields focus may be lost in user interface in order to determine the edits. 
	 * 
	 * @sample
	 * //This method can be used to loop through all outstanding changes,
	 * //the application.output line contains all the changed data, their tablename and primary key
	 * var editr = databaseManager.getEditedRecords()
	 * for (x=0;x<editr.length;x++)
	 * {
	 * 	var ds = editr[x].getChangedData();
	 * 	var jstable = databaseManager.getTable(editr[x]);
	 * 	var tableSQLName = jstable.getSQLName();
	 * 	var pkrec = jstable.getRowIdentifierColumnNames().join(',');
	 * 	var pkvals = new Array();
	 * 	for (var j = 0; j < jstable.getRowIdentifierColumnNames().length; j++)
	 * 	{
	 * 		pkvals[j] = editr[x][jstable.getRowIdentifierColumnNames()[j]];
	 * 	}
	 * 	application.output('Table: '+tableSQLName +', PKs: '+ pkvals.join(',') +' ('+pkrec +')');
	 * 	// Get a dataset with outstanding changes on a record
	 * 	for( var i = 1 ; i <= ds.getMaxRowIndex() ; i++ )
	 * 	{
	 * 		application.output('Column: '+ ds.getValue(i,1) +', oldValue: '+ ds.getValue(i,2) +', newValue: '+ ds.getValue(i,3));
	 * 	}
	 * }
	 * //in most cases you will want to set autoSave back on now
	 * databaseManager.setAutoSave(true);
	 * 
	 * @return Array of outstanding/unsaved JSRecords.
	 */
	public IRecordInternal[] js_getEditedRecords()
	{
		return application.getFoundSetManager().getEditRecordList().getEditedRecords();
	}

	/**
	 * Returns a dataset with outstanding (not saved) changed data on a record
	 * 
	 * NOTE: To return an array of records with oustanding changed data, see the function databaseManager.getEditedRecords(). 
	 *
	 * @sample
	 * var dataset = databaseManager.getChangedRecordData(record)
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	application.output(dataset.getValue(i,1) +' '+ dataset.getValue(i,2) +' '+ dataset.getValue(i,3));
	 * }
	 *
	 * @param record The specified record.
	 *
	 * @see com.servoy.j2db.dataprocessing.JSDatabaseManager#js_getEditedRecords()
	 * @see com.servoy.j2db.dataprocessing.Record#js_getChangedData()
	 */
	@Deprecated
	public JSDataSet js_getChangedRecordData(Object r) throws ServoyException
	{
		checkAuthorized();
		if (r instanceof IRecordInternal)
		{
			IRecordInternal rec = ((IRecordInternal)r);
			if (rec.getParentFoundSet() != null && rec.getRawData() != null)
			{
				String[] cnames = rec.getParentFoundSet().getSQLSheet().getColumnNames();
				Object[] oldd = rec.getRawData().getRawOldColumnData();
				List<Object[]> rows = new ArrayList<Object[]>();
				if (oldd != null || !rec.getRawData().existInDB())
				{
					Object[] newd = rec.getRawData().getRawColumnData();
					for (int i = 0; i < cnames.length; i++)
					{
						Object oldv = (oldd == null ? null : oldd[i]);
						if (!Utils.equalObjects(oldv, newd[i])) rows.add(new Object[] { cnames[i], oldv, newd[i] });
					}
				}
				return new JSDataSet(application, new BufferedDataSet(new String[] { "col_name", "old_value", "new_value" }, rows)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		return null;
	}

	/**
	 * Returns the internal SQL which defines the specified (related)foundset.
	 * Optionally, the foundset and table filter params can be excluded in the sql (includeFilters=false).
	 * Make sure to set the applicable filters when the sql is used in a loadRecords() call.
	 *
	 * @sample var sql = databaseManager.getSQL(foundset)
	 *
	 * @param foundset The JSFoundset to get the sql for.
	 * @param includeFilters optional, include the foundset and table filters, default true.
	 * 
	 * @return String representing the sql of the JSFoundset.
	 */
	public String js_getSQL(Object foundset, boolean includeFilters) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet)
		{
			try
			{
				QuerySet querySet = getQuerySet((FoundSet)foundset, includeFilters);
				StringBuilder sql = new StringBuilder();
				QueryString[] prepares = querySet.getPrepares();
				for (int i = 0; prepares != null && i < prepares.length; i++)
				{
					// TODO parameters from updates and cleanups
					// sql.append(updates[i].getSql());
					// sql.append("\n"); //$NON-NLS-1$
				}
				sql.append(querySet.getSelect().getSql());
				QueryString[] cleanups = querySet.getCleanups();
				for (int i = 0; cleanups != null && i < cleanups.length; i++)
				{
					// TODO parameters from updates and cleanups
					//sql.append("\n"); //$NON-NLS-1$
					//sql.append(cleanups[i].getSql());
				}
				return sql.toString();
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	public String js_getSQL(Object foundset) throws ServoyException
	{
		checkAuthorized();
		return js_getSQL(foundset, true);
	}

	/**
	 * Returns the internal SQL parameters, as an array, that are used to define the specified (related)foundset.
	 *
	 * @sample var sqlParameterArray = databaseManager.getSQLParameters(foundset)
	 *
	 * @param foundset The JSFoundset to get the sql parameters for.
	 * @param includeFilters optional, include the parameters for the filters, default true.
	 * 
	 * @return An Array with the sql parameter values.
	 */
	public Object[] js_getSQLParameters(Object foundset, boolean includeFilters) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet)
		{
			try
			{
				// TODO parameters from updates and cleanups
				QuerySet querySet = getQuerySet((FoundSet)foundset, includeFilters);
				Object[][] qsParams = querySet.getSelect().getParameters();
				if (qsParams == null || qsParams.length == 0)
				{
					return null;
				}
				return qsParams[0];
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return null;
	}

	public Object[] js_getSQLParameters(Object foundset) throws ServoyException
	{
		checkAuthorized();
		return js_getSQLParameters(foundset, true);
	}

	private QuerySet getQuerySet(FoundSet fs, boolean includeFilters) throws RepositoryException, RemoteException
	{
		String serverName = fs.getSQLSheet().getServerName();
		QuerySelect sqlSelect = fs.getPksAndRecords().getQuerySelectForReading();
		ArrayList<TableFilter> tableFilterParams;
		if (includeFilters)
		{
			tableFilterParams = ((FoundSetManager)application.getFoundSetManager()).getTableFilterParams(serverName, sqlSelect);
		}
		else
		{
			// get the sql without any filters
			sqlSelect = AbstractBaseQuery.deepClone(sqlSelect);
			sqlSelect.clearCondition(SQLGenerator.CONDITION_FILTER);
			tableFilterParams = null;
		}
		return application.getDataServer().getSQLQuerySet(serverName, sqlSelect, tableFilterParams, 0, -1, true);
	}

	/**
	 * Flushes the client data cache and requeries the data for a record (based on the record index) in a foundset or all records in the foundset.
	 * Used where a program external to Servoy has modified the database record.
	 * Record index of -1 will refresh all records in the foundset and 0 the selected record.
	 * 
	 * @sample
	 * //refresh the second record from the foundset.
	 * databaseManager.refreshRecordFromDatabase(foundset,2)
	 * //flushes all records in the related foundset  (-1 is or can be an expensive operation)
	 * databaseManager.refreshRecordFromDatabase(order_to_orderdetails,-1);
	 *
	 * @param foundset The JSFoundset to refresh
	 * @param index The index of the JSRecord that must be refreshed (or -1 for all).
	 * 
	 * @return true if the refresh was done. 
	 */
	public boolean js_refreshRecordFromDatabase(Object foundset, int index) throws ServoyException
	{
		checkAuthorized();
		int idx = index;
		if (foundset instanceof IFoundSetInternal && ((IFoundSetInternal)foundset).getTable() != null)
		{
			if (idx == -1)//refresh all
			{
				// TODO should be checked if the foundset is completely loaded and only has X records?
				// So we only flush those records not the complete table?
				((FoundSetManager)application.getFoundSetManager()).flushCachedDatabaseData(application.getFoundSetManager().getDataSource(
					((IFoundSetInternal)foundset).getTable()));
				return true;
			}
			else
			{
				if (idx == 0)
				{
					idx = ((IFoundSetInternal)foundset).getSelectedIndex() + 1;
				}
				IRecordInternal rec = ((IFoundSetInternal)foundset).getRecord(idx - 1);//row because used by javascript 1 based
				if (rec != null)
				{
					Row r = rec.getRawData();
					if (r != null)
					{
						try
						{
							r.rollbackFromDB();
							return true;
						}
						catch (Exception e)
						{
							Debug.error(e);
							return false;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns a foundset dataprovider (normally a column) as JavaScript array.
	 *
	 * @sample
	 * // returns an array with all order_id values of the specified foundset. 
	 * var array = databaseManager.getFoundSetDataProviderAsArray(foundset,'order_id');
	 *
	 * @param foundset The foundset
	 * @param dataprovider The dataprovider for the values of the array. 
	 * 
	 * @return An Array with the column values.
	 */
	public Object[] js_getFoundSetDataProviderAsArray(Object foundset, String dataprovider) throws ServoyException
	{
		checkAuthorized();
		if (foundset instanceof FoundSet && ((FoundSet)foundset).getSQLSheet().getTable() != null)
		{
			FoundSet fs = (FoundSet)foundset;
			FoundSetManager fsm = (FoundSetManager)application.getFoundSetManager();
			SQLSheet sheet = fs.getSQLSheet();
			Column column = sheet.getTable().getColumn(dataprovider);
			if (column != null)
			{
				IDataSet dataSet = null;
				if ((fs.hadMoreRows() || fs.getSize() > fsm.pkChunkSize) && !fsm.getEditRecordList().hasEditedRecords(fs))
				{
					// large foundset, query the column in 1 go
					QuerySelect sqlSelect = AbstractBaseQuery.deepClone(fs.getSqlSelect());
					ArrayList<IQuerySelectValue> cols = new ArrayList<IQuerySelectValue>(1);
					cols.add(new QueryColumn(sqlSelect.getTable(), column.getID(), column.getSQLName(), column.getType(), column.getLength()));
					sqlSelect.setColumns(cols);
					try
					{
						dataSet = fsm.getDataServer().performQuery(fsm.getApplication().getClientID(), sheet.getServerName(), fsm.getTransactionID(sheet),
							sqlSelect, fsm.getTableFilterParams(sheet.getServerName(), sqlSelect), false, 0, -1, IDataServer.FOUNDSET_LOAD_QUERY);
					}
					catch (RemoteException e)
					{
						Debug.error(e);
						return new Object[0];
					}
					catch (ServoyException e)
					{
						Debug.error(e);
						return new Object[0];
					}
				}
				else
				{
					// small foundset or there are edited records
					List<Column> pks = fs.getSQLSheet().getTable().getRowIdentColumns();
					if (pks.size() == 1 && pks.get(0).equals(column)) //if is pk optimize
					{
						PksAndRecordsHolder pksAndRecordsCopy = fs.getPksAndRecords().shallowCopy();
						if (pksAndRecordsCopy.getPks().hadMoreRows())
						{
							fs.queryForMorePKs(pksAndRecordsCopy, -1, true);
						}
						dataSet = pksAndRecordsCopy.getPks();
					}
				}

				if (dataSet != null)
				{
					Object[] retval = new Object[dataSet.getRowCount()];
					for (int i = 0; i < retval.length; i++)
					{
						Object value = dataSet.getRow(i)[0];
						if (column.hasFlag(Column.UUID_COLUMN))
						{
							// this is a UUID column, first convert to UUID (could be string or byte array (media)) - so we can get/use it as a valid uuid string
							value = Utils.getAsUUID(value, false);
						}
						retval[i] = value;
					}
					return retval;
				}
			}
			// cannot het the data via a dataset, use the records (could be slow)
			List<Object> lst = new ArrayList<Object>();
			for (int i = 0; i < fs.getSize(); i++)
			{
				IRecordInternal r = fs.getRecord(i);
				Object value = r.getValue(dataprovider);
				if (value instanceof Date)
				{
					value = new Date(((Date)value).getTime());
				}
				lst.add(value);
			}
			return lst.toArray();
		}
		return new Object[0];
	}

	/**
	 * Returns the server name from the datasource, or null if not a database datasource.
	 *
	 * @sample var servername = databaseManager.getDataSourceServerName(datasource);
	 *
	 * @param dataSource The datasource string to get the server name from.
	 * 
	 * @return The servername of the datasource.
	 */
	public String js_getDataSourceServerName(String dataSource)
	{
		String[] retval = DataSourceUtils.getDBServernameTablename(dataSource);
		if (retval == null) return null;
		return retval[0];
	}

	/**
	 * Returns the table name from the datasource, or null if not a database datasource.
	 *
	 * @sample var tablename = databaseManager.getDataSourceTableName(datasource);
	 *
	 * @param dataSource The datasource string to get the tablename from.
	 * 
	 * @return The tablename of the datasource.
	 */
	public String js_getDataSourceTableName(String dataSource)
	{
		String[] retval = DataSourceUtils.getDBServernameTablename(dataSource);
		if (retval == null) return null;
		return retval[1];
	}

	/**
	 * Returns the JSTable object from which more info can be obtained (like columns).
	 * The parameter can be a JSFoundset,JSRecord,datasource string or server/tablename combination.
	 *
	 * @sample
	 * var jstable = databaseManager.getTable(controller.getDataSource());
	 * //var jstable = databaseManager.getTable(foundset);
	 * //var jstable = databaseManager.getTable(record);
	 * //var jstable = databaseManager.getTable(datasource);
	 * var tableSQLName = jstable.getSQLName();
	 * var columnNamesArray = jstable.getColumnNames();
	 * var firstColumnName = columnNamesArray[0];
	 * var jscolumn = jstable.getColumn(firstColumnName);
	 * var columnLength = jscolumn.getLength();
	 * var columnType = jscolumn.getTypeAsString();
	 * var columnSQLName = jscolumn.getSQLName();
	 * var isPrimaryKey = jscolumn.isRowIdentifier();
	 *
	 * @param foundset/record/datasource/server_name The data where the JSTable can be get from.
	 * @param table_name optional The tablename of the first param is a servername string.
	 * 
	 * @return the JSTable get from the input.
	 */
	public JSTable js_getTable(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		try
		{
			String serverName = null;
			String tableName = null;
			if (vargs.length == 1)
			{
				if (vargs[0] instanceof IFoundSetInternal)
				{
					IFoundSetInternal fs = (IFoundSetInternal)vargs[0];
					if (fs.getTable() != null)
					{
						serverName = fs.getTable().getServerName();
						tableName = fs.getTable().getName();
					}
				}
				if (vargs[0] instanceof IRecordInternal)
				{
					IRecordInternal rec = (IRecordInternal)vargs[0];
					IFoundSetInternal fs = rec.getParentFoundSet();
					if (fs != null && fs.getTable() != null)
					{
						serverName = fs.getTable().getServerName();
						tableName = fs.getTable().getName();
					}
				}
				if (vargs[0] instanceof String)
				{
					String[] server_table = DataSourceUtils.getDBServernameTablename(vargs[0].toString());
					if (server_table != null)
					{
						serverName = server_table[0];
						tableName = server_table[1];
					}
				}
			}
			else if (vargs.length == 2)
			{
				if (vargs[0] instanceof String && vargs[1] instanceof String)
				{
					serverName = vargs[0].toString();
					tableName = vargs[1].toString();
				}
			}
			if (serverName != null)
			{
				IServer server = application.getSolution().getServer(serverName);
				if (server != null && tableName != null)
				{
					ITable t = server.getTable(tableName);
					if (t != null)
					{
						return new JSTable(t, server);
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	//strongly recommended to use a transaction
	//currently does not support compound pks
	/**
	 * Merge records from the same foundset, updates entire datamodel (via foreign type on columns) with destination 
	 * record pk, deletes source record. Do use a transaction!
	 * 
	 * This function is very handy in situations where duplicate data exists. It allows you to merge the two records 
	 * and move all related records in one go. Say the source_record is "Ikea" and the combined_destination_record is "IKEA", the 
	 * "Ikea" record is deleted and all records related to it (think of contacts and orders, for instance) will be related 
	 * to the "IKEA" record. 
	 * 
	 * The function takes an optional array of column names. If provided, the data in the named columns will be copied 
	 * from source_record to combined_destination_record. 
	 * 
	 * Note that it is essential for both records to originate from the same foundset, as shown in the sample code. 
	 * 
	 * @sample databaseManager.mergeRecords(foundset.getRecord(1),foundset.getRecord(2));
	 *
	 * @param source_record The source JSRecord to copy from.
	 * @param combined_destination_record The target/destination JSRecord to copy into.
	 * @param columnnamesarray_to_copy optional The column names Array that should be copied.
	 * 
	 * @return true if the records could me merged.
	 */
	public boolean js_mergeRecords(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		if (vargs.length >= 2 && vargs[0] instanceof IRecordInternal && vargs[1] instanceof IRecordInternal)
		{
			FoundSetManager fsm = (FoundSetManager)application.getFoundSetManager();
			try
			{
				IRecordInternal sourceRecord = (IRecordInternal)vargs[0];
				IRecordInternal combinedDestinationRecord = (IRecordInternal)vargs[1];

				if (sourceRecord.getParentFoundSet() != combinedDestinationRecord.getParentFoundSet())
				{
					return false;
				}

				Table mainTable = (Table)combinedDestinationRecord.getParentFoundSet().getTable();
				String mainTableForeignType = mainTable.getName();
				String transaction_id = fsm.getTransactionID(mainTable.getServerName());

				Object sourceRecordPK = null;
				Object combinedDestinationRecordPK = null;

				Column pkc = null;
				Iterator<Column> pk_it = mainTable.getRowIdentColumns().iterator();
				if (pk_it.hasNext())
				{
					pkc = pk_it.next();
					sourceRecordPK = sourceRecord.getValue(pkc.getDataProviderID());
					if (sourceRecordPK == null) sourceRecordPK = ValueFactory.createNullValue(pkc.getType());
					combinedDestinationRecordPK = combinedDestinationRecord.getValue(pkc.getDataProviderID());
					if (combinedDestinationRecordPK == null) combinedDestinationRecordPK = ValueFactory.createNullValue(pkc.getType());
					if (pk_it.hasNext()) return false;//multipk not supported
				}

				List<SQLStatement> updates = new ArrayList<SQLStatement>();

				IServer server = application.getSolution().getServer(mainTable.getServerName());
				if (server != null)
				{
					Iterator<String> it = server.getTableNames(false).iterator();
					while (it.hasNext())
					{
						String tableName = it.next();
						Table table = (Table)server.getTable(tableName);
						if (table.getRowIdentColumnsCount() > 1) continue;//not supported

						Iterator<Column> it2 = table.getColumns().iterator();
						while (it2.hasNext())
						{
							Column c = it2.next();
							if (c.getColumnInfo() != null)
							{
								if (mainTableForeignType.equalsIgnoreCase(c.getColumnInfo().getForeignType()))
								{
									//update table set foreigntypecolumn = combinedDestinationRecordPK where foreigntypecolumn = sourceRecordPK

									QueryTable qTable = new QueryTable(table.getName(), table.getCatalog(), table.getSchema());
									QueryUpdate qUpdate = new QueryUpdate(qTable);

									QueryColumn qc = new QueryColumn(qTable, c.getID(), c.getSQLName(), c.getType(), c.getLength(), c.getScale());
									qUpdate.addValue(qc, combinedDestinationRecordPK);

									ISQLCondition condition = new CompareCondition(ISQLCondition.EQUALS_OPERATOR, qc, sourceRecordPK);
									qUpdate.setCondition(condition);

									IDataSet pks = new BufferedDataSet();
									pks.addRow(new Object[] { ValueFactory.createTableFlushValue() });//unknown number of records changed

									SQLStatement statement = new SQLStatement(ISQLActionTypes.UPDATE_ACTION, table.getServerName(), table.getName(), pks,
										transaction_id, qUpdate, fsm.getTableFilterParams(table.getServerName(), qUpdate));

									updates.add(statement);
								}
							}
						}
					}
				}

				IDataSet pks = new BufferedDataSet();
				pks.addRow(new Object[] { sourceRecordPK });
				QueryTable qTable = new QueryTable(mainTable.getName(), mainTable.getCatalog(), mainTable.getSchema());
				QueryDelete qDelete = new QueryDelete(qTable);
				QueryColumn qc = new QueryColumn(qTable, pkc.getID(), pkc.getSQLName(), pkc.getType(), pkc.getLength(), pkc.getScale());
				ISQLCondition condition = new CompareCondition(ISQLCondition.EQUALS_OPERATOR, qc, sourceRecordPK);
				qDelete.setCondition(condition);
				SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, mainTable.getServerName(), mainTable.getName(), pks, transaction_id,
					qDelete, fsm.getTableFilterParams(mainTable.getServerName(), qDelete));
				statement.setExpectedUpdateCount(1); // check that the row is really deleted
				updates.add(statement);

				IFoundSetInternal sfs = sourceRecord.getParentFoundSet();
				if (combinedDestinationRecord.startEditing())
				{
					if (vargs.length >= 3 && vargs[2] != null && vargs[2].getClass().isArray())
					{
						Object dps[] = (Object[])vargs[2];
						for (Object element : dps)
						{
							if (element == null) continue;
							String dp = element.toString();
							if (sfs.getSQLSheet().getColumnIndex(dp) >= 0)
							{
								combinedDestinationRecord.setValue(dp, sourceRecord.getValue(dp));
							}
						}
					}
					fsm.getEditRecordList().stopEditing(true, combinedDestinationRecord);
				}
				else
				{
					return false;
				}

				Object[] results = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), updates.toArray(new ISQLStatement[updates.size()]));
				for (int i = 0; results != null && i < results.length; i++)
				{
					if (results[i] instanceof ServoyException)
					{
						throw (ServoyException)results[i];
					}
				}
				//sfs.deleteRecord(sfs.getRecordIndex(sourceRecord), true); not needed, will be flushed from memory in finally
				return true;
			}
			catch (Exception ex)
			{
				application.handleException(
					application.getI18NMessage("servoy.foundsetupdater.updateFailed"), new ApplicationException(ServoyException.SAVE_FAILED, ex)); //$NON-NLS-1$
			}
			finally
			{
				fsm.flushCachedDatabaseData(null);
			}
		}
		return false;
	}

	/**
	 * Returns the total number of records(rows) in a table.
	 * 
	 * NOTE: This can be an expensive operation (time-wise) if your resultset is large
	 *
	 * @sample
	 * //return the total number of rows in a table.
	 * var count = databaseManager.getTableCount(foundset);
	 *
	 * @param dataSource Data where a server table can be get from. Can be a foundset, a datasource name or a JSTable.
	 * 
	 * @return the total table count.
	 */
	public int js_getTableCount(Object dataSource) throws ServoyException
	{
		checkAuthorized();
		ITable table = null;
		if (dataSource instanceof IFoundSetInternal)
		{
			IFoundSetInternal foundset = (IFoundSetInternal)dataSource;
			table = foundset.getTable();
		}
		if (dataSource instanceof String)
		{
			JSTable jstable = js_getTable(new Object[] { dataSource.toString() });
			if (jstable != null)
			{
				table = jstable.getTable();
			}
		}
		else if (dataSource instanceof JSTable)
		{
			table = ((JSTable)dataSource).getTable();
		}
		return ((FoundSetManager)application.getFoundSetManager()).getTableCount(table);
	}

	/**
	 * Switches a named server to another named server with the same datamodel (recommended to be used in an onOpen method for a solution).
	 * return true if successful.
	 *
	 * @sample
	 * //dynamically changes a server for the entire solution, destination database server must contain the same tables/columns!
	 * //will fail if there is a lock, transaction , if repository_server is used or if destination server is invalid
	 * //in the solution keep using the sourceName every where to reference the server!  
	 * var success = databaseManager.switchServer('crm', 'crm1')
	 *
	 * @param sourceName The name of the source database server connection
	 * @param destinationName The name of the destination database server connection. 
	 * 
	 * @return true if the switch could be done.
	 */
	public boolean js_switchServer(String sourceName, String destinationName) throws ServoyException
	{
		checkAuthorized();
		if (IServer.REPOSITORY_SERVER.equals(sourceName)) return false;
		if (IServer.REPOSITORY_SERVER.equals(destinationName)) return false;
		if (((FoundSetManager)application.getFoundSetManager()).hasTransaction()) return false;
		if (((FoundSetManager)application.getFoundSetManager()).hasLocks(null)) return false;
		IServer server = null;
		try
		{
			server = application.getSolution().getServer(destinationName);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		try
		{
			if (server == null || !server.isValid()) return false;
		}
		catch (RemoteException e)
		{
			Debug.error(e);
			return false;
		}

		DataServerProxy pds = application.proxyDataServer();
		if (pds == null)
		{
			// no dataserver access yet?
			return false;
		}

		pds.switchServer(sourceName, destinationName);
		((FoundSetManager)application.getFoundSetManager()).flushCachedDatabaseData(null);//flush all
		return true;
	}


	/**
	 * Saves all outstanding (unsaved) data and exits the current record. 
	 * Optionally, by specifying a record or foundset, can save a single record or all reacords from foundset instead of all the data.
	 * 
	 * NOTE: The fields focus may be lost in user interface in order to determine the edits. 
	 * 
	 * @sample
	 * databaseManager.saveData();
	 * //databaseManager.saveData(foundset.getRecord(1));//save specific record
	 * //databaseManager.saveData(foundset);//save all records from foundset
	 *
	 * @param record/foundset optional The JSRecord to save.
	 * 
	 * @return true if the save was done without an error.
	 */
	public boolean js_saveData(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		if (vargs.length >= 1 && vargs[0] instanceof IRecordInternal)
		{
			return application.getFoundSetManager().getEditRecordList().stopEditing(true, (IRecordInternal)vargs[0]) == ISaveConstants.STOPPED;
		}
		if (vargs.length >= 1 && vargs[0] instanceof IFoundSetInternal)
		{
			return application.getFoundSetManager().getEditRecordList().stopEditing(true,
				Arrays.asList(application.getFoundSetManager().getEditRecordList().getEditedRecords((IFoundSetInternal)vargs[0]))) == ISaveConstants.STOPPED;
		}
		return application.getFoundSetManager().getEditRecordList().stopEditing(true) == ISaveConstants.STOPPED;
	}

	/**
	 * Returns a foundset object for a specified datasource or server and tablename. 
	 *
	 * @sample
	 * var fs = databaseManager.getFoundSet(controller.getDataSource())
	 * var ridx = fs.newRecord()
	 * var record = fs.getRecord(ridx)
	 * record.emp_name = 'John'
	 * databaseManager.saveData()
	 *
	 * @param server_name/data_source The servername or datasource to get a JSFoundset for.
	 * @param table_name optional The tablename of the first param was the servername.
	 * 
	 * @return A new JSFoundset for that datasource.
	 */
	public FoundSet js_getFoundSet(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		String dataSource;
		if (vargs != null && vargs.length == 1)
		{
			dataSource = String.valueOf(vargs[0]);
		}
		else if (vargs != null && vargs.length == 2)
		{
			// serverName, tableName
			dataSource = DataSourceUtils.createDBTableDataSource(String.valueOf(vargs[0]), String.valueOf(vargs[1]));
		}
		else
		{
			return null;
		}
		try
		{
			IFoundSetInternal fs = application.getFoundSetManager().getNewFoundSet(dataSource, null);
			fs.clear();//have to deliver a initialized foundset, user might call new record as next call on this one
			return (FoundSet)fs;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Gets the next sequence for a column which has a sequence defined in its column dataprovider properties.
	 * 
	 * NOTE: For more infomation on configuring the sequence for a column, see the section Auto enter options for a column from the Dataproviders chapter in the Servoy Developer User's Guide.
	 *
	 * @sample 
	 * var seqDataSource = forms.seq_table.controller.getDataSource();
	 * var nextValue = databaseManager.getNextSequence(seqDataSource, 'seq_table_value');
	 * application.output(nextValue);
	 * 
	 * nextValue = databaseManager.getNextSequence(databaseManager.getDataSourceServerName(seqDataSource), databaseManager.getDataSourceTableName(seqDataSource), 'seq_table_value')
	 * application.output(nextValue);
	 *
	 * @param dataSource|serverName The datasource that points to the table which has the column with the sequence,
	 * 								or the name of the server where the table can be found. If the name of the server
	 * 								is specified, then a second optional parameter specifying the name of the table
	 * 								must be used. If the datasource is specified, then the name of the table is not needed
	 * 								as the second argument.
	 * @param tableName optional The name of the table that has the column with the sequence. Use this parameter
	 * 							only if you specified the name of the server as the first parameter.
	 * @param columnName The name of the column that has a sequence defined in its properties.
	 * 
	 * @return The next sequence for the column, null if there was no sequence for that column 
	 *         or if there is no column with the given name.
	 */
	public Object js_getNextSequence(String dataSource, String columnName) throws ServoyException
	{
		checkAuthorized();
		String serverName = js_getDataSourceServerName(dataSource);
		if (serverName != null)
		{
			String tableName = js_getDataSourceTableName(dataSource);
			if (tableName != null) return js_getNextSequence(serverName, tableName, columnName);
		}
		return null;
	}

	@Deprecated
	public Object js_getNextSequence(String serverName, String tableName, String columnName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			IServer server = application.getRepository().getServer(serverName);
			if (server == null) return null;

			Table table = (Table)server.getTable(tableName);
			if (table == null) return null;

			int columnInfoID = table.getColumnInfoID(columnName);
			if (columnInfoID == -1) return null;

			return application.getDataServer().getNextSequence(serverName, tableName, columnName, columnInfoID);
		}
		catch (Exception e)
		{
			Debug.error(e);
			return null;
		}
	}

	/**
	 * Returns an array with all the server names used in the solution.
	 * 
	 * NOTE: For more detail on named server connections, see the chapter on Database Connections, beginning with the Introduction to database connections in the Servoy Developer User's Guide. 
	 *
	 * @sample var array = databaseManager.getServerNames()
	 * 
	 * @return An Array of servernames.
	 */
	public String[] js_getServerNames() throws ServoyException
	{
		checkAuthorized();
		//we use flattensolution to be sure we also take the combined server proxies from modules (which are combined in flatten solution)
		Map<String, IServer> sp = application.getFlattenedSolution().getSolution().getServerProxies();
		if (sp != null)
		{
			synchronized (sp)
			{
				return sp.keySet().toArray(new String[sp.size()]);
			}
		}
		return new String[0];
	}

	/**
	 * Returns the database product name as supplied by the driver for a server.
	 * 
	 * NOTE: For more detail on named server connections, see the chapter on Database Connections, beginning with the Introduction to database connections in the Servoy Developer User's Guide. 
	 *
	 * @sample var databaseProductName = databaseManager.getDatabaseProductName(servername)
	 *
	 * @param serverName The specified name of the database server connection.
	 * 
	 * @return A database product name.
	 */
	public String js_getDatabaseProductName(String serverName) throws ServoyException
	{
		checkAuthorized();
		try
		{
			IServer s = application.getSolution().getServer(serverName);
			if (s != null)
			{
				return s.getDatabaseProductName();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	/**
	 * Returns an array of all table names for a specified server.
	 *
	 * @sample
	 * //return all the table names as array
	 * var tableNamesArray =databaseManager.getTableNames('user_data');
	 * var firstTableName = tableNamesArray[0];
	 *
	 * @param serverName The server name to get the table names from.
	 * 
	 * @return An Array with the tables names of that server.
	 */
	public String[] js_getTableNames(String serverName) throws ServoyException
	{
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).getTableNames(serverName);
	}

	/**
	 * Returns an array of all view names for a specified server.
	 *
	 * @sample
	 * //return all the view names as array
	 * var viewNamesArray =databaseManager.getViewNames('user_data');
	 * var firstViewName = viewNamesArray[0];
	 *
	 * @param serverName The server name to get the view names from.
	 * 
	 * @return An Array with the view names of that server.
	 */
	public String[] js_getViewNames(String serverName) throws ServoyException
	{
		checkAuthorized();
		return ((FoundSetManager)application.getFoundSetManager()).getViewNames(serverName);
	}

	/**
	 * Returns true if the current client has any or the specified lock(s) acquired.
	 *
	 * @sample var hasLocks = databaseManager.hasLocks('mylock')
	 *
	 * @param lock_name optional The lock name to check.
	 * 
	 * @return true if the current client has locks or the lock.
	 */
	public boolean js_hasLocks(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		String lockName = (vargs != null && vargs.length > 0 && vargs[0] != null) ? vargs[0].toString() : null;
		return ((FoundSetManager)application.getFoundSetManager()).hasLocks(lockName);
	}

	/**
	 * Release all current locks the client has (optionally limited to named locks).
	 * return true if the locks are released.
	 *
	 * @sample databaseManager.releaseAllLocks('mylock')
	 *
	 * @param lock_name optional The lock name to release.
	 * 
	 * @return true if all locks or the lock is released. 
	 */
	public boolean js_releaseAllLocks(Object[] vargs) throws ServoyException
	{
		checkAuthorized();
		String lockName = (vargs != null && vargs.length > 0 && vargs[0] != null) ? vargs[0].toString() : null;
		return ((FoundSetManager)application.getFoundSetManager()).releaseAllLocks(lockName);
	}

	/*
	 * _____________________________________________________________ transaction methods
	 */

	/**
	 * Returns true if a transaction is committed; rollback if commit fails. 
	 * 
	 * @param saveFirst optional save edited records to the database first (default true)
	 * 
	 * @sampleas js_startTransaction()
	 * 
	 * @return if the transaction could be committed.
	 */
	public boolean js_commitTransaction(boolean saveFirst) throws ServoyException
	{
		checkAuthorized();
		IFoundSetManagerInternal fsm = application.getFoundSetManager();
		return fsm.commitTransaction(saveFirst);
	}

	public boolean js_commitTransaction() throws ServoyException
	{
		return js_commitTransaction(true);
	}

	/**
	 * Rollback a transaction started by databaseManager.startTransaction().
	 * 
	 * @param rollbackEdited optional also rollback deletes that are done between start and a rollback call that are not handled by autosave false and rollbackEditedRecords()  (default true)
	 * 
	 * @sampleas js_startTransaction() 
	 */
	public void js_rollbackTransaction(boolean rollbackEdited) throws ServoyException
	{
		checkAuthorized();
		IFoundSetManagerInternal fsm = application.getFoundSetManager();
		fsm.rollbackTransaction(rollbackEdited, true);
	}

	public void js_rollbackTransaction() throws ServoyException
	{
		js_rollbackTransaction(true);
	}

	/**
	 * Start a database transaction.
	 * If you want to avoid round trips to the server or avoid the posibility of blocking other clients 
	 * because of your pending changes, you can use databaseManager.setAutoSave(false/true) and databaseManager.rollbackEditedRecords().
	 * 
	 * startTransaction, commit/rollbackTransacton() does support rollbacking of record deletes which autoSave = false doesnt support.
	 *
	 * @sample 
	 * // starts a database transaction
	 * databaseManager.startTransaction()
	 * //Now let users input data
	 * 
	 * //when data has been entered do a commit or rollback if the data entry is canceld or the the commit did fail.  
	 * if (cancel || !databaseManager.commitTransaction())
	 * {
	 *   databaseManager.rollbackTransaction();
	 * }
	 */
	public void js_startTransaction() throws ServoyException
	{
		checkAuthorized();
		application.getFoundSetManager().startTransaction();
	}

	/**
	 * Enable/disable the default null validator for non null columns, makes it possible todo the checks later on when saving, when for example autosave is disabled.
	 *
	 * @sample
	 * databaseManager.nullColumnValidatorEnabled = false;//disable
	 * 
	 * //test if enabled
	 * if(databaseManager.nullColumnValidatorEnabled) application.output('null validation enabled')
	 */
	public boolean js_getNullColumnValidatorEnabled()
	{
		return ((FoundSetManager)application.getFoundSetManager()).getNullColumnValidatorEnabled();
	}

	public void js_setNullColumnValidatorEnabled(boolean enable)
	{
		((FoundSetManager)application.getFoundSetManager()).setNullColumnValidatorEnabled(enable);
	}

	/**
	 * Set autosave, if false then no saves will happen by the ui (not including deletes!). 
	 * Until you call databaseManager.saveData() or setAutoSave(true)
	 * 
	 * If you also want to be able to rollback deletes then you have to use databaseManager.startTransaction().
	 * Because even if autosave is false deletes of records will be done. 
	 *
	 * @sample
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 * 
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * databaseManager.setAutoSave(true)
	 *
	 * @param autoSave Boolean to enable or disable autosave.
	 * 
	 * @return false if the current edited record could not be saved.
	 */
	public boolean js_setAutoSave(boolean autoSave)
	{
		if (Debug.tracing())
		{
			if (autoSave) Debug.trace("Auto save enable"); //$NON-NLS-1$
			else Debug.trace("Auto save disabled"); //$NON-NLS-1$
		}
		return ((FoundSetManager)application.getFoundSetManager()).getEditRecordList().setAutoSave(autoSave);
	}

	/**
	 * Returns true or false if autosave is enabled or disabled.
	 *
	 * @sample
	 * //Set autosave, if false then no saves will happen by the ui (not including deletes!). Until you call saveData or setAutoSave(true)
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 * 
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * databaseManager.setAutoSave(true)
	 * 
	 * @return true if autosave if enabled.
	 */
	public boolean js_getAutoSave()
	{
		return application.getFoundSetManager().getEditRecordList().getAutoSave();
	}

	/**
	 * Turnoff the initial form foundset record loading, set this in the solution open method.
	 * Simular to calling foundset.clear() in the form's onload event.
	 * 
	 * NOTE: When the foundset record loading is turned off, controller.find or controller.loadAllRecords must be called to display the records
	 *
	 * @sample
	 * //this has to be called in the solution open method
	 * databaseManager.setCreateEmptyFormFoundsets()
	 */
	public void js_setCreateEmptyFormFoundsets()
	{
		((FoundSetManager)application.getFoundSetManager()).createEmptyFormFoundsets();
	}

	/**
	 * Rolls back in memory edited records that are outstanding (not saved). 
	 * Can specify a record or foundset as parameter to rollback.
	 * Best used in combination with the function databaseManager.setAutoSave()
	 * This does not include deletes, they do not honor the autosafe false flag so they cant be rollbacked by this call.
	 *
	 * @sample
	 * //Set autosave, if false then no saves will happen by the ui (not including deletes!). Until you call saveData or setAutoSave(true)
	 * //Rollbacks in mem the records that were edited and not yet saved. Best used in combination with autosave false.
	 * databaseManager.setAutoSave(false)
	 * //Now let users input data
	 * 
	 * //On save or cancel, when data has been entered:
	 * if (cancel) databaseManager.rollbackEditedRecords()
	 * //databaseManager.rollbackEditedRecords(foundset); // rollback all records from foundset
	 * //databaseManager.rollbackEditedRecords(foundset.getSelectedRecord()); // rollback only one record
	 * databaseManager.setAutoSave(true)
	 * 
	 * @param foundset/record optional A JSFoundset or a JSRecord to rollback
	 */
	public void js_rollbackEditedRecords(Object[] values) throws ServoyException
	{
		checkAuthorized();
		if (values.length == 0)
		{
			application.getFoundSetManager().getEditRecordList().rollbackRecords();
		}
		else
		{
			List<IRecordInternal> records = new ArrayList<IRecordInternal>();
			if (values[0] instanceof IRecordInternal)
			{
				records.add((IRecordInternal)values[0]);
			}
			if (values[0] instanceof IFoundSetInternal)
			{
				records.addAll(Arrays.asList(application.getFoundSetManager().getEditRecordList().getEditedRecords((IFoundSetInternal)values[0])));
				records.addAll(Arrays.asList(application.getFoundSetManager().getEditRecordList().getFailedRecords((IFoundSetInternal)values[0])));
			}
			if (records.size() > 0) application.getFoundSetManager().getEditRecordList().rollbackRecords(records);
		}
	}

	/**
	 * Returns true if there is an transaction active for this client.
	 *
	 * @sample var hasTransaction = databaseManager.hasTransaction()
	 * 
	 * @return true if the client has a transaction.
	 */
	public boolean js_hasTransaction()
	{
		return application.getFoundSetManager().hasTransaction();
	}

	/*
	 * _____________________________________________________________ helper methods methods
	 */

	/**
	 * Returns true if the (related)foundset exists and has records.
	 *
	 * @sample
	 * if (%%elementName%%.hasRecords(orders_to_orderitems))
	 * {
	 * 	//do work on relatedFoundSet
	 * }
	 * //if (%%elementName%%.hasRecords(foundset.getSelectedRecord(),'orders_to_orderitems.orderitems_to_products'))
	 * //{
	 * //	//do work on deeper relatedFoundSet
	 * //}
	 *
	 * @param foundset/record A JSFoundset to test or a JSRecord for which to test a relation 
	 * @param qualifiedRelationString optional The relationname if the first param is a JSRecord.
	 * 
	 * @return true if the foundset/relation has records.
	 */
	public boolean js_hasRecords(Object[] values)
	{
		return hasRecords(values);
	}

	public static boolean hasRecords(Object[] values)
	{
		if (values.length == 0) return false;

		if (values[0] instanceof IFoundSetInternal)
		{
			return (((IFoundSetInternal)values[0]).getSize() > 0);
		}
		else if (values[0] instanceof IRecordInternal)
		{
			IRecordInternal rec = (IRecordInternal)values[0];
			if (values.length > 1)
			{
				boolean retval = false;
				String relatedFoundSets = String.valueOf(values[1]);
				StringTokenizer tk = new StringTokenizer(relatedFoundSets, "."); //$NON-NLS-1$
				while (tk.hasMoreTokens())
				{
					String relationName = tk.nextToken();
					IFoundSetInternal rfs = rec.getRelatedFoundSet(relationName, null);
					if (rfs != null && rfs.getSize() > 0)
					{
						retval = true;
						rec = rfs.getRecord(0);
					}
					else
					{
						retval = false;
						break;
					}
				}
				return retval;
			}
		}
		return false;
	}

	/**
	 * Returns true if the specified foundset, on a specific index or in any of its records, or the specified record has changes.
	 *
	 * NOTE: The fields focus may be lost in user interface in order to determine the edits. 
	 * 
	 * @sample
	 * if (databaseManager.hasRecordChanges(foundset,2))
	 * {
	 * 	//do save or something else
	 * }
	 *
	 * @param foundset/record The JSFoundset or JSRecord to test if it has changes.
	 * @param foundset_index optional The record index in the foundset to test (not specified means has the foundset any changed records)
	 * 
	 * @return true if there are changes in the JSFoundset or JSRecord.
	 */
	public boolean js_hasRecordChanges(Object[] values)
	{
		if (values.length == 0) return false;

		IRecordInternal rec = null;
		if (values[0] instanceof IFoundSetInternal)
		{
			int rec_ind = 0;
			if (values.length > 1 && (rec_ind = Utils.getAsInteger(values[1])) > 0)
			{
				rec = ((IFoundSetInternal)values[0]).getRecord(rec_ind - 1);
			}
			else
			{
				EditRecordList el = application.getFoundSetManager().getEditRecordList();
				el.removeUnChangedRecords(true, false);
				IFoundSetInternal foundset = (IFoundSetInternal)values[0];
				// first the quick way of testing the foundset itself.
				if (el.hasEditedRecords(foundset))
				{
					return true;
				}
				// if not found then look if other foundsets had record(s) that are changed that also are in this foundset.
//				String ds = foundset.getDataSource();
//				IRecordInternal[] editedRecords = el.getEditedRecords();
//				for (IRecordInternal editedRecord : editedRecords)
//				{
//					IRecordInternal record = editedRecord;
//					if (record.getRawData() != null && !record.existInDataSource())
//					{
//						if (record.getParentFoundSet().getDataSource().equals(ds))
//						{
//							if (foundset.getRecord(record.getPK()) != null)
//							{
//								return true;
//							}
//						}
//					}
//				}
			}

		}
		else if (values[0] instanceof IRecordInternal)
		{
			rec = (IRecordInternal)values[0];
		}
		if (rec != null && rec.getRawData() != null)
		{
			return rec.getRawData().isChanged();
		}
		return false;
	}

	/**
	 * Returns true if the argument (foundSet / record) has at least one row that was not yet saved in the database.
	 *
	 * @sample
	 * var fs = databaseManager.getFoundSet(databaseManager.getDataSourceServerName(controller.getDataSource()),'employees');
	 * 	databaseManager.startTransaction();
	 * 	var ridx = fs.newRecord();
	 * 	var record = fs.getRecord(ridx);
	 * 	record.emp_name = 'John';
	 * 	if (databaseManager.hasNewRecords(fs)) {
	 * 		application.output("new records");
	 * 	} else {
	 * 		application.output("no new records");
	 * 	}
	 * 	databaseManager.saveData();
	 * 	databaseManager.commitTransaction();
	 *
	 * @param foundset/record The JSFoundset or JSRecord to test.
	 * @param foundset_index optional The record index in the foundset to test (not specified means has the foundset any new records)
	 * 
	 * @return true if the JSFoundset has new records or JSRecord is a new record.
	 */
	public boolean js_hasNewRecords(Object[] values)
	{
		if (values.length == 0) return false;

		IRecordInternal rec = null;
		if (values[0] instanceof IFoundSetInternal)
		{
			int rec_ind = 0;
			if (values.length > 1 && (rec_ind = Utils.getAsInteger(values[1])) > 0)
			{
				rec = ((IFoundSetInternal)values[0]).getRecord(rec_ind - 1);
			}
			else
			{
				IFoundSetInternal foundset = (IFoundSetInternal)values[0];
				EditRecordList el = application.getFoundSetManager().getEditRecordList();
				// fist test quickly for this foundset only.
				IRecordInternal[] editedRecords = el.getEditedRecords(foundset);
				for (IRecordInternal editedRecord : editedRecords)
				{
					IRecordInternal record = editedRecord;
					if (record.getRawData() != null && !record.existInDataSource())
					{
						return true;
					}
				}
				// if not found then look if other foundsets had record(s) that are new that also are in this foundset.
				String ds = foundset.getDataSource();
				editedRecords = el.getEditedRecords();
				for (IRecordInternal editedRecord : editedRecords)
				{
					IRecordInternal record = editedRecord;
					if (record.getRawData() != null && !record.existInDataSource())
					{
						if (record.getParentFoundSet().getDataSource().equals(ds))
						{
							if (foundset.getRecord(record.getPK()) != null)
							{
								return true;
							}
						}
					}
				}

			}

		}
		else if (values[0] instanceof IRecordInternal)
		{
			rec = (IRecordInternal)values[0];
		}
		if (rec != null && rec.getRawData() != null)
		{
			return !rec.existInDataSource();
		}
		return false;
	}

	/**
	 * Copies all matching non empty columns (if overwrite boolean is given all columns except pk/ident, if array then all columns except pk and array names).
	 * returns true if no error did happen.
	 * 
	 * NOTE: This function could be used to store a copy of records in an archive table. Use the getRecord() function to get the record as an object. 
	 *
	 * @sample
	 * for( var i = 1 ; i <= foundset.getSize() ; i++ )
	 * {
	 * 	var srcRecord = foundset.getRecord(i);
	 * 	var destRecord = otherfoundset.getRecord(i);
	 * 	if (srcRecord == null || destRecord == null) break;
	 * 	databaseManager.copyMatchingColumns(srcRecord,destRecord,true)
	 * }
	 * //saves any outstanding changes to the dest foundset
	 * controller.saveData();
	 *
	 * @param src_record The source record to be copied.
	 * @param dest_record The destination record to copy to.
	 * @param overwrite/array_of_names_not_overwritten optional true (default false) if everything can be overwritten or an array of names that shouldnt be overwritten.
	 * 
	 * @return true if no errors happend.
	 */
	public boolean js_copyMatchingColumns(Object[] values) throws ServoyException
	{
		checkAuthorized();
		Object src_record = values[0];
		Object dest_record = values[1];
		List<Object> al = new ArrayList<Object>();
		boolean overwrite = false;
		if (values.length > 2)
		{
			if (values[2] instanceof Boolean)
			{
				overwrite = ((Boolean)values[2]).booleanValue();
			}
			else if (values[2].getClass().isArray())
			{
				al = Arrays.asList((Object[])values[2]);
			}
		}
		if (src_record instanceof IRecordInternal && dest_record instanceof IRecordInternal)
		{
			try
			{
				IRecordInternal src_rec = (IRecordInternal)src_record;
				IRecordInternal dest_rec = (IRecordInternal)dest_record;

				SQLSheet destSheet = dest_rec.getParentFoundSet().getSQLSheet();
				Table dest_table = destSheet.getTable();
				boolean wasEditing = dest_rec.isEditing();
				if (dest_rec.startEditing())
				{
					Iterator<Column> it = dest_table.getColumns().iterator();
					while (it.hasNext())
					{
						Column c = it.next();
						ColumnInfo ci = c.getColumnInfo();
						if (ci != null && ci.isExcluded())
						{
							continue;
						}

						if (al.contains(c.getDataProviderID()))
						{
							// skip, also if value in dest_rec is null
							continue;
						}

						Object dval = dest_rec.getValue(c.getDataProviderID());
						if (dval == null ||
							(!dest_table.getRowIdentColumns().contains(c) && (overwrite || (al.size() > 0 && !al.contains(c.getDataProviderID())))))
						{
							int index = src_rec.getParentFoundSet().getSQLSheet().getColumnIndex(c.getDataProviderID());
							if (index != -1)
							{
								Object sval = src_rec.getValue(c.getDataProviderID());
								dest_rec.setValue(c.getDataProviderID(), sval);
							}
						}
					}
					if (!wasEditing)
					{
						dest_rec.stopEditing();
					}
					return true;
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		return false;
	}


	/**
	 * @see com.servoy.j2db.dataprocessing.JSDataSet#js_createDataSource(String, Object)
	 */
	@Deprecated
	public String js_createDataSource(Object[] args) throws ServoyException
	{
		checkAuthorized();
		if (args.length >= 3)
		{
			String name = String.valueOf(args[0]);
			if (args[1] instanceof IDataSet && args[2] instanceof Object[])
			{

				int[] intTypes = new int[((Object[])args[2]).length];
				for (int i = 0; i < ((Object[])args[2]).length; i++)
				{
					intTypes[i] = Utils.getAsInteger(((Object[])(args[2]))[i]);
				}
				try
				{
					return application.getFoundSetManager().createDataSourceFromDataSet(name, (IDataSet)args[1], intTypes);
				}
				catch (ServoyException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		return null;
	}

	/**
	 * Free resources allocated for a previously created data source
	 *
	 * @sample databaseManager.removeDataSource(uri);
	 *
	 * @param uri 
	 */
	@Deprecated
	public boolean js_removeDataSource(String uri)
	{
		try
		{
			return application.getFoundSetManager().removeDataSource(uri);
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "DatabaseManager"; //$NON-NLS-1$
	}

	public void destroy()
	{
		application = null;
	}

}
