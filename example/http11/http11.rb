# 
# To change this template, choose Tools | Templates
# and open the template in the editor.

include Java
m = org.jruby.ext.loadmod.ModuleLoader.new
m.load(self, "http11")
parser = Mongrel::HttpParser.new
req = {}
http = "GET / HTTP/1.1\r\n\r\n"
nread = parser.execute(req, http, 0)
