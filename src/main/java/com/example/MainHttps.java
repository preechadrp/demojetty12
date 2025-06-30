package com.example;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.security.ConstraintAware;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.SecurityHandler;
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

public class MainHttps {

	public static void main(String[] args) throws Exception {
		
		// ตัวอย่างทำ https 
		//จาก https://github.com/jetty/jetty-examples/blob/12.0.x/embedded/ee10-servlet-security/src/main/java/examples/ServletTransportGuaranteeExample.java

		Server server = new Server();
		int httpPort = 8080;
		int httpsPort = 8443;

		//======== http =======
		// Setup HTTP Connector
		HttpConfiguration httpConf = new HttpConfiguration();
		httpConf.setSecurePort(httpsPort);
		httpConf.setSecureScheme("https");

		// Establish the HTTP ServerConnector
		ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConf));
		httpConnector.setPort(httpPort);
		server.addConnector(httpConnector);

		//======== https =======
		// Setup SSL
		ResourceFactory resourceFactory = ResourceFactory.of(server);
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStoreResource(findKeyStore(resourceFactory));
		sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
		sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");

		// Setup HTTPS Configuration
		HttpConfiguration httpsConf = new HttpConfiguration();
		httpsConf.setSecurePort(httpsPort);
		httpsConf.setSecureScheme("https");
		httpsConf.addCustomizer(new SecureRequestCustomizer()); // adds ssl info to request object

		// Establish the HTTPS ServerConnector
		ServerConnector httpsConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(httpsConf));
		httpsConnector.setPort(httpsPort);

		server.addConnector(httpsConnector);

		// Add a Handler for requests
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SECURITY);
		context.setContextPath("/");

		// Setup security constraint
		SecurityHandler security = context.getSecurityHandler();
		if (security instanceof ConstraintAware) {
			ConstraintAware constraint = (ConstraintAware) security;
			ConstraintMapping mapping = new ConstraintMapping();
			mapping.setPathSpec("/*");
			Constraint dc = new Constraint.Builder()
					.transport(Constraint.Transport.SECURE)
					.build();
			mapping.setConstraint(dc);
			constraint.addConstraintMapping(mapping);
		} else {
			throw new RuntimeException("Not a ConstraintAware SecurityHandler: " + security);
		}

		// Add servlet to produce output
		ServletHolder helloHolder = context.addServlet(BlockingServlet.class, "/*");
		helloHolder.setInitParameter("message", "Hello Secure Servlet World");

		server.setHandler(context);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				// ใช้เวลาหยุดเซิร์ฟเวอร์
				server.stop();
				System.out.println("Jetty server stopped gracefully");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		server.start();
		server.join();
	}

	private static Resource findKeyStore(ResourceFactory resourceFactory) {
		String resourceName = "ssl/keystore";
		Resource resource = resourceFactory.newClassLoaderResource(resourceName);
		if (!Resources.isReadableFile(resource)) {
			throw new RuntimeException("Unable to read " + resourceName);
		}
		return resource;
	}

}
