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

package com.servoy.j2db.ui;

import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.JSRenderEvent;

/**
 * On render event executor.
 * 
 * @author gboros
 *
 */
public class RenderEventExecutor implements IRenderEventExecutor
{
	private String renderCallback;
	private IScriptExecuter renderScriptExecuter;
	private IRecordInternal renderRecord;
	private int renderIndex;
	private boolean renderIsSelected;

	private boolean isHiddenOnRender;

	public void setRenderCallback(String id)
	{
		renderCallback = id;
	}

	public void setRenderScriptExecuter(IScriptExecuter scriptExecuter)
	{
		renderScriptExecuter = scriptExecuter;
	}

	public boolean hasRenderCallback()
	{
		return renderCallback != null;
	}

	public void setRenderState(IRecordInternal record, int index, boolean isSelected)
	{
		renderRecord = record;
		renderIndex = index;
		renderIsSelected = isSelected;
	}

	private boolean isOnRenderRunningOnComponentPaint;

	public boolean isOnRenderRunningOnComponentPaint()
	{
		return isOnRenderRunningOnComponentPaint;
	}

	private boolean isFocused;

	public void fireOnRender(ISupportOnRenderCallback display, boolean hasFocus)
	{
		// don't fire if already is focused as no changes on component can happen
		if (!hasFocus || (hasFocus != isFocused)) fireOnRender(display, hasFocus, true);
		isFocused = hasFocus;
	}

	public void fireOnRender(ISupportOnRenderCallback display, boolean hasFocus, boolean isRunningOnComponentPaint)
	{
		if (renderScriptExecuter != null && renderCallback != null)
		{
			isOnRenderRunningOnComponentPaint = isRunningOnComponentPaint;
			isHiddenOnRender = false;
			boolean isDisplayVisible = display.isVisible();
			JSRenderEvent event = new JSRenderEvent();
			event.setElement(display);
			event.setHasFocus(hasFocus);
			event.setRecord(renderRecord);
			event.setIndex(renderIndex);
			event.setSelected(renderIsSelected);
			renderScriptExecuter.executeFunction(renderCallback, new Object[] { event }, false, display, false,
				StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName(), true);
			isOnRenderRunningOnComponentPaint = false;
			isHiddenOnRender = !display.isVisible() && (isDisplayVisible != display.isVisible());
		}
	}

	public boolean isComponentHiddenOnRender()
	{
		return isHiddenOnRender;
	}
}