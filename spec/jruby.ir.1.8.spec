# Default RubySpec/CI settings for JRuby.

# detect windows platform:
require 'rbconfig'
require 'java'
require 'jruby/util'

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/
WINDOWS = Config::CONFIG['host_os'] =~ /mswin/

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS_DIR)

class MSpecScript
  # Language features specs
  set :language, [ SPEC_DIR + '/language' ]

  # Core library specs
  set :core, [
    SPEC_DIR + '/core',

    # Current crashers
    '^' + SPEC_DIR + '/core/binding',        # AddBindingInstructions pass bug
    '^' + SPEC_DIR + '/core/kernel/binding', # AddBindingInstructions pass bug
    '^' + SPEC_DIR + '/core/thread/run',     # infinite loop because of AddBindingInstructions bugs
    '^' + SPEC_DIR + '/core/thread/raise',   # infinite loop because of AddBindingInstructions bugs
    '^' + SPEC_DIR + '/core/thread/wakeup',  # infinite loop because of AddBindingInstructions bugs

    # 1.9
    '^' + SPEC_DIR + '/core/basicobject'
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
    '^' + SPEC_DIR + '/library/readline',

    # unstable
    '^' + SPEC_DIR + '/library/socket',
    '^' + SPEC_DIR + '/library/syslog',

    # 1.9 feature
    '^' + SPEC_DIR + '/library/cmath',
    '^' + SPEC_DIR + '/library/continuation',
    '^' + SPEC_DIR + '/library/coverage',
    '^' + SPEC_DIR + '/library/fiber',
    '^' + SPEC_DIR + '/library/json',
    '^' + SPEC_DIR + '/library/minitest',
    '^' + SPEC_DIR + '/library/prime',
    '^' + SPEC_DIR + '/library/ripper',
    '^' + SPEC_DIR + '/library/rake',
    '^' + SPEC_DIR + '/library/rubygems',
  ]

  # Command Line specs
  set :command_line, [ SPEC_DIR + '/command_line' ]

  if WINDOWS
    # Some specs on Windows will fail in we launch JRuby via
    # ruby_exe() in-process (see core/argf/gets_spec.rb)
    JRuby.runtime.instance_config.run_ruby_in_process = false
    # core
    get(:core) << '^' + SPEC_DIR + '/core/file/stat'    # many failures

    # exclude specs tagged with 'windows' keyword
    set :ci_xtags, ['windows']
  end

  set :ci_files, get(:language) + get(:core) + get(:command_line) + get(:library)

  # The default implementation to run the specs.
  set :target, File.dirname(__FILE__) + '/../bin/' + Config::CONFIG['ruby_install_name'] + Config::CONFIG['EXEEXT']

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(^.*/language/),     TAGS_DIR + '/1.8/ruby/language/'],
                        [%r(^.*/core/),         TAGS_DIR + '/1.8/ruby/core/'],
                        [%r(^.*/command_line/), TAGS_DIR + '/1.8/ruby/command_line/'],
                        [%r(^.*/library/),      TAGS_DIR + '/1.8/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]

  # Disable features (not currently supported)
  MSpec.disable_feature :fork

  # Enable features
  MSpec.enable_feature :continuation
  MSpec.enable_feature :readline
  # These are encoding-aware methods backported to 1.8.7+ (eg String#bytes)
  MSpec.enable_feature :encoding_transition
end
