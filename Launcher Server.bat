@echo off
@title TwMS 063 Server Debug Mode
set CLASSPATH=.;dist\*;lib\*
java -server server.swing.WvsCenter
pause
