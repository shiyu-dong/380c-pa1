run: compile
	java BaliCompiler
compile: BaliCompiler.java Sam-2.6.2.jar
	javac -cp Sam-2.6.2.jar BaliCompiler.java
clean:
	rm -rf *.class

