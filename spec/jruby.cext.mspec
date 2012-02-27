# Default RubySpec/CI settings for JRuby.

# detect windows platform:
require 'rbconfig'
require 'java'
require 'jruby/util'

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/
WINDOWS = RbConfig::CONFIG['host_os'] =~ /mswin/

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS_DIR)

class MSpecScript
  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :ci_files, [
    SPEC_DIR + '/optional/capi',
  ]

  # The default implementation to run the specs.
  set :target, File.dirname(__FILE__) + '/../bin/' + RbConfig::CONFIG['ruby_install_name'] + RbConfig::CONFIG['EXEEXT']

  set :backtrace_filter, /mspec\//

  # Disable features (not currently supported)
  MSpec.disable_feature :continuation
  MSpec.disable_feature :fork

  # Enable features
  MSpec.enable_feature :readline
  # These are encoding-aware methods backported to 1.8.7+ (eg String#bytes)
  MSpec.enable_feature :encoding_transition
end
