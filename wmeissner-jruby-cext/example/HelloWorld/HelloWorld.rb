# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
include Java

m = org.jruby.cext.ModuleLoader.new
m.load(self, "hello")
include Hello
puts get_hello
say_hello "Fubar Fubar!"
