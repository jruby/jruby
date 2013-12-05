#
# Rakefile for JRuby.
#
# At this time, most JRuby build tasks still use build.xml and Apache
# Ant. This Rakefile has some additional tasks. We hope to migrate
# more out of build.xml and into Rake in the future.
#
# See also rakelib/*.rake for more tasks and details.

require File.dirname(__FILE__) + '/rakelib/helpers.rb'

# Suppress .java lines from non-traced backtrace
begin
  Rake.application.options.suppress_backtrace_pattern =
    /(#{Rake::Backtrace::SUPPRESS_PATTERN})|(^org\/jruby)/
rescue
end

task :default => [:build]

desc "Build JRuby"
task :build do
  ant "jar"
end

task :jar => :build

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
  mkdir_p 'build/src_gen'
  system 'apt -nocompile -cp lib/jruby.jar:build_lib/asm-4.0.jar:build_lib/asm-util-4.0.jar -factory org.jruby.anno.AnnotationBinder src/org/jruby/*.java'
  system 'javac -cp lib/jruby.jar build/src_gen/*.java'
  system 'jar -uf lib/jruby.jar -C build/src_gen .'
end
