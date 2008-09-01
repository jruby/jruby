import "StringBuffer", "java.lang.StringBuffer"
import "ArrayList", "java.util.ArrayList"

list = ArrayList.new
sb = StringBuffer.new("Hello")
sb.append(", world")
list.add(sb)
puts list
