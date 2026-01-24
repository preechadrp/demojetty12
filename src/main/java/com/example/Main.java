package com.example;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.concurrent.Executors;

import org.eclipse.jetty.ee10.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.ee10.jsp.JettyJspServlet;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.VirtualThreadPool;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Main {

	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Main.class);

	public Server server = null;
	int http_server_port = 8080;
	int https_server_port = 8443;
	public static final boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("window") >= 0;
	public static Main main = null;

	/**
	 * สำหรับ winsw  ผ่าน java -jar myapp.jar
	 * @param args
	 */
	public static void main(String[] args) {
		main = new Main();
		main.startServer();	
	}
	
	/**
	 * สำหรับ apache procrun ใน mode = jvm
	 * @param args
	 */
	public static void startApp(String[] args) {
		main = new Main();
		main.startServer();	
	}
	
	/**
	 * สำหรับ apache procrun ใน mode = jvm
	 * @param args
	 */
	public static void stopApp(String[] args) {
		if (main != null) {
	        main.stopServer();
	    }
	}
	
	public void startServer() {
		try {

			// == ตัวอย่าง
			// ใช้ jetty-ee10-webapp 12.0.23
			// การหา resource แบบปลอดภัย
			// ทำ stop gracefull

			var threadPool = new VirtualThreadPool();
			//กำหนดให้ทำงานแบบ Virtual Threads
			// threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
			// สามารถกำหนดชื่อ prefix ของ Virtual Threads ได้เพื่อการ Debug
			threadPool.setVirtualThreadsExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("jetty-vt-", 0).factory()));

			server = new Server(threadPool);
			//ดึงจาก System property การส่งมา เช่น java -Djetty.https.enabled=true -jar app.jar
			//ถ้าไม่เจอดึงจากตัวแปรระบบ
			boolean enableJettyHttps = Boolean.parseBoolean(
					System.getProperty("jetty.https.enabled",
							System.getenv().getOrDefault("JETTY_HTTPS_ENABLED", "false")));

			if (enableJettyHttps == false) {
				addHttpConnector(http_server_port);
			} else {
				addHttpsConnector(https_server_port, true);
			}
			addContext();

			server.setStopTimeout(60000);
			server.setStopAtShutdown(true);
			server.addEventListener(new LifeCycle.Listener() {
				@Override
				public void lifeCycleStopping(LifeCycle event) {
					log.info("Jetty is stopping gracefully");
				}
				@Override
				public void lifeCycleStopped(LifeCycle event) {
					log.info("Jetty fully stopped");
				}
			});

			server.start();
			server.join();
			
			if (isWindows) {
				System.exit(0);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * สำหรับ apache procrun mode jvm
	 */
	public void stopServer() {
	    try {
	        if (server != null && server.isRunning()) {
	            server.stop();
	            server.join();   // ⭐ รอให้ stop เสร็จก่อน
	        }
	    } catch (Exception e) {
	        log.error(e.getMessage(), e);
	    } finally {
	        System.exit(0);
	    }
	}
	
	public void addHttpConnector(int port) throws Exception {
		HttpConfiguration httpConf = new HttpConfiguration();

		httpConf.addCustomizer(new ForwardedRequestCustomizer()); //Jetty อยู่ หลัง nginx / proxy , อ่าน header จาก proxy: แล้ว “หลอก” Jetty ว่า request เดิมเป็น HTTPS

		ServerConnector httpConnector = new ServerConnector(
				server,
				new HttpConnectionFactory(httpConf));
		httpConnector.setPort(port);
		server.addConnector(httpConnector);
	}
	
	public void addHttpsConnector(int port, boolean useTrustStore) throws Exception {

		// Setup HTTPS Configuration
		HttpConfiguration httpsConf = new HttpConfiguration();
		httpsConf.setSecurePort(port);
		httpsConf.setSecureScheme("https");
		
		httpsConf.addCustomizer(new SecureRequestCustomizer()); // Jetty ทำ HTTPS เอง

		// Setup SSL
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

		// กำหนด KeyStore (สำหรับ Server Certificate)
		sslContextFactory.setKeyStorePath("./keystore.jks"); //แบบดึงจาก working directory
		sslContextFactory.setKeyStorePassword("mykeystore");
		sslContextFactory.setKeyManagerPassword("mykeystoreManagerPassword");

		String[] allowedCiphers = {
				 "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
				 "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
				// เพิ่ม Cipher Suites อื่นๆ ที่คุณต้องการ (และรองรับใน JVM ของคุณ)
				// หลีกเลี่ยง Cipher Suites ที่มี SHA1, RC4, 3DES, หรือไม่มี Forward Secrecy
				//หรือถ้าใช้ TLS 1.3 เป็นหลัก → ไม่ต้องตั้ง cipher เลยก็ได้ (ปล่อย JVM เลือก)
		};
		if (allowedCiphers.length > 0) {
			sslContextFactory.setIncludeCipherSuites(allowedCiphers);
		}

		// กำหนด Protocol ที่อนุญาต (Allowed Protocols) ***
		// ควรใช้ TLSv1.2 และ TLSv1.3 เท่านั้น หลีกเลี่ยง SSLv3, TLSv1, TLSv1.1
		sslContextFactory.setIncludeProtocols("TLSv1.2", "TLSv1.3");

		if (useTrustStore) {

			//แบบดึงใน .jar ตอน dev อยู่ใน src/main/resources/truststore.jks
			//URL truststoreUrl = Main.class.getClassLoader().getResource("truststore.jks"); 
			//sslContextFactory.setTrustStorePath(truststoreUrl.toExternalForm());
			
			sslContextFactory.setTrustStorePath("./truststore.jks");
			sslContextFactory.setTrustStorePassword("password"); // รหัสผ่าน TrustStore

			// ถ้าต้องการให้เซิร์ฟเวอร์ร้องขอ Client Certificate (Mutual TLS)
			// sslContextFactory.setNeedClientAuth(true); // บังคับให้ไคลเอนต์ส่งใบรับรอง
			// sslContextFactory.setWantClientAuth(true); // ร้องขอแต่ไม่บังคับ

		}

		// Establish the HTTPS ServerConnector
		ServerConnector httpsConnector = new ServerConnector(
				server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(httpsConf));
		httpsConnector.setPort(port);

		server.addConnector(httpsConnector);

	}

	private void addContext() throws URISyntaxException, IOException {

		var context = new WebAppContext();

		// add servlet
		addMainApi(context);

		// add filter
		addWebFilter(context);

		// set web resource
		URL rscURL = Main.class.getResource("/webapp");
		log.info("Using BaseResource: " + rscURL.toExternalForm());
		context.setBaseResourceAsString(rscURL.toExternalForm());
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "index.jsp" });
		context.setParentLoaderPriority(true);
		// context.getSessionHandler().setMaxInactiveInterval(900);//ไม่ผ่านต้องใช้ไฟล์ /WEB-INF/web.xml ถึงจะผ่าน ,test 7/7/68

		// กำหนด Temp Directory สำหรับ JSP Compilation** 
		// JSP ต้องมีที่เก็บไฟล์ Java ที่ถูกคอมไพล์ (Scratch directory) 
		File tempDir = Files.createTempDirectory("jetty-jsp-scratch").toFile();
		tempDir.deleteOnExit(); // ลบเมื่อโปรแกรมปิด 
		context.setAttribute(ServletContext.TEMPDIR, tempDir);
		log.info("JSP Scratch Directory (getAbsolutePath): " + tempDir.getAbsolutePath());
		log.info("JSP Scratch Directory (getPath): " + tempDir.getPath());
		log.info("JSP Scratch Directory (getCanonicalPath): " + tempDir.getCanonicalPath());
		log.info("JSP Scratch Directory (getCanonicalFile): " + tempDir.getCanonicalFile());
		
		/*
		 * ตัวอย่าง path ต่างๆ
		 JSP Scratch Directory (getAbsolutePath):  C:\Users\HP12D5~1\AppData\Local\Temp\jetty-jsp-scratch16538553756296998470
		 JSP Scratch Directory (getPath):          C:\Users\HP12D5~1\AppData\Local\Temp\jetty-jsp-scratch16538553756296998470
         JSP Scratch Directory (getCanonicalPath): C:\Users\H P\AppData\Local\Temp\jetty-jsp-scratch16538553756296998470
         JSP Scratch Directory (getCanonicalFile): C:\Users\H P\AppData\Local\Temp\jetty-jsp-scratch16538553756296998470
		 */

		// เพิ่ม JSP Initializer** 
		// JSP ต้องใช้ ServletContainerInitializer ในการเริ่มต้น 
		context.addServletContainerInitializer(new JettyJasperInitializer());
		// เพิ่ม JSP Servlet** 
		// เพิ่มตัวจัดการสำหรับไฟล์ *.jsp 
		context.addServlet(new JettyJspServlet(), "*.jsp");

		// เพิ่ม Default Servlet** 
		// เพื่อจัดการไฟล์ Static (เช่น HTML, CSS) และเป็นตัว fallback 
		context.addServlet(new DefaultServlet(), "/");

		server.setHandler(context);

	}
	
	private void addMainApi(WebAppContext context) {

		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				log.info("Request handled by thread: {}", Thread.currentThread().getName());
				log.info("request.getSession().getId() : {}", request.getSession(true).getId());
				log.info("session timeout : {}", request.getSession().getMaxInactiveInterval());// seconds unit

				response.setContentType("application/json");
				response.setStatus(HttpServletResponse.SC_OK);
				//java.util.Base64.getEncoder().encode(JwtUtil.getJwt().getBytes("UTF-8"))
				//System.out.println(java.util.Base64.getEncoder().encode(JwtUtil.getJwt().));
				response.getWriter().println("{ \"jwt\": \"" + JwtUtil.getJwt() + "\"}");

			}

		}, "/getjwt");// test link = http://localhost:8080/getjwt

		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				log.info("Request handled by thread: {}", Thread.currentThread().getName());
				log.info("call /api/blocking");
				log.info("request.getSession().getId() : {}", request.getSession(true).getId());
				log.info("session timeout : {}", request.getSession().getMaxInactiveInterval());// seconds unit

				response.setContentType("application/json");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("{ \"status\": \"ok\"}");

			}

		}, "/api/blocking");// test link = http://localhost:8080/api/blocking

	}

	private void addWebFilter(WebAppContext context) {

		context.addFilter(new Filter() {

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {

				log.info("hello from filter");
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				try {
					String token = getBearerToken(httpRequest);
					if (token != null && !token.isEmpty()) {
						JwtUtil.verifyToken(token);
					} else {
						log.info("No JWT provided, go on unauthenticated");
						throw new IOException("unauthenticated");
					}
					chain.doFilter(request, response);
				} catch (Exception e) {
					log.info(e.getMessage(), e);
					HttpServletResponse httpResponse = (HttpServletResponse) response;
					httpResponse.setContentLength(0);
					httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				}

			}

			/**
			 * Get the bearer token from the HTTP request. The token is in the HTTP request
			 * "Authorization" header in the form of: "Bearer [token]"
			 */
			private String getBearerToken(HttpServletRequest request) {
				String authPrefix = "Bearer ";
				String authHeader = request.getHeader("Authorization");
				if (authHeader != null && authHeader.startsWith(authPrefix)) {
					return authHeader.substring(authPrefix.length());
				}
				return null;
			}

		}, "/api/*", EnumSet.of(DispatcherType.REQUEST));

	}

}
