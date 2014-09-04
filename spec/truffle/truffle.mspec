class MSpecScript

  set :language, [
    "spec/ruby/language",
    "^spec/ruby/language/regexp/anchors_spec.rb"
  ]

  set :core, [
    "spec/ruby/core/array",
    "spec/ruby/core/basicobject",
    "spec/ruby/core/bignum",
    "spec/ruby/core/binding",
    "spec/ruby/core/class",
    "spec/ruby/core/comparable",
    "spec/ruby/core/continuation",
    #"spec/ruby/core/dir",
    "spec/ruby/core/encoding",
    "spec/ruby/core/exception",
    "spec/ruby/core/false",
    "spec/ruby/core/fiber",
    #"spec/ruby/core/file",
    "spec/ruby/core/fixnum",
    "^spec/ruby/core/fixnum/exponent_spec.rb",
    "^spec/ruby/core/fixnum/right_shift_spec.rb",
    "spec/ruby/core/float",
    "spec/ruby/core/hash",
    #"spec/ruby/core/io",
    #"spec/ruby/core/kernel",
    #"spec/ruby/core/main",
    "spec/ruby/core/matchdata",
    #"spec/ruby/core/math",
    #"spec/ruby/core/module",
    "spec/ruby/core/nil",
    #"spec/ruby/core/object",
    "spec/ruby/core/objectspace",
    #"spec/ruby/core/process",
    "spec/ruby/core/proc",
    "spec/ruby/core/range",
    #"spec/ruby/core/regexp",
    #"spec/ruby/core/signal",
    #"spec/ruby/core/string",
    #"spec/ruby/core/struct",
    #"spec/ruby/core/symbol",
    #"spec/ruby/core/system",
    #"spec/ruby/core/thread",
    "spec/ruby/core/time",
    "spec/ruby/core/true"
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
