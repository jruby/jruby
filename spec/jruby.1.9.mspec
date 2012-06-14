# Default RubySpec/CI settings for JRuby in 1.9 mode.

# detect windows platform:
require 'rbconfig'
require 'java'
require 'jruby'

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/
WINDOWS = RbConfig::CONFIG['host_os'] =~ /mswin/

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS_DIR)

# Add --1.9 to JRUBY_OPTS env so we can be sure it propagates
ENV['JRUBY_OPTS'] = ENV['JRUBY_OPTS'].to_s + " --1.9"

class MSpecScript
  # Language features specs
  set :language, [
    SPEC_DIR + '/language',
  ]

  # Core library specs
  set :core, [
    SPEC_DIR + '/core',

    '^' + SPEC_DIR + '/core/continuation',
    '^' + SPEC_DIR + '/core/encoding/converter'
  ]

  # Filter out ObjectSpace specs if ObjectSpace is disabled
  unless JRuby.objectspace
    get(:core) << '^' + SPEC_DIR + '/core/objectspace/_id2ref'
    get(:core) << '^' + SPEC_DIR + '/core/objectspace/each_object'
  end

  if IKVM
    # ftype_spec freezes for some reason under IKVM
    set(:core, get(:core) + ['^' + SPEC_DIR + '/core/file'])
    # Process.kill spec hangs
    set(:core, get(:core) + ['^' + SPEC_DIR + '/core/process'])
  end

  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :library, [
    SPEC_DIR + '/library',

    # excluded for some reason, see JRUBY-4020
    '^' + SPEC_DIR + '/library/drb',
    '^' + SPEC_DIR + '/library/etc',
    '^' + SPEC_DIR + '/library/net',
    '^' + SPEC_DIR + '/library/openssl',
    '^' + SPEC_DIR + '/library/ping',

    # unstable
    '^' + SPEC_DIR + '/library/syslog',

    # masked out because of load-time errors that can't be tagged
    '^' + SPEC_DIR + '/library/net/http'
  ]

  # Command Line specs
  set :command_line, [ SPEC_DIR + '/command_line' ]

  # Enable features
  MSpec.enable_feature :continuation
  MSpec.enable_feature :fiber
  MSpec.enable_feature :fiber_library
  MSpec.enable_feature :encoding
  MSpec.enable_feature :encoding_transition
  MSpec.enable_feature :readline

  # prepare additional tags for CI
  set(:ci_xtags, ["java#{ENV_JAVA['java.specification.version']}"]) # Java version

  if WINDOWS
    # Some specs on Windows will fail in we launch JRuby via
    # ruby_exe() in-process (see core/argf/gets_spec.rb)
    JRuby.runtime.instance_config.run_ruby_in_process = false
    # core
    get(:core) << '^' + SPEC_DIR + '/core/file/stat'    # many failures

    # exclude specs tagged with 'windows' keyword
    get(:ci_xtags) << 'windows'
  end

  # FIXME: add 1.9 library back at a later date
  set :ci_files, get(:language) + get(:core) + get(:command_line) #+ get(:library)

  # Optional library specs
  set :ffi, SPEC_DIR + '/optional/ffi'

  # A list of _all_ optional library specs
  set :optional, [get(:ffi)]

  set :target, File.dirname(__FILE__) + '/../bin/' + RbConfig::CONFIG['ruby_install_name'] + RbConfig::CONFIG['EXEEXT']

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(^.*/language/),     TAGS_DIR + '/1.9/ruby/language/'],
                        [%r(^.*/core/),         TAGS_DIR + '/1.9/ruby/core/'],
                        [%r(^.*/command_line/), TAGS_DIR + '/1.9/ruby/command_line/'],
                        [%r(^.*/library/),      TAGS_DIR + '/1.9/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
  # If running specs with jit threshold = 1 or force (AOT) compile, additional tags
  if JRuby.runtime.instance_config.compile_mode.to_s == "FORCE" ||
      JRuby.runtime.instance_config.jit_threshold == 1
    set(:ci_xtags, (get(:ci_xtags) || []) + ['compiler'])
  end
end
