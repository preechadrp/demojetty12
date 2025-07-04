package com.example;

import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
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

	static int httpPort = 8080;
	public static void main(String[] args) throws Exception {

		//== ตัวอย่าง
		//ใช้ jetty-ee10-webapp 12.0.22
		//การหา resource แบบปลอดภัย
		//ทำ stop gracefull

		//set thread pool
		int maxThreads = 100;
		int minThreads = 10;
		int idleTimeout = 120;

		var threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
		Server server = new Server(threadPool);

		//add Connector
		addConnector(server);

		//add context
		WebAppContext context = new WebAppContext();
		server.setHandler(context);

		//add servlet
		addServlet(context);

		//add filter
		addWebFilter(context);

		//set web resource
		URL rscURL = Main.class.getResource("/webapp/");
		Resource baseResource = ResourceFactory.of(context).newResource(rscURL.toURI());
		System.out.println("Using BaseResource: " + baseResource);

		context.setBaseResource(baseResource);
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "welcome.html" });
		context.setParentLoaderPriority(true);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				// ใช้เวลาหยุดเซิร์ฟเวอร์
				server.setStopTimeout(60 * 1000l);//รอ 60 นาทีก่อนจะบังคับปิด
				server.stop();
				System.out.println("Jetty server stopped gracefully");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));

		server.start();
		server.join();

	}

	private static void addConnector(Server server) {

		ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(httpPort);
		server.addConnector(httpConnector);

	}

	private static void addServlet(WebAppContext context) {

		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				System.out.println("call /api/blocking");

				response.setContentType("application/json");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("{ \"status\": \"ok\"}");

			}

		}, "/api/blocking");//test link = http://localhost:8080/api/blocking

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
