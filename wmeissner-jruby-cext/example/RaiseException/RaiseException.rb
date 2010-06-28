# 
# To change this template, choose Tools | Templates
# and open the template in the editor.
 
include Java

m = org.jruby.cext.ModuleLoader.new
#ext = File.join(File.dirname(__FILE__), "raiseexception")
m.load(self, "raiseexception")
begin
  RaiseException::explode
  puts "ERROR: no exception raised"
rescue RuntimeError => re
  puts "PASS: #{re}"
  $stdout.flush
end
