require 'fileutils'

RSpec::Matchers.define :have_jar_entries do |expected|
  def jar_entries(full_entries)
    full_entries.map { |e| e.gsub /^[^!]+!/, '' }
  end

  match { |actual| jar_entries(actual).sort == expected.sort }
  failure_message_for_should do |actual|
    "\nexpected: #{expected.sort.inspect}\n    got: #{jar_entries(actual).sort.inspect}\n"
  end
end

require 'java' # this will make failure more clear if accidentally running through MRI

describe 'Dir globs (Dir.glob and Dir.[])' do
  before :all do
    FileUtils.rm "glob_test/glob-test.jar", :force => true
    begin
      FileUtils.rm_rf "glob_test"
    rescue Errno::ENOENT; end
    
    FileUtils.mkdir_p 'glob_target'
    File.open('glob_target/bar.txt', 'w') {|file| file << 'Some text.'}
    `jar -cf glob-test.jar glob_target/bar.txt`
    FileUtils.mkdir_p 'glob_test'
    FileUtils.cp "glob-test.jar", 'glob_test/'
  end
  
  after :all do
    FileUtils.rm    'glob_target/bar.txt',     :force => true
    FileUtils.rmdir 'glob_target'
    FileUtils.rm    "glob-test.jar",           :force => true
    begin
      FileUtils.rm    "glob_test/glob-test.jar"
      FileUtils.rmdir 'glob_test'
    rescue Errno::EACCES => e
      puts "Couldn't delete glob_test/glob-test.jar - Windows bug with JarFile holding write-lock after closed"
    end
  end
  
  it "finds the contents inside a jar with Dir.[] in a dir inside the jar" do
    FileUtils.cd('glob_test') do
      Dir["file:#{File.expand_path(Dir.pwd)}/glob-test.jar!/glob_target/**/*"].should have_jar_entries([
        '/glob_target/bar.txt'
      ])
    end
  end
  
  it "finds the contents inside a jar with Dir.glob in a dir inside the jar" do
    FileUtils.cd('glob_test') do
      Dir.glob("file:#{File.expand_path(Dir.pwd)}/glob-test.jar!/glob_target/**/*").should have_jar_entries([
        '/glob_target/bar.txt'
      ])
    end
  end

  it "finds the contents inside a jar with Dir.[] at the root of the jar" do
    FileUtils.cd('glob_test') do
      Dir["file:#{File.expand_path(Dir.pwd)}/glob-test.jar!/**/*"].should have_jar_entries([
        '/META-INF',
        '/META-INF/MANIFEST.MF',
        '/glob_target',
        '/glob_target/bar.txt'
      ])
    end
  end
    
  it "finds the contents inside a jar with Dir.glob at the root of the jar" do
    FileUtils.cd('glob_test') do
      Dir.glob("file:#{File.expand_path(Dir.pwd)}/glob-test.jar!/**/*").should have_jar_entries([
        '/META-INF',
        '/META-INF/MANIFEST.MF',
        '/glob_target',
        '/glob_target/bar.txt'
      ])
    end
  end

  it "supports expansions within the jar entry path" do
    FileUtils.cd('glob_test') do
      jar_path = "file:#{File.expand_path(Dir.pwd)}/glob-test.jar"

      Dir.glob("#{jar_path}!/foo/./bar/../../**/*").should have_jar_entries([
        '/foo/./bar/../../META-INF',
        '/foo/./bar/../../META-INF/MANIFEST.MF',
        '/foo/./bar/../../glob_target',
        '/foo/./bar/../../glob_target/bar.txt'
      ])

      Dir.glob("#{jar_path}!foo/./bar/../../glob_target/**").should have_jar_entries([
        "foo/./bar/../../glob_target/bar.txt"
      ])
    end
  end

=begin For some reason, mtime does not update on Jenkins on Cloudbees
  it "respects jar content filesystem changes" do
    jar_path = File.join(Dir.pwd, 'glob_test', 'modified-glob-test.jar')
    FileUtils.cp 'glob-test.jar', jar_path

    before = Dir.glob("#{jar_path}!/**/*").size

    puts File.mtime(jar_path)

    # Need to sleep a little bit to make sure that modified time is updated
    sleep 2

    # This should delete the /glob_target and /glob_target/bar.txt entries
    `zip -d #{jar_path} glob_target/bar.txt`

    puts File.mtime(jar_path)

    # Explicitly touch the file in case mtime and zip don't agree
    `touch #{jar_path}`

    after = Dir.glob("#{jar_path}!/**/*").size

    expect(after - before).to eq(-2)
  end
=end
end

describe 'Dir globs (Dir.glob and Dir.[]) +' do
  it "doesn't break when given incorrect URIs" do
    require 'rbconfig'
    prefix = RbConfig::CONFIG['host_os'] =~ /^mswin/i ? 'file:/' : 'file:'
    # file:/ in front when not looking a jar produces the error
#    java.lang.Runtime.getRuntime.add_shutdown_hook Thread.new do
#
#    end

    lambda{ Dir.glob(prefix + File.expand_path(Dir.pwd)) }.should_not raise_error
  end
end

describe "File.expand_path in a jar" do
  context "with spaces in the name" do
    before do
      Dir.mkdir 'spaces test' unless File.exist? 'spaces test'
      File.open('spaces_file.rb', 'w') do |file|
        file << <<-CODE
      $foo_dir = File.expand_path(File.dirname(__FILE__))
CODE
      end
      `jar -cf test.jar spaces_file.rb`

      File.delete('spaces_file.rb')
      FileUtils.move 'test.jar', 'spaces test'
    end

    after do
      begin
        File.delete('spaces test/test.jar')
        Dir.rmdir 'spaces test'
      rescue Errno::EACCES => e
        puts "Couldn't delete 'spaces test/test.jar' - Windows bug with JarFile holding write-lock after closed"
      end
      $foo_dir = nil
    end

    it "does not encode URIs for jars on a filesystem" do
      require 'spaces test/test.jar'
      require 'spaces_file'
      $foo_dir.should_not match(/%20/)
    end
  end

  it "expands the path relative to the jar" do
    jar_path = File.expand_path("file:/Users/foo/dev/ruby/jruby/lib/jruby-complete.jar")
    current = "#{jar_path}!/META-INF/jruby.home/lib/ruby/1.8"
    expected = "#{jar_path}!/META-INF/jruby.home/lib/ruby"
    File.expand_path(File.join(current, "..")).should == expected
    File.expand_path("..", current).should == expected
  end
end

describe "Dir.glob and Dir[] with multiple magic modifiers" do
  before :all do
    FileUtils.mkpath("jruby-4396/top/builtin/A")
    FileUtils.mkpath("jruby-4396/top/builtin/B")
    FileUtils.mkpath("jruby-4396/top/builtin/C")
    FileUtils.mkpath("jruby-4396/top/dir2/dir2a")
    `touch            jruby-4396/top/dir2/dir2a/1`
    `touch            jruby-4396/top/dir2/dir2a/2`
    `touch            jruby-4396/top/dir2/dir2a/3`
    FileUtils.mkpath("jruby-4396/top/dir2/dir2b")
    `touch            jruby-4396/top/dir2/dir2b/4`
    `touch            jruby-4396/top/dir2/dir2b/5`
    FileUtils.mkpath("jruby-4396/top/dir2/dir2c")
    `touch            jruby-4396/top/dir2/dir2c/6`
    FileUtils.cd('jruby-4396') { `jar -cvf top.jar top` }
  end

  after :all do
    FileUtils.rm_rf("jruby-4396")
  end

  it "returns directories when the magic modifier is a star" do
    FileUtils.cd('jruby-4396') do
      Dir["file:#{File.expand_path(Dir.pwd)}/top.jar!top/builtin/*/"].size.should == 3
    end
  end

  # GH-1466
  #it "iterates over directories when there are more than one magic modifier" do
  #  FileUtils.cd('jruby-4396') do      
  #    Dir.glob("file:#{File.expand_path(Dir.pwd)}/top.jar!top/dir2/**/*/**").size.should == 6
  #  end
  #end
end

