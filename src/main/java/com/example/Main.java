package com.example;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class Main {

	public static void main(String[] args) throws Exception {

		//== ตัวอย่าง
		//ใช้ jetty-ee10-webapp 12.0.22
		//การหา resource แบบปลอดภัย
		//ทำ stop gracefull

		int port = 8080;
		Server server = newServer(port);
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

	public static Server newServer(int port) {

		Server server = new Server(port);
		WebAppContext context = new WebAppContext();

		Resource baseResource = findBaseResource2(context);
		System.out.println("Using BaseResource: " + baseResource);

		context.setBaseResource(baseResource);
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "welcome.html" });
		context.setParentLoaderPriority(true);

		//add servlet
		context.addServlet(BlockingServlet.class, "/blocking");//test link = http://localhost:8080/blocking

		server.setHandler(context);
		return server;
	}

	private static Resource findBaseResource2(WebAppContext context) {

		try {

			URL rscURL = Main.class.getResource("/webapp/");

			if (rscURL != null) { // รันโดยใช้ .jar ไฟล์

				// ชี้ไปยัง directory webapp ใน jar ไฟล์อีกทีหนึ่ง ซึ่ง pom.xml ต้องเพิ่ม includes resource webapp เข้าไปด้วย

				return ResourceFactory.of(context).newResource(rscURL.toURI());

			} else {//รันใน eclipse

				String webAppDir = new File("src/main/webapp").getAbsolutePath();
				URI baseURI = new File(webAppDir).toURI();
				return ResourceFactory.of(context).newResource(baseURI);

			}

		} catch (URISyntaxException e) {
			throw new RuntimeException("Bad ClassPath reference for: WEB-INF", e);
		}
	}

}
