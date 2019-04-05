/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.sablo.InMemPackageReader;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.specification.Package;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.IPersistIndex;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.PersistIndexCache;
import com.servoy.j2db.persistence.ChangeHandler;
import com.servoy.j2db.persistence.IItemChangeListener;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISequenceProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.endpoint.NGClientEndpoint;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.property.types.Types;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 *
 */
public abstract class AbstractSolutionTest
{
	static
	{
		// tell log4j to print to console output
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		AppenderComponentBuilder console = builder.newAppender("stdout", "Console");
		builder.add(console);

		RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ERROR);
		rootLogger.add(builder.newAppenderRef("stdout"));
		builder.add(rootLogger);

		Configurator.initialize(builder.build());
	}

	protected static IServer DUMMY_ISERVER = new IServer()
	{

		@Override
		public ITable getTable(String tableName) throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public ITable getTableBySqlname(String tableSQLName) throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public List<String> getTableAndViewNames(boolean hideTemporary) throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public List<String> getTableNames(boolean hideTempTables) throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public Map<String, ITable> getInitializedTables() throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public List<String> getViewNames(boolean hideTempViews) throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public int getTableType(String tableName) throws RepositoryException, RemoteException
		{
			return 0;
		}

		@Override
		public Collection<Procedure> getProcedures() throws RepositoryException, RemoteException
		{
			return Collections.emptySet();
		}

		@Override
		public String getName() throws RemoteException
		{
			return null;
		}

		@Override
		public boolean isValid() throws RemoteException
		{
			return true;
		}

		@Override
		public String getDatabaseProductName() throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public String getQuotedIdentifier(String tableSqlName, String columnSqlName) throws RepositoryException, RemoteException
		{
			return null;
		}

		@Override
		public String[] getDataModelClonesFrom() throws RemoteException
		{
			return null;
		}

		@Override
		public ISequenceProvider getSequenceProvider()
		{
			return null;
		}
	};

	private static IPackageReader[] getReaders(File[] packages, IPackageReader customComponents)
	{
		ArrayList<IPackageReader> readers = new ArrayList<>();
		if (customComponents != null) readers.add(customComponents);
		for (File f : packages)
		{
			if (f.exists())
			{
				if (f.isDirectory()) readers.add(new Package.DirPackageReader(f));
				else readers.add(new Package.ZipPackageReader(f, f.getName().substring(0, f.getName().length() - 4)));
			}
			else
			{
				Debug.error("A web component package location does not exist: " + f.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		return readers.toArray(new IPackageReader[readers.size()]);
	}

	protected IValidateName validator = new IValidateName()
	{
		@Override
		public void checkName(String nameToCheck, int skip_element_id, ValidatorSearchContext searchContext, boolean sqlRelated) throws RepositoryException
		{
		}
	};
	protected Solution solution;
	protected TestNGClient client;
	protected NGClientEndpoint endpoint;

	public AbstractSolutionTest()
	{
		super();
	}

	private static class ZipPackageReader implements Package.IPackageReader
	{
		private final ZipFile file;
		private final String pathPrefix;

		public ZipPackageReader(ZipFile file, String pathPrefix)
		{
			this.file = file;
			this.pathPrefix = pathPrefix;
		}

		@Override
		public String getName()
		{
			String[] split = file.getEntry(pathPrefix).getName().split("/");
			return split[split.length - 1].replace("/", "");
		}

		@Override
		public String getPackageName()
		{
			try
			{
				String packageDisplayname = Package.getPackageName(getManifest());
				if (packageDisplayname != null) return packageDisplayname;
			}
			catch (IOException e)
			{
				Debug.log(e);
			}

			// fall back to symbolic name
			return FilenameUtils.getBaseName(getName());
		}

		@Override
		public String getPackageDisplayname()
		{
			try
			{
				String packageDisplayname = Package.getPackageDisplayname(getManifest());
				if (packageDisplayname != null) return packageDisplayname;
			}
			catch (IOException e)
			{
				Debug.log(e);
			}

			// fall back to symbolic name
			return getPackageName();
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			ZipEntry m = file.getEntry(pathPrefix + "META-INF/MANIFEST.MF");
			try (InputStream is = file.getInputStream(m))
			{
				return new Manifest(is);
			}
		}

		@Override
		public String getVersion()
		{
			try
			{
				return getManifest().getMainAttributes().getValue("Bundle-Version");
			}
			catch (IOException e)
			{
			}
			return null;
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			ZipEntry m = file.getEntry(pathPrefix + path);
			try (InputStream is = file.getInputStream(m))
			{
				return Utils.getTXTFileContent(is, charset);
			}
		}

		@Override
		public URL getUrlForPath(String path) throws MalformedURLException
		{
			return null;
		}

		@Override
		public URL getPackageURL()
		{
			return null;
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			System.err.println(e.getMessage());
		}

		@Override
		public void clearError()
		{
		}

		@Override
		public String getPackageType()
		{
			try
			{
				return Package.getPackageType(getManifest());
			}
			catch (IOException e)
			{
				Debug.log("Error getting package type." + getName(), e);
			}
			return null;
		}

		@Override
		public File getResource()
		{
			return null;
		}

	}

	@Before
	public void buildSolution() throws Exception
	{
		TestNGClient.initSettings();
		Types.getTypesInstance().registerTypes();

		final File f = new File(NGClient.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		IPackageReader[] servicesReaders = null;
		IPackageReader[] componentsReaders = null;
		InMemPackageReader inMemPackageReader = getTestComponents();
		if (f.isFile() && f.getName().startsWith("servoy_ngclient") && f.getName().endsWith(".jar"))
		{
			// it is running from bundles/jars
			ZipFile zipFile = new ZipFile(f);
			componentsReaders = inMemPackageReader != null
				? new IPackageReader[] { new ZipPackageReader(zipFile, "war/servoycore/"), new ZipPackageReader(zipFile,
					"war/servoydefault/"), inMemPackageReader }
				: new IPackageReader[] { new ZipPackageReader(zipFile, "war/servoycore/"), new ZipPackageReader(zipFile, "war/servoydefault/") };
			servicesReaders = new IPackageReader[] { new ZipPackageReader(zipFile, "war/servoyservices/") };
		}
		else
		{
			// it is running from sources/projects
			File ngClientProjDir = f;
			if (!new File(ngClientProjDir, "/war/servoycore/").exists())
			{
				ngClientProjDir = ngClientProjDir.getParentFile();
			}
			componentsReaders = getReaders(new File[] { new File(ngClientProjDir.getAbsoluteFile() + "/war/servoycore/"), new File(
				ngClientProjDir.getAbsoluteFile() + "/war/servoydefault/") }, inMemPackageReader); //in eclipse we .. out of bin, in jenkins we .. out of @dot
			servicesReaders = getReaders(new File[] { new File(ngClientProjDir.getAbsoluteFile(), "/war/servoyservices/") }, null);
		}

		WebComponentSpecProvider.init(componentsReaders);
		WebServiceSpecProvider.init(servicesReaders);

		final TestRepository tr = new TestRepository();
		try
		{
			ApplicationServerRegistry.setApplicationServerSingleton(new TestApplicationServer(tr));
			UUID uuid = UUID.randomUUID();
			final RootObjectMetaData metadata = tr.createRootObjectMetaData(tr.getElementIdForUUID(uuid), uuid, "Test", IRepository.SOLUTIONS, 1, 1);

			solution = (Solution)tr.createRootObject(metadata);
			tr.cacheRootObject(solution);
			solution.setChangeHandler(new ChangeHandler(tr));
			fillTestSolution();

			HttpSession testHttpsession = new TestHttpsession();

			endpoint = new NGClientEndpoint()
			{
				// for testing onstart of the NGClientEndpoint should not run
				@Override
				public void onStart()
				{
				}

				@Override
				protected HttpSession getHttpSession(Session session)
				{
					return testHttpsession;
				}
			};

			NGClientWebsocketSession session = new NGClientWebsocketSession(new WebsocketSessionKey(testHttpsession.getId(), 1))
			{
				@Override
				public void init(Map<String, List<String>> requestParams) throws Exception
				{
					// override default init, shouldnt make another client.
				}

				@Override
				protected IEventDispatcher createEventDispatcher()
				{
					return new TestNGEventDispatcher(endpoint);
				}
			};

			WebsocketSessionManager.addSession(session);

			NGClientWebsocketSessionWindows windows = new NGClientWebsocketSessionWindows(session);
			CurrentWindow.set(windows);

			client = new TestNGClient(tr, session)
			{
				@Override
				public boolean loadSolutionsAndModules(SolutionMetaData solutionMetaData)
				{
					boolean b = super.loadSolutionsAndModules(solutionMetaData);
					IPersistIndex index = PersistIndexCache.getCachedIndex(solution);
					solution.getChangeHandler().addIPersistListener((IItemChangeListener<IPersist>)index);

					try
					{
						setupData();
					}
					catch (ServoyException e)
					{
						e.printStackTrace();
					}
					return b;
				}

			};
			J2DBGlobals.setServiceProvider(client);
			client.setUseLoginSolution(false);

			endpoint.start(new TestSession(), String.valueOf(session.getSessionKey().getClientnr()), "null", "42");

			CurrentWindow.set(session.getWindows().iterator().next());
		}
		catch (RepositoryException e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@After
	public void tearDown() throws Exception
	{
		CurrentWindow.set(null);
		FormElementHelper.INSTANCE.reload();
		ApplicationServerRegistry.clear();
	}

	protected abstract void setupData() throws ServoyException;

	/**
	 * @throws RepositoryException
	 * @throws ServoyException
	 */
	protected abstract void fillTestSolution() throws ServoyException;

	/**
	 * @return
	 * @throws IOException
	 */
	protected abstract InMemPackageReader getTestComponents() throws IOException;

	private static class TestHttpsession implements HttpSession
	{
		private final String id = randomUUID().toString();

		private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

		@Override
		public long getCreationTime()
		{
			return 0;
		}

		@Override
		public String getId()
		{
			return id;
		}

		@Override
		public long getLastAccessedTime()
		{
			return 0;
		}

		@Override
		public ServletContext getServletContext()
		{
			return null;
		}

		@Override
		public void setMaxInactiveInterval(int interval)
		{
		}

		@Override
		public int getMaxInactiveInterval()
		{
			return 0;
		}

		@Override
		public HttpSessionContext getSessionContext()
		{
			return null;
		}

		@Override
		public Object getAttribute(String name)
		{
			return attributes.get(name);
		}

		@Override
		public Object getValue(String name)
		{
			return null;
		}

		@Override
		public Enumeration<String> getAttributeNames()
		{
			return Collections.enumeration(attributes.keySet());
		}

		@Override
		public String[] getValueNames()
		{
			return null;
		}

		@Override
		public void setAttribute(String name, Object value)
		{
			attributes.put(name, value);
		}

		@Override
		public void putValue(String name, Object value)
		{
		}

		@Override
		public void removeAttribute(String name)
		{
			attributes.remove(name);
		}

		@Override
		public void removeValue(String name)
		{
		}

		@Override
		public void invalidate()
		{
		}

		@Override
		public boolean isNew()
		{
			return false;
		}
	}

	private static class TestSession implements Session
	{
		@Override
		public void setMaxTextMessageBufferSize(int arg0)
		{
		}

		@Override
		public void setMaxIdleTimeout(long arg0)
		{
		}

		@Override
		public void setMaxBinaryMessageBufferSize(int arg0)
		{
		}

		@Override
		public void removeMessageHandler(MessageHandler arg0)
		{
		}

		@Override
		public boolean isSecure()
		{
			return false;
		}

		@Override
		public boolean isOpen()
		{
			return true;
		}

		@Override
		public Map<String, Object> getUserProperties()
		{
			return null;
		}

		@Override
		public Principal getUserPrincipal()
		{
			return null;
		}

		@Override
		public URI getRequestURI()
		{
			return null;
		}

		@Override
		public Map<String, List<String>> getRequestParameterMap()
		{
			return Collections.singletonMap("solution", Arrays.asList("Test"));
		}

		@Override
		public String getQueryString()
		{
			return null;
		}

		@Override
		public String getProtocolVersion()
		{
			return null;
		}

		@Override
		public Map<String, String> getPathParameters()
		{
			return null;
		}

		@Override
		public Set<Session> getOpenSessions()
		{
			return null;
		}

		@Override
		public String getNegotiatedSubprotocol()
		{
			return null;
		}

		@Override
		public List<Extension> getNegotiatedExtensions()
		{
			return null;
		}

		@Override
		public Set<MessageHandler> getMessageHandlers()
		{
			return null;
		}

		@Override
		public int getMaxTextMessageBufferSize()
		{
			return 0;
		}

		@Override
		public long getMaxIdleTimeout()
		{
			return 0;
		}

		@Override
		public int getMaxBinaryMessageBufferSize()
		{
			return 0;
		}

		@Override
		public String getId()
		{
			return null;
		}

		@Override
		public WebSocketContainer getContainer()
		{
			return null;
		}

		@Override
		public Basic getBasicRemote()
		{
			return new Basic()
			{
				@Override
				public void setBatchingAllowed(boolean arg0) throws IOException
				{
				}

				@Override
				public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException
				{
				}

				@Override
				public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException
				{
				}

				@Override
				public boolean getBatchingAllowed()
				{
					return false;
				}

				@Override
				public void flushBatch() throws IOException
				{
				}

				@Override
				public void sendText(String arg0, boolean arg1) throws IOException
				{
				}

				@Override
				public void sendText(String arg0) throws IOException
				{
				}

				@Override
				public void sendObject(Object arg0) throws IOException, EncodeException
				{
				}

				@Override
				public void sendBinary(ByteBuffer arg0, boolean arg1) throws IOException
				{
				}

				@Override
				public void sendBinary(ByteBuffer arg0) throws IOException
				{
				}

				@Override
				public Writer getSendWriter() throws IOException
				{
					return null;
				}

				@Override
				public OutputStream getSendStream() throws IOException
				{
					return null;
				}
			};
		}

		@Override
		public Async getAsyncRemote()
		{
			return null;
		}

		@Override
		public void close(CloseReason arg0) throws IOException
		{
		}

		@Override
		public void close() throws IOException
		{
		}

		@Override
		public <T> void addMessageHandler(Class<T> arg0, Whole<T> arg1) throws IllegalStateException
		{
		}

		@Override
		public <T> void addMessageHandler(Class<T> arg0, Partial<T> arg1) throws IllegalStateException
		{
		}

		@Override
		public void addMessageHandler(MessageHandler arg0) throws IllegalStateException
		{
		}
	}
}