package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.resource.Resources;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Main {

	static int server_port = 8080;

	public static void main(String[] args) throws Exception {

		try {

			// == ตัวอย่าง
			// ใช้ jetty-ee10-webapp 12.0.22
			// การหา resource แบบปลอดภัย
			// ทำ stop gracefull

			// set thread pool
			int maxThreads = 100;
			int minThreads = 10;
			int idleTimeout = 120;

			var threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
			Server server = new Server(threadPool);

			addConnectorHttp(server);
			// addConnectorHttps(server);
			addContext(server);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					// ใช้เวลาหยุดเซิร์ฟเวอร์
					server.setStopTimeout(60 * 1000l);// รอ 60 นาทีก่อนจะบังคับปิด
					server.stop();
					System.out.println("Jetty server stopped gracefully");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}));

			server.start();
			server.join();

		} catch (Exception e2) {
			e2.printStackTrace();
		}

	}

	public static void addConnectorHttp(Server server) {

		ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(server_port);
		server.addConnector(httpConnector);

	}

	public static void addConnectorHttps(Server server) {

		// ======== https =======
		// Setup SSL
		ResourceFactory resourceFactory = ResourceFactory.of(server);
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStoreResource(findKeyStore(resourceFactory));
		sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
		sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");

		// Setup HTTPS Configuration
		HttpConfiguration httpsConf = new HttpConfiguration();
		httpsConf.setSecurePort(server_port);
		httpsConf.setSecureScheme("https");
		httpsConf.addCustomizer(new SecureRequestCustomizer()); // adds ssl info to request object

		// Establish the HTTPS ServerConnector
		ServerConnector httpsConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConf));
		httpsConnector.setPort(server_port);

		server.addConnector(httpsConnector);

	}

	private static Resource findKeyStore(ResourceFactory resourceFactory) {
		String resourceName = "ssl/keystore";
		Resource resource = resourceFactory.newClassLoaderResource(resourceName);
		if (!Resources.isReadableFile(resource)) {
			throw new RuntimeException("Unable to read " + resourceName);
		}
		return resource;
	}

	private static void addContext(Server server) throws URISyntaxException {

		WebAppContext context = new WebAppContext();
		server.setHandler(context);

		// set session handler
		SessionHandler sessionHld = new SessionHandler();
		sessionHld.setMaxInactiveInterval((int) TimeUnit.MINUTES.toSeconds(10));// in seconds default : 1800
		sessionHld.setServer(server);
		context.setSessionHandler(sessionHld);

		// add servlet
		addServlet(context);

		// add filter
		addWebFilter(context);

		// set web resource
		URL rscURL = Main.class.getResource("/webapp/");
		Resource baseResource = ResourceFactory.of(context).newResource(rscURL.toURI());
		System.out.println("Using BaseResource: " + baseResource);

		context.setBaseResource(baseResource);
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "welcome.html" });
		context.setParentLoaderPriority(true);

	}

	private static void addServlet(WebAppContext context) {

		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				System.out.println("call /api/blocking");
				System.out.println("request.getSession().getId() : " + request.getSession(true).getId());
				System.out.println("session timeout : " + request.getSession().getMaxInactiveInterval());// seconds unit

				response.setContentType("application/json");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("{ \"status\": \"ok\"}");

			}

		}, "/api/blocking");// test link = http://localhost:8080/api/blocking

	}

	private static void addWebFilter(WebAppContext context) {

		context.addFilter(new Filter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				System.out.println("hello from filter");

				chain.doFilter(request, response);

			}

		}, "/api/*", EnumSet.of(DispatcherType.REQUEST));

	}

}
