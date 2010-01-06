# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
include Java

m = org.jruby.cext.ModuleLoader.new
m.load(self, "array")
a = ArrayTest::new_size(128)
puts "a.length=#{a.length}"
a = ArrayTest::new_empty
puts "a.length=#{a.length}"
init = [ 1, 2, 3]
a = ArrayTest::new_elements(init)
puts "a.length=#{a.length}"
a = []
ArrayTest::unshift(a, "foo")
puts "after unshift, length=#{a.length}"
puts "after unshift a[0]=#{a[0]}"

