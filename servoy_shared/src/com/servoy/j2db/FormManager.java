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
package com.servoy.j2db;


import java.applet.Applet;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.Action;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.cmd.ICmdManagerInternal;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.RelatedFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.AppletController;

/**
 * This class keeps track of all the forms and handles the window menu
 * 
 * @author jblok, jcompagner
 */
public abstract class FormManager implements PropertyChangeListener, IFormManager
{
	public static final String DEFAULT_DIALOG_NAME = "dialog"; //$NON-NLS-1$
	public static final String NO_TITLE_TEXT = "-none-"; //$NON-NLS-1$

	public static final Rectangle FULL_SCREEN = new Rectangle(IApplication.FULL_SCREEN, IApplication.FULL_SCREEN, IApplication.FULL_SCREEN,
		IApplication.FULL_SCREEN);

	private static final int MAX_FORMS_LOADED;
	static
	{
		// TODO web clients?? Can they also get 128 forms??
		long maxMem = Runtime.getRuntime().maxMemory();
		if (maxMem > 200000000)
		{
			MAX_FORMS_LOADED = 128;
		}
		else if (maxMem > 100000000)
		{
			MAX_FORMS_LOADED = 64;
		}
		else
		{
			MAX_FORMS_LOADED = 32;
		}
		Debug.trace("MaxFormsLoaded set to:" + MAX_FORMS_LOADED); //$NON-NLS-1$
	}

	private final IApplication application;

	protected final ConcurrentMap<String, IMainContainer> containers; //windowname -> IMainContainer
	protected IMainContainer currentContainer;
	protected IMainContainer mainContainer;

	protected final ConcurrentMap<String, Form> possibleForms; // formName -> Form
	protected final ConcurrentMap<String, FormController> createdFormControllers; // formName -> FormController
	protected LinkedList<FormController> leaseHistory;

	private final AppletController appletContext; //incase we use applets on form

	private volatile boolean destroyed;


	/*
	 * _____________________________________________________________ Declaration and definition of constructors
	 */
	public FormManager(IApplication app, IMainContainer mainContainer)
	{
		application = app;
		containers = new ConcurrentHashMap<String, IMainContainer>()
		{
			private final String NULL_KEY = "-NULL_KEY-"; //$NON-NLS-1$

			@Override
			public IMainContainer put(String key, IMainContainer value)
			{
				if (key == null) return super.put(NULL_KEY, value);
				return super.put(key, value);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.util.concurrent.ConcurrentHashMap#get(java.lang.Object)
			 */
			@Override
			public IMainContainer get(Object key)
			{
				if (key == null) return super.get(NULL_KEY);
				return super.get(key);
			}
		};
		containers.put(mainContainer.getContainerName(), mainContainer);
		currentContainer = mainContainer;
		this.mainContainer = mainContainer;
		leaseHistory = new LinkedList<FormController>();
		createdFormControllers = new ConcurrentHashMap<String, FormController>();
		possibleForms = new ConcurrentHashMap<String, Form>();
		appletContext = new AppletController(app);
	}


	public IApplication getApplication()
	{
		return application;
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		String name = evt.getPropertyName();
		if ("solution".equals(name)) //$NON-NLS-1$
		{
			final Solution s = (Solution)evt.getNewValue();
			destroySolutionSettings();//must run on same thread
			if (s != null)
			{
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						makeSolutionSettings(s);
					}
				});
			}
			else
			{
				lastModalContainer = null;
			}
		}
		else if ("mode".equals(name)) //$NON-NLS-1$
		{
			int oldmode = ((Integer)evt.getOldValue()).intValue();
			int newmode = ((Integer)evt.getNewValue()).intValue();

			handleModeChange(oldmode, newmode);
		}
	}

	/*
	 * _____________________________________________________________ The methods below belong to this class
	 */
	public void showSolutionLoading(boolean b)
	{
		getCurrentContainer().showSolutionLoading(b);
	}

	public boolean isFormReadOnly(String formName)
	{
		return readOnlyCheck.contains(formName);
	}

	protected List<String> readOnlyCheck = new ArrayList<String>();

	public void setFormReadOnly(String formName, boolean readOnly)
	{
		if (readOnly && readOnlyCheck.contains(formName)) return;

		if (readOnly)
		{
			readOnlyCheck.add(formName);
		}
		else
		{
			readOnlyCheck.remove(formName);
		}
	}

	public void addForm(Form form, boolean selected)
	{
		Form f = possibleForms.put(form.getName(), form);
		if (f != null && form != f)
		{
			// replace all occurrences to the previous form to this new form (newFormInstances) 
			Iterator<Entry<String, Form>> iterator = possibleForms.entrySet().iterator();
			while (iterator.hasNext())
			{
				Entry<String, Form> next = iterator.next();
				if (next.getValue() == f)
				{
					next.setValue(form);
				}
			}
		}
	}

	public boolean removeForm(Form form)
	{
		boolean removed = destroyFormInstance(form.getName());
		if (removed)
		{
			possibleForms.remove(form.getName());
		}
		return removed;
	}

	protected abstract void selectFormMenuItem(Form form);

	public abstract void fillScriptMenu();

	protected abstract void enableCmds(boolean enable);

	protected Form loginForm = null;//as long this is set do not allow to leave this form!

	public Iterator<String> getPossibleFormNames()
	{
		return possibleForms.keySet().iterator();
	}

	public Form getPossibleForm(String name)
	{
		return possibleForms.get(name);
	}

	//initialize this manager for the solution
	protected void makeSolutionSettings(Solution s)
	{
		destroyed = false;
		Solution solution = s;

		Iterator<Form> e = application.getFlattenedSolution().getForms(true);
		// add all forms first, they may be referred to in the login form
		Form first = application.getFlattenedSolution().getForm(solution.getFirstFormID());
		boolean firstFormCanBeInstantiated = application.getFlattenedSolution().formCanBeInstantiated(first);
		while (e.hasNext())
		{
			Form form = e.next();
			if (application.getFlattenedSolution().formCanBeInstantiated(form))
			{
				if (!firstFormCanBeInstantiated) first = form;
				firstFormCanBeInstantiated = true;
			}
			// add anyway, the form may be used in scripting
			addForm(form, form.equals(first));
		}

		if (!firstFormCanBeInstantiated)
		{
			//hmm no forms
			showSolutionLoading(false);
		}
		else
		{
			application.getModeManager().setMode(IModeManager.EDIT_MODE);//start in browse mode
		}


		if (solution.getLoginFormID() > 0 && solution.getMustAuthenticate() && application.getUserUID() == null)
		{
			Form login = application.getFlattenedSolution().getForm(solution.getLoginFormID());
			if (application.getFlattenedSolution().formCanBeInstantiated(login) && loginForm == null)
			{
				loginForm = login;//must set the login form early so its even correct if onload of login form is called
				showSolutionLoading(false);
				showFormInMainPanel(login.getName());
				getMainContainer(null).setComponentVisible(true);
				return; //stop and recall this method from security.login(...)!
			}
		}
		if (solution.getLoginFormID() > 0 && solution.getMustAuthenticate() && application.getUserUID() != null && loginForm != null)
		{
			if (currentContainer.getController() != null && loginForm.getName().equals(currentContainer.getController().getForm().getName()))
			{
				currentContainer.setFormController(null);
			}
			if (mainContainer.getController() != null && loginForm.getName().equals(mainContainer.getController().getForm().getName()))
			{
				mainContainer.setFormController(null);
			}
			loginForm = null;//clear and continue
		}

		ScriptMethod sm = null;
		int modifiers = Utils.getAsInteger(application.getRuntimeProperties().get("load.solution.modifiers")); //$NON-NLS-1$
		if ((modifiers & Event.SHIFT_MASK) != Event.SHIFT_MASK)
		{
			sm = application.getFlattenedSolution().getScriptMethod(solution.getOnOpenMethodID());
		}

		Object[] solutionOpenMethodArgs = null;
		String preferedSolutionMethodName = ((ClientState)application).getPreferedSolutionMethodNameToCall();
		if (preferedSolutionMethodName == null && ((ClientState)application).getPreferedSolutionMethodArguments() != null)
		{
			solutionOpenMethodArgs = ((ClientState)application).getPreferedSolutionMethodArguments();
		}

		if (sm != null)
		{
			try
			{
				GlobalScope gscope = application.getScriptEngine().getSolutionScope().getGlobalScope();
				Object function = gscope.get(sm.getName());
				if (function instanceof Function)
				{
					application.getScriptEngine().executeFunction(((Function)function), gscope, gscope,
						Utils.arrayMerge(solutionOpenMethodArgs, Utils.parseJSExpressions(solution.getInstanceMethodArguments("onOpenMethodID"))), false, false); //$NON-NLS-1$
					if (application.getSolution() == null) return;
				}
			}
			catch (Exception e1)
			{
				application.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { sm.getName() }), e1); //$NON-NLS-1$
			}
		}

		showSolutionLoading(false);

		if (first != null && currentContainer != null && currentContainer.getController() != null &&
			currentContainer.getController().getName().equals(first.getName()))
		{
			currentContainer.setFormController(null);
		}

		IMainContainer modalContainer = getModalDialogContainer(); // onOpen event might have opened a modal popup with another form
		if (first != null && mainContainer.getController() == null)
		{
			if (modalContainer != mainContainer)
			{
				currentContainer.setComponentVisible(true);
				setCurrentContainer(modalContainer, null); // if we had a modal dialog displayed, it must remain the current container
				if (currentContainer.getController() == null)
				{
					showFormInCurrentContainer(first.getName());
				}
				else
				{
					showFormInMainPanel(first.getName()); //we only set if the solution startup did not yet show a form already
				}
			}
			else
			{
				showFormInMainPanel(first.getName()); //we only set if the solution startup did not yet show a form already
			}
		}

		currentContainer.setComponentVisible(true);

		if (preferedSolutionMethodName != null && application.getFlattenedSolution().isMainSolutionLoaded())
		{
			try
			{
				Object[] args = ((ClientState)application).getPreferedSolutionMethodArguments();

				// avoid stack overflows when an execute method URL is used to open the solution, and that method does call JSSecurity login
				((ClientState)application).resetPreferedSolutionMethodNameToCall();

				GlobalScope gscope = application.getScriptEngine().getSolutionScope().getGlobalScope();
				Object function = gscope.get(preferedSolutionMethodName, gscope);
				if (function instanceof Function)
				{
					int solutionType = application.getSolution().getSolutionType();
					Object result = application.getScriptEngine().executeFunction(((Function)function), gscope, gscope, args, false, false);
					if (solutionType == SolutionMetaData.AUTHENTICATOR)
					{
						application.getRuntimeProperties().put(IServiceProvider.RT_OPEN_METHOD_RESULT, result);
					}
				}
			}
			catch (Exception e1)
			{
				application.reportError(
					Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { preferedSolutionMethodName }), e1); //$NON-NLS-1$
			}
		}
	}


	private IMainContainer lastModalContainer;

	//uninit
	protected void destroySolutionSettings()
	{
		destroyed = true;
		loginForm = null;
		try
		{
			Iterator<Map.Entry<String, IMainContainer>> it = containers.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry<String, IMainContainer> entry = it.next();
				IMainContainer container = entry.getValue();
				destroyContainer(container);
				if (entry.getKey() != null) // remove all none null 
				{
					it.remove();
				}
			}
		}
		catch (Exception e)
		{
			Debug.trace(e);//trace not important.
		}

		removeAllFormPanels();

		currentContainer = getMainContainer(null);

		leaseHistory = new LinkedList<FormController>();
		createdFormControllers.clear();
		possibleForms.clear();
	}

	protected void destroyContainer(IMainContainer container)
	{
		RuntimeWindow w = application.getRuntimeWindowManager().getWindow(container.getContainerName());
		if (w != null) w.destroy();

		FormController fc = container.getController();
		if (fc != null && fc.isFormVisible())
		{
			fc.getFormUI().setComponentVisible(false);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			fc.notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);
		}

		container.flushCachedItems();
	}

	public abstract IMainContainer getOrCreateMainContainer(String name);

	public void showFormInDialog(String formName, Rectangle bounds, String title, boolean resizeble, boolean showTextToolbar, boolean closeAll, boolean modal,
		String windowName)
	{
		boolean legacyV3Behavior = false; // first window is modal, second reuses same dialog
		if (windowName == null)
		{
			windowName = DEFAULT_DIALOG_NAME;
			legacyV3Behavior = true;
		}

		RuntimeWindow thisWindow = application.getRuntimeWindowManager().getWindow(windowName);
		if (thisWindow != null && thisWindow.getType() != JSWindow.DIALOG && thisWindow.getType() != JSWindow.MODAL_DIALOG)
		{
			thisWindow.hide(true); // make sure it is closed before reference to it is lost
			thisWindow.destroy();
			thisWindow = null;
		}

		if (thisWindow == null)
		{
			thisWindow = application.getRuntimeWindowManager().createWindow(windowName, modal ? JSWindow.MODAL_DIALOG : JSWindow.DIALOG, null);
		}
		thisWindow.setInitialBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		thisWindow.showTextToolbar(showTextToolbar);
		thisWindow.setTitle(title);
		thisWindow.setResizable(resizeble);

		thisWindow.oldShow(formName, closeAll, legacyV3Behavior);
	}

	public void showFormInFrame(String formName, Rectangle bounds, String windowTitle, boolean resizeble, boolean showTextToolbar, String windowName)
	{
		if (windowName == null)
		{
			windowName = DEFAULT_DIALOG_NAME;
		}

		RuntimeWindow thisWindow = application.getRuntimeWindowManager().getWindow(windowName);

		if (thisWindow != null && thisWindow.getType() != JSWindow.WINDOW)
		{
			thisWindow.hide(true); // make sure it's closed before reference to it is lost
			thisWindow.destroy();
			thisWindow = null;
		}

		if (thisWindow == null)
		{
			thisWindow = application.getRuntimeWindowManager().createWindow(windowName, JSWindow.WINDOW, null);
		}
		thisWindow.setInitialBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		thisWindow.showTextToolbar(showTextToolbar);
		thisWindow.setTitle(windowTitle);
		thisWindow.setResizable(resizeble);

		thisWindow.oldShow(formName, true, false); // last two params are really not relevant for windows
	}

	public IMainContainer getMainContainer(String dialogName)
	{
		if (dialogName == null) return mainContainer;
		return containers.get(dialogName);
	}

	public List<String> getCreatedMainContainerKeys()
	{
		return new ArrayList<String>(containers.keySet());
	}

	/**
	 * @return
	 */
	public IMainContainer getCurrentContainer()
	{
		if (currentContainer == null)
		{
			return mainContainer;
		}
		return currentContainer;
	}

	public IMainContainer getModalDialogContainer()
	{
		if (lastModalContainer != null) return lastModalContainer;
		return getCurrentContainer();
	}


	public IMainContainer setModalDialogContainer(IMainContainer container)
	{
		IMainContainer previous = lastModalContainer;
		lastModalContainer = container;
		return previous;
	}

	public FormController showFormInMainPanel(final String formName)
	{
		return showFormInMainPanel(formName, getMainContainer(null), null, true, null);
	}

	public FormController showFormInCurrentContainer(final String formName)
	{
		return showFormInMainPanel(formName, getCurrentContainer(), null, true, application.getRuntimeWindowManager().getCurrentWindowName());
	}

	public void clearLoginForm()
	{
		if (application.getSolution().getMustAuthenticate()) makeSolutionSettings(application.getSolution());
	}

	protected boolean design = false;

	//show a form in the main panel
	public FormController showFormInMainPanel(final String formName, final IMainContainer container, final String title, final boolean closeAll,
		final String dialogName)
	{
		if (loginForm != null && loginForm.getName() != formName)
		{
			return null;//not allowed to leave here...or show anything else than login form
		}
		if (formName == null) throw new IllegalArgumentException(application.getI18NMessage("servoy.formManager.error.SettingVoidForm")); //$NON-NLS-1$

		FormController currentMainShowingForm = null;

		if (currentContainer != null)
		{
			currentMainShowingForm = container.getController();
		}

		boolean containerSwitch = container != currentContainer;

		if (currentMainShowingForm != null && formName.equals(currentMainShowingForm.getName()) && !containerSwitch && !design) return leaseFormPanel(currentMainShowingForm.getName());

		final Form f = possibleForms.get(formName);
		if (f == null)
		{
			return null;
		}

		try
		{
			int access = application.getFlattenedSolution().getSecurityAccess(f.getUUID());
			if (access != -1)
			{
				boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
				if (!b_visible)
				{
					application.invokeLater(new Runnable()
					{
						public void run()
						{
							application.reportWarningInStatus(application.getI18NMessage("servoy.formManager.warningAccessForm")); //$NON-NLS-1$
						}
					});
					return null;
				}
			}

			//handle old panel
			if (currentMainShowingForm != null)
			{
				//leave forms in browse mode // TODO can this be set if notifyVisible returns false (current form is being kept)
				if (!containerSwitch && application.getModeManager().getMode() != IModeManager.EDIT_MODE)
				{
					application.getModeManager().setMode(IModeManager.EDIT_MODE);
				}

				FormController fp = leaseFormPanel(currentMainShowingForm.getName());
				if (fp != null && !containerSwitch)
				{
					List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
					boolean ok = fp.notifyVisible(false, invokeLaterRunnables);
					Utils.invokeLater(application, invokeLaterRunnables);

					// solution closed in onhide method of previous form?
					if (application.getSolution() == null) return null;

					if (!ok)
					{
						selectFormMenuItem(currentMainShowingForm.getForm());
						return fp;
					}
				}
			}

			//set main
			FormController tmpForm = currentMainShowingForm;

			final FormController fp = leaseFormPanel(formName);
			currentMainShowingForm = fp;

			currentContainer = container;

			if (fp != null)
			{
				if (application.getCmdManager() instanceof ICmdManagerInternal)
				{
					((ICmdManagerInternal)application.getCmdManager()).setCurrentUndoManager(fp.getUndoManager());
				}
				boolean isNewUser = checkAndUpdateFormUser(fp, container);
				if (isNewUser)
				{
					container.add(fp.getFormUI(), formName);
				}
				// this code must be below the checkAndUpdateUser because setFormController can already set the formui
				currentContainer.setFormController(fp);
				SolutionScope ss = application.getScriptEngine().getSolutionScope();
				Context.enter();
				try
				{
					ss.put("currentcontroller", ss, new NativeJavaObject(ss, fp.initForJSUsage(), new InstanceJavaMembers(ss, JSForm.class))); //$NON-NLS-1$
				}
				finally
				{
					Context.exit();
				}
				fp.setView(fp.getView());
				fp.executeOnLoadMethod();
				// test if solution is closed in the onload method.
				if (application.getSolution() == null) return null;

				//correct script menu for this form
				fillScriptMenu();

				// if this is first show and we have to mimic the legacy 3.1 behavior of dialogs (show all in 1 window), allow 100 forms
				if (!container.isVisible() && !closeAll)
				{
					((FormManager)application.getFormManager()).getHistory(currentContainer).clear(100);
				}

				//add to history
				getHistory(currentContainer).add(fp.getName());

				//check for programatic change
				selectFormMenuItem(f);

				//show panel as main
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				fp.notifyVisible(true, invokeLaterRunnables);

				// only enable command when it is the default container
				//if (getMainContainer(null) == currentContainer)
				// Command should rightly enabled for the forms
				enableCmds(true);

				final IMainContainer cachedContainer = currentContainer;
				Runnable title_focus = new Runnable()
				{
					public void run()
					{
						FormController fc = cachedContainer.getController();
						if (fc != null && fc == fp && application.getSolution() != null)
						{
							//correct title
							String titleText = title;
							if (titleText == null) titleText = f.getTitleText();
							if (titleText == null || titleText.equals("")) titleText = fc.getName(); //$NON-NLS-1$
							if (NO_TITLE_TEXT.equals(titleText)) titleText = ""; //$NON-NLS-1$
							cachedContainer.setTitle(titleText);
							cachedContainer.requestFocus();
						}
					}
				};

				if (isNewUser)
				{
					final IMainContainer showContainer = currentContainer;
					currentContainer.showBlankPanel();//to overcome paint problem in swing...
					invokeLaterRunnables.add(new Runnable()
					{
						public void run()
						{
							// only call show if it is still the right form.
							FormController currentController = showContainer.getController();
							if (currentController != null && fp.getName().equals(currentController.getName()))
							{
								showContainer.show(fp.getName());
								application.getRuntimeWindowManager().setCurrentWindowName(dialogName);
							}
						}
					});
				}
				else
				{
					currentContainer.show(fp.getName());
					application.getRuntimeWindowManager().setCurrentWindowName(dialogName);
				}
				invokeLaterRunnables.add(title_focus);
				Utils.invokeLater(application, invokeLaterRunnables);
			}
			else
			{
				currentContainer.setFormController(null);
			}
			J2DBGlobals.firePropertyChange(this, "form", tmpForm, currentMainShowingForm); //$NON-NLS-1$

			return fp;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	public IForm getCurrentForm()
	{
		return getCurrentMainShowingFormController();
	}

	public FormController getCurrentMainShowingFormController()
	{
		if (currentContainer != null) return currentContainer.getController();
		return null;
	}

	public boolean isCurrentTheMainContainer()
	{
		return getCurrentContainer() == getMainContainer(null);
	}

	//get a FormPanelfor use in tabpanes and dialogs
	public FormController getFormController(String formName, Object parent)
	{
		if (formName == null || parent == null) return null;
		IMainContainer container = currentContainer;
		if (parent instanceof IMainContainer) container = (IMainContainer)parent;
		if (container.getController() != null && formName == container.getController().getName()) return null;//savety for circular reference: from A has tabpanel Showing Form B which has tabpanel showing A

		Form f = possibleForms.get(formName);
		if (f == null) return null;

		int access = application.getFlattenedSolution().getSecurityAccess(f.getUUID());
		if (access != -1)
		{
			boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
			if (!b_visible)
			{
				return null;//user has no access
			}
		}

		FormController fp = leaseFormPanel(formName);
		if (fp != null)
		{
			checkAndUpdateFormUser(fp, parent);
			fp.initForJSUsage();
			fp.setView(fp.getView());
			fp.executeOnLoadMethod();

			//the form now has to be called to be usable with:
			//fp.notifyVisible(true);
			//fp.executeOnShowMethod();
			//if (!fp.isShowingData()) fp.loadAllRecordsImpl(true);
		}
		return fp;
	}

	public void removeAllFormPanels()
	{
		FormController fp;
		boolean hadFormPanels = false;
		// also first try to get the lock on this, because destroy() call can result in that. 
		// Then a deadlock will happen, so first get it before the leasHistory.. 
		synchronized (this)
		{
			synchronized (leaseHistory)
			{
				hadFormPanels = leaseHistory.size() > 0;
				while (leaseHistory.size() > 0)
				{
					fp = leaseHistory.removeFirst();
					fp.destroy();
				}
			}

		}
		if (hadFormPanels)
		{
			SolutionScope ss = application.getScriptEngine().getSolutionScope();
			if (ss != null) ss.put("currentcontroller", ss, null); //$NON-NLS-1$
		}
	}

	void removeFormPanel(FormController fp)
	{
		synchronized (leaseHistory)
		{
			leaseHistory.remove(fp);
		}
		createdFormControllers.remove(fp.getName());
		removeFormUser(fp);
	}

	public void touch(FormController fp)
	{
		synchronized (leaseHistory)
		{
			if (!leaseHistory.isEmpty() && leaseHistory.getLast() != fp)
			{
				leaseHistory.remove(fp);//to prevent the panel is added more than once
				leaseHistory.add(fp);
			}
		}
	}

	/**
	 * method to comply to interface, internally use leaseForm
	 */
	public IForm getForm(String formName)
	{
		return leaseFormPanel(formName);
	}

	//for non graphical usage only
	public synchronized FormController leaseFormPanel(String formName)
	{
		if (formName == null || destroyed) return null;

		String name = formName;
		if (application.getApplicationType() == IApplication.WEB_CLIENT && Utils.getAsBoolean(application.getRuntimeProperties().get("isPrinting"))) //$NON-NLS-1$
		{
			name += "_printing"; //$NON-NLS-1$
		}

		FormController fp = createdFormControllers.get(name);
		if (fp == null)
		{
			Form f = possibleForms.get(formName);
			if (f == null) return null;
			try
			{
				application.blockGUI(application.getI18NMessage("servoy.formManager.loadingForm") + formName); //$NON-NLS-1$

				f = application.getFlattenedSolution().getFlattenedForm(f);

				fp = new FormController(application, f, name);
				createdFormControllers.put(fp.getName(), fp);
				fp.init();
				FormController toBeRemoved = null;
				synchronized (leaseHistory)
				{
					int leaseHistorySize = leaseHistory.size();
					if (leaseHistorySize > MAX_FORMS_LOADED)
					{
						for (int i = 0; i < leaseHistorySize; i++)
						{
							FormController fc = leaseHistory.get(i);
							if (canBeDeleted(fc))
							{
								toBeRemoved = fc;
								break;
							}
						}
					}
					leaseHistory.remove(fp);//to prevent the panel is added more than once
					leaseHistory.add(fp);
					if (Debug.tracing())
					{
						Debug.trace("FormPanel '" + fp.getName() + "' created, Loaded forms: " + leaseHistory.size() + " of " + MAX_FORMS_LOADED + " (max)."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					}
				}
				if (toBeRemoved != null)
				{
					if (Debug.tracing())
					{
						Debug.trace("FormPanel '" + toBeRemoved.getName() + "' removed because of MAX_FORMS_LOADED (" + MAX_FORMS_LOADED + //$NON-NLS-1$ //$NON-NLS-2$
							") was passed."); //$NON-NLS-1$
					}
					toBeRemoved.destroy();
				}
			}
			finally
			{
				application.releaseGUI();
			}
		}
		else
		{
			synchronized (leaseHistory)
			{
				if (leaseHistory.size() != 0 && leaseHistory.getLast() != fp)
				{
					leaseHistory.remove(fp); // move to the end of the list (LRU policy)
					leaseHistory.add(fp);
				}
			}
		}
		return fp;
	}

	public boolean isPossibleForm(String formName)
	{
		return possibleForms.containsKey(formName);
	}

	/**
	 * Only to be used from printing, use leaseFormPanel or getFormPanel!
	 * 
	 * @param f the form to check if there is a controller instance for
	 * @return null if not found
	 */
	public synchronized FormController getCachedFormController(String formName)
	{
		if (formName == null) return null;
		FormController fp = createdFormControllers.get(formName);
		return fp;
	}

	protected boolean canBeDeleted(FormController fp)
	{
		if (fp.isFormVisible())
		{
			return false;
		}

		//  cannot be deleted if a global var has a ref
		GlobalScope globalScope = application.getScriptEngine().getGlobalScope();
		if (hasReferenceInScriptable(globalScope, fp))
		{
			return false;
		}

		if (fp.isFormExecutingFunction())
		{
			return false;
		}

		// the cached currentcontroller may not be destroyed
		SolutionScope ss = application.getScriptEngine().getSolutionScope();
		return ss == null || fp.initForJSUsage() != ss.get("currentcontroller", ss); //$NON-NLS-1$
	}

	private boolean hasReferenceInScriptable(Scriptable scriptVar, FormController fc)
	{
		if (scriptVar instanceof FormScope)
		{
			return ((FormScope)scriptVar).getFormController().equals(fc);
		}
		else if (scriptVar instanceof GlobalScope || scriptVar instanceof NativeArray) // if(o instanceof Scriptable) for all scriptable ?
		{
			Object[] propertyIDs = scriptVar.getIds();
			if (propertyIDs != null)
			{
				Object propertyValue;
				for (Object element : propertyIDs)
				{
					if (element != null)
					{
						if (element instanceof Integer) propertyValue = scriptVar.get(((Integer)element).intValue(), scriptVar);
						else propertyValue = scriptVar.get(element.toString(), scriptVar);

						if (propertyValue != null && propertyValue.equals(fc)) return true;
						else if (propertyValue instanceof Scriptable && hasReferenceInScriptable((Scriptable)propertyValue, fc)) return true;
					}
				}
			}
		}

		return false;
	}

	protected abstract boolean checkAndUpdateFormUser(FormController fp, Object parentContainer);

	protected abstract void removeFormUser(FormController fp);

	public abstract void showPreview(final FormController afp, final IFoundSetInternal foundset, int zoomFactor, final PrinterJob printJob);

	protected abstract FormController removePreview();

	protected void handleModeChange(int oldmode, int newmode)
	{
		FormController fp = getCurrentMainShowingFormController();
		if (fp != null)
		{
			FormController fpBeforePreview = fp;
			switch (oldmode)
			{
				case IModeManager.PREVIEW_MODE :
					fpBeforePreview = removePreview();
					break;

				case IModeManager.FIND_MODE :
					//				FormController fp = (FormController)leaseFormPanel(f.getName());
					fp.setMode(newmode);
					return;
			}

			switch (newmode)
			{
				case IModeManager.FIND_MODE :
					//					FormController fp = (FormController)leaseFormPanel(f);
					fp.setMode(newmode);
					break;
				case IModeManager.EDIT_MODE :
					if (fpBeforePreview != fp)
					{
						History history = getCurrentContainer().getHistory();
						if (history.getIndex() > 0 && history.getFormName(history.getIndex()).equals(fp.getName()))
						{
							history.removeIndex(history.getIndex());
						}
						else if (history.getIndex() > 0 && history.getFormName(history.getIndex()).equals(fpBeforePreview.getName()))
						{
							if (history.getLength() > history.getIndex() + 1 && history.getFormName(history.getIndex() + 1).equals(fp.getName()))
							{
								history.removeIndex(history.getIndex() + 1);
							}
						}
						else
						{
							showFormInCurrentContainer(fpBeforePreview.getName());
						}
					}
					else
					{
						final FormController fp1 = fp;
						if (fp1 != null)
						{
							getCurrentContainer().show(fp1.getName());
							List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
							fp1.notifyVisible(true, invokeLaterRunnables);
							invokeLaterRunnables.add(new Runnable()
							{
								public void run()
								{
									fp1.requestFocus();
								}
							});
							Utils.invokeLater(application, invokeLaterRunnables);
						}
					}
					break;
			}
		}
	}

	/**
	 * returns the history of the current container if set. Else it returns the history of the main container.
	 * 
	 * @return
	 */
	public History getHistory()
	{
		return getHistory(null);
	}

	public History getHistory(IMainContainer container)
	{
		IMainContainer c = container;
		if (c == null)
		{
			if (currentContainer != null)
			{
				c = currentContainer;
			}
			else
			{
				c = getMainContainer(null);
			}
		}
		return c.getHistory();
	}

	@Override
	public String toString()
	{
		// JS toString text!
		return "History Manager[back,forward,go,length]"; //$NON-NLS-1$
	}

	/**
	 * @see com.servoy.j2db.IManager#flushCachedItems()
	 */
	public void flushCachedItems()
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.IManager#init()
	 */
	public void init()
	{
		//ignore
	}

	protected abstract boolean isShowingPrintPreview();

	@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "History", scriptingName = "history")
	public static class HistoryProvider
	{
		private volatile IApplication application;

		public HistoryProvider(IApplication app)
		{
			application = app;
		}

		@Override
		public String toString()
		{
			return "History"; //$NON-NLS-1$
		}

		private IMainContainer getCurrentContainer()
		{
			return ((FormManager)application.getFormManager()).getCurrentContainer();
		}

		/**
		 * Clear the entire history stack.
		 *
		 * @sample history.clear();
		 */
		public void js_clear()
		{
			IMainContainer container = getCurrentContainer();
			container.getHistory().clear();
		}

		public void js_setButtonsEnabled(boolean b)
		{
			IMainContainer container = getCurrentContainer();
			container.getHistory().setButtonsEnabled(b);
		}

		/**
		 * Set/Get the history buttons enabled.
		 *
		 * @sample
		 * history.buttonsEnabled = true;
		 * var status = history.buttonsEnabled;
		 */
		public boolean js_getButtonsEnabled()
		{
			IMainContainer container = getCurrentContainer();
			return container.getHistory().getButtonsEnabled();
		}

		/**
		 * Get the form name based on the specified absolute index in the history stack location.
		 * 
		 * @sample var name = history.getFormName(history.getCurrentIndex());
		 * @param i the absolute index
		 * @return the formName
		 */
		public String js_getFormName(int i)
		{
			IMainContainer container = getCurrentContainer();
			return container.getHistory().getFormName(i - 1); // js offset 1
		}

		/**
		 * Navigates to the relative index based on current position in the history.
		 * 
		 * @sample history.go(-3);
		 * @param i the absolute index
		 */
		public void js_go(int i)
		{
			IMainContainer container = getCurrentContainer();
			container.getHistory().go(i);
		}

		/**
		 * Navigates back in the history stack; shows the previous form (if present). 
		 *
		 * @sample history.back();
		 */
		public void js_back()
		{
			// TODO printpreview must be in this container...
			if (((FormManager)application.getFormManager()).isShowingPrintPreview())
			{
				application.getModeManager().setMode(IModeManager.EDIT_MODE);
			}
			else
			{
				js_go(-1);
			}
		}

		/**
		 * Navigates forward in the history stack; shows the next form (if present).
		 *
		 * @sample history.forward();
		 */
		public void js_forward()
		{
			js_go(1);
		}

		/**
		 * Returns the total size of the history stack.
		 *
		 * @sample var size = history.size();
		 * @return the size
		 */
		public int js_size()
		{
			IMainContainer container = getCurrentContainer();
			return container.getHistory().getLength();
		}

		/**
		 * Get the current absolute index in the history stack.
		 *
		 * @sample var abs_index = history.getCurrentIndex();
		 * @return the current absolute index
		 */
		public int js_getCurrentIndex()
		{
			IMainContainer container = getCurrentContainer();
			return container.getHistory().getIndex() + 1; // js offset 1
		}

		/**
		 * Removes an absolute index based history stack form item.
		 *
		 * @sample var done = history.removeIndex(history.getCurrentIndex()+1);
		 * 
		 * @param index the index of the form to remove.
		 * 
		 * @return true if successful
		 */
		public boolean js_removeIndex(int index)
		{
			IMainContainer container = getCurrentContainer();
			return container.getHistory().removeIndex(index - 1); // js offset 1
		}

		/**
		 * Removes the named form item from the history stack (and from memory) if not currently shown.
		 *
		 * @sample var done = history.removeForm('mypreviousform');
		 * 
		 * @param formName the name of the form to remove.
		 * 
		 * @return true if successful
		 */
		public boolean js_removeForm(String formName)
		{
			IMainContainer container = getCurrentContainer();
			return container.getHistory().removeForm(formName);
		}

		public void destroy()
		{
			application = null;
		}
	}

	public static class History
	{
		private static final int DEFAULT_HISTORY_SIZE = 10;
		private SafeArrayList<String> list;
		private int index = -1;
		private int length = 0;
		private final IApplication application;
		private final IMainContainer container;
		private boolean buttonsEnabled = true;
		private int size = DEFAULT_HISTORY_SIZE;

		public History(IApplication application, IMainContainer container)
		{
			this.application = application;
			this.container = container;
			list = new SafeArrayList<String>(DEFAULT_HISTORY_SIZE + 1);
		}

		/**
		 * @param string
		 */
		public boolean removeForm(String formName)
		{
			int i = list.indexOf(formName);
			if (i != -1 && !removeIndex(i))
			{
				return false;
			}
			return ((FormManager)application.getFormManager()).destroyFormInstance(formName);
		}

		/**
		 * @param i
		 */
		public boolean removeIndex(int i)
		{
			// removing the last form, nothing else to show
			if (length == 1 && i == 0)
			{
				clear(); // sets the buttons and index
				return true;
			}

			// if the currently shown item is removed, show the one before it
			if (i == index && !go(i == 0 ? 1 : -1))
			{
				// could not hide, do nothing
				return false;
			}

			list.remove(i);
			length--;

			if (i < index)
			{
				index--;
			}
			if (buttonsEnabled)
			{
				enableButtons(index != 0, index != length - 1);
			}
			return true;
		}

		/**
		 * @param i
		 */
		public boolean go(int i)
		{
			int idx = index + i;
			if (idx >= length || idx < 0)
			{
				return false;
			}
			String f = list.get(idx);
			if (f == null)
			{
				return false;
			}

			int saveIndex = index;
			index = idx; // must set index now to prevent add() from adding same form twice
			FormController fc = ((FormManager)application.getFormManager()).showFormInMainPanel(f, container, null, true,
				application.getRuntimeWindowManager().getCurrentWindowName());
			if (fc == null || !fc.getName().equals(f))
			{
				index = saveIndex;
				return false;
			}
			if (buttonsEnabled)
			{
				enableButtons(index != 0, index != length - 1);
			}
			return true;
		}

		/**
		 * Enable or disable the backward and forward button, only if not in a dialog
		 */
		private void enableButtons(boolean enableBackward, boolean enableForWard)
		{
			// buttons are currently only used in the main window, not in dialogs
			if (container == ((FormManager)application.getFormManager()).getMainContainer(null))
			{
				Action back = application.getCmdManager().getRegisteredAction("cmdhistoryback"); //$NON-NLS-1$
				if (back != null) back.setEnabled(enableBackward);
				Action forward = application.getCmdManager().getRegisteredAction("cmdhistoryforward"); //$NON-NLS-1$
				if (forward != null) forward.setEnabled(enableForWard);
			}
		}

		/**
		 * @param i
		 */
		public String getFormName(int i)
		{
			return list.get(i);
		}

		/**
		 * 
		 */
		public boolean getButtonsEnabled()
		{
			return buttonsEnabled;
		}

		/**
		 * @param b
		 */
		public void setButtonsEnabled(boolean b)
		{
			if (!b)
			{
				enableButtons(false, false);
			}
			buttonsEnabled = b;
		}

		public int getIndex()
		{
			return index;
		}

		/**
		 * 
		 */
		public void clear()
		{
			clear(size);
		}

		public void clear(int newSize)
		{
			if (length > 0)
			{
				list = new SafeArrayList<String>(20);
				index = -1;
				length = 0;

				enableButtons(false, false);
			}
			size = newSize;
		}

		public void add(String obj)
		{
			if (obj.equals(list.get(index))) return;

			if (length > 0 && buttonsEnabled)
			{
				enableButtons(true, false);
			}

			index++;
			list.set(index, obj);
			length = index + 1;

			if (length == size)
			{
				list.remove(0);
				length--;
				index--;
			}
		}

		public int getLength()
		{
			return length;
		}
	}

	public abstract IFormUIInternal getFormUI(FormController formController);

	public void initializeApplet(Applet applet, Dimension initialSize)
	{
		try
		{
			UIUtils.initializeApplet(appletContext, applet, initialSize);
		}
		catch (Throwable e)
		{
			//its not made active
			Debug.error(e);
			applet.destroy();//call to leave invalid
		}
	}

	/**
	 * There could be one or more forms currently in find mode. Normally you only have one visible set of related forms in find mode at a time. In order to stop
	 * find mode or perform search you need to know the "root" form in each such relation hierarchy. <br>
	 * For example, in case of related forms a -> b -> c -> of which a and b are visible, a would be the form to use for "stop find mode"/"perform search"
	 * operations. In such a relation hierarchy, applying those operations on any other than the root node would result in problems: only a part of those forms
	 * would exit find mode/perform search. (this can lead to future class cast exceptions when trying to get remaining forms out of find mode + user
	 * confusion).
	 * 
	 * @return an array containing forms that are the "root" of a form relation hierarchy in find mode.
	 */
	public IForm[] getVisibleRootFormsInFind()
	{
		ArrayList<IForm> al = new ArrayList<IForm>();
		FormController currentForm = getCurrentMainShowingFormController();
		if (currentForm != null && ifRootFormInFind(currentForm)) al.add(currentForm);
		Iterator<FormController> it = createdFormControllers.values().iterator();
		while (it.hasNext())
		{
			FormController fc = it.next();
			if (fc != currentForm && ifRootFormInFind(fc))
			{
				al.add(fc);
			}
		}
		return al.toArray(new IForm[al.size()]);
	}

	private boolean ifRootFormInFind(FormController fc)
	{
		boolean result = false;
		if (fc.isFormVisible() && fc.isInFindMode())
		{
			IFoundSetInternal foundSet = fc.getFoundSet();
			if (foundSet instanceof RelatedFoundSet)
			{
				// see if any parent foundset is in find mode; if not, then add it as root
				// form in find mode
				List<IRecordInternal> parentRecords = ((RelatedFoundSet)foundSet).getParents();
				boolean hasParentsInFindMode = false;
				for (IRecordInternal record : parentRecords)
				{
					IFoundSetInternal parentFoundSet = record.getParentFoundSet();
					if (parentFoundSet instanceof FoundSet && ((FoundSet)parentFoundSet).isInFindMode())
					{
						hasParentsInFindMode = true;
						break;
					}
				}
				if (!hasParentsInFindMode)
				{
					result = true;
				}
			}
			else
			{
				result = true;
			}
		}
		return result;
	}

	/**
	 * @param mainContainer
	 */
	public void setCurrentContainer(IMainContainer mainContainer, String name)
	{
		if (mainContainer != null)
		{
			currentContainer = mainContainer;
			if (name != null)
			{
				// reset it in the containers (must be done for the webclient)
				containers.put(name, mainContainer);
			}
		}
		else
		{
			currentContainer = getMainContainer(null);
		}
		enableCmds(true);

		FormController formController = currentContainer.getController();
		if (formController != null)
		{
			IExecutingEnviroment scriptEngine = application.getScriptEngine();
			if (scriptEngine != null)
			{
				SolutionScope ss = scriptEngine.getSolutionScope();
				Context.enter();
				try
				{
					ss.put("currentcontroller", ss, new NativeJavaObject(ss, formController.initForJSUsage(), new InstanceJavaMembers(ss, JSForm.class))); //$NON-NLS-1$
				}
				finally
				{
					Context.exit();
				}
			}
		}
		application.getRuntimeWindowManager().setCurrentWindowName(name);
	}

	public boolean createNewFormInstance(String designFormName, String newInstanceScriptName)
	{
		Form f = possibleForms.get(designFormName);
		Form test = possibleForms.get(newInstanceScriptName);
		if (f != null && test == null)
		{
			possibleForms.put(newInstanceScriptName, f);
			return true;
		}
		return false;
	}

	public boolean destroyFormInstance(String formName)
	{
		Form test = possibleForms.get(formName);
		if (test != null)
		{
			// If form found, test if there is a formcontroller alive.
			FormController fc = getCachedFormController(formName);
			if (fc != null)
			{
				// if that one can be deleted destroy it.
				if (!canBeDeleted(fc))
				{
					return false;
				}
				fc.destroy();
			}
			// if the formname is an alias then remove the alias.
			if (!test.getName().equals(formName))
			{
				possibleForms.remove(formName);
			}
			setFormReadOnly(formName, false);
			return true;
		}
		return false;
	}

	/**
	 * @param form
	 * @return
	 */
	public List<FormController> getCachedFormControllers(Form form)
	{
		ArrayList<FormController> al = new ArrayList<FormController>();
		for (FormController controller : createdFormControllers.values())
		{
			if (controller.getForm().getName().equals(form.getName()))
			{
				al.add(controller);
			}
			else if (controller.getForm() instanceof FlattenedForm)
			{
				List<Form> formHierarchy = application.getFlattenedSolution().getFormHierarchy(controller.getForm());
				if (formHierarchy.contains(form))
				{
					al.add(controller);
				}
			}
		}
		return al;
	}

	/**
	 * @param form
	 * @return
	 */
	public List<FormController> getCachedFormControllers()
	{
		return new ArrayList<FormController>(createdFormControllers.values());
	}

	/**
	 * @param name
	 * @return
	 */
	public boolean recreateForm(String name)
	{
		Form test = possibleForms.get(name);
		if (test != null)
		{
			// If form found, test if there is a formcontroller alive.
			FormController fc = getCachedFormController(name);
			if (fc != null)
			{
				fc.recreateUI();
			}
		}
		return false;
	}

	/**
	 * @param name
	 */
	public void removeContainer(String name)
	{
		containers.remove(name);
	}
}
