# Configuration file for Ruby >= 2.0 implementations.

class MSpecScript
  # Language features specs
  set :language, [ 'language' ]

  # Core library specs
  set :core, [ 'core' ]

  # Standard library specs
  set :library, [ 'library' ]

  # An ordered list of the directories containing specs to run
  set :files, get(:language) + get(:core) + get(:library)

  # This set of files is run by mspec ci
  set :ci_files, get(:files)

  # Optional specs
  set :capi, 'optional/capi'

  # A list of _all_ optional specs
  set :optional, [get(:capi)]

  # The default implementation to run the specs.
  # TODO: this needs to be more sophisticated since the
  # executable is not consistently named.
  set :target, 'ruby'

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
                        [%r(language/),     'tags/1.9/language/'],
                        [%r(core/),         'tags/1.9/core/'],
                        [%r(command_line/), 'tags/1.9/command_line/'],
                        [%r(library/),      'tags/1.9/library/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]

  # Enable features
  MSpec.enable_feature :fiber
  MSpec.enable_feature :fiber_library
  MSpec.enable_feature :continuation_library
  MSpec.enable_feature :fork
  MSpec.enable_feature :encoding

  # These are encoding-aware methods backported to 1.8.7+ (eg String#bytes)
  MSpec.enable_feature :encoding_transition

  # The Readline specs are not enabled by default because the functionality
  # depends heavily on the underlying library, including whether certain
  # methods are implemented or not. This makes it extremely difficult to
  # make the specs consistently pass. Until a suitable scheme to handle
  # all these issues, the specs will not be enabled by default.
  #
  # MSpec.enable_feature :readline
end
