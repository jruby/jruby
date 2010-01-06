# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
include Java

m = org.jruby.cext.ModuleLoader.new
ext = File.join(File.dirname(__FILE__), "mytest")
m.load(self, ext)
include MyTest
puts test1
