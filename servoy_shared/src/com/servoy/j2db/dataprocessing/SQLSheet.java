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


import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Function;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLCondition;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryInsert;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryUpdate;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.OpenProperties;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Place holder for the sqlstatement needed by the foundset generated by the SQLGenerator
 * 
 * @author jblok
 */
public class SQLSheet
{
	public static final int SELECT = 0;//select based on pimary key
	public static final int DELETE = 1;
	public static final int INSERT = 2;
	public static final int UPDATE = 3;
	public static final int RELATED_SELECT = 6; //select based on foreign key

	static int sheetIDCounter = 0;//for debugging only

	private final String connectionName;
	private final SafeArrayList<SQLDescription> sql;
	private final Map<Relation, SQLSheet> nonGlobalRelatedSQLSheets;//relation -> SQLSheet
	private Map<String, Integer> columnIndexes;//dataproviderID -> columnIndex
	private final int sheetID;
	private final Table table;
	private List<SortColumn> defaultSort;
	private final Map<String, SQLDescription> relatedForeignSQLAccess;

	private Map<String, Integer> allCalculationsTypes; //dataproviderID -> type
	private final IServiceProvider application;

	public SQLSheet(IServiceProvider app, String connectionName, Table table)//for root
	{
		this.table = table;
		this.connectionName = connectionName;
		this.application = app;
		sql = new SafeArrayList<SQLDescription>();
		nonGlobalRelatedSQLSheets = new ConcurrentHashMap<Relation, SQLSheet>();
		relatedForeignSQLAccess = new ConcurrentHashMap<String, SQLDescription>();
		sheetID = ++sheetIDCounter;
		flush(app, null);
	}

	/**
	 * This regenerates the calculations when a change to them happens in the developer. So only used when developing the solution.
	 */
	public void flush(IServiceProvider app, Relation relation)
	{
		if (relation != null)
		{
			nonGlobalRelatedSQLSheets.remove(relation);
			relatedForeignSQLAccess.remove(relation.getName());
		}
		else
		{
			// regenerate all cached calculations:
			allCalculationsTypes = new HashMap<String, Integer>(8);
			if (table != null)
			{
				try
				{
					Iterator< ? > it4 = app.getFlattenedSolution().getScriptCalculations(table, false);
					while (it4.hasNext())
					{
						ScriptCalculation sp = (ScriptCalculation)it4.next();
						allCalculationsTypes.put(sp.getDataProviderID(), new Integer(sp.getDataProviderType()));
					}
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
			}
		}
	}

	VariableInfo getCalculationOrColumnVariableInfo(String dataProviderID, int columnIndex)
	{
		if (columnIndex != -1)
		{
			Column c = table.getColumn(dataProviderID);
			return new VariableInfo(c.getType(), c.getLength(), c.getFlags());
		}

		Integer retVal = allCalculationsTypes.get(dataProviderID);
		return new VariableInfo((retVal != null ? retVal.intValue() : 0), Integer.MAX_VALUE /* allow unlimited value for unstored calcs */,
			Column.NORMAL_COLUMN);
	}

	public boolean containsCalculation(String dataProviderID)
	{
		return allCalculationsTypes.containsKey(dataProviderID);
	}

	public Iterator<String> getAllCalculationNames()
	{
		return allCalculationsTypes.keySet().iterator();
	}

	public List<String> getStoredCalculationNames()
	{
		List<String> storedCalcs = new ArrayList<String>();
		for (String calc : allCalculationsTypes.keySet())
		{
			if (getDataProviderIDsColumnMap().containsKey(calc))
			{
				storedCalcs.add(calc);
			}
		}

		return storedCalcs;
	}

	Map<String, Object> getAllUnstoredCalculationNamesWithNoValue()
	{
		Map< ? , Integer> columns = getDataProviderIDsColumnMap();
		HashMap<String, Object> retval = new HashMap<String, Object>(5);
		Iterator<String> it = allCalculationsTypes.keySet().iterator();
		while (it.hasNext())
		{
			String dp = it.next();
			if (!columns.containsKey(dp))
			{
				retval.put(dp, Row.UNINITIALIZED);
			}
		}
		return retval;
	}

	public Table getTable()
	{
		return table;
	}

	public Object[] getDuplicateRecordData(IServiceProvider app, Row toDuplicateRow)
	{
		Object[] toDuplicate = toDuplicateRow.getRawColumnData();
		SQLDescription desc = getSQLDescription(SELECT);
		List< ? > list = desc.getDataProviderIDsDilivery();
		Object[] array = new Object[toDuplicate.length];
		if (list.size() != array.length) throw new IllegalArgumentException("Data to duplicate MUST be created with (help) of this sheet"); //$NON-NLS-1$
		for (int i = 0; i < toDuplicate.length; i++)
		{
			Object obj = toDuplicate[i];
			if (obj instanceof ValueFactory.BlobMarkerValue)
			{
				obj = toDuplicateRow.getValue(i);
			}
			else if (obj instanceof ValueFactory.DbIdentValue && ((DbIdentValue)obj).getRow() == toDuplicateRow)
			{
				// only create a new db ident value if that db ident value belongs to the duplicated row (== pk db ident instead a a relation db ident that has to be kept!)
				obj = ValueFactory.createDbIdentValue();
			}
			array[i] = obj;
			try
			{
				Column c = table.getColumn((String)list.get(i));
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null)
				{
					if (ci.isDBIdentity())
					{
						array[i] = ValueFactory.createDbIdentValue();
					}
					else if (ci.hasSequence())
					{
						array[i] = c.getNewRecordValue(app);
					}
					if (ci.hasSystemValue())
					{
						array[i] = c.getNewRecordValue(app);
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		return array;
	}

	Object[] getNewRecordData(IServiceProvider app, FoundSet fs)
	{
		Object[][] creationArgs = null;

		SQLDescription desc = getSQLDescription(SELECT); //INSERT
		List< ? > list = desc.getDataProviderIDsDilivery();// RequiredDataProviderIDs();
		Column[] fcols = null;
		Relation relation = null;
		String relationName = fs.getRelationName();
		if (relationName != null)
		{
			try
			{
				relation = app.getFlattenedSolution().getRelation(relationName);
				if (relation != null)
				{
					fcols = relation.getForeignColumns();

					QuerySelect creationSQLString = fs.getCreationSqlSelect();
					Placeholder ph = creationSQLString.getPlaceholder(SQLGenerator.createRelationKeyPlaceholderKey(creationSQLString.getTable(),
						relation.getName()));
					if (ph != null && ph.isSet())
					{
						// a matrix as wide as the relation keys and 1 deep
						creationArgs = (Object[][])ph.getValue();
					}
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		Object[] array = new Object[list.size()];
		for (int i = 0; i < list.size(); i++)
		{
			try
			{
				boolean filled = false;
				Column c = table.getColumn((String)list.get(i));
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null && ci.isDBIdentity())
				{
					array[i] = ValueFactory.createDbIdentValue();
					filled = true;
				}
				else if (c.getRowIdentType() != Column.NORMAL_COLUMN && ci != null && ci.hasSequence())
				{
					//this is here for safety, it can happen that a form has (unwanted) still a related foundset which is created by relation based on primary key
					array[i] = c.getNewRecordValue(app);
					filled = true;
				}
				else
				{
					if (creationArgs != null && creationArgs.length != 0 && fcols != null) //created via relation, so fill the foreign key with foreign value
					{
						for (int j = 0; j < fcols.length; j++)
						{
							if (c.equals(fcols[j]) && ((relation.getOperators()[j] & ISQLCondition.OPERATOR_MASK) == ISQLCondition.EQUALS_OPERATOR))
							{
								// creationArgs is a matrix as wide as the relation keys and 1 deep
								array[i] = creationArgs[j][0];
								filled = true;
								break;
							}
						}
					}

				}
				if (!filled)
				{
					array[i] = c.getNewRecordValue(app);
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		return array;
	}

	void processCopyValues(IRecordInternal s)
	{
		SQLDescription desc = getSQLDescription(SELECT);
		if (desc == null)
		{
			return;
		}
		List< ? > list = desc.getDataProviderIDsDilivery();
		for (int i = 0; i < list.size(); i++)
		{
			try
			{
				String id = (String)list.get(i);
				Column c = table.getColumn(id);
				if (c != null)
				{
					ColumnInfo ci = c.getColumnInfo();
					if (ci != null && ci.getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
					{
						String lookupDataProviderID = ci.getLookupValue();
						Object obj = s.getValue(lookupDataProviderID);
						if (lookupDataProviderID != null && lookupDataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX) && !s.has(lookupDataProviderID))
						{
							ScriptMethod globalScriptMethod = application.getFlattenedSolution().getScriptMethod(lookupDataProviderID);
							if (globalScriptMethod != null)
							{
								IExecutingEnviroment scriptEngine = application.getScriptEngine();
								GlobalScope gscope = scriptEngine.getSolutionScope().getGlobalScope();
								Object function = gscope.get(globalScriptMethod.getName());
								if (function instanceof Function)
								{
									try
									{
										obj = scriptEngine.executeFunction(((Function)function), gscope, gscope, null, false, false);
									}
									catch (Exception e)
									{
										Debug.error(e);
									}
								}
							}
						}
						// Protect to writing null to null-protected columns. An exception gets written in the log.
						if (!((obj == null) && !c.getAllowNull())) s.setValue(id, obj, false);
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
	}

	/**
	 * Convert the raw value using the column converters when configured.
	 * @param val
	 * @param columnIndex
	 * @param sheet
	 * @param columnConverterManager
	 * @return
	 */
	Object convertValueToObject(Object val, int columnIndex, IColumnConverterManager columnConverterManager)
	{
		Object value = val;

		// check if column uses a converter
		if (columnIndex >= 0)
		{
			String dataProviderID = getColumnNames()[columnIndex];
			VariableInfo variableInfo = getCalculationOrColumnVariableInfo(dataProviderID, columnIndex);

			if ((variableInfo.flags & Column.UUID_COLUMN) != 0)
			{
				// this is a UUID column, first convert to UUID (could be string or byte array (media)) - so we can get/use it as a valid uuid string
				value = Utils.getAsUUID(value, false);
			}

			Pair<String, String> converterInfo = getColumnConverterInfo(columnIndex);
			if (converterInfo != null)
			{
				IColumnConverter conv = columnConverterManager.getConverter(converterInfo.getLeft());
				if (conv != null)
				{
					try
					{
						OpenProperties props = new OpenProperties();
						if (converterInfo.getRight() != null) props.load(new StringReader(converterInfo.getRight()));
						value = conv.convertToObject(props, variableInfo.type, value);
					}
					catch (Exception e)
					{
						Debug.error(e);
						throw new IllegalArgumentException(Messages.getString(
							"servoy.record.error.gettingDataprovider", new Object[] { dataProviderID, Column.getDisplayTypeString(variableInfo.type) }), e); //$NON-NLS-1$
					}
				}
				else
				{
					throw new IllegalArgumentException(Messages.getString(
						"servoy.record.error.gettingDataprovider", new Object[] { dataProviderID, Column.getDisplayTypeString(variableInfo.type) })); //$NON-NLS-1$
				}
			}
		}

		return value;
	}

	void addSelect(QuerySelect select, List<String> dataProviderIDsDilivery, List<String> requiredDataProviderIDs, List<String> oldRequiredDataProviderIDs)
	{
		sql.set(SELECT, new SQLDescription(select, dataProviderIDsDilivery, requiredDataProviderIDs, oldRequiredDataProviderIDs));
	}

	void addInsert(QueryInsert insert, List<String> requiredDataProviderIDs)
	{
		sql.set(INSERT, new SQLDescription(insert, null, requiredDataProviderIDs, null));
	}

	void addUpdate(QueryUpdate update, List<String> requiredDataProviderIDs, List<String> oldRequiredDataProviderIDs)
	{
		sql.set(UPDATE, new SQLDescription(update, null, requiredDataProviderIDs, oldRequiredDataProviderIDs));
	}

	void addDelete(QueryDelete delete, List<String> oldRequiredDataProviderIDs)
	{
		sql.set(DELETE, new SQLDescription(delete, null, null, oldRequiredDataProviderIDs));
	}

	void addRelatedSelect(String relationName, QuerySelect sqlQuery, List<String> dataProviderIDsDilivery, List<String> requiredDataProviderIDs,
		List<String> oldRequiredDataProviderIDs)
	{
		relatedForeignSQLAccess.put(relationName, new SQLDescription(sqlQuery, dataProviderIDsDilivery, requiredDataProviderIDs, oldRequiredDataProviderIDs));
	}


	public String getServerName()
	{
		return connectionName;
	}

	SQLSheet getRelatedSQLSheet(Relation relation, SQLGenerator generator) throws ServoyException
	{
		SQLSheet retval = nonGlobalRelatedSQLSheets.get(relation);
		if (retval == null)
		{
			if (relation.isParentRef())
			{
				retval = this;
			}
			else
			{
				retval = generator.getCachedTableSQLSheet(relation.getForeignDataSource());
				generator.makeRelatedSQL(retval, relation);
			}
			nonGlobalRelatedSQLSheets.put(relation, retval);
		}
		return retval;
	}

	public List<SortColumn> getDefaultPKSort()
	{
		if (table == null)
		{
			return null;
		}
		if (defaultSort == null)
		{
			List<SortColumn> ds = null;

			if (table.getRowIdentColumns().size() > 1)
			{
				// find a match with an index
				List<SortColumn>[] indexes = table.getIndexes();
				for (int i = 0; ds == null && indexes != null && i < indexes.length; i++)
				{
					List<SortColumn> index = indexes[i];
					List<Column> rowIdentColumnsCopy = new ArrayList<Column>(table.getRowIdentColumns());
					boolean match = index.size() == rowIdentColumnsCopy.size();
					for (int c = 0; match && c < index.size(); c++)
					{
						match = rowIdentColumnsCopy.remove(index.get(c).getColumn());
					}
					if (match)
					{
						ds = index;
					}
				}
			}

			if (ds == null) // no match on index
			{
				ds = new ArrayList<SortColumn>(table.getRowIdentColumns().size());
				// get key columns in db defined order
				for (Column column : table.getColumns())
				{
					if (table.getRowIdentColumns().contains(column))
					{
						ds.add(new SortColumn(column));
					}
				}
			}
			defaultSort = ds;
		}
		return defaultSort;
	}

	public SQLSheet getRelatedSheet(Relation relation, SQLGenerator generator)
	{
		try
		{
			if (relation != null && !relation.isGlobal())
			{
				return getRelatedSQLSheet(relation, generator);
			}
		}
		catch (ServoyException e)
		{
			Debug.error("cant get related sheet " + relation, e); //$NON-NLS-1$
		}
		return null;
	}

	public ISQLQuery getSQL(int type)
	{
		if (sql == null) return null;

		if (type < sql.size())
		{
			SQLDescription desc = sql.get(type);
			if (desc != null)
			{
				return desc.getSQLQuery();
			}
		}
		return null;
	}

	public Map< ? , Integer> getDataProviderIDsColumnMap()
	{
		if (columnIndexes == null)
		{
			Map<String, Integer> cols = new HashMap<String, Integer>(32);
			SQLDescription desc = sql.get(SELECT);
			if (desc != null)
			{
				List<String> dataProviderIDsDilivery = desc.getDataProviderIDsDilivery();
				for (int i = 0; i < dataProviderIDsDilivery.size(); i++)
				{
					cols.put(dataProviderIDsDilivery.get(i), new Integer(i));
				}
			}
			columnIndexes = cols;
		}
		return columnIndexes;
	}

	//used by subsummaryfoundset
	public void setDataProviderIDsColumnMap(HashMap<String, Integer> indexes)
	{
		if (columnIndexes != null) throw new IllegalStateException("cannot set the indexes when they are already set and used from other rootsets"); //$NON-NLS-1$
		columnIndexes = indexes;
	}

	/**
	 * Get the column index based on a dataProviderID
	 * 
	 * @param dataProviderID the dataprovider index to retrieve
	 * @return the index (-1 if not found)
	 */
	public int getColumnIndex(String dataProviderID)
	{
		Integer index = getDataProviderIDsColumnMap().get(dataProviderID);
		if (index == null)
		{
			return -1;
		}
		return index.intValue();
	}

	private String[] allNames;

	public String[] getColumnNames()
	{
		if (allNames == null)
		{
			SQLDescription desc = sql.get(SELECT); // not based on a table
			if (desc == null)
			{
				allNames = new String[0];
			}
			else
			{
				List< ? > dataProviderIDsDilivery = desc.getDataProviderIDsDilivery();
				allNames = dataProviderIDsDilivery.toArray(new String[dataProviderIDsDilivery.size()]);
			}
		}
		return allNames;
	}

	public SQLDescription getSQLDescription(int sqlType)
	{
		return sql.get(sqlType);
	}

	public SQLDescription getRelatedSQLDescription(String relationName)
	{
		return relatedForeignSQLAccess.get(relationName);
	}

	private int[] sheetPKindexes;

	int[] getPKIndexes()
	{
		if (sheetPKindexes == null)
		{
			SQLSheet.SQLDescription desc = getSQLDescription(SQLSheet.UPDATE);
			List< ? > list = desc.getOldRequiredDataProviderIDs(); //==pk
			int[] pis = new int[list.size()];
			for (int i = 0; i < list.size(); i++)
			{
				String id = (String)list.get(i);
				pis[i] = getColumnIndex(id);
				if (pis[i] == -1)
				{
					throw new IllegalStateException("Could not find PK index for column " + id + " in sheet " + this); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			sheetPKindexes = pis;
		}
		return sheetPKindexes;
	}

	private Integer sheetIdentIndex;

	/**
	 * Find the index of an identity column. This may be a non-pk column.
	 * 
	 * @return identity column index, -1 if not found
	 */
	int getIdentIndex()
	{
		if (sheetIdentIndex == null)
		{
			SQLSheet.SQLDescription desc = getSQLDescription(SQLSheet.UPDATE);
			List< ? > list = desc.getRequiredDataProviderIDs();

			for (int i = 0; sheetIdentIndex == null && i < list.size(); i++)
			{
				String dataProviderID = (String)list.get(i);
				Column c = table.getColumn(dataProviderID);

				if (c.getColumnInfo().isDBIdentity())
				{
					sheetIdentIndex = new Integer(i);
				}
			}
			if (sheetIdentIndex == null)
			{
				sheetIdentIndex = new Integer(-1);
			}
		}
		return sheetIdentIndex.intValue();
	}

	private String[] sheetPKColumns = null;

	public String[] getPKColumnDataProvidersAsArray()
	{
		if (sheetPKColumns == null)
		{
			SQLSheet.SQLDescription desc = getSQLDescription(SQLSheet.UPDATE);
			List< ? > list = desc.getOldRequiredDataProviderIDs(); //==pk
			String[] spks = new String[list.size()];
			for (int i = 0; i < list.size(); i++)
			{
				String id = (String)list.get(i);
				spks[i] = id;
			}
			sheetPKColumns = spks;
		}
		return sheetPKColumns;
	}

	public int getSheetID()
	{
		return sheetID;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("SQLSheet [" + sheetID + "]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		int type = 0;
		Iterator<SQLDescription> it = sql.iterator();
		while (it.hasNext())
		{
			SQLDescription element = it.next();
			sb.append("SQL [" + type + "] " + element + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			type++;
		}
		Iterator< ? > it2 = relatedForeignSQLAccess.entrySet().iterator();
		while (it2.hasNext())
		{
			Map.Entry< ? , ? > entry = (Map.Entry< ? , ? >)it2.next();
			SQLDescription element = (SQLDescription)entry.getValue();
			sb.append("Related SQL [relation: " + entry.getKey() + "] " + element + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			type++;
		}
		return sb.toString();
	}

	class SQLDescription
	{
		private final ISQLQuery sqlQuery;
		private final List<String> dataProviderIDsDilivery;
		private final List<String> requiredDataProviderIDs;
		private final List<String> oldRequiredDataProviderIDs;

		SQLDescription(ISQLQuery sqlQuery, List<String> dataProviderIDsDilivery, List<String> requiredDataProviderIDs, List<String> oldRequiredDataProviderIDs)
		{
			this.dataProviderIDsDilivery = dataProviderIDsDilivery;
			this.requiredDataProviderIDs = requiredDataProviderIDs;
			this.oldRequiredDataProviderIDs = oldRequiredDataProviderIDs;
			this.sqlQuery = sqlQuery;
		}

		public ISQLQuery getSQLQuery()
		{
			return sqlQuery;
		}

		public List<String> getDataProviderIDsDilivery()
		{
			return dataProviderIDsDilivery;
		}

		public List<String> getRequiredDataProviderIDs()
		{
			return requiredDataProviderIDs;
		}

		public List<String> getOldRequiredDataProviderIDs()
		{
			return oldRequiredDataProviderIDs;
		}

		@Override
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append(getSQLQuery());
			sb.append(" ["); //$NON-NLS-1$
			sb.append((dataProviderIDsDilivery != null ? dataProviderIDsDilivery.size() : 0));
			sb.append(","); //$NON-NLS-1$
			sb.append((requiredDataProviderIDs != null ? requiredDataProviderIDs.size() : 0));
			sb.append(","); //$NON-NLS-1$
			sb.append((oldRequiredDataProviderIDs != null ? oldRequiredDataProviderIDs.size() : 0));
			sb.append("]"); //$NON-NLS-1$
			return sb.toString();
		}
	}

	private HashMap<String, QuerySelect> aggregate;
	private HashMap<String, Collection<String>> aggregate_dataproviders;

	void addAggregate(String name, String dataProviderIDToAggregate, QuerySelect sqlSelect)
	{
		if (aggregate == null)
		{
			aggregate = new HashMap<String, QuerySelect>(3);
			aggregate_dataproviders = new HashMap<String, Collection<String>>(3);
		}
		aggregate.put(name, sqlSelect);
		Collection<String> aggregates = aggregate_dataproviders.get(dataProviderIDToAggregate);
		if (aggregates == null)
		{
			aggregates = new ArrayList<String>();
			aggregate_dataproviders.put(dataProviderIDToAggregate, aggregates);
		}
		aggregates.add(name); //lowercase is only safety ,due to bad impl earlier
	}

	Map<String, QuerySelect> getAggregates()
	{
		return aggregate;
	}

	public boolean isUsedByAggregate(String dataProviderIDToAggregate)
	{
		if (aggregate_dataproviders == null) return false;
		return (aggregate_dataproviders.containsKey(dataProviderIDToAggregate));
	}

	public Collection<String> getAggregateName(String dataProviderIDToAggregate) //reverse lookup
	{
		if (aggregate_dataproviders == null) return null;
		return aggregate_dataproviders.get(dataProviderIDToAggregate);
	}

	public boolean containsAggregate(String name)
	{
		if (aggregate == null || name == null) return false;
		return aggregate.containsKey(name);
	}

	public String[] getAggregateNames()
	{
		if (aggregate == null) return new String[0];
		String[] retval = new String[aggregate.size()];
		aggregate.keySet().toArray(retval);
		return retval;
	}

	public String[] getCalculationNames()
	{
		String[] retval = new String[allCalculationsTypes.size()];
		allCalculationsTypes.keySet().toArray(retval);
		return retval;
	}

	private Pair<String, String>[] converterInfos;


	public Pair<String, String> getColumnConverterInfo(int columnIndex)
	{
		if (converterInfos == null)
		{
			SQLDescription desc = sql.get(SELECT);
			List< ? > dataProviderIDsDilivery = desc.getDataProviderIDsDilivery();
			@SuppressWarnings("unchecked")
			Pair<String, String>[] cis = new Pair[dataProviderIDsDilivery.size()];
			int i = 0;
			Iterator< ? > it = dataProviderIDsDilivery.iterator();
			while (it.hasNext())
			{
				String cdp = (String)it.next();
				Column c = table.getColumn(cdp);
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null && ci.getConverterName() != null && ci.getConverterName().trim().length() != 0)
				{
					cis[i] = new Pair<String, String>(ci.getConverterName(), ci.getConverterProperties());
				}
				i++;
			}
			converterInfos = cis;
		}

		return converterInfos[columnIndex];
	}

	private Pair<String, String>[] validatorInfos;


	public Pair<String, String> getColumnValidatorInfo(int columnIndex)
	{
		if (validatorInfos == null)
		{
			SQLDescription desc = sql.get(SELECT);
			List< ? > dataProviderIDsDilivery = desc.getDataProviderIDsDilivery();
			@SuppressWarnings("unchecked")
			Pair<String, String>[] vis = new Pair[dataProviderIDsDilivery.size()];
			int i = 0;
			Iterator< ? > it = dataProviderIDsDilivery.iterator();
			while (it.hasNext())
			{
				String cdp = (String)it.next();
				Column c = table.getColumn(cdp);
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null && ci.getValidatorName() != null && ci.getValidatorName().trim().length() != 0)
				{
					vis[i] = new Pair<String, String>(ci.getValidatorName(), ci.getValidatorProperties());
				}
				i++;
			}
			validatorInfos = vis;
		}

		return validatorInfos[columnIndex];
	}

	public static boolean isOracleServer(IServer server)
	{
		try
		{
			return (server.getDatabaseProductName().toLowerCase().indexOf("oracle") != -1); //$NON-NLS-1$
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Struct class for variable info
	 * 
	 * @author rgansevles
	 * 
	 */
	public class VariableInfo
	{
		public final int type;
		public final int length;
		public final int flags;

		public VariableInfo(int type, int length, int flags)
		{
			this.type = type;
			this.length = length;
			this.flags = flags;
		}
	}

}
