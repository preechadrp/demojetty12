-jetty server 12.0.x สำหรับทำ API 
-virtual thread
-ทำ https
-graceful shutdown
-รองรับ .jsp
-รองรับ webfilter
-การใช้ jwt
-logback
-การใช้ apache ant
 *ใช้คำสั่ง mvn dependency:copy-dependencies -DoutputDirectory=lib -DincludeScope=runtime  เพื่อดึงไฟล์ dependency ด้วย maven มาไว้ที่ lib directory
  โดยใน pom.xml ต้องมีการเรียกใช้ maven-dependency-plugin ด้วย
 *ถ้ารันใน eclipse ใช้แค่ dependency:copy-dependencies -DoutputDirectory=lib -DincludeScope=runtime 
  เมื่อได้ lib directory ที่มี .jar แล้วให้เรา commit เก็บไว้ใน git server ด้วย เพราะเราจะใช้ build ด้วย apache ant แบบ offline
 *กรณีมีการแก้ไข dependency ใน pom.xml ให้ลบ lib ก่อนแล้วรันคำสั่งเพื่อรวบรวม lib ใหม่เพื่อลบ .jar ที่เราไม่ได้ใช้ออก
-การนำ .jar ไปสร้างเป็น windows service ด้วย winsw , apache procrun (ไม่ work กับ procrun เพราะวางแอป path ยาวๆ ไม่ได้และ stop service ไม่ได้ )
