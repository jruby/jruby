class MSpecScript

  set :language, [
    "spec/ruby/language",
    "^spec/ruby/language/regexp/anchors_spec.rb"
  ]

  set :core, [
    "spec/ruby/core/fixnum",
    "^spec/ruby/core/fixnum/exponent_spec.rb",
    "^spec/ruby/core/fixnum/right_shift_spec.rb"
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
