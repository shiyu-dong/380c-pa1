run: compile
	java -cp '.:SaM-2.6.2.jar' BaliCompiler good.expr-1.bali good.expr.out
compile: BaliCompiler.java Sam-2.6.2.jar
	javac -cp '.:SaM-2.6.2.jar' -Xlint BaliCompiler.java
clean:
	rm -rf *.class *.out *.log

