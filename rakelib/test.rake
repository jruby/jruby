desc "Alias for test:short"
task :test => "test:short"

desc "Alias for spec:ci"
task :spec => "spec:ci"

desc "Run the suite of tests in 1.9 mode"
task :test19 => ['test:jruby19', 'test:mri19', 'test:rubicon19']

namespace :test do
  desc "Compile test code"
  task :compile do
    ant "compile-test"
  end

  desc "Run the basic set of tests"
  task :short do
    ant "test"
  end

  desc "Run the complete set of tests (will take a while)"
  task :all do
    ant "test-all"
  end

  desc "FIXME: Not sure about what this should be called (name came from ant)"
  task :rake_targets => ['install_gems', 'spec:ji:quiet', 'spec:compiler', 'spec:ffi', 'spec:regression'] do
    jrake(BASE_DIR, 'test:tracing') { arg :line => '--debug' }
  end

  desc "Run tracing tests (do not forget to pass --debug)"
  task :tracing do
    require 'rake/testtask'
    Rake::TestTask.new('test:tracing') do |t|
      t.pattern = 'test/tracing/test_*.rb'
      t.verbose = true
      t.ruby_opts << '--debug'
      t.ruby_opts << '--1.8'
    end
  end
  desc "Run tracing tests (do not forget to pass --debug)"

  task :mri19 => ['install_dev_gems'] do
    require 'rake/testtask'
    Rake::TestTask.new('test:mri19') do |t|
      files = []
      File.open('test/mri.1.9.index') do |f|
        f.lines.each do |line|
          filename = "test/#{line.chomp}.rb"
          next unless File.exist? filename
          files << filename
        end
      end
      t.test_files = files
      t.verbose = true
      ENV['EXCLUDE_DIR'] = 'test/externals/ruby1.9/excludes'
      t.ruby_opts << '--debug'
      t.ruby_opts << '--1.9'
      t.ruby_opts << '-I test/externals/ruby1.9'
      t.ruby_opts << '-r minitest/excludes'
      t.ruby_opts << '-X-C'
    end
  end

  task :jruby19 => ['test:compile', :install_dev_gems] do
    require 'rake/testtask'
    Rake::TestTask.new('test:jruby19') do |t|
      files = []
      File.open('test/jruby.1.9.index') do |f|
        f.lines.each do |line|
          filename = "test/#{line.chomp}.rb"
          next unless File.exist? filename
          files << filename
        end
      end
      t.test_files = files
      t.verbose = true
      t.ruby_opts << '-J-cp build/classes/test'
      t.ruby_opts << '--debug'
      t.ruby_opts << '--1.9'
      t.ruby_opts << '-X-C'
    end
  end

  task :jruby19 => ['test:compile', :install_dev_gems] do
    require 'rake/testtask'
    Rake::TestTask.new('test:jruby19') do |t|
      files = []
      File.open('test/jruby.1.9.index') do |f|
        f.lines.each do |line|
          filename = "test/#{line.chomp}.rb"
          next unless File.exist? filename
          files << filename
        end
      end
      t.test_files = files
      t.verbose = true
      t.ruby_opts << '-J-cp build/classes/test'
      t.ruby_opts << '--debug'
      t.ruby_opts << '--1.9'
      t.ruby_opts << '-X-C'
    end
  end

  task :rubicon19 => ['test:compile', :install_dev_gems] do
    require 'rake/testtask'
    Rake::TestTask.new('test:rubicon19') do |t|
      files = []
      File.open('test/rubicon.1.9.index') do |f|
        f.lines.each do |line|
          filename = "test/#{line.chomp}.rb"
          next unless File.exist? filename
          files << filename
        end
      end
      t.test_files = files
      t.verbose = true
      t.ruby_opts << '-J-cp build/classes/test'
      t.ruby_opts << '--debug'
      t.ruby_opts << '--1.9'
      t.ruby_opts << '-X-C'
      t.ruby_opts << '-X+O'
    end
  end

  task :rails => [:jar, :install_build_gems, :fetch_latest_rails_repo] do
    # Need to disable assertions because of a rogue assert in OpenSSL
    jrake("#{RAILS_DIR}/activesupport", "test") { jvmarg :line => "-da" }
    jrake("#{RAILS_DIR}/actionmailer", "test")
    jrake("#{RAILS_DIR}/activemodel", "test")
    jrake("#{RAILS_DIR}/railties", "test")
  end

  task :prawn => [:jar, :install_build_gems, :fetch_latest_prawn_repo] do
    jrake PRAWN_DIR, "test examples"
  end

  # Complementary tasks for testing

  desc "Retrieve latest stable rails git repository"
  task :fetch_latest_rails_repo do
    unless git_repo_exists? RAILS_DIR
      git_shallow_clone('rails', RAILS_GIT_REPO, RAILS_DIR)
    else
      git_pull('rails', RAILS_DIR)
    end
  end

  desc "Retrieve latest stable prawn git repository"
  task :fetch_latest_prawn_repo do
    unless git_repo_exists? PRAWN_DIR
      git_shallow_clone('prawn', PRAWN_GIT_REPO, PRAWN_DIR) do
        sh "git checkout #{PRAWN_STABLE_VERSION}"
        sh "git submodule init"
        sh "git submodule update"
      end
    else
      git_pull('prawn', PRAWN_DIR) do
        sh "git checkout #{PRAWN_STABLE_VERSION}"
        sh "git submodule update"
      end
    end
  end
end
