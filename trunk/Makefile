compile: clean BaliCompiler.java Sam-2.6.2.jar
	javac -cp '.:SaM-2.6.2.jar' -Xlint BaliCompiler.java
run: compile
	./run.sh
clean:
	rm -rf *.class *.out *.log *.sam ./examples/*.sam ./tests/*.sam

