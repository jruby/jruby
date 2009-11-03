desc "Alias for test:short"
task :test => "test:short"

desc "Alias for spec:ci"
task :spec => "spec:ci"

namespace :test do
  desc "Compile test code"
  task :compile do
    ant "compile-test"
    system "jar cf build/jruby-test-classes.jar -C build/classes/test ."
  end

  desc "Run the basic set of tests"
  task :short do
    ant "test"
  end

  desc "Run the complete set of tests (will take a while)"
  task :all do
    ant "test-all"
  end

  desc "Run tracing tests (do not forget to pass --debug)"
  task :tracing do
    require 'rake/testtask'
    Rake::TestTask.new('test:tracing') do |t|
      t.pattern = 'test/tracing/test_*.rb'
      t.verbose = true
      t.ruby_opts << '--debug'
    end
  end
end

file "build/jruby-test-classes.jar" do
  Rake::Task['test:compile'].invoke
end

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
end
