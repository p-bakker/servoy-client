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


import java.awt.Dimension;
import java.awt.Point;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * A web component persist that is a child of another web-component (so as a property of another web-component)
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ChildWebComponent extends WebComponent implements IChildWebObject
{

//	private static final long serialVersionUID = 1L; // this shouldn't get serialized anyway for now; parent WebComponent just serializes it's json

	public static final String COMPONENT_PROPERTY_TYPE_NAME = "component";

	public final static String TYPE_NAME_KEY = "typeName";
	public final static String DEFINITION_KEY = "definition";
	public static final PropertyDescription NAME_PROPERTY_DESCRIPTION = new PropertyDescription(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(),
		StringPropertyType.INSTANCE);
	public static final PropertyDescription SIZE_PROPERTY_DESCRIPTION = new PropertyDescription(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(),
		DimensionPropertyType.INSTANCE);
	public static final PropertyDescription LOCATION_PROPERTY_DESCRIPTION = new PropertyDescription(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(),
		PointPropertyType.INSTANCE);
	public static final PropertyDescription ANCHORS_PROPERTY_DESCRIPTION = new PropertyDescription(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(),
		IntPropertyType.INSTANCE);

	private transient final String jsonKey;
	private transient int index;

	private final JSONObject fullJSONInFrmFile;

	private final PropertyDescription pdAsChildComponent;

	public static ChildWebComponent createNewInstance(IBasicWebObject webObject, PropertyDescription childPd, String jsonKey, int index, boolean isNew)
	{
		Pair<Integer, UUID> idAndUUID = WebObjectImpl.getNewIdAndUUID(webObject);
		return new ChildWebComponent(webObject, idAndUUID.getLeft().intValue(), idAndUUID.getRight(), jsonKey, index, isNew, childPd);
	}

	private ChildWebComponent(IBasicWebObject parent, int element_id, UUID uuid, String jsonKey, int index, boolean isNew,
		PropertyDescription pdAsChildComponent)
	{
		super(parent, element_id, uuid);

		this.jsonKey = jsonKey;
		this.index = index;

		JSONObject json = WebObjectImpl.getFullJSONInFrmFile(this, isNew);
		fullJSONInFrmFile = json != null ? json : new ServoyJSONObject();
		if (!fullJSONInFrmFile.has(DEFINITION_KEY)) fullJSONInFrmFile.put(DEFINITION_KEY, new ServoyJSONObject());
		this.pdAsChildComponent = pdAsChildComponent;
	}

	@Override
	public JSONObject getFullJsonInFrmFile()
	{
		return fullJSONInFrmFile;
	}

	@Override
	public IWebComponent getParentComponent()
	{
		IBasicWebComponent parentComponent = getParent().getParentComponent();
		if (parentComponent instanceof IWebComponent) return (IWebComponent)parentComponent;
		else return super.getParentComponent();
	}

	@Override
	public IBasicWebObject getParent()
	{
		return (IBasicWebObject)super.getParent();
	}

	@Override
	protected WebObjectBasicImpl createWebObjectImpl()
	{
		return new WebObjectImpl(this)
		{
			@Override
			public String getTypeName()
			{
				return getFullJsonInFrmFile().optString(TYPE_NAME_KEY);
			}

			@Override
			public void setTypeName(String arg)
			{
				getFullJsonInFrmFile().put(TYPE_NAME_KEY, arg);
			}

			@Override
			public JSONObject getJson()
			{
				return getFullJsonInFrmFile().optJSONObject(DEFINITION_KEY);
			}

			@Override
			public void setJsonInternal(JSONObject arg)
			{
				getFullJsonInFrmFile().put(DEFINITION_KEY, arg);
			}

			@Override
			protected PropertyDescription getChildPropertyDescription(String propertyName)
			{
				PropertyDescription pd = super.getChildPropertyDescription(propertyName);
				if (pd == null)
				{
					if (StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(propertyName))
					{
						return NAME_PROPERTY_DESCRIPTION;
					}
					if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(propertyName))
					{
						return SIZE_PROPERTY_DESCRIPTION;
					}
					if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(propertyName))
					{
						return LOCATION_PROPERTY_DESCRIPTION;
					}
					if (StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName().equals(propertyName))
					{
						return ANCHORS_PROPERTY_DESCRIPTION;
					}
				}
				return pd;
			}
		};
	}

	public String getJsonKey()
	{
		return jsonKey;
	}

	public int getIndex()
	{
		return index;
	}

	public void setIndex(int i)
	{
		index = i;
	}

	@Override
	public PropertyDescription getPropertyDescription()
	{
		return pdAsChildComponent;
	}

	/*
	 * redirect all methods to json, nothing else is saved
	 */

	@Override
	protected boolean hasPersistProperty(String propertyName)
	{
		return super.hasPersistProperty(propertyName) && !StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(propertyName) &&
			!StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(propertyName) &&
			!StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(propertyName) &&
			!StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName().equals(propertyName);
	}

	@Override
	public void setName(String arg)
	{
		setProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), arg);
	}

	@Override
	public String getName()
	{
		return (String)getProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
	}

	@Override
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		setProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), arg);
	}

	@Override
	public void setAnchors(int arg)
	{
		setProperty(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), arg);
	}

	@Override
	public int getAnchors()
	{
		return Utils.getAsInteger(getProperty(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName()));
	}

	@Override
	public void setSize(Dimension arg)
	{
		setProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), arg);
	}

	@Override
	public Dimension getSize()
	{
		Dimension size = (Dimension)getProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		if (size == null)
		{
			return new java.awt.Dimension(140, 20);
		}
		return size;
	}

	@Override
	public void setLocation(Point arg)
	{
		setProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), arg);
	}

	@Override
	public Point getLocation()
	{
		java.awt.Point point = (Point)getProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		if (point == null)
		{
			point = new Point(10, 10);
		}
		return point;
	}

}