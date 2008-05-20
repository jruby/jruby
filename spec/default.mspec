class MSpecScript
  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :ci_files, [
    'spec/ruby/1.8/core',
    'spec/ruby/1.8/language',
    'spec/ruby/1.8/library'
  ]
  
  # The directory to search for tags for each spec file
  set :tags_dir, File.expand_path('spec/tags')

  # The default implementation to run the specs.
  set :target, 'bin/jruby'
end
