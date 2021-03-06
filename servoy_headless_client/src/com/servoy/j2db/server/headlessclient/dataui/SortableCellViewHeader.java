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

import java.awt.Dimension;
import java.awt.Event;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.text.html.CSS;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.Resource;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.SimpleAttributeModifier;
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
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportText;
import com.servoy.j2db.persistence.ISupportTextSetup;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.headlessclient.MainPage;
import com.servoy.j2db.server.headlessclient.TabIndexHelper;
import com.servoy.j2db.server.headlessclient.WebClient;
import com.servoy.j2db.server.headlessclient.WebClientSession;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.BorderAndPadding;
import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;
import com.servoy.j2db.server.headlessclient.dnd.DraggableBehavior;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.ui.IStylePropertyChanges;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Pair;
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
				int eventModifiers = WebEventExecutor.convertModifiers(getModifiers());
				group.sort(componentId, view, eventModifiers);

				SortableCellViewHeader sortableCellViewHeader;
				for (Object header : view.getHeaderComponents())
				{
					sortableCellViewHeader = (SortableCellViewHeader)header;
					if (sortableCellViewHeader.equals(SortableCellViewHeader.this) && form.getOnSortCmdMethodID() >= 0)
					{
						sortableCellViewHeader.setResizeImage(group.getSortDirection() == SortColumn.DESCENDING ? view.R_ARROW_UP : view.R_ARROW_DOWN);
					}
					else if ((eventModifiers & Event.SHIFT_MASK) == 0)
					{
						sortableCellViewHeader.setResizeImage(view.R_ARROW_OFF);
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

							WebClient webClient = ((WebClientSession)Session.get()).getWebClient();
							text = StripHTMLTagsConverter.convertMediaReferences(text, webClient.getSolutionName(), new ResourceReference("media"), "", false).toString(); //$NON-NLS-1$ //$NON-NLS-2$

							Label headerText = new Label("headertext", text); //$NON-NLS-1$
							if (width > -1)
							{
								headerText.add(new StyleAppendingModifier(new Model<String>()
								{
									@Override
									public String getObject()
									{
										return "width: " + LabelResolverLink.this.width + "px";
									}
								}));

								headerText.add(new StyleAppendingModifier(new Model<String>()
								{
									@Override
									public String getObject()
									{
										return "position: relative";
									}
								}));
							}
							String inlineStyleStr = view.getHeaderMarginStyle();
							if (inlineStyleStr != null)
							{
								applyInlineStyleString(headerText, inlineStyleStr);
							}
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

		private int width = -1;

		void setWidth(int width)
		{
			this.width = width;
			add(new StyleAppendingModifier(new Model<String>()
			{
				@Override
				public String getObject()
				{
					return "width: " + LabelResolverLink.this.width + "px";
				}
			}));
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

	private boolean isUnmovable;

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
		labelResolver.add(new StyleAppendingModifier(new Model<String>("white-space: nowrap;text-overflow: clip;")));
		boolean hasBgImage = false;
		// append background image in case of labelFor for the current label (goes through all labelFor components in the map to get the component by name )
		GraphicalComponent labelFor = getLabelComponent();
		if (labelFor != null)
		{
			Pair<IStyleSheet, IStyleRule> pair = ComponentFactory.getStyleForBasicComponent(application, labelFor, form);
			IStyleRule cssRule = pair == null || pair.getRight() == null ? null : pair.getRight();
			if (cssRule != null && cssRule.hasAttribute(CSS.Attribute.BACKGROUND_IMAGE.toString()))
			{
				TextualStyle headerStyle = new TextualStyle();
				headerStyle.setProperty(CSS.Attribute.BACKGROUND_IMAGE.toString(), cssRule.getValues(CSS.Attribute.BACKGROUND_IMAGE.toString()), true);
				String text = headerStyle.getValuesAsString(null);
				WebClient webClient = ((WebClientSession)Session.get()).getWebClient();
				text = StripHTMLTagsConverter.convertMediaReferences(text, webClient.getSolutionName(), new ResourceReference("media"), "", false).toString();
				add(new StyleAppendingModifier(new Model<String>(text)));
				hasBgImage = true;
			}
		}


		ChangesRecorder changesRecorder = new ChangesRecorder();
		changesRecorder.setBorder(view.getHeaderBorder());
		String inlineStyleStr = view.getHeaderBgColorStyle();


		if (view.getHeaderBorder() != null)
		{
			add(new StyleAppendingModifier(new Model<String>("border-right: none; padding: 0px;"))); //$NON-NLS-1$
		}

		final Properties changes = changesRecorder.getChanges();
		if (changes.size() > 0) applyStyleChanges(headerColumnTable, changes);
		if (inlineStyleStr != null) applyInlineStyleString(this, inlineStyleStr);

		inlineStyleStr = view.getHeaderBgImageStyle();
		if (inlineStyleStr != null) applyInlineStyleString(headerColumnTable, inlineStyleStr);

		//margin is applied to LabelResolverLink on the label (to mimic Label component margin behavior)

		ChangesRecorder textChangesRecorder = new ChangesRecorder();
		textChangesRecorder.setFont(view.getHeaderFont());
		inlineStyleStr = view.getHeaderFgColorStyle();

		final Properties textChanges = textChangesRecorder.getChanges();
		if (textChanges.size() > 0) applyStyleChanges(labelResolver, textChanges);
		if (inlineStyleStr != null) applyInlineStyleString(labelResolver, inlineStyleStr);

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
					}

					isUnmovable = ((anchors & IAnchorConstants.NORTH) == IAnchorConstants.NORTH) &&
						((anchors & IAnchorConstants.SOUTH) == IAnchorConstants.SOUTH);
					break;
				}
			}
		}

		if (((!(cellview instanceof Portal) || ((Portal)cellview).getReorderable()) && useAJAX) && !isUnmovable())
		{
			DraggableBehavior dragMoveBehavior = new DraggableBehavior()
			{
				private int startX;

				@Override
				protected void onDragEnd(String componentId, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
				{
					view.moveColumn(SortableCellViewHeader.this, x - startX, ajaxRequestTarget);
					labelResolver.setDropped(true);
				}

				@Override
				protected boolean onDragStart(String componentId, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
				{
					startX = x;
					return true;
				}

				@Override
				protected void onDrop(String componentId, String targetid, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
				{
					// TODO Auto-generated method stub

				}

				@Override
				protected void onDropHover(String componentId, String targeid, int m, AjaxRequestTarget ajaxRequestTarget)
				{
					// TODO Auto-generated method stub

				}
			};
			dragMoveBehavior.setRenderOnHead(false);
			dragMoveBehavior.setYConstraint(true);
			dragMoveBehavior.setUseProxy(true);
			dragMoveBehavior.setResizeProxyFrame(true);
			headerColumnTable.add(dragMoveBehavior);
		}
		else if (isUnmovable())
		{
			headerColumnTable.add(new SimpleAttributeModifier("ondragstart", "return false;"));
		}

		if (cellview instanceof Portal && !((Portal)cellview).getSortable())
		{
			labelResolver.setSortable(false);
			resizeBar = new WebMarkupContainer("resizeBar"); //$NON-NLS-1$
		}
		else
		{
			resizeBar = new Image("resizeBar", view.R_ARROW_OFF); //$NON-NLS-1$
		}

		if (!blockResize && (!(cellview instanceof Portal) || ((Portal)cellview).getResizeble()) && useAJAX)
		{
			DraggableBehavior dragResizeBehavior = new DraggableBehavior()
			{
				private int startX;

				@Override
				protected void onDragEnd(String componentId, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
				{
					view.resizeColumn(SortableCellViewHeader.this, x - startX);
				}

				@Override
				protected boolean onDragStart(String componentId, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
				{
					startX = x;
					return true;
				}

				@Override
				protected void onDrop(String componentId, String targetid, int x, int y, int m, AjaxRequestTarget ajaxRequestTarget)
				{
				}

				@Override
				protected void onDropHover(String componentId, String targeid, int m, AjaxRequestTarget ajaxRequestTarget)
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
			Iterator<IPersist> it2 = cellview.getAllObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			int height = -1;
			while (it2.hasNext())
			{
				IPersist element = it2.next();
				if (id.equals(ComponentFactory.getWebID(form, element)))
				{
					final GraphicalComponent gc = (GraphicalComponent)view.labelsFor.get(((ISupportName)element).getName());
					if (gc != null && height < 0)
					{
						height = gc.getSize().height;
					}
					if (gc != null && gc.getImageMediaID() > 0)
					{
						final int media_id = gc.getImageMediaID();
						final Media media = application.getFlattenedSolution().getMedia(media_id);
						if (media != null)
						{
							hasBgImage = true;
							final int headerHeight = height;
							Pair<IStyleSheet, IStyleRule> pair = labelFor != null ? ComponentFactory.getStyleForBasicComponent(application, labelFor, form)
								: null;
							final IStyleRule cssRule = pair == null || pair.getRight() == null ? null : pair.getRight();
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
											MediaResource tempIcon = new MediaResource(media.getMediaData(), gc.getMediaOptions());
											(tempIcon).checkResize(new Dimension(width, headerHeight));
											ImageIcon icon = new ImageIcon(tempIcon.resized);
											imgRes.setImage(ImageLoader.imageToBufferedImage((icon).getImage()));

											return imgRes;
										}
									};

									TextualStyle style = new TextualStyle();
									style.setProperty(CSS.Attribute.BACKGROUND_REPEAT.toString(), "no-repeat"); //$NON-NLS-1$
									style.setProperty(CSS.Attribute.BACKGROUND_POSITION.toString(), "center right"); //$NON-NLS-1$
									if (cssRule != null)
									{
										if (cssRule.hasAttribute(CSS.Attribute.BACKGROUND.toString())) style.setProperty(CSS.Attribute.BACKGROUND.toString(),
											cssRule.getValue(CSS.Attribute.BACKGROUND.toString()));
										if (cssRule.hasAttribute(CSS.Attribute.BACKGROUND_ATTACHMENT.toString())) style.setProperty(
											CSS.Attribute.BACKGROUND_ATTACHMENT.toString(), cssRule.getValue(CSS.Attribute.BACKGROUND_ATTACHMENT.toString()));
										if (cssRule.hasAttribute(CSS.Attribute.BACKGROUND_COLOR.toString())) style.setProperty(
											CSS.Attribute.BACKGROUND_COLOR.toString(), cssRule.getValue(CSS.Attribute.BACKGROUND_COLOR.toString()));
										if (cssRule.hasAttribute(CSS.Attribute.BACKGROUND_POSITION.toString())) style.setProperty(
											CSS.Attribute.BACKGROUND_POSITION.toString(), cssRule.getValue(CSS.Attribute.BACKGROUND_POSITION.toString()), true);
										if (cssRule.hasAttribute(CSS.Attribute.BACKGROUND_REPEAT.toString())) style.setProperty(
											CSS.Attribute.BACKGROUND_REPEAT.toString(), cssRule.getValue(CSS.Attribute.BACKGROUND_REPEAT.toString()), true);
									}
									style.setProperty(CSS.Attribute.BACKGROUND_IMAGE.toString(), "url(" + urlFor(iconReference) + "?id=" + media_id + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									return style.getValuesAsString(null);
								}
							}));
						}

					}
					else if (gc != null && gc.getToolTipText() != null)
					{
						tooltip = application.getI18NMessageIfPrefixed(gc.getToolTipText());
						add(TooltipAttributeModifier.INSTANCE);
					}
					if (gc != null)
					{
						int style_valign = -1;
						Pair<IStyleSheet, IStyleRule> styleInfo = ComponentFactory.getStyleForBasicComponent(application, gc, form);
						if (styleInfo != null)
						{
							IStyleSheet ss = styleInfo.getLeft();
							IStyleRule s = styleInfo.getRight();
							if (ss != null && s != null)
							{
								style_valign = ss.getVAlign(s);
							}
						}
						final int styleValign = style_valign;
						add(new StyleAppendingModifier(new Model<String>()
						{
							private static final long serialVersionUID = 1L;

							@Override
							public String getObject()
							{
								int valign = ISupportTextSetup.CENTER;
								if (gc.getVerticalAlignment() >= 0)
								{
									valign = gc.getVerticalAlignment();
								}
								else if (styleValign >= 0)
								{
									valign = styleValign;
								}
								return "vertical-align:" + TemplateGenerator.getVerticalAlignValue(valign) + ";"; //$NON-NLS-1$//$NON-NLS-2$
							}
						}));
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
		if (labelFor != null && hasBgImage && labelFor.getOnActionMethodID() > 0)
		{
			add(new AttributeModifier(
				"onclick",
				true,
				new Model<String>(
					"var target = event.target || event.srcElement; var aEl = this.getElementsByTagName('a')[0]; if($(target).parents('#'+aEl.id).length == 0) { aEl.click(); }")));
		}
		Boolean dir = group.get(id);
		if (dir != null && form.getOnSortCmdMethodID() >= 0)
		{
			setResizeImage(dir ? view.R_ARROW_DOWN : view.R_ARROW_UP);
		}
	}

	/**
	 * 
	 * @return GraphicalComponent -  the "label for" component ,if it has one
	 */
	private GraphicalComponent getLabelComponent()
	{
		Iterator<IPersist> iterator = cellview.getAllObjects();
		while (iterator.hasNext())
		{
			IPersist element = iterator.next();
			if (id.equals(ComponentFactory.getWebID(form, element)))
			{
				GraphicalComponent gc = (GraphicalComponent)view.labelsFor.get(((ISupportName)element).getName());
				if (gc != null)
				{
					return gc;
				}
			}
		}
		return null;
	}


	private void applyStyleChanges(Component c, final Properties changes)
	{
		StyleAppendingModifier headerStyles = new StyleAppendingModifier(new Model<String>()
		{
			@Override
			public String getObject()
			{
				StringBuilder headerStyle = new StringBuilder();
				Iterator<Entry<Object, Object>> headerStyleIte = changes.entrySet().iterator();
				Entry<Object, Object> headerStyleEntry;
				while (headerStyleIte.hasNext())
				{
					headerStyleEntry = headerStyleIte.next();
					headerStyle.append(headerStyleEntry.getKey()).append(":").append(headerStyleEntry.getValue()).append(";");
				}
				return headerStyle.toString();
			}
		})
		{
			@Override
			public boolean isEnabled(Component c)
			{
				Iterator<IPersist> it2 = SortableCellViewHeader.this.cellview.getAllObjects();
				while (it2.hasNext())
				{
					IPersist element = it2.next();
					if (SortableCellViewHeader.this.id.equals(ComponentFactory.getWebID(SortableCellViewHeader.this.form, element)) &&
						SortableCellViewHeader.this.view.labelsFor.get(((ISupportName)element).getName()) != null)
					{
						return false;
					}
				}
				return true;
			}
		};

		c.add(headerStyles);
	}

	private void applyInlineStyleString(Component c, final String inlineCSSSting)
	{
		StyleAppendingModifier headerStyles = new StyleAppendingModifier(new Model<String>()
		{
			@Override
			public String getObject()
			{
				return inlineCSSSting;
			}
		})
		{
			@Override
			public boolean isEnabled(Component c)
			{
				Iterator<IPersist> it2 = SortableCellViewHeader.this.cellview.getAllObjects();
				while (it2.hasNext())
				{
					IPersist element = it2.next();
					if (SortableCellViewHeader.this.id.equals(ComponentFactory.getWebID(SortableCellViewHeader.this.form, element)) &&
						SortableCellViewHeader.this.view.labelsFor.get(((ISupportName)element).getName()) != null)
					{
						return false;
					}
				}
				return true;
			}
		};

		c.add(headerStyles);
	}


	public boolean isUnmovable()
	{
		return isUnmovable;
	}

	public String getToolTipText()
	{
		return tooltip;
	}

	public void setResizeImage(ResourceReference resourceReference)
	{
		if (resizeBar instanceof Image && Application.exists())
		{
			((Image)resizeBar).setImageResourceReference(resourceReference);
		}
	}

	public void setResizeClass(String resizeClass)
	{
		resizeBar.add(new SimpleAttributeModifier("class", "resize_" + resizeClass));
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

		int headerPadding = TemplateGenerator.SORTABLE_HEADER_PADDING;
		// If there is no label-for, we leave place for the default border placed at the right of headers.
		if (view.labelsFor.size() == 0)
		{
			int extraWidth = TemplateGenerator.NO_LABELFOR_DEFAULT_BORDER_WIDTH;

			String headerBorder = view.getHeaderBorder();
			if (headerBorder != null)
			{
				Properties properties = new Properties();
				Insets borderIns = ComponentFactoryHelper.createBorderCSSProperties(headerBorder, properties);
				extraWidth = borderIns.left + borderIns.right;
				headerPadding = 0;
			}

			borderWidth += extraWidth;
		}

		int clientWidth = width - headerPadding - borderWidth; // we have only left padding

		// if we have grid_style for the header, then don't change the header width
		this.width = (view.labelsFor.size() == 0 && view.getHeaderBorder() != null) ? width : clientWidth;

		StyleAppendingModifier widthStyleModifier = new StyleAppendingModifier(new Model<String>()
		{
			@Override
			public String getObject()
			{
				return "width: " + SortableCellViewHeader.this.width + "px"; //$NON-NLS-1$//$NON-NLS-2$
			}
		});

		add(widthStyleModifier);
		headerColumnTable.add(widthStyleModifier);

		labelResolver.setWidth(clientWidth - SortableCellViewHeader.ARROW_WIDTH);
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

	public void setTabSequenceIndex(int tabIndex)
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
