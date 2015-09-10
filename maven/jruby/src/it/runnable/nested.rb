# TODO needs fix in jruby
#require_relative 'test'
require 'uri:classloader:/test'

# not use Singleton scope since in this case it would use the on global runtime
other = org.jruby.embed.IsolatedScriptingContainer.new(org.jruby.embed.LocalContextScope::THREADSAFE)

other.runScriptlet("$other = #{JRuby.runtime.object_id}")

# TODO needs fix in jruby
#other.runScriptlet( "require_relative 'test_other'" )
other.runScriptlet( "require 'uri:classloader:/test_other'" )

other.terminate
