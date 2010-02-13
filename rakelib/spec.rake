namespace :spec do
  desc "Run the rubyspecs expected to pass (version-frozen)"
  task :ci do
    ant "spec"
  end

  desc "Run all the specs including failures (version-frozen)"
  task :all do
    ant "spec-all"
  end

  gem 'rspec'
  require 'spec/rake/spectask'
  desc "Runs Java Integration Specs"
  Spec::Rake::SpecTask.new("ji" => "build/jruby-test-classes.jar") do |t|
    t.spec_opts ||= []
    t.spec_opts << "--options" << "spec/java_integration/spec.opts"
    t.spec_files = FileList['spec/java_integration/**/*_spec.rb']
  end

  desc "Runs Java Integration specs quietly"
  Spec::Rake::SpecTask.new("ji:quiet" => "build/jruby-test-classes.jar") do |t|
    t.spec_opts ||= []
    t.spec_opts << "--options" << "spec/java_integration/spec.quiet.opts"
    t.spec_files = FileList['spec/java_integration/**/*_spec.rb']
  end

  desc "Runs Compiler Specs"
  Spec::Rake::SpecTask.new("compiler" => "build/jruby-test-classes.jar") do |t|
    t.spec_files = FileList['spec/compiler/**/*_spec.rb']
  end

  desc "Runs FFI specs"
  Spec::Rake::SpecTask.new("ffi" => "build/jruby-test-classes.jar") do |t|
    t.spec_files = FileList['spec/ffi/**/*_spec.rb']
  end

  # Complimentary tasks for running specs

  desc "Retrieve latest tagged rubyspec git repository"
  task :fetch_latest_rubyspec_repo => :clear_spec_dirs do
    unless git_repo_exists? RUBYSPEC_DIR
      git_shallow_clone('rubyspec', RUBYSPEC_GIT_REPO, RUBYSPEC_DIR)
    else
      git_pull('rubyspec', RUBYSPEC_DIR)
    end
  end

  desc "Retrieve latest tagged mspec git repository"
  task :fetch_latest_mspec_repo do
    unless git_repo_exists? MSPEC_DIR
      git_shallow_clone('rubyspec', MSPEC_GIT_REPO, MSPEC_DIR)
    else
      git_pull('rubyspec', MSPEC_DIR)
    end
  end

  # FIXME: If we can detect whether at latest of stable we can do this
  # More selectively
  task :clear_spec_dirs do
    rm_rf RUBYSPEC_DIR
    rm_rf MSPEC_DIR
    rm_f RUBYSPEC_TAR_FILE
    rm_f MSPEC_TAR_FILE
    rm_f File.join(SPEC_DIR, "rubyspecs.current.revision")
  end
end
