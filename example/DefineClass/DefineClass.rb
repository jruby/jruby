# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
include Java

m = org.jruby.cext.ModuleLoader.new
m.load(self, "defineclass")

h = Hello.new
puts "Hello.new returned #{h.inspect}"
puts "h.get_hello returns #{h.get_hello}"
