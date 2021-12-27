JC = javac
RM = rm -rf
.SUFFIXES: .java .class
.java.class:
	$(JC) -d ./out $*.java

CLASSES = \
	./src/Search/*.java \
	./src/IndexEngine/*.java
	
default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) out
