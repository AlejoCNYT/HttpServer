@echo off
cd /d %~dp0
mvn -q clean package
java -cp target\classes com.mycompany.httpserver.HttpServer
