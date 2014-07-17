require 'test/unit'
require 'jruby'

class TestJarFile < Test::Unit::TestCase
  def test_stat_file_in_jar
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
end
