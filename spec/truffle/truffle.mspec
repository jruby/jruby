class MSpecScript

  set :language, [
    "../ruby/language"
  ]

  set :tags_patterns, [
                        [%r(^.*/language/),     'tags/language/'],
                        [/_spec.rb$/,       '_tags.txt']
                      ]

  MSpec.enable_feature :encoding
  MSpec.enable_feature :continuation
  MSpec.enable_feature :fiber
  MSpec.enable_feature :fork
  MSpec.enable_feature :generator

  set :files, get(:language)

end
