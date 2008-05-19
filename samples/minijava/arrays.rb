require 'minijava'

import 'java.lang.String', 'JString'

# simple array
str_ary = JString[4]
str_ary[0] = 'foo'.to_java
str_ary[1] = 'bar'.to_java
str_ary[2] = 'baz'.to_java

puts str_ary[0]
puts str_ary[3]
puts str_ary.length

import 'java.util.Arrays'

puts Arrays.asList(str_ary)

# multi-dimensional array
str_matrix = JString[2,2]
str_matrix[0,0] = 'foo'.to_java
str_matrix[0,1] = 'bar'.to_java
str_matrix[1,0] = 'baz'.to_java

puts str_matrix[0,0]
puts str_matrix[1,1]
puts str_matrix[0]

puts Arrays.asList(str_matrix)
puts Arrays.asList(str_matrix[0])

# primitive array
import 'i', 'Jint'
import 'float', 'Jfloat'

iarray = Jint[2]
farray = Jfloat[1]

puts iarray.length
puts farray.length

iarray[0] = iarray.length

puts iarray[0]
puts iarray[1]
puts farray[0]

puts iarray.class.java_class
puts farray.class.java_class
