/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.scripting.solutionhelper;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.base.solutionmodel.IBaseSMMethod;

/**
 * Interface for mobile client inset list/list form manipulation.
 * @author acostescu
 */
@ServoyClientSupport(ng = false, mc = true, wc = false, sc = false)
public interface IBaseSHList
{
	/**
	 * This dataprovider's value will be presented as a 'count bubble' in each item of the list.
	 * @sample
	 * var list = solutionModel.getForm('created_by_sm_1').getInsetList(solutionModel.getForm('il1');
	 * 
	 * var newList = solutionModel.newListForm('created_by_sm_3', list.dataSource, list.textDataProviderID);
	 * newList.onAction = newList.getForm().newMethod('function aMethod(event){application.output("Hello world!");}');
	 */
	public String getCountDataProviderID();

	public void setCountDataProviderID(String countDataProviderID);

	/**
	 * This text will appear as the main text of items in the list if 'textDataProviderID' is not set.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public String getText();

	public void setText(String text);

	/**
	 * The text of this data-provider will appear as the main text of items in the list; if not specified, 'text' will be used instead.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public String getTextDataProviderID();

	public void setTextDataProviderID(String textDataPRoviderID);

	/**
	 * This text will appear as the secondary (smaller) text of items in the list if 'subtextDataProviderID' is not set.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public String getSubtext();

	public void setSubtext(String subtext);

	/**
	 * The text of this data-provider will appear as the secondary (smaller) text of items in the list; if not specified, 'subtext' will be used instead.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public String getSubtextDataProviderID();

	public void setSubtextDataProviderID(String subtextDataProviderID);

	/**
	 * This predefined icon will appear on items in the list if 'dataIconDataProviderID' is not set.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public String getDataIconType();

	public void setDataIconType(String dataIconType);

	/**
	 * The predefined icon of this data-provider will appear on items in the list; if not specified, 'dataIconType' will be used instead.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public String getDataIconDataProviderID();

	public void setDataIconDataProviderID(String dataIconDataProviderID);

	public void setOnAction(IBaseSMMethod method);

	/**
	 * This action will be executed when an item in the list is clicked.
	 * @sampleas {@link #getCountDataProviderID()}
	 */
	public IBaseSMMethod getOnAction();

	/**
	 * The styleClass of the list. Can have values from 'a' to 'e'.
	 * 
	 * @sample
	 * var list = solutionModel.getForm('created_by_sm_1').getInsetList('il1');
	 * 
	 * var newList = solutionModel.newListForm('created_by_sm_3', list.dataSource, list.textDataProviderID);
	 * newList.listStyleClass = 'e';
	 */
	public String getListStyleClass();

	public void setListStyleClass(String styleClass);

}
