require 'test/unit'

class TestJar < Test::Unit::TestCase

  def test_stat_file_in_jar
    require 'jruby'
    begin
      url = JRuby.runtime.jruby_class_loader.get_resource 'org/jruby/Ruby.class'
      conn = url.open_connection
      real_size = conn.content_length
      stat_size = File.stat(url.to_uri.to_s).size

      assert_equal(real_size, stat_size)
    rescue
      conn.close rescue nil
    end
  end

  def test_jar_on_load_path
    $LOAD_PATH << "test/jruby/test_jruby_1332.jar!"
    require 'test_jruby_1332.rb'
    assert($jruby_1332)
  end

  def test_file_jar_path
    jar_file = 'file:' + File.expand_path('gem.jar', File.dirname(__FILE__))
    in_jar_file = "#{jar_file}!/specifications"
    assert File.directory? in_jar_file
    Dir.chdir in_jar_file
    assert File.exists?(Dir.pwd), "#{Dir.pwd} does not exist!"
    some_path = File.expand_path('SOME')
    assert some_path.end_with?('test/jruby/gem.jar!/specifications/SOME')
    assert_false File.exists?(some_path), "#{some_path} does exist!"
    existing_path = File.expand_path('mygem-1.0.0.gemspec')
    assert_true File.exists?(existing_path), "#{existing_path} does not exist!"
    assert_true File.file?(existing_path)
  end

  @@work_dir = Dir.pwd

  def teardown
    Dir.chdir(@@work_dir)
  end

end
