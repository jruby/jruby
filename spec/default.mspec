class MSpecScript
  # An ordered list of the directories containing specs to run
  # as the CI process.
  set :ci_files, [
    File.dirname(__FILE__) + '/ruby/1.8/core',
    File.dirname(__FILE__) + '/ruby/1.8/language',
    File.dirname(__FILE__) + '/ruby/1.8/library'
  ]

  # The default implementation to run the specs.
  set :target, File.dirname(__FILE__) + '/../bin/jruby'
end
