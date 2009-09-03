# Configuration file for Ruby 1.9-compatible Ruby implementations.
#
# Unless you passed to --config (or -B) to MSpec, MSpec will automatically
# use this config file if the Ruby interpreter with which MSpec advertises
# itself with RUBY_VERSION =~ /1.9/

require 'rbconfig'

DIR = File.dirname(__FILE__)

class MSpecScript
  # Language features specs
  set :language, [ DIR + '/ruby/language' ]

  # Core library specs
  set :core, [
    DIR + '/ruby/core',

    '^' + DIR + '/ruby/core/continuation'
  ]

  # Standard library specs
  set :library, [
    DIR + '/ruby/library',

    # obsolete libraries
    '^' + DIR + '/ruby/library/cgi-lib',
    '^' + DIR + '/ruby/library/date2',
    '^' + DIR + '/ruby/library/enumerator',
    '^' + DIR + '/ruby/library/eregex',
    '^' + DIR + '/ruby/library/finalize',
    '^' + DIR + '/ruby/library/ftools',
    '^' + DIR + '/ruby/library/generator',
    '^' + DIR + '/ruby/library/getopts',
    '^' + DIR + '/ruby/library/importenv',
    '^' + DIR + '/ruby/library/jcode',
    '^' + DIR + '/ruby/library/mailread',
    '^' + DIR + '/ruby/library/parsearg',
    '^' + DIR + '/ruby/library/parsedate',
    '^' + DIR + '/ruby/library/ping',
    '^' + DIR + '/ruby/library/readbytes',
    '^' + DIR + '/ruby/library/rubyunit',
    '^' + DIR + '/ruby/library/runit',
    '^' + DIR + '/ruby/library/soap',
    '^' + DIR + '/ruby/library/wsdl',
    '^' + DIR + '/ruby/library/xsd',
    '^' + DIR + '/ruby/library/Win32API',

    '^' + DIR + '/ruby/library/test/unit/collector',
    '^' + DIR + '/ruby/library/test/unit/ui',
    '^' + DIR + '/ruby/library/test/unit/util',

    '^' + DIR + '/ruby/library/dl',  # reimplemented and API changed
  ]

  # An ordered list of the directories containing specs to run
  # FIXME: add 1.9 library back at a later date
  set :files, get(:language) + get(:core) #+ get(:library)

  # This set of files is run by mspec ci
  set :ci_files, get(:files)

  # Optional library specs
  set :ffi, DIR + '/ruby/optional/ffi'

  # A list of _all_ optional library specs
  set :optional, [get(:ffi)]

  set :target, DIR + '/../bin/' + Config::CONFIG['ruby_install_name']

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(ruby/language/),     'tags/1.9/ruby/language/'],
                        [%r(ruby/core/),         'tags/1.9/ruby/core/'],
                        [%r(ruby/command_line/), 'tags/1.9/ruby/command_line/'],
                        [%r(ruby/library/),      'tags/1.9/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
