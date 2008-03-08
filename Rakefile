task :default => [:build]

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

desc "Run the basic set of tests"
task :test do
  ant "test"
end

namespace :test do
  desc "Run the complete set of tests (will take a while)"
  task :all do
    ant "test-all"
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
