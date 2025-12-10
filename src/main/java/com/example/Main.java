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
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
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
	private int server_port = 8080;
	public static Main main = null;

	/**
	 * นำไปใช้กับ apache procrun ตอน start service ได้ด้วย
	 * @param args
	 */
	public static void main(String[] args) {
		if (args != null && args.length > 0 && args[0].trim().equals("shutdown")) {
			stopServiceByUrl();
		} else {
			main = new Main();
			main.startServer();
		}
	}

	private static void stopServiceByUrl() {
		try {
			java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
					.uri(java.net.URI.create("http://127.0.0.1:8080/shutdown?token=secret123"))
					.GET()
					.build();

			// ส่ง Request และรับ Response
			java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

			// แสดงผลลัพธ์
			if (response.statusCode() != 200) {
				log.info("Status code: {}", response.statusCode());
				log.info(response.body());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * นำไปใช้กับ apache procrun ตอน stop service
	 * @param args
	 */
	public static void stopService(String[] args) {
		main.stopServer();
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

			addConnector(false, false);
			addContext();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> stopServer()));

			server.setStopTimeout(60000l);// รอ 60 นาทีก่อนจะบังคับปิด
			server.start();
			server.join();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void stopServer() {
		try {
			// ใช้เวลาหยุดเซิร์ฟเวอร์
			if (server != null && server.isRunning()) {
				log.warn("init stop");
				server.stop();
				log.info("Jetty server stopped gracefully");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void addConnector(boolean useHttps, boolean useTrustStore) throws Exception {

		if (!useHttps) {

			ServerConnector httpConnector = new ServerConnector(server);
			httpConnector.setPort(server_port);
			server.addConnector(httpConnector);

		} else { // เมื่อต้องการทำ https  หมายเหตุยังไม่ทดสอบ

			// ======== https =======
			// Setup SSL
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

			// กำหนด KeyStore (สำหรับ Server Certificate)
			// ตรวจสอบให้แน่ใจว่าไฟล์ keystore.jks อยู่ใน classpath หรือระบุพาธที่ถูกต้อง
			URL keystoreUrl = Main.class.getClassLoader().getResource("/keystore.jks");
			if (keystoreUrl == null) {
				throw new IllegalStateException("keystore.jks not found in classpath. Please ensure it's in src/main/resources or similar.");
			}
			sslContextFactory.setKeyStorePath(keystoreUrl.toExternalForm());
			sslContextFactory.setKeyStorePassword("mykeystore");
			sslContextFactory.setKeyManagerPassword("mykeystore");

			String[] allowedCiphers = {
					"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
					"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
					"TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
					"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
					// เพิ่ม Cipher Suites อื่นๆ ที่คุณต้องการ (และรองรับใน JVM ของคุณ)
					// หลีกเลี่ยง Cipher Suites ที่มี SHA1, RC4, 3DES, หรือไม่มี Forward Secrecy
			};
			if (allowedCiphers.length > 0) {
				sslContextFactory.setIncludeCipherSuites(allowedCiphers);
			}

			// กำหนด Protocol ที่อนุญาต (Allowed Protocols) ***
			// ควรใช้ TLSv1.2 และ TLSv1.3 เท่านั้น หลีกเลี่ยง SSLv3, TLSv1, TLSv1.1
			sslContextFactory.setIncludeProtocols("TLSv1.2", "TLSv1.3");

			if (useTrustStore) {

				URL truststoreUrl = Main.class.getClassLoader().getResource("/truststore.jks");
				if (truststoreUrl != null) {
					sslContextFactory.setTrustStorePath(truststoreUrl.toExternalForm());
					sslContextFactory.setTrustStorePassword("password"); // รหัสผ่าน TrustStore

					// ถ้าต้องการให้เซิร์ฟเวอร์ร้องขอ Client Certificate (Mutual TLS)
					// sslContextFactory.setNeedClientAuth(true); // บังคับให้ไคลเอนต์ส่งใบรับรอง
					// sslContextFactory.setWantClientAuth(true); // ร้องขอแต่ไม่บังคับ
				} else {
					log.warn("Not found truststore.jks");
				}

			}

			// สร้าง SslConnectionFactory ---
			// SslConnectionFactory ทำหน้าที่จัดการการเชื่อมต่อ SSL/TLS
			SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, "http/1.1");

			// Setup HTTPS Configuration
			HttpConfiguration httpsConf = new HttpConfiguration();
			httpsConf.setSecurePort(server_port);
			httpsConf.setSecureScheme("https");
			httpsConf.addCustomizer(new SecureRequestCustomizer()); // adds ssl info to request object

			// Establish the HTTPS ServerConnector
			ServerConnector httpsConnector = new ServerConnector(
					server,
					sslConnectionFactory,
					new HttpConnectionFactory(httpsConf));
			httpsConnector.setPort(server_port);

			server.addConnector(httpsConnector);

		}

	}

	private void addContext() throws URISyntaxException, IOException {

		var context = new WebAppContext();

		// add servlet
		addServlet(context);

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
		log.info("JSP Scratch Directory: " + tempDir.getAbsolutePath());

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

	private void addServlet(WebAppContext context) {

		//สำหรับ shutdown ด้วย winsw/curl ด้วย
		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				log.info("Requested Shutdown");

				//จำกัดให้เรียกได้เฉพาะ localhost
				String remoteAddr = request.getRemoteAddr();
				if (!"127.0.0.1".equals(remoteAddr) && !"0:0:0:0:0:0:0:1".equals(remoteAddr)) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.getWriter().println("Access denied");
					log.warn("Rejected shutdown from: {}", remoteAddr);
					return;
				}

				//ตรวจ token
				String tokenParam = request.getParameter("token");
				if (!"secret123".equals(tokenParam)) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().println("Invalid token");
					log.warn("Invalid token");
					return;
				}

				//เริ่มหยุด Jetty
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("Shutting down Jetty...");

				log.warn(">>> Shutdown requested via /shutdown from {}", remoteAddr);

				new Thread(() -> {
					try {
						Thread.sleep(500); // รอให้ response ส่งกลับ
						server.stop();
						log.info("Jetty server stopped gracefully.");
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}).start();

			}

		}, "/shutdown");// test link = http://localhost:8080/shutdown

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
			protected void doPost(HttpServletRequest request, HttpServletResponse response)
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
