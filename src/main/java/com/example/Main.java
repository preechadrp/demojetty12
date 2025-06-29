package com.example;

import java.net.URL;
import java.util.EnumSet;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

import jakarta.servlet.DispatcherType;

public class Main {

	public static void main(String[] args) throws Exception {

		//== ตัวอย่าง
		//ใช้ jetty-ee10-webapp 12.0.22
		//การหา resource แบบปลอดภัย
		//ทำ stop gracefull

		int port = 8080;
		
		Server server = new Server(port);
		WebAppContext context = new WebAppContext();

		//set web resource
		URL rscURL = Main.class.getResource("/webapp/");
		Resource baseResource = ResourceFactory.of(context).newResource(rscURL.toURI());
		System.out.println("Using BaseResource: " + baseResource);

		context.setBaseResource(baseResource);
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "welcome.html" });
		context.setParentLoaderPriority(true);

		//add servlet
		context.addServlet(BlockingServlet.class, "/blocking");//test link = http://localhost:8080/blocking

		// เพิ่ม filter
		context.addFilter(WebFilter01.class, "/api/*", EnumSet.of(DispatcherType.REQUEST));

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

}
