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

    # as_superuser, as_user, Process.euid
    "^spec/ruby/core/file/chown_spec.rb",
    "^spec/ruby/core/file/lchown_spec.rb",
    "^spec/ruby/core/process/euid_spec.rb",
    "^spec/ruby/core/process/kill_spec.rb",
    "^spec/ruby/core/process/setpriority_spec.rb",
    "^spec/ruby/core/process/uid_spec.rb",

    # require 'socket'
    "^spec/ruby/core/file/socket_spec.rb",

    # FileTest in describe
    "^spec/ruby/core/filetest",

    # STDOUT.tty?
    "^spec/ruby/core/io/tty_spec.rb",
    "^spec/ruby/core/io/isatty_spec.rb",

    # require 'fcntl'
    "^spec/ruby/core/io/reopen_spec.rb",

    # __method__ in fixtures
    "^spec/ruby/core/kernel/__method___spec.rb",

    # autoload in describe
    "^spec/ruby/core/kernel/autoload_spec.rb",

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
    "spec/ruby/library/erb",
    "spec/ruby/library/set",
    "spec/ruby/library/strscan",
    "spec/ruby/library/stringio",
    "spec/ruby/library/thread"
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
