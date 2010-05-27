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
package com.servoy.j2db.persistence;


import java.util.Iterator;

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.util.UUID;

/**
 * A normal tabpanel
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME)
public class TabPanel extends BaseComponent implements ISupportChilds, ISupportTabSeq
{
	//orientations, see also SwingConstants.TOP,RIGHT,BOTTOM,LEFT
	public static final int DEFAULT = 0;
	public static final int HIDE = -1;
	public static final int SPLIT_HORIZONTAL = -2;
	public static final int SPLIT_VERTICAL = -3;

	/*
	 * Attributes, do not change default values do to repository default_textual_classvalue
	 */
	private int tabOrientation;
	private java.awt.Color selectedTabColor = null;
	private boolean scrollTabs;
	private boolean closeOnTabs;
	private int onTabChangeMethodID;
	private int tabSeq = ISupportTabSeq.DEFAULT;

	/**
	 * Constructor I
	 */
	TabPanel(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.TABPANELS, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */
	/**
	 * Set the tabOrientation
	 * 
	 * @param arg the tabOrientation
	 */
	public void setTabOrientation(int arg)
	{
		checkForChange(tabOrientation, arg);
		tabOrientation = arg;
	}

	/**
	 * The position of the tabs related to the tab panel. Can be one of TOP, RIGHT, BOTTOM, LEFT,
	 * HIDE, SPLIT_HORIZONTAL, SPLIT_VERTICAL. The HIDE option makes the tabs invisible, SPLIT_HORIZONTAL
	 * makes the tab panel horizontal split pane, SPLIT_VERTICAL makes the tab panel vertical split pane.
	 */
	public int getTabOrientation()
	{
		return tabOrientation;
	}

	/*
	 * _____________________________________________________________ Methods for Tab handling
	 */
	public Iterator<IPersist> getTabs()
	{
		return new SortedTypeIterator<IPersist>(getAllObjectsAsList(), IRepository.TABS, PositionComparator.XY_PERSIST_COMPARATOR);
	}

	public Tab createNewTab(String text, String relationName, Form f) throws RepositoryException
	{
		Tab obj = (Tab)getRootObject().getChangeHandler().createNewObject(this, IRepository.TABS);
		//set all the required properties

		obj.setText(text);
		obj.setRelationName(relationName);
		obj.setContainsFormID(f.getID());

		addChild(obj);
		return obj;
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	/**
	 * Returns the selectedTabColor.
	 * 
	 * @return java.awt.Color
	 */
	@Deprecated
	public java.awt.Color getSelectedTabColor()
	{
		return selectedTabColor;
	}

	/**
	 * Sets the selectedTabColor.
	 * 
	 * @param selectedTabColor The selectedTabColor to set
	 */
	@Deprecated
	public void setSelectedTabColor(java.awt.Color arg)
	{
		checkForChange(selectedTabColor, arg);
		selectedTabColor = arg;
	}

	public boolean hasOneTab()
	{
		Iterator<IPersist> it = getTabs();
		if (it.hasNext()) it.next();
		if (it.hasNext()) return false;
		return true;
	}

	/**
	 * Flag that tells how to arrange the tabs if they don't fit on a single line.
	 * If this flag is set, then the tabs will stay on a single line, but there will
	 * be the possibility to scroll them to the left and to the right. If this flag
	 * is not set, then the tabs will be arranged on multiple lines.
	 */
	public boolean getScrollTabs()
	{
		return scrollTabs;
	}

	public void setScrollTabs(boolean arg)
	{
		checkForChange(scrollTabs, arg);
		scrollTabs = arg;
	}

	@Deprecated
	public boolean getCloseOnTabs()
	{
		return closeOnTabs;
	}

	@Deprecated
	public void setCloseOnTabs(boolean arg)
	{
		checkForChange(closeOnTabs, arg);
		closeOnTabs = arg;
	}

	/**
	 * Method to be executed when the selected tab is changed in the tab panel.
	 * 
	 * @templatedescription Callback method when the user changes tab in a tab panel
	 * @templatename onTabChange
	 * @templateparam Number previousIndex index of tab shown before the change
	 * @templateparam JSEvent event the event that triggered the action
	 * @templateaddtodo
	 */
	public int getOnTabChangeMethodID()
	{
		return onTabChangeMethodID;
	}

	/**
	 * Sets the onTabChangeMethodID.
	 * 
	 * @param arg The onTabChangeMethodID to set
	 */
	public void setOnTabChangeMethodID(int arg)
	{
		checkForChange(onTabChangeMethodID, arg);
		onTabChangeMethodID = arg;
	}

	public void setTabSeq(int arg)
	{
		if (arg < 1 && arg != ISupportTabSeq.DEFAULT && arg != ISupportTabSeq.SKIP) return;//irrelevant value from editor
		checkForChange(tabSeq, arg);
		tabSeq = arg;
	}

	public int getTabSeq()
	{
		return tabSeq;
	}

	@Override
	public String toString()
	{
		String name = getName();
		if (name != null && !(name = getName().trim()).equals("")) //$NON-NLS-1$
		{
			return name;
		}
		else
		{
			return "no name/provider"; //$NON-NLS-1$
		}
	}

}
