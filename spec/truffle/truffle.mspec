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

    # require 'socket'
    "^spec/ruby/core/file/socket_spec.rb",

    # require 'fcntl'
    "^spec/ruby/core/io/reopen_spec.rb",

    # seem side-effecting when not run in isolation
    "^spec/ruby/core/marshal/dump_spec.rb",
    "^spec/ruby/core/marshal/float_spec.rb",
    "^spec/ruby/core/marshal/load_spec.rb",
    "^spec/ruby/core/marshal/restore_spec.rb",

    # require 'timeout'
    "^spec/ruby/core/process/detach_spec.rb",

    # problems with comparing special characters and tags
    "^spec/ruby/core/regexp/compile_spec.rb",
    "^spec/ruby/core/string/chomp_spec.rb",
    "^spec/ruby/core/string/modulo_spec.rb",

    # NullPointerException on load
    "^spec/ruby/core/regexp/encoding_spec.rb",

    # error compiling regex on load
    "^spec/ruby/core/regexp/source_spec.rb",
    "^spec/ruby/core/string/match_spec.rb",

    # infinite loop on some examples
    # "^spec/ruby/core/string/gsub_spec.rb",
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
    "spec/ruby/library/abbrev",
    "spec/ruby/library/base64",
    "spec/ruby/library/complex",
    "spec/ruby/library/conditionvariable",
    "spec/ruby/library/date",
    "spec/ruby/library/datetime",
    "spec/ruby/library/delegate",
    "spec/ruby/library/erb",
    "spec/ruby/library/getoptlong",
    "spec/ruby/library/matrix",
    "spec/ruby/library/observer",
    "spec/ruby/library/open3",
    "spec/ruby/library/openstruct",
    "spec/ruby/library/pathname",
    "spec/ruby/library/prime",
    "spec/ruby/library/scanf",
    "spec/ruby/library/set",
    "spec/ruby/library/shellwords",
    "spec/ruby/library/singleton",
    "spec/ruby/library/stringio",
    "spec/ruby/library/strscan",
    "spec/ruby/library/tempfile",
    "spec/ruby/library/thread",
    "spec/ruby/library/time",
    "spec/ruby/library/tmpdir",
    "spec/ruby/library/uri",

    # Not yet explored
    "^spec/ruby/library/bigdecimal",
    "^spec/ruby/library/cgi",
    "^spec/ruby/library/continuation",
    "^spec/ruby/library/csv",
    "^spec/ruby/library/digest",
    "^spec/ruby/library/drb",
    "^spec/ruby/library/etc",
    "^spec/ruby/library/expect",
    "^spec/ruby/library/fiber",
    "^spec/ruby/library/ipaddr",
    "^spec/ruby/library/logger",
    "^spec/ruby/library/mathn",
    "^spec/ruby/library/net",
    "^spec/ruby/library/openssl",
    "^spec/ruby/library/readline",
    "^spec/ruby/library/resolv",
    "^spec/ruby/library/rexml",
    "^spec/ruby/library/securerandom",
    "^spec/ruby/library/stringscanner",
    "^spec/ruby/library/syslog",
    "^spec/ruby/library/timeout",
    "^spec/ruby/library/weakref",
    "^spec/ruby/library/win32ole",
    "^spec/ruby/library/zlib",
    "^spec/ruby/library/yaml",
    "^spec/ruby/library/socket",

    # Load issues with 'delegate'.
    "^spec/ruby/library/delegate/delegate_class/instance_method_spec.rb",
    "^spec/ruby/library/delegate/delegator/protected_methods_spec.rb",

    # LoadError for `load "prime.rb"`
    "^spec/ruby/library/prime/each_spec.rb",
  ]

  set :tags_patterns, [
                        [%r(^.*/language/),     'spec/truffle/tags/language/'],
                        [%r(^.*/core/),         'spec/truffle/tags/core/'],
                        [%r(^.*/library/),      'spec/truffle/tags/library/'],
                        [/_spec.rb$/,           '_tags.txt']
                      ]

  if windows?
    # exclude specs tagged with 'windows'
    set :xtags, (get(:xtags) || []) + ['windows']
  end

  MSpec.enable_feature :encoding
  MSpec.enable_feature :fiber
  MSpec.disable_feature :fork
  MSpec.enable_feature :generator

  set :files, get(:language) + get(:core) + get(:library)

end
