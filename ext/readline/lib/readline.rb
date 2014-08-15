require 'readline/version'

# load jline and our readline into classloader
begin
  # if we have jar-dependencies we let it track the jars
  require 'jar-dependencies'
  require_jar( 'jline', 'jline', Readline::Version::JLINE_VERSION )
rescue LoadError
  require "jline/jline/jline-#{Readline::Version::JLINE_VERSION}.jar"
end

require "readline.jar"

# boot extension
begin
  org.jruby.ext.readline.ReadlineService.new.load(JRuby.runtime, false)
rescue NameError => ne
  raise NameError, "unable to load readline subsystem: #{ne.message}", ne.backtrace
end
