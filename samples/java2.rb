#import java.io will make all the classes in 
#this package available.  What happens is that 
#whenever a constant lookup fails the runtime
#will try to resolve this constant as the name of
#a class in the imported java packages.
Java::import "java.io"
#rename the java.io.File to javaFile otherwise
#it would clash with the built in File class
Java::name "java.io.File", "JavaFile"

filename = "./samples/java2.rb"

#creates an instance of a java.io.File object,
#file is its representation in jruby
file = JavaFile.new filename

puts 1
#java.io.FileReader does not conflict with any
#built in names, it can be used normaly since we
#imported the java.io package
fr = FileReader.new file

puts 2
#same remark
#the runtime will find the constructor most appropriate
#to the type and number of argument passed.
#this is different from java which does this resolution 
#at compile time.  Here we effectively have a double dispatch,
#on the actual type of the receiver and on the type of the
#parameters
br = BufferedReader.new fr

puts 3

#this is calling a java method on the Buffered reader
s = br.readLine

print "------ ", filename, "------\n"

while s
  puts s.to_s
  s = br.readLine
end

print "------ ", filename, " end ------\n";

br.close
