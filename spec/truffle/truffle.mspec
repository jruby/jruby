class MSpecScript

  def self.windows?
    ENV.key?('WINDIR') || ENV.key?('windir')
  end

  set :target, File.expand_path("../../../bin/jruby#{windows? ? '.bat' : ''}", __FILE__)
  set :flags, %w[-X+T -J-ea -J-Xmx2G]

  set :language, [
    "spec/ruby/language"
  ]

  core = [
    "spec/ruby/core",

    # Can't load these - so tags aren't enough to exclude them. The problem is
    # either fixtures or syntax. Some of them are probably easy fixes.

    # This seems to hang sometimes on Travis
    "^spec/ruby/core/signal",

    # require 'fcntl'
    "^spec/ruby/core/io/reopen_spec.rb",

    # seem side-effecting when not run in isolation
    "^spec/ruby/core/marshal/dump_spec.rb",
    "^spec/ruby/core/marshal/float_spec.rb",
    "^spec/ruby/core/marshal/load_spec.rb",
    "^spec/ruby/core/marshal/restore_spec.rb",

    # require 'timeout'
    "^spec/ruby/core/process/detach_spec.rb",

    # fail tag not excluding
    "^spec/ruby/core/string/modulo_spec.rb",

    # require etc, linux only spec
    "^spec/ruby/core/io/advise_spec.rb",

    # Pollutes other tests
    "^spec/ruby/core/argf/binmode_spec.rb"
  ]

  core += [
    # Windows
    "^spec/ruby/core/method/source_location_spec.rb",
    "^spec/ruby/core/struct/each_spec.rb",
    "^spec/ruby/core/struct/element_reference_spec.rb",
    "^spec/ruby/core/struct/element_set_spec.rb",
    "^spec/ruby/core/struct/eql_spec.rb",
    "^spec/ruby/core/struct/equal_value_spec.rb",
    "^spec/ruby/core/struct/hash_spec.rb",
    "^spec/ruby/core/struct/initialize_copy_spec.rb",
    "^spec/ruby/core/struct/initialize_spec.rb",
    "^spec/ruby/core/struct/inspect_spec.rb",
    "^spec/ruby/core/struct/instance_variables_spec.rb",
    "^spec/ruby/core/struct/length_spec.rb",
    "^spec/ruby/core/struct/members_spec.rb",
    "^spec/ruby/core/struct/new_spec.rb",
    "^spec/ruby/core/struct/select_spec.rb",
    "^spec/ruby/core/struct/size_spec.rb",
    "^spec/ruby/core/struct/struct_spec.rb",
    "^spec/ruby/core/struct/to_a_spec.rb",
    "^spec/ruby/core/struct/to_h_spec.rb",
    "^spec/ruby/core/struct/to_s_spec.rb",
    "^spec/ruby/core/struct/values_at_spec.rb",
    "^spec/ruby/core/struct/values_spec.rb",
    "^spec/ruby/core/symbol/versions/encoding_1.9_spec.rb",
    "^spec/ruby/core/unboundmethod/source_location_spec.rb",
  ] if windows?
  
  set :core, core

  set :library, [
    "spec/ruby/library",

    # Not yet explored
    "^spec/ruby/library/continuation",
    "^spec/ruby/library/fiber",
    "^spec/ruby/library/mathn",
    "^spec/ruby/library/readline",
    "^spec/ruby/library/syslog",
    "^spec/ruby/library/weakref",

    # Doesn't exist as Ruby code - basically need to write from scratch
    "^spec/ruby/library/win32ole",

    # Uses the Rubinius FFI generator
    "^spec/ruby/library/etc",

    # Hangs
    "^spec/ruby/library/net/http",
    "^spec/ruby/library/net/ftp",

    # Load issues with 'delegate'.
    "^spec/ruby/library/delegate/delegate_class/instance_method_spec.rb",
    "^spec/ruby/library/delegate/delegator/protected_methods_spec.rb",

    # LoadError for `load "prime.rb"`
    "^spec/ruby/library/prime/each_spec.rb",
  ]

  set :truffle, [
    "spec/truffle/specs"
  ]

  set :backtrace_filter, /mspec\//

  set :tags_patterns, [
    [%r(^.*/language/),                 'spec/truffle/tags/language/'],
    [%r(^.*/core/),                     'spec/truffle/tags/core/'],
    [%r(^.*/library/),                  'spec/truffle/tags/library/'],
    [%r(^.*/truffle/specs/truffle),     'spec/truffle/tags/truffle/'],
    [/_spec.rb$/,                       '_tags.txt']
  ]

  if windows?
    # exclude specs tagged with 'windows'
    set :xtags, (get(:xtags) || []) + ['windows']
  end

  MSpec.enable_feature :encoding
  MSpec.enable_feature :fiber
  MSpec.disable_feature :fork
  MSpec.enable_feature :generator

  set :files, get(:language) + get(:core) + get(:library) + get(:truffle)
end

if respond_to?(:ruby_exe)
  class SlowSpecsTagger
    def initialize
      MSpec.register :exception, self
    end

    def exception(state)
      if state.exception.is_a? SlowSpecException
        tag = SpecTag.new("slow:#{state.describe} #{state.it}")
        MSpec.write_tag(tag)
      end
    end
  end

  class SlowSpecException < Exception
  end

  class ::Object
    alias old_ruby_exe ruby_exe
    def ruby_exe(*args, &block)
      if (MSpecScript.get(:xtags) || []).include? 'slow'
        raise SlowSpecException, "Was tagged as slow as it uses ruby_exe(). Rerun specs."
      end
      old_ruby_exe(*args, &block)
    end
  end

  SlowSpecsTagger.new
end
