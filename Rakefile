#
# Rakefile for JRuby.
#
# At this time, most JRuby build tasks still use build.xml and Apache
# Ant. This Rakefile has some additional tasks. We hope to migrate
# more out of build.xml and into Rake in the future.
#
# See also rakelib/*.rake for more tasks and details.

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
  mkdir_p 'src_gen'
  system 'apt -nocompile -cp lib/jruby.jar:build_lib/asm-3.0.jar:build_lib/asm-util-3.0.jar -factory org.jruby.anno.AnnotationBinder src/org/jruby/*.java'
  system 'javac -cp lib/jruby.jar src_gen/*.java'
  system 'jar -uf lib/jruby.jar -C src_gen .'
end

task :fetch_latest_rails_repo do
  unless git_repo_exists? RAILS_DIR
    git_shallow_clone('rails', RAILS_GIT_REPO, RAILS_DIR)
  else
    git_pull('rails', RAILS_DIR)
  end
end

task :fetch_latest_prawn_repo do
  unless git_repo_exists? PRAWN_DIR
    git_shallow_clone('prawn', PRAWN_GIT_REPO, PRAWN_DIR) do
      `git checkout #{PRAWN_STABLE_VERSION}`
      `git submodule init`
      `git submodule update`
    end
  else
    git_pull('prawn', PRAWN_DIR) do
      `git checkout #{PRAWN_STABLE_VERSION}`
      `git submodule update`
    end
  end
end
