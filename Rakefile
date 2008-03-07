task :default => [:build]

def ant(*args)
  system "ant #{args.join(' ')}"
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
  ant "clean"
end
