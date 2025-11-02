-jetty server 12.0.x สำหรับทำ API 
-ทำ https
-graceful shutdown
-รองรับ .jsp
-รองรับ webfilter
-logback
-ทดสอบการใช้ apache ant
 -ใช้คำสั่ง mvn dependency:copy-dependencies -DoutputDirectory=lib -DincludeScope=runtime  เพื่อดึงไฟล์ dependency ด้วย maven มาไว้ที่ lib directory
  โดยใน pom.xml ต้องมีการเรียกใช้ maven-dependency-plugin ด้วย
