@echo off
javac -encoding UTF-8 -d build MidiSynthesizer.java
java -cp build MidiSynthesizer
pause
