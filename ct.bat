@echo off
rem Develop/Debug launch helper
java --enable-native-access=copy.tool -p target/classes -m copy.tool/ct.app.App %*