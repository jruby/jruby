# default RubySpec/CI settings for JRuby.

# detect windows platform:
require 'rbconfig'
require 'java'

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS_DIR)

class MSpecScript
  # Language features specs
  set :language, [ SPEC_DIR + '/language' ]

  # Core library specs
  set :core, [
    SPEC_DIR + '/core',

    # 1.9
    '^' + SPEC_DIR + '/core/basicobject'
  ]

  if IKVM
    # ftype_spec freezes for some reason under IKVM
    set(:core, get(:core) + ['^' + SPEC_DIR + '/core/file'])
    # Process.kill spec hangs
    set(:core, get(:core) + ['^' + SPEC_DIR + '/core/process'])
  end

  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :library, [
    SPEC_DIR + '/library/abbrev',
    SPEC_DIR + '/library/base64',
    SPEC_DIR + '/library/bigdecimal',
    SPEC_DIR + '/library/cgi',
    SPEC_DIR + '/library/complex',
    SPEC_DIR + '/library/conditionvariable',
    SPEC_DIR + '/library/csv',
    SPEC_DIR + '/library/date',
    SPEC_DIR + '/library/digest',
    SPEC_DIR + '/library/enumerator',
    SPEC_DIR + '/library/erb',
    SPEC_DIR + '/library/ftools',
    SPEC_DIR + '/library/generator',
    SPEC_DIR + '/library/getoptlong',
    SPEC_DIR + '/library/iconv',
    SPEC_DIR + '/library/ipaddr',
    SPEC_DIR + '/library/logger',
    SPEC_DIR + '/library/mathn',
    SPEC_DIR + '/library/matrix',
    SPEC_DIR + '/library/mutex',
    SPEC_DIR + '/library/observer',
    SPEC_DIR + '/library/openstruct',
    SPEC_DIR + '/library/parsedate',
    SPEC_DIR + '/library/pathname',
    SPEC_DIR + '/library/queue',
    SPEC_DIR + '/library/rational',
    SPEC_DIR + '/library/resolv',
    SPEC_DIR + '/library/rexml',
    SPEC_DIR + '/library/scanf',
    SPEC_DIR + '/library/set',
    SPEC_DIR + '/library/shellwords',
    SPEC_DIR + '/library/singleton',
    SPEC_DIR + '/library/stringio',
    SPEC_DIR + '/library/stringscanner',
    SPEC_DIR + '/library/tempfile',
    SPEC_DIR + '/library/time',
    SPEC_DIR + '/library/timeout',
    SPEC_DIR + '/library/tmpdir',
    SPEC_DIR + '/library/uri',
    SPEC_DIR + '/library/yaml',
    SPEC_DIR + '/library/zlib',

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

  set :ci_files, get(:language) + get(:core) + get(:library)

  # The default implementation to run the specs.
  set :target, File.dirname(__FILE__) + '/../bin/' + Config::CONFIG['ruby_install_name']

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(^.*/language/),     TAGS_DIR + '/1.8/ruby/language/'],
                        [%r(^.*/core/),         TAGS_DIR + '/1.8/ruby/core/'],
                        [%r(^.*/command_line/), TAGS_DIR + '/1.8/ruby/command_line/'],
                        [%r(^.*/library/),      TAGS_DIR + '/1.8/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
