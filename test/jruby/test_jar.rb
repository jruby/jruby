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

end
