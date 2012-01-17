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

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableAndRelationProvider;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.IQueryBuilderFactory;
import com.servoy.j2db.util.ServoyException;

/**
 * The foundset manager interface for handling all kinds of database functions.
 * 
 * @author jblok
 */
public interface IDatabaseManager extends ISaveConstants, ITableAndRelationProvider
{
	/**
	 * Start a transaction
	 */
	public void startTransaction();

	/**
	 * Commit a transaction
	 */
	public boolean commitTransaction();

	/**
	 * rollback a transaction
	 */
	public void rollbackTransaction();

	/**
	 * Check if a transaction is present
	 */
	public boolean hasTransaction();

	/**
	 * Get the transaction id, the client may have.
	 * 
	 * @param serverName the server name for which a transaction id is requested
	 * @return String the transaction id, returns null if none present.
	 */
	public String getTransactionID(String serverName) throws ServoyException;

	/**
	 * Get a datasource for a table object interface
	 * 
	 * @param table the table
	 * @return the datasource
	 */
	public String getDataSource(ITable table);

	/**
	 * Save data
	 * 
	 * @see ISaveConstants
	 * @return a constant
	 */
	public int saveData();

	/**
	 * Get a query factory for building queries.
	 * 
	 * @return a query factory
	 * @since 6.1
	 */
	public IQueryBuilderFactory getQueryFactory();

	/**
	 * Get a new foundset for the data source.
	 * @since 6.1
	 */
	public IFoundSet getFoundSet(String dataSource) throws ServoyException;

	/**
	 * Get a new foundset for the query.
	 * @since 6.1
	 */
	public IFoundSet getFoundSet(IQueryBuilder query) throws ServoyException;


	/**
	 * Performs a sql query with a query builder object.
	 * Will throw an exception if anything did go wrong when executing the query.
	 *
	 * <br>Table filters on the involved tables in the query are applied.
	 * 
	 * @param query IQueryBuilder query.
	 * @param max_returned_rows The maximum number of rows returned by the query.  
	 * 
	 * @return The IDataSet containing the results of the query.
	 * 
	 * @since 6.1
	 */
	public IDataSet getDataSetByQuery(IQueryBuilder query, int max_returned_rows) throws ServoyException;
}
