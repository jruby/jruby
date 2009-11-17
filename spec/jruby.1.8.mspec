# default RubySpec/CI settings for JRuby.

# detect windows platform:
require 'rbconfig'
require 'java'
require 'jruby'

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

    # FIXME: Temporarily exclusion until JRUBY-4180 is fixed.
    '^' + SPEC_DIR + '/core/proc/case_compare_spec.rb',
    '^' + SPEC_DIR + '/core/proc/element_reference_spec.rb',
    '^' + SPEC_DIR + '/core/proc/yield_spec.rb',
    '^' + SPEC_DIR + '/core/proc/call_spec.rb',

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

  if WINDOWS
    # core
    get(:core) << '^' + SPEC_DIR + '/core/argf'          # hangs
    get(:core) << '^' + SPEC_DIR + '/core/dir'           # many failures
    get(:core) << '^' + SPEC_DIR + '/core/env'           # many failures
    get(:core) << '^' + SPEC_DIR + '/core/file'          # many failures
    get(:core) << '^' + SPEC_DIR + '/core/filetest'      # many failures
    get(:core) << '^' + SPEC_DIR + '/core/io'            # many failures
    get(:core) << '^' + SPEC_DIR + '/core/kernel'        # many failures
    get(:core) << '^' + SPEC_DIR + '/core/process'       # many failures

    # library
    get(:library) << '^' + SPEC_DIR + '/library/logger'   # many failures
    get(:library) << '^' + SPEC_DIR + '/library/ftools'   # many failures
    get(:library) << '^' + SPEC_DIR + '/library/resolv'   # many failures
    get(:library) << '^' + SPEC_DIR + '/library/tempfile' # many failures

    # exclude specs tagged with 'windows' keyword
    set :xtags, ['windows']
  end

  set :ci_files, get(:language) + get(:core) + get(:library)

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
end
