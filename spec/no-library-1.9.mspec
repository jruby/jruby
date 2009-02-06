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
    File.dirname(__FILE__) + '/ruby/library/abbrev',
    File.dirname(__FILE__) + '/ruby/library/base64',
    File.dirname(__FILE__) + '/ruby/library/bigdecimal',
    File.dirname(__FILE__) + '/ruby/library/cgi',
    File.dirname(__FILE__) + '/ruby/library/complex',
    File.dirname(__FILE__) + '/ruby/library/csv',
    File.dirname(__FILE__) + '/ruby/library/date',
    File.dirname(__FILE__) + '/ruby/library/digest',
    File.dirname(__FILE__) + '/ruby/library/enumerator',
    File.dirname(__FILE__) + '/ruby/library/erb',
    File.dirname(__FILE__) + '/ruby/library/iconv',
    File.dirname(__FILE__) + '/ruby/library/ipaddr',
    File.dirname(__FILE__) + '/ruby/library/logger',
    File.dirname(__FILE__) + '/ruby/library/mathn',
    File.dirname(__FILE__) + '/ruby/library/matrix',
    File.dirname(__FILE__) + '/ruby/library/mutex',
    File.dirname(__FILE__) + '/ruby/library/observer',
    File.dirname(__FILE__) + '/ruby/library/openstruct',
    File.dirname(__FILE__) + '/ruby/library/pathname',
    File.dirname(__FILE__) + '/ruby/library/rational',
    File.dirname(__FILE__) + '/ruby/library/resolv',
    File.dirname(__FILE__) + '/ruby/library/rexml',
    File.dirname(__FILE__) + '/ruby/library/scanf',
    File.dirname(__FILE__) + '/ruby/library/set',
    File.dirname(__FILE__) + '/ruby/library/shellwords',
    File.dirname(__FILE__) + '/ruby/library/singleton',
    File.dirname(__FILE__) + '/ruby/library/stringio',
    File.dirname(__FILE__) + '/ruby/library/stringscanner',
    File.dirname(__FILE__) + '/ruby/library/tempfile',
    File.dirname(__FILE__) + '/ruby/library/time',
    File.dirname(__FILE__) + '/ruby/library/tmpdir',
    File.dirname(__FILE__) + '/ruby/library/uri',
    File.dirname(__FILE__) + '/ruby/library/yaml',
    File.dirname(__FILE__) + '/ruby/library/zlib',

    # 1.9 feature
    File.dirname(__FILE__) + 'ruby/library/cmath',
    File.dirname(__FILE__) + 'ruby/library/continuation',
    File.dirname(__FILE__) + 'ruby/library/coverage',
    File.dirname(__FILE__) + 'ruby/library/fiber',
    File.dirname(__FILE__) + 'ruby/library/json',
    File.dirname(__FILE__) + 'ruby/library/minitest',
    File.dirname(__FILE__) + 'ruby/library/prime',
    File.dirname(__FILE__) + 'ruby/library/ripper',
    File.dirname(__FILE__) + 'ruby/library/rake',
    File.dirname(__FILE__) + 'ruby/library/rubygems',
  ]

  # The default implementation to run the specs.
  if WINDOWS
    jruby_script = 'jruby.bat'
  else
    jruby_script = 'jruby'
  end

  set :target, File.dirname(__FILE__) + '/../bin/' + jruby_script
end
