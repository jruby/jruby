
include Java

m = org.jruby.ext.loadmod.ModuleLoader.new
m.load(self, "syslog")
