# default RubySpec/CI settings for JRuby.

# detect windows platform:
require 'rbconfig'
WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

class MSpecScript
  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :ci_files, [
    File.dirname(__FILE__) + '/ruby/core',
    File.dirname(__FILE__) + '/ruby/language',
  ]

  # The default implementation to run the specs.
  if WINDOWS
    jruby_script = 'jruby.bat'
  else
    jruby_script = 'jruby'
  end

  set :target, File.dirname(__FILE__) + '/../bin/' + jruby_script
end
