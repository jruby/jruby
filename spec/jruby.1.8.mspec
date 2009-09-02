# default RubySpec/CI settings for JRuby.

# detect windows platform:
require 'rbconfig'
require 'java'

IKVM = java.lang.System.get_property('java.vm.name') =~ /IKVM\.NET/
DIR = File.dirname(__FILE__)

class MSpecScript
  # Language features specs
  set :language, [ DIR + '/ruby/language' ]

  # Core library specs
  set :core, [
    DIR + '/ruby/core',

    # 1.9
    '^' + DIR + '/ruby/core/basicobject'
  ]

  if IKVM
    # ftype_spec freezes for some reason under IKVM
    set(:core, get(:core) + ['^' + DIR + '/ruby/core/file'])
    # Process.kill spec hangs
    set(:core, get(:core) + ['^' + DIR + '/ruby/core/process'])
  end

  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :library, [
    DIR + '/ruby/library/abbrev',
    DIR + '/ruby/library/base64',
    DIR + '/ruby/library/bigdecimal',
    DIR + '/ruby/library/cgi',
    DIR + '/ruby/library/complex',
    DIR + '/ruby/library/conditionvariable',
    DIR + '/ruby/library/csv',
    DIR + '/ruby/library/date',
    DIR + '/ruby/library/digest',
    DIR + '/ruby/library/enumerator',
    DIR + '/ruby/library/erb',
    DIR + '/ruby/library/ftools',
    DIR + '/ruby/library/generator',
    DIR + '/ruby/library/getoptlong',
    DIR + '/ruby/library/iconv',
    DIR + '/ruby/library/ipaddr',
    DIR + '/ruby/library/logger',
    DIR + '/ruby/library/mathn',
    DIR + '/ruby/library/matrix',
    DIR + '/ruby/library/mutex',
    DIR + '/ruby/library/observer',
    DIR + '/ruby/library/openstruct',
    DIR + '/ruby/library/parsedate',
    DIR + '/ruby/library/pathname',
    DIR + '/ruby/library/queue',
    DIR + '/ruby/library/rational',
    DIR + '/ruby/library/resolv',
    DIR + '/ruby/library/rexml',
    DIR + '/ruby/library/scanf',
    DIR + '/ruby/library/set',
    DIR + '/ruby/library/shellwords',
    DIR + '/ruby/library/singleton',
    DIR + '/ruby/library/stringio',
    DIR + '/ruby/library/stringscanner',
    DIR + '/ruby/library/tempfile',
    DIR + '/ruby/library/time',
    DIR + '/ruby/library/timeout',
    DIR + '/ruby/library/tmpdir',
    DIR + '/ruby/library/uri',
    DIR + '/ruby/library/yaml',
    DIR + '/ruby/library/zlib',

    # 1.9 feature
    '^' + DIR + 'ruby/library/cmath',
    '^' + DIR + 'ruby/library/continuation',
    '^' + DIR + 'ruby/library/coverage',
    '^' + DIR + 'ruby/library/fiber',
    '^' + DIR + 'ruby/library/json',
    '^' + DIR + 'ruby/library/minitest',
    '^' + DIR + 'ruby/library/prime',
    '^' + DIR + 'ruby/library/ripper',
    '^' + DIR + 'ruby/library/rake',
    '^' + DIR + 'ruby/library/rubygems',
  ]

  set :ci_files, get(:language) + get(:core) + get(:library)

  # The default implementation to run the specs.
  set :target, DIR + '/../bin/' + Config::CONFIG['ruby_install_name']

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(ruby/language/),     'tags/1.8/ruby/language/'],
                        [%r(ruby/core/),         'tags/1.8/ruby/core/'],
                        [%r(ruby/command_line/), 'tags/1.8/ruby/command_line/'],
                        [%r(ruby/library/),      'tags/1.8/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
