
include Java

m = org.jruby.ext.loadmod.ModuleLoader.new
m.load(self, "etc")
name = CEtc.getlogin
puts "login name = #{name}"
pwd = CEtc.getpwnam(name)
puts "pwd=#{pwd}"