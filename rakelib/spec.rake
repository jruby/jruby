namespace :spec do
  desc "Run the rubyspecs expected to pass (version-frozen)"
  task :ci do
    ant "spec"
  end

  desc "Run all the specs including failures (version-frozen)"
  task :all do
    ant "spec-all"
  end

  desc "Run all 1.8 tagged specs in interpreted, JIT, and pre-compiled modes"
  task :all_tagged_1_8 => [:interpreted_tagged_1_8, :compiled_tagged_1_8, 
                           :precompiled_tagged_1_8]

  desc "Tagged 1.8 specs in interpreted mode only"
  task :interpreted_tagged_1_8 do
    mspec :compile_mode => "OFF", :spec_config => RUBY18_MSPEC_FILE
  end

  desc "Tagged 1.8 specs in JIT mode only (threshold=0)"
  task :compiled_tagged_1_8 do
    mspec :compile_mode => "JIT", :spec_config => RUBY18_MSPEC_FILE, 
       :jit_threshold => 0
  end

  desc "Tagged 1.8 specs in AOT mode only"
  task :precompiled_tagged_1_8 do
    mspec :compile_mode => "FORCE", :spec_config => RUBY18_MSPEC_FILE, 
       :jit_threshold => 0
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

  task :fetch_specs => [:install_build_gems, :fetch_latest_rubyspec_repo, :fetch_latest_mspec_repo]

  desc "Retrieve latest tagged rubyspec git repository"
  task :fetch_latest_rubyspec_repo do
    unless git_repo_exists? RUBYSPEC_DIR
      clear_spec_dirs
      git_shallow_clone('rubyspec', RUBYSPEC_GIT_REPO, RUBYSPEC_DIR)
    else
      git_fetch('rubyspec', RUBYSPEC_DIR)
    end
  end

  desc "Retrieve latest tagged mspec git repository"
  task :fetch_latest_mspec_repo do
    unless git_repo_exists? MSPEC_DIR
      git_shallow_clone('rubyspec', MSPEC_GIT_REPO, MSPEC_DIR)
    else
      git_fetch('rubyspec', MSPEC_DIR)
    end
  end

  def clear_spec_dirs
    # rm_rf RUBYSPEC_DIR # Note: Already done by git_shallow_clone
    rm_rf MSPEC_DIR
    rm_f RUBYSPEC_TAR_FILE
    rm_f MSPEC_TAR_FILE
    rm_f File.join(SPEC_DIR, "rubyspecs.current.revision")
  end
end
