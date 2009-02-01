task :default => [:build]

Object.const_set(:BASE_DIR, Dir.pwd)

File.open("default.build.properties") do |props|
  props.each_line do |line|
    # skip comments
    next if line =~ /^\W*#/
    
    # build const name
    name, value = line.split("=")
    name.gsub!(".", "_").upcase!
    Object.const_set(name.to_sym, value)
    
    # substitute embedded props
    value.chop!.gsub!(/\$\{([^}]+)\}/) do |embed|
      Object.const_get($1.gsub!(".", "_").upcase!)
    end
  end
end

def ant(*args)
  system "ant -logger org.apache.tools.ant.NoBannerLogger #{args.join(' ')}"
end

desc "Build JRuby"
task :build do
  ant "jar"
end
task :jar => :build

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

desc "Clean all built output"
task :clean do
  delete_files = FileList.new do |fl|
    fl.
      include("#{BUILD_DIR}/**").
      exclude("#{BUILD_DIR}/rubyspec").
      include(DIST_DIR).
      include(API_DOCS_DIR)
  end
  
  delete_files.each {|files| rm_rf files, :verbose => true}
end

desc "Generate sources, compile and add to jar file"
task :gen do
  mkdir_p 'src_gen'
  system 'apt -nocompile -cp lib/jruby.jar:build_lib/asm-3.0.jar:build_lib/asm-util-3.0.jar -factory org.jruby.anno.AnnotationBinder src/org/jruby/*.java'
  system 'javac -cp lib/jruby.jar src_gen/*.java'
  system 'jar -uf lib/jruby.jar -C src_gen .'
end
