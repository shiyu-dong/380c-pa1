ls examples/good.*.bali | awk '{print "java -cp .:SaM-2.6.2.jar BaliCompiler ./" $1 " " $1 ".sam; echo " $1 "; echo "}' | sh
ls tests/*.bali | awk '{print "java -cp .:SaM-2.6.2.jar BaliCompiler ./" $1 " " $1 ".sam; echo " $1 "; echo "}' | sh
