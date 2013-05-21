# load jline and our readline into classpath
require File.dirname(__FILE__) + "/readline/jline-2.11.jar"
require File.dirname(__FILE__) + "/readline/readline.jar"

# boot extension
org.jruby.ext.readline.ReadlineService.new.load(JRuby.runtime, false)
