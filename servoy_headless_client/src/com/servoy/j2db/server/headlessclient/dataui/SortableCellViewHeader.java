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
package com.servoy.j2db.server.headlessclient.dataui;

import java.awt.Insets;
import java.util.Iterator;

import javax.swing.ImageIcon;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.Resource;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.resource.BufferedDynamicImageResource;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.model.Model;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IScriptExecuter;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportText;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.BorderAndPadding;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.headlessclient.dnd.DraggableBehavior;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Represents the component of a colunm header in a {@link WebCellBasedView}
 * 
 * @author jblok
 */
public class SortableCellViewHeader extends WebMarkupContainer implements IProviderStylePropertyChanges, ISupportWebTabSeq
{
	private static final long serialVersionUID = 1L;

	public static final ResourceReference R_ARROW_OFF = new ResourceReference(IApplication.class, "images/arrow_off.png"); //$NON-NLS-1$
	public static final ResourceReference R_ARROW_DOWN = new ResourceReference(IApplication.class, "images/arrow_down.png"); //$NON-NLS-1$
	public static final ResourceReference R_ARROW_UP = new ResourceReference(IApplication.class, "images/arrow_up.png"); //$NON-NLS-1$

	public static final int ARROW_WIDTH = 7 + 2; // width of the arrow image in px; we leave 1px space on the left and 1px space on the right

	private final AbstractBase cellview;
	private final WebCellBasedView view;
	private String tooltip;

	private final class LabelResolverLink extends ServoySubmitLink implements IComponentResolver
	{
		private static final long serialVersionUID = 1L;
		private final SortableCellViewHeaderGroup group;
		private final String componentId;
		private boolean sortable = true;
		private boolean dropped;

		private LabelResolverLink(String id, boolean useAJAX, SortableCellViewHeaderGroup group, String componentId)
		{
			super(id, useAJAX);
			this.group = group;
			this.componentId = componentId;
		}

		public void setSortable(boolean sortable)
		{
			this.sortable = sortable;
		}

		public void setDropped(boolean dropped)
		{
			this.dropped = dropped;
		}

		/**
		 * @see wicket.ajax.markup.html.AjaxFallbackLink#onClick(wicket.ajax.AjaxRequestTarget)
		 */
		@Override
		public void onClick(AjaxRequestTarget target)
		{
			if (sortable && !dropped)
			{
				Page page = getPage();
				if (page instanceof MainPage)
				{
					// make sure the link is focused; there is difference across browsers
					((MainPage)page).componentToFocus(this);
				}
				group.sort(componentId, view);

				SortableCellViewHeader sortableCellViewHeader;
				for (Object header : view.getHeaderComponents())
				{
					sortableCellViewHeader = (SortableCellViewHeader)header;
					if (sortableCellViewHeader.equals(SortableCellViewHeader.this) && form.getOnSortCmdMethodID() >= 0)
					{
						sortableCellViewHeader.setResizeImage(group.getSortDirection() == SortColumn.DESCENDING ? SortableCellViewHeader.R_ARROW_UP
							: SortableCellViewHeader.R_ARROW_DOWN);
					}
					else
					{
						sortableCellViewHeader.setResizeImage(SortableCellViewHeader.R_ARROW_OFF);
					}
				}

				WebEventExecutor.generateResponse(target, page);
			}

			dropped = false;
		}

		/**
		 * @throws RepositoryException
		 * @see wicket.markup.resolver.IComponentResolver#resolve(wicket.MarkupContainer, wicket.markup.MarkupStream, wicket.markup.ComponentTag)
		 */
		public boolean resolve(MarkupContainer container, MarkupStream markupStream, ComponentTag tag)
		{
			if (tag.getName().equalsIgnoreCase("div") && get("headertext") == null) //$NON-NLS-1$ //$NON-NLS-2$
			{
				try
				{
					Iterator<IPersist> it2 = cellview.getAllObjects();
					while (it2.hasNext())
					{
						IPersist element = it2.next();
						if (componentId.equals(ComponentFactory.getWebID(form, element)))
						{
							String text = ""; //$NON-NLS-1$
							if (element instanceof ISupportText)
							{
								text = ((ISupportText)element).getText();
							}
							GraphicalComponent gc = (GraphicalComponent)view.labelsFor.get(((ISupportName)element).getName());
							if (gc != null)
							{
								text = gc.getText();
							}

							text = J2DBGlobals.getServiceProvider().getI18NMessageIfPrefixed(text);
							boolean hasHtml = TemplateGenerator.hasHTMLText(text);
							text = TemplateGenerator.getSafeText(text);
							if (!hasHtml)
							{
								text = Utils.stringReplace(text, " ", "&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
							}

							text = Text.processTags(text, view.getDataAdapterList());
							if (text != null && text.trim().equals("")) //$NON-NLS-1$
							{
								text = "&nbsp;"; //$NON-NLS-1$
							}

							Label headerText = new Label("headertext", text); //$NON-NLS-1$
							headerText.setEscapeModelStrings(false);
							autoAdd(headerText);

							return true;
						}
					}
				}
				catch (Exception ex)
				{
					Debug.error("can't create html table for " + cellview, ex); //$NON-NLS-1$
				}
			}
			return false;
		}
	}

	private final WebMarkupContainer headerColumnTable;
	private LabelResolverLink labelResolver;
	private Component resizeBar;
	private final String id;
	private final Form form;
	private WebEventExecutor executor;
	private String labelName = null;

	private final IApplication application;

	/**
	 * Construct.
	 * 
	 * @param id The component's id
	 * @param group The group of headers the new one will be added to
	 * @param cellview
	 */
	public SortableCellViewHeader(Form form, final WebCellBasedView view, final String id, final SortableCellViewHeaderGroup group, AbstractBase cellview,
		boolean useAJAX, IApplication application)
	{
		super(id);
		this.id = id;
		this.cellview = cellview;
		this.view = view;
		this.application = application;
		this.form = form;

		headerColumnTable = new WebMarkupContainer("headerColumnTable"); //$NON-NLS-1$
		headerColumnTable.add(labelResolver = new LabelResolverLink("sortLink", useAJAX, group, id)); //$NON-NLS-1$
		labelResolver.add(new AttributeModifier("class", true, group)); //$NON-NLS-1$

		boolean blockResize = false;

		Iterator<IPersist> iter = cellview.getAllObjects();
		while (iter.hasNext())
		{
			IPersist element = iter.next();
			if (element instanceof ISupportAnchors)
			{
				if (id.equals(ComponentFactory.getWebID(form, element)))
				{
					int anchors = ((ISupportAnchors)element).getAnchors();
					if (((anchors & IAnchorConstants.EAST) == 0) || ((anchors & IAnchorConstants.WEST) == 0))
					{
						blockResize = true;
						break;
					}
				}
			}
		}

		if ((!(cellview instanceof Portal) || ((Portal)cellview).getReorderable()) && useAJAX)
		{
			DraggableBehavior dragMoveBehavior = new DraggableBehavior()
			{
				private int startX;

				@Override
				protected void onDragEnd(String componentId, int x, int y, AjaxRequestTarget ajaxRequestTarget)
				{
					view.moveColumn(SortableCellViewHeader.this, x - startX, ajaxRequestTarget);
					labelResolver.setDropped(true);
				}

				@Override
				protected boolean onDragStart(String componentId, int x, int y, AjaxRequestTarget ajaxRequestTarget)
				{
					startX = x;
					return true;
				}

				@Override
				protected void onDrop(String componentId, String targetid, int x, int y, AjaxRequestTarget ajaxRequestTarget)
				{
					// TODO Auto-generated method stub

				}

				@Override
				protected void onDropHover(String componentId, String targeid, AjaxRequestTarget ajaxRequestTarget)
				{
					// TODO Auto-generated method stub

				}
			};
			dragMoveBehavior.setRenderOnHead(false);
			dragMoveBehavior.setYConstraint(true);
			dragMoveBehavior.setUseProxy(true);
			headerColumnTable.add(dragMoveBehavior);
		}

		if (cellview instanceof Portal && !((Portal)cellview).getSortable())
		{
			labelResolver.setSortable(false);
			resizeBar = new WebMarkupContainer("resizeBar"); //$NON-NLS-1$
		}
		else
		{
			resizeBar = new Image("resizeBar", SortableCellViewHeader.R_ARROW_OFF); //$NON-NLS-1$
		}

		if (!blockResize && (!(cellview instanceof Portal) || ((Portal)cellview).getResizeble()) && useAJAX)
		{
			DraggableBehavior dragResizeBehavior = new DraggableBehavior()
			{
				private int startX;

				@Override
				protected void onDragEnd(String componentId, int x, int y, AjaxRequestTarget ajaxRequestTarget)
				{
					view.resizeColumn(SortableCellViewHeader.this, x - startX);
				}

				@Override
				protected boolean onDragStart(String componentId, int x, int y, AjaxRequestTarget ajaxRequestTarget)
				{
					startX = x;
					return true;
				}

				@Override
				protected void onDrop(String componentId, String targetid, int x, int y, AjaxRequestTarget ajaxRequestTarget)
				{
				}

				@Override
				protected void onDropHover(String componentId, String targeid, AjaxRequestTarget ajaxRequestTarget)
				{
				}
			};
			dragResizeBehavior.setRenderOnHead(false);
			dragResizeBehavior.setYConstraint(true);
			resizeBar.add(dragResizeBehavior);
			resizeBar.add(new AttributeModifier("style", true, new Model<String>("cursor: col-resize;"))); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			resizeBar.add(new AttributeModifier("style", true, new Model<String>("cursor: pointer;"))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		headerColumnTable.add(resizeBar);
		add(headerColumnTable);


		try
		{
			Iterator<IPersist> it2 = cellview.getAllObjects();
			while (it2.hasNext())
			{
				IPersist element = it2.next();
				if (id.equals(ComponentFactory.getWebID(form, element)))
				{
					GraphicalComponent gc = (GraphicalComponent)view.labelsFor.get(((ISupportName)element).getName());
					if (gc != null && gc.getImageMediaID() > 0)
					{
						final int media_id = gc.getImageMediaID();
						final Media media = application.getFlattenedSolution().getMedia(media_id);
						if (media != null)
						{
							add(new StyleAppendingModifier(new Model<String>()
							{
								@Override
								public String getObject()
								{
									ResourceReference iconReference = new ResourceReference(media.getName())
									{
										private static final long serialVersionUID = 1L;

										@Override
										protected Resource newResource()
										{
											BufferedDynamicImageResource imgRes = new BufferedDynamicImageResource();
											ImageIcon icon = new ImageIcon(media.getMediaData());
											imgRes.setImage(ImageLoader.imageToBufferedImage((icon).getImage()));

											return imgRes;
										}
									};
									return "background-image: url(" + urlFor(iconReference) + "?id=" + media_id + //$NON-NLS-1$ //$NON-NLS-2$
										"); background-repeat: no-repeat; background-position: center right"; //$NON-NLS-1$
								}
							}));
						}

					}
					else if (gc != null && gc.getToolTipText() != null)
					{
						tooltip = application.getI18NMessageIfPrefixed(gc.getToolTipText());
						add(TooltipAttributeModifier.INSTANCE);
					}
					if (gc != null && gc.getOnRightClickMethodID() > 0)
					{
						executor = new WebEventExecutor(this, useAJAX);
						executor.setRightClickCmd(String.valueOf(gc.getOnRightClickMethodID()), null);
						labelName = gc.getName();
					}
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		Boolean dir = group.get(id);
		if (dir != null && form.getOnSortCmdMethodID() >= 0)
		{
			setResizeImage(dir ? SortableCellViewHeader.R_ARROW_DOWN : SortableCellViewHeader.R_ARROW_UP);
		}
	}

	public String getToolTipText()
	{
		return tooltip;
	}

	public void setResizeImage(ResourceReference resourceReference)
	{
		if (resizeBar instanceof Image)
		{
			((Image)resizeBar).setImageResourceReference(resourceReference);
		}
	}

	protected ChangesRecorder jsChangeRecorder = new ChangesRecorder(null, null);
	private int width;

	public void setWidth(int width)
	{
		int borderWidth = 0;
		Iterator<IPersist> it2 = cellview.getAllObjects();
		while (it2.hasNext())
		{
			IPersist element = it2.next();
			if (id.equals(ComponentFactory.getWebID(form, element)))
			{
				GraphicalComponent gc = (GraphicalComponent)view.labelsFor.get(((ISupportName)element).getName());
				if (gc != null)
				{
					TextualStyle styleObj = new TextualStyle();
					BorderAndPadding ins = TemplateGenerator.applyBaseComponentProperties(gc, view.fc.getForm(), styleObj,
						(Insets)TemplateGenerator.DEFAULT_LABEL_PADDING.clone(), null, application);
					if (ins.border != null) borderWidth = ins.border.left + ins.border.right;
				}
			}
		}

		// If there is no label-for, we leave place for the default border placed at the right of headers.
		if (view.labelsFor.size() == 0) borderWidth += TemplateGenerator.NO_LABELFOR_DEFAULT_BORDER_WIDTH;

		final int clientWidth = width - TemplateGenerator.SORTABLE_HEADER_PADDING - borderWidth; // we have only left padding
		this.width = clientWidth;
		StyleAppendingModifier styleModifier = new StyleAppendingModifier(new Model<String>()
		{
			@Override
			public String getObject()
			{
				return "width: " + clientWidth + "px"; //$NON-NLS-1$//$NON-NLS-2$
			}
		});
		add(styleModifier);
		headerColumnTable.add(styleModifier);
		labelResolver.add(new StyleAppendingModifier(new Model<String>()
		{
			@Override
			public String getObject()
			{
				return "width: " + (clientWidth - SortableCellViewHeader.ARROW_WIDTH) + "px"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}));
		getStylePropertyChanges().setChanged();
	}

	public int getWidth()
	{
		return width;
	}

	public void resetAutoAdd()
	{
		setAuto(false);
	}

	public IStylePropertyChanges getStylePropertyChanges()
	{
		return jsChangeRecorder;
	}

	public void setTabIndex(int tabIndex)
	{
		TabIndexHelper.setUpTabIndexAttributeModifier(labelResolver, tabIndex);
	}

	@Override
	protected void onRender(final MarkupStream markupStream)
	{
		super.onRender(markupStream);
		getStylePropertyChanges().setRendered();
	}

	public void setScriptExecuter(IScriptExecuter el)
	{
		if (executor != null) executor.setScriptExecuter(el);
	}

	public String getName()
	{
		return labelName;
	}
}
