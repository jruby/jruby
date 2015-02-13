class MSpecScript

  def self.windows?
    ENV.key?('WINDIR') || ENV.key?('windir')
  end

  def self.path_separator
    # This is going to be used in a regexp, so we need to escape the backslash character in the regexp as well,
    # thus the double-escape here.
    windows? ? "\\\\" : '/'
  end

  set :target, File.join(File.dirname(__FILE__), "spec-wrapper#{windows? ? '.bat' : ''}")

  set :language, [
    "spec/ruby/language"
  ]

  set :core, [
    "spec/ruby/core",

    # Can't load these - so tags aren't enough to exclude them. The problem is
    # either fixtures or syntax. Some of them are probably easy fixes.

    "^spec/ruby/core/enumerable/find_index_spec.rb",
    "^spec/ruby/core/enumerable/to_a_spec.rb",
    "^spec/ruby/core/enumerable/to_h_spec.rb",
    "^spec/ruby/core/file/chown_spec.rb",
    "^spec/ruby/core/file/lchown_spec.rb",
    "^spec/ruby/core/file/socket_spec.rb",
    "^spec/ruby/core/file/stat/sticky_spec.rb",
    "^spec/ruby/core/file/sticky_spec.rb",
    "^spec/ruby/core/filetest",
    "^spec/ruby/core/io/isatty_spec.rb",
    "^spec/ruby/core/io/reopen_spec.rb",
    "^spec/ruby/core/io/tty_spec.rb",
    "^spec/ruby/core/kernel/__method___spec.rb",
    "^spec/ruby/core/kernel/autoload_spec.rb",
    "^spec/ruby/core/marshal/dump_spec.rb",
    "^spec/ruby/core/marshal/float_spec.rb",
    "^spec/ruby/core/marshal/load_spec.rb",
    "^spec/ruby/core/marshal/restore_spec.rb",
    "^spec/ruby/core/method/source_location_spec.rb",   # Windows
    "^spec/ruby/core/numeric/denominator_spec.rb",
    "^spec/ruby/core/numeric/numerator_spec.rb",
    "^spec/ruby/core/numeric/to_c_spec.rb",
    "^spec/ruby/core/process/detach_spec.rb",
    "^spec/ruby/core/process/euid_spec.rb",
    "^spec/ruby/core/process/kill_spec.rb",
    "^spec/ruby/core/process/setpriority_spec.rb",
    "^spec/ruby/core/process/uid_spec.rb",
    "^spec/ruby/core/regexp/compile_spec.rb",
    "^spec/ruby/core/regexp/encoding_spec.rb",
    "^spec/ruby/core/regexp/source_spec.rb",
    "^spec/ruby/core/signal/list_spec.rb",
    "^spec/ruby/core/string/chomp_spec.rb",
    "^spec/ruby/core/string/crypt_spec.rb",
    "^spec/ruby/core/string/gsub_spec.rb",
    "^spec/ruby/core/string/match_spec.rb",
    "^spec/ruby/core/string/modulo_spec.rb",
    "^spec/ruby/core/struct/each_spec.rb",                   # Windows
    "^spec/ruby/core/struct/element_reference_spec.rb",      # Windows
    "^spec/ruby/core/struct/element_set_spec.rb",            # Windows
    "^spec/ruby/core/struct/eql_spec.rb",                    # Windows
    "^spec/ruby/core/struct/equal_value_spec.rb",            # Windows
    "^spec/ruby/core/struct/hash_spec.rb",                   # Windows
    "^spec/ruby/core/struct/initialize_copy_spec.rb",        # Windows
    "^spec/ruby/core/struct/initialize_spec.rb",             # Windows
    "^spec/ruby/core/struct/inspect_spec.rb",                # Windows
    "^spec/ruby/core/struct/instance_variables_spec.rb",     # Windows
    "^spec/ruby/core/struct/length_spec.rb",                 # Windows
    "^spec/ruby/core/struct/members_spec.rb",                # Windows
    "^spec/ruby/core/struct/new_spec.rb",                    # Windows
    "^spec/ruby/core/struct/select_spec.rb",                 # Windows
    "^spec/ruby/core/struct/size_spec.rb",                   # Windows
    "^spec/ruby/core/struct/struct_spec.rb",                 # Windows
    "^spec/ruby/core/struct/to_a_spec.rb",                   # Windows
    "^spec/ruby/core/struct/to_h_spec.rb",                   # Windows
    "^spec/ruby/core/struct/to_s_spec.rb",                   # Windows
    "^spec/ruby/core/struct/values_at_spec.rb",              # Windows
    "^spec/ruby/core/struct/values_spec.rb",                 # Windows
    "^spec/ruby/core/symbol/versions/encoding_1.9_spec.rb",  # Windows
    "^spec/ruby/core/unboundmethod/source_location_spec.rb"  # Windows
  ]

  set :rubysl, [
    "spec/truffle/spec/rubysl/rubysl-erb/spec",
    "spec/truffle/spec/rubysl/rubysl-set/spec",
    "spec/truffle/spec/rubysl/rubysl-erb/spec",

    # Can't load these - so tags aren't enough to exclude them. The problem is
    # either fixtures or 'before' blocks.
  ]

  set :tags_patterns, [
                        [%r(^.*#{path_separator}language#{path_separator}),     'spec/truffle/tags/language/'],
                        [%r(^.*#{path_separator}core#{path_separator}),         'spec/truffle/tags/core/'],
                        [%r(^.*#{path_separator}rubysl#{path_separator}),       'spec/truffle/tags/rubysl/'],
                        [/_spec.rb$/,           '_tags.txt']
                      ]

  MSpec.enable_feature :encoding
  MSpec.enable_feature :fiber
  MSpec.disable_feature :fork
  MSpec.enable_feature :generator

  set :files, get(:language) + get(:core) + get(:rubysl)

end
