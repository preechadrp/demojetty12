#====== สร้างไฟล์ /opt/jetty-app/run_jetty.sh   

#!/bin/bash

# Path to your Java installation (adjust if necessary)
JAVA_HOME="/usr/bin/java" # Or wherever your Java executable is located

# Path to your .jar file
JAR_FILE="/opt/jetty-app/your-application.jar" # IMPORTANT: Change this to your actual JAR path

# Optional: Java memory settings
JAVA_OPTS="-Xms256m -Xmx512m"

# Optional: Arguments to pass to your JAR application
APP_ARGS=""

# Run the JAR file
exec "$JAVA_HOME" $JAVA_OPTS -jar "$JAR_FILE" $APP_ARGS


##====== สร้างไฟล์ .service
sudo nano /etc/systemd/system/your-application.service

##=======มีข้อความดังนี้ =======
[Unit]
Description=My Jetty Embedded Application # คำอธิบายสำหรับบริการของคุณ
After=network.target # ให้บริการนี้รันหลังจากเครือข่ายพร้อมใช้งานแล้ว

[Service]
# User and Group to run the service as (recommended for security)
User=youruser # IMPORTANT: Change to an unprivileged user (e.g., 'jettyapp' or 'www-data')
Group=yourgroup # IMPORTANT: Change to a suitable group

# Type of service (simple for most applications)
Type=simple

# Working directory for your application
WorkingDirectory=/opt/jetty-app/ # IMPORTANT: Change to the directory where your JAR/script resides

# Command to execute (choose one of the options below)

# Option 1: If you created a separate shell script (recommended)
ExecStart=/opt/jetty-app/run_jetty.sh

# Option 2: If you want to run the JAR directly without a separate script
# ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/jetty-app/your-application.jar

# Restart policy
Restart=on-failure # Restart if the application exits with an error
RestartSec=5s      # Wait 5 seconds before attempting to restart

# Standard output and error logs
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=jetty-app # Identifier for logs in syslog

# กำหนดเวลาที่ systemd จะรอให้แอปพลิเคชันปิดตัวลงอย่างสง่างาม
# ก่อนที่จะส่ง SIGKILL (บังคับปิด)
# ค่าเริ่มต้นคือ 90 วินาที
# ตัวอย่าง: ตั้งค่าเป็น 60 วินาที (1 นาที)
TimeoutStopSec=60s

[Install]
WantedBy=multi-user.target # บริการนี้จะถูกเรียกใช้เมื่อระบบเข้าสู่ multi-user mode (ปกติคือ boot up)

##======= จบข้อความ 

sudo systemctl daemon-reload #โหลด daemon ใหม่
sudo systemctl enable your-application.service #เปิดใช้งานบริการ (เพื่อให้รันอัตโนมัติเมื่อบูตเครื่อง)
sudo systemctl start your-application.service #เริ่มโปรแกรม
sudo journalctl -u your-application.service  #ดู Logs
