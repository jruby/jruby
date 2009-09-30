# Configuration file for Ruby 1.9-compatible Ruby implementations.
#
# Unless you passed to --config (or -B) to MSpec, MSpec will automatically
# use this config file if the Ruby interpreter with which MSpec advertises
# itself with RUBY_VERSION =~ /1.9/

require 'rbconfig'

SPEC_DIR = File.join(File.dirname(__FILE__), 'ruby') unless defined?(SPEC_DIR)
TAGS_DIR = File.join(File.dirname(__FILE__), 'tags') unless defined?(TAGS__DIR)

class MSpecScript
  # Language features specs
  set :language, [ SPEC_DIR + '/language' ]

  # Core library specs
  set :core, [
    SPEC_DIR + '/core',

    '^' + SPEC_DIR + '/core/continuation'
  ]

  # Standard library specs
  set :library, [
    SPEC_DIR + '/library',

    # unstable
    '^' + SPEC_DIR + '/library/socket',
    '^' + SPEC_DIR + '/library/syslog',

    # obsolete libraries
    '^' + SPEC_DIR + '/library/enumerator',
    '^' + SPEC_DIR + '/library/ftools',
    '^' + SPEC_DIR + '/library/generator',
    '^' + SPEC_DIR + '/library/parsedate',
    '^' + SPEC_DIR + '/library/ping',
  ]

  # An ordered list of the directories containing specs to run
  # FIXME: add 1.9 library back at a later date
  set :files, get(:language) + get(:core) #+ get(:library)

  # This set of files is run by mspec ci
  set :ci_files, get(:files)

  # Optional library specs
  set :ffi, SPEC_DIR + '/optional/ffi'

  # A list of _all_ optional library specs
  set :optional, [get(:ffi)]

  set :target, File.dirname(__FILE__) + '/../bin/' + Config::CONFIG['ruby_install_name']

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(^.*/language/),     TAGS_DIR + '/1.9/ruby/language/'],
                        [%r(^.*/core/),         TAGS_DIR + '/1.9/ruby/core/'],
                        [%r(^.*/command_line/), TAGS_DIR + '/1.9/ruby/command_line/'],
                        [%r(^.*/library/),      TAGS_DIR + '/1.9/ruby/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]
end
