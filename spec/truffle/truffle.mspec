class MSpecScript

  set :language, [
    "spec/ruby/language",
    "^spec/ruby/language/method_spec.rb",
    "^spec/ruby/language/numbers_spec.rb",
    "^spec/ruby/language/lambda_spec.rb"
  ]

  set :core, [
    "spec/ruby/core/array",
    "spec/ruby/core/basicobject",
    "spec/ruby/core/bignum",
    "spec/ruby/core/binding",
    "spec/ruby/core/class",
    "spec/ruby/core/comparable",
    "spec/ruby/core/continuation",
    "spec/ruby/core/dir",
    "spec/ruby/core/encoding",
    "spec/ruby/core/exception",
    "spec/ruby/core/false",
    "spec/ruby/core/fiber",
    "spec/ruby/core/file",
    "spec/ruby/core/fixnum",
    "spec/ruby/core/float",
    "spec/ruby/core/gc",
    "spec/ruby/core/hash",
    "spec/ruby/core/io",
    "spec/ruby/core/kernel",
    "spec/ruby/core/main",
    "spec/ruby/core/matchdata",
    "spec/ruby/core/math",
    "spec/ruby/core/module",
    "spec/ruby/core/nil",
    "spec/ruby/core/object",
    "spec/ruby/core/objectspace",
    "spec/ruby/core/process",
    "spec/ruby/core/proc",
    "spec/ruby/core/range",
    "spec/ruby/core/regexp",
    "spec/ruby/core/signal",
    "spec/ruby/core/string",
    "spec/ruby/core/symbol",
    "spec/ruby/core/thread",
    "spec/ruby/core/time",
    "spec/ruby/core/true",

    # Can't load these - so tags aren't enough to exclude them

    "^spec/ruby/core/class/dup_spec.rb",
    "^spec/ruby/core/class/inherited_spec.rb",
    "^spec/ruby/core/class/new_spec.rb",
    "^spec/ruby/core/class/superclass_spec.rb",
    "^spec/ruby/core/dir/chroot_spec.rb",
    "^spec/ruby/core/dir/element_reference_spec.rb",
    "^spec/ruby/core/dir/glob_spec.rb",
    "^spec/ruby/core/file/chown_spec.rb",
    "^spec/ruby/core/file/ftype_spec.rb",
    "^spec/ruby/core/file/lchown_spec.rb",
    "^spec/ruby/core/file/socket_spec.rb",
    "^spec/ruby/core/file/stat/ftype_spec.rb",
    "^spec/ruby/core/file/stat/sticky_spec.rb",
    "^spec/ruby/core/file/sticky_spec.rb",
    "^spec/ruby/core/fixnum/exponent_spec.rb",
    "^spec/ruby/core/fixnum/right_shift_spec.rb",
    "^spec/ruby/core/io/isatty_spec.rb",
    "^spec/ruby/core/io/tty_spec.rb",
    "^spec/ruby/core/io/print_spec.rb",
    "^spec/ruby/core/io/reopen_spec.rb",
    "^spec/ruby/core/kernel/__method___spec.rb",
    "^spec/ruby/core/kernel/autoload_spec.rb",
    "^spec/ruby/core/kernel/gsub_spec.rb",
    "^spec/ruby/core/kernel/methods_spec.rb",
    "^spec/ruby/core/kernel/private_methods_spec.rb",
    "^spec/ruby/core/kernel/protected_methods_spec.rb",
    "^spec/ruby/core/kernel/public_methods_spec.rb",
    "^spec/ruby/core/kernel/singleton_methods_spec.rb",
    "^spec/ruby/core/kernel/sub_spec.rb",
    "^spec/ruby/core/kernel/define_singleton_method_spec.rb",
    "^spec/ruby/core/main/def_spec.rb",
    "^spec/ruby/core/main/define_method_spec.rb",
    "^spec/ruby/core/main/include_spec.rb",
    "^spec/ruby/core/main/private_spec.rb",
    "^spec/ruby/core/main/public_spec.rb",
    "^spec/ruby/core/object/is_a_spec.rb",
    "^spec/ruby/core/object/kind_of_spec.rb",
    "^spec/ruby/core/object/instance_of_spec.rb",
    "^spec/ruby/core/process/detach_spec.rb",
    "^spec/ruby/core/process/euid_spec.rb",
    "^spec/ruby/core/process/kill_spec.rb",
    "^spec/ruby/core/process/setpriority_spec.rb",
    "^spec/ruby/core/process/uid_spec.rb",
    "^spec/ruby/core/regexp/compile_spec.rb",
    "^spec/ruby/core/regexp/source_spec.rb",
    "^spec/ruby/core/regexp/fixed_encoding_spec.rb",
    "^spec/ruby/core/regexp/encoding_spec.rb",
    "^spec/ruby/core/signal/list_spec.rb",
    "^spec/ruby/core/string/chomp_spec.rb",
    "^spec/ruby/core/string/crypt_spec.rb",
    "^spec/ruby/core/string/element_set_spec.rb",
    "^spec/ruby/core/string/gsub_spec.rb",
    "^spec/ruby/core/string/index_spec.rb",
    "^spec/ruby/core/string/match_spec.rb",
    "^spec/ruby/core/string/modulo_spec.rb",
    "^spec/ruby/core/string/rindex_spec.rb",
    "^spec/ruby/core/symbol/encoding_spec.rb",
    "^spec/ruby/core/thread/raise_spec.rb",
  ]

  set :tags_patterns, [
                        [%r(^.*/language/),     'spec/truffle/tags/language/'],
                        [%r(^.*/core/),         'spec/truffle/tags/core/'],
                        [/_spec.rb$/,           '_tags.txt']
                      ]

  MSpec.enable_feature :encoding
  MSpec.enable_feature :continuation
  MSpec.enable_feature :fiber
  MSpec.enable_feature :fork
  MSpec.enable_feature :generator

  set :files, get(:language) + get(:core)

end
