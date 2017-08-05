# Default RubySpec/CI settings for JRuby.

require 'rbconfig'
require 'java'
require 'jruby'

# Some non-deterministic specs assume a GC will actually fire.  For spec
# runs we change our noop version of GC.start to requesting we actually
# perform a GC on the JVM.
module GC
  def start
    java.lang.System.gc
  end
  module_function :start
end

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/
WINDOWS = RbConfig::CONFIG['host_os'] =~ /mswin/

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS_DIR)

class MSpecScript
  jruby = RbConfig::CONFIG['ruby_install_name'] + RbConfig::CONFIG['EXEEXT']
  jruby = File.expand_path("../../bin/#{jruby}", __FILE__)
  set :target, jruby

  # Command Line specs
  set :command_line, [
    SPEC_DIR + '/command_line',
  ]

  # Language features specs
  set :language, [
    SPEC_DIR + '/language',
  ]

  # Core library specs
  set :core, [
    SPEC_DIR + '/core',
  ]

  # Standard library specs
  set :library, [
    SPEC_DIR + '/library',
  ]

  set :fast, [
    *get(:language),
    *get(:core),
    *get(:library),

    # These all spawn sub-rubies, making them very slow to run
    '^' + SPEC_DIR + '/core/process',
    '^' + SPEC_DIR + '/core/io/popen',
    '^' + SPEC_DIR + '/core/argf/gets_spec.rb',
    '^' + SPEC_DIR + '/core/argf/read_spec.rb',
    '^' + SPEC_DIR + '/core/argf/readline_spec.rb',
    '^' + SPEC_DIR + '/core/encoding/default_external_spec.rb',
    '^' + SPEC_DIR + '/core/encoding/default_internal_spec.rb',
    '^' + SPEC_DIR + '/core/io/pid_spec.rb',
    '^' + SPEC_DIR + '/core/kernel/at_exit_spec.rb',
    '^' + SPEC_DIR + '/language/predefined_spec.rb',
    '^' + SPEC_DIR + '/language/predefined/data_spec.rb',
    '^' + SPEC_DIR + '/library/net/http',
  ]

  # Enable features
  MSpec.enable_feature :fiber
  MSpec.enable_feature :fiber_library
  MSpec.enable_feature :continuation_library
  MSpec.disable_feature :fork
  MSpec.enable_feature :encoding

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

  get(:ci_xtags) << "java#{ENV_JAVA['java.specification.version']}" # Java version

  if WINDOWS
    # Some specs on Windows will fail in we launch JRuby via
    # ruby_exe() in-process (see core/argf/gets_spec.rb)
    JRuby.runtime.instance_config.run_ruby_in_process = false
    # core
    get(:core) << '^' + SPEC_DIR + '/core/file/stat'    # many failures

    # exclude specs tagged with 'windows' keyword
    get(:ci_xtags) << 'windows'
  end

  # If running specs with jit threshold = 1 or force (AOT) compile, additional tags
  if JRuby.runtime.instance_config.compile_mode.to_s == "FORCE" ||
     JRuby.runtime.instance_config.jit_threshold == 1
    get(:ci_xtags) << 'compiler'
  end

  # This set of files is run by mspec ci
  set :ci_files, get(:language) + get(:core) + get(:command_line) + get(:library)

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(^.*/language/),     TAGS_DIR + '/ruby/language/'],
                        [%r(^.*/core/),         TAGS_DIR + '/ruby/core/'],
                        [%r(^.*/command_line/), TAGS_DIR + '/ruby/command_line/'],
                        [%r(^.*/library/),      TAGS_DIR + '/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
