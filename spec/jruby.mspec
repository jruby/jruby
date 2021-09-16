# Default RubySpec/CI settings for JRuby.

require 'rbconfig'
require 'java'
require 'jruby'

# Inherit from the default configuration
load "#{__dir__}/ruby/default.mspec"

# Some non-deterministic specs assume a GC will actually fire.  For spec
# runs we change our noop version of GC.start to requesting we actually
# perform a GC on the JVM.
module GC
  def start(full_mark: true, immediate_sweep: true)
    java.lang.System.gc
  end
  module_function :start
end

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/
HOST_OS = RbConfig::CONFIG['host_os']
WINDOWS = HOST_OS =~ /mswin/

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS_DIR)

class MSpecScript
  set :prefix, 'spec/ruby'

  jruby = RbConfig::CONFIG['ruby_install_name'] + RbConfig::CONFIG['EXEEXT']
  jruby = File.expand_path("../../bin/#{jruby}", __FILE__)
  set :target, jruby

  slow_specs = [
      SPEC_DIR + '/core/process',
      SPEC_DIR + '/core/io/popen',
      SPEC_DIR + '/core/argf/gets_spec.rb',
      SPEC_DIR + '/core/argf/read_spec.rb',
      SPEC_DIR + '/core/argf/readline_spec.rb',
      SPEC_DIR + '/core/encoding/default_external_spec.rb',
      SPEC_DIR + '/core/encoding/default_internal_spec.rb',
      SPEC_DIR + '/core/io/pid_spec.rb',
      SPEC_DIR + '/core/kernel/at_exit_spec.rb',
      SPEC_DIR + '/language/predefined_spec.rb',
      SPEC_DIR + '/language/predefined/data_spec.rb',
      SPEC_DIR + '/language/magic_comment_spec.rb',
      SPEC_DIR + '/library/net/http',
      # This requires --debug which slows down or changes other spec results
      SPEC_DIR + '/core/tracepoint',
      *get(:command_line),
      *get(:security),
  ]

  set :fast, [
      *get(:language),
      *get(:core),
      *get(:library),

      # These all spawn sub-rubies, making them very slow to run
      *slow_specs.map {|name| '^' + name},
  ]

  set :slow, slow_specs

  # Filter out ObjectSpace specs if ObjectSpace is disabled
  unless JRuby.objectspace
    get(:core) << '^' + SPEC_DIR + '/core/objectspace/_id2ref'
    get(:core) << '^' + SPEC_DIR + '/core/objectspace/each_object'
  end

  if IKVM
    # ftype_spec freezes for some reason under IKVM
    get(:core) << '^' + SPEC_DIR + '/core/file'
    # Process.kill spec hangs
    get(:core) << '^' + SPEC_DIR + '/core/process'
  end

  # prepare exclusion tags
  set(:xtags, get(:xtags) || [])
  set(:ci_xtags, get(:ci_xtags) || [])

  get(:xtags) << 'critical'
  get(:ci_xtags) << 'critical'
  get(:xtags) << 'hangs'
  get(:ci_xtags) << 'hangs'

  get(:ci_xtags) << "java#{ENV_JAVA['java.specification.version']}" # Java version

  if (ENV["TRAVIS"] == "true")
    get(:ci_xtags) << "travis" # Failing only on Travis
  end

  get(:ci_xtags) << HOST_OS

  instance_config = JRuby.runtime.instance_config

  if WINDOWS
    # Some specs on Windows will fail in we launch JRuby via
    # ruby_exe() in-process (see core/argf/gets_spec.rb)
    instance_config.run_ruby_in_process = false

    # exclude specs tagged with 'windows' keyword
    get(:ci_xtags) << 'windows'
  end

  # If running specs with jit threshold = 0 or force (AOT) compile, additional tags
  if instance_config.compile_mode.to_s == "FORCE" ||
      instance_config.jit_threshold == 0
    get(:ci_xtags) << 'jit'
  end

  # This set of files is run by mspec ci
  set :ci_files, get(:language) + get(:core) + get(:command_line) + get(:library) + get(:security)

  set :tags_patterns, [
                        [%r(^.*/language/),     TAGS_DIR + '/ruby/language/'],
                        [%r(^.*/core/),         TAGS_DIR + '/ruby/core/'],
                        [%r(^.*/command_line/), TAGS_DIR + '/ruby/command_line/'],
                        [%r(^.*/library/),      TAGS_DIR + '/ruby/library/'],
                        [%r(^.*/security/),     TAGS_DIR + '/ruby/security/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
