# FIXME: Move into helper file somewhere for checksum methods
require 'digest/md5'
require 'digest/sha1'

class HashTask < Struct.new(:hash, :file)
  BUF = 100 * 1024 

  def calculate_hash
    open(file) do |io|
      while !io.eof
        hash.update io.readpartial(BUF)
      end
    end
    hash.hexdigest
  end

  def self.hash_for(filename, method=Digest::MD5)
    File.open(filename + "."+ method.name.split('::').last.downcase, 'w') do |f|
      f.puts HashTask.new(method.new, filename).calculate_hash
    end
  end
end

# Calculate a md5 checksum and save the file as same name + ".md5"
def md5_checksum(filename)
  HashTask.hash_for(filename)
end

# Calculate a sha1 checksum and save the file as same name + ".sha1"
def sha1_checksum(filename)
  HashTask.hash_for(filename, Digest::SHA1)
end


task :default => [:build]

Object.const_set(:BASE_DIR, Dir.pwd)

# FIXME: Move into helper method and call it
File.open("default.build.properties") do |props|
  props.each_line do |line|
    # skip comments
    next if line =~ /(^\W*#|^$)/
    
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

namespace :maven do
  class Pom
    def initialize(filename)
      @filename = filename
      @lines = IO.readlines(filename)
    end

    def update_version(version)
      group = nil
      artifact = nil
      @lines.each do |line|
        match = line.match(%r{<groupId>([^<]+)</groupId>})
        if match
          group = match[1]
          next
        end
        match = line.match(%r{<artifactId>([^<]+)</artifactId>})
        if match
          artifact = match[1]
          next
        end
        if line =~ %r{<version>[0-9][^<]+</version>} && group =~ /^org.jruby/ && artifact =~ /^(jruby|shared)/
          line.sub!(/<version>([^<]+)<\/version>/, "<version>#{version}</version>")
        end
      end
    end

    def save
      File.open(@filename, 'w') {|f| @lines.each {|l| f << l } }
    end
  end

  desc "Update versions in maven poms with string passed in ENV['VERSION']"
  task :updatepoms do
    version = ENV['VERSION'] or abort("Pass the new version with VERSION={version}")
    dir =  Dir.pwd
    Dir["#{dir}/**/pom.xml"].each do |f|
      puts "updating #{f}"
      pom = Pom.new(f)
      pom.update_version(version)
      pom.save
    end
  end
end

task :installer do
  ant "dist"
  sh "#{INSTALL4J_EXECUTABLE} -m win32 -D jruby.version=#{VERSION_JRUBY} " \
     "install/jruby.install4j" do |ok, result|
    $stderr.puts "** Something went wrong: #{result}" unless ok
  end
  Dir["#{BUILD_DIR}/installers/*.exe"].each do |file|
    md5_checksum file
    sha1_checksum file
  end 
end
