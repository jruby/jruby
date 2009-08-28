# Configuration file for Ruby 1.9-compatible Ruby implementations.
#
# Unless you passed to --config (or -B) to MSpec, MSpec will automatically
# use this config file if the Ruby interpreter with which MSpec advertises
# itself with RUBY_VERSION =~ /1.9/

DIR = File.join(File.dirname(__FILE__), "ruby")

class MSpecScript
  # Language features specs
  set :language, [ DIR + '/language' ]

  # Core library specs
  set :core, [
    DIR + '/core',

    '^' + DIR + '/core/continuation'
  ]

  # Standard library specs
  set :library, [
    DIR + '/library',

    # obsolete libraries
    '^' + DIR + '/library/cgi-lib',
    '^' + DIR + '/library/date2',
    '^' + DIR + '/library/enumerator',
    '^' + DIR + '/library/eregex',
    '^' + DIR + '/library/finalize',
    '^' + DIR + '/library/ftools',
    '^' + DIR + '/library/generator',
    '^' + DIR + '/library/getopts',
    '^' + DIR + '/library/importenv',
    '^' + DIR + '/library/jcode',
    '^' + DIR + '/library/mailread',
    '^' + DIR + '/library/parsearg',
    '^' + DIR + '/library/parsedate',
    '^' + DIR + '/library/ping',
    '^' + DIR + '/library/readbytes',
    '^' + DIR + '/library/rubyunit',
    '^' + DIR + '/library/runit',
    '^' + DIR + '/library/soap',
    '^' + DIR + '/library/wsdl',
    '^' + DIR + '/library/xsd',
    '^' + DIR + '/library/Win32API',

    '^' + DIR + '/library/test/unit/collector',
    '^' + DIR + '/library/test/unit/ui',
    '^' + DIR + '/library/test/unit/util',

    '^' + DIR + '/library/dl',  # reimplemented and API changed
  ]

  # An ordered list of the directories containing specs to run
  set :files, get(:language) + get(:core) + get(:library)

  # This set of files is run by mspec ci
  set :ci_files, get(:files)

  # Optional library specs
  set :ffi, DIR + '/optional/ffi'

  # A list of _all_ optional library specs
  set :optional, [get(:ffi)]

  # The default implementation to run the specs.
  # TODO: this needs to be more sophisticated since the
  # executable is not consistently named.
  set :target, 'jruby'

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(language/),     'tags/1.9/language/'],
                        [%r(core/),         'tags/1.9/core/'],
                        [%r(command_line/), 'tags/1.9/command_line/'],
                        [%r(library/),      'tags/1.9/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
