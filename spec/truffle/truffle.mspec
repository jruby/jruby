class MSpecScript

  set :language, [
    "spec/ruby/language"
  ]

  set :tags_patterns, [
                        [%r(^.*/language/),     'spec/truffle/tags/language/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]

  MSpec.enable_feature :encoding
  MSpec.enable_feature :continuation
  MSpec.enable_feature :fiber
  MSpec.enable_feature :fork
  MSpec.enable_feature :generator

  set :files, get(:language)

end
