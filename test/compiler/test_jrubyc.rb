require 'test/unit'
require 'stringio'
require 'java'

class TestJrubyc < Test::Unit::TestCase
  def setup
    @jrubyc_command = File.join(ENV_JAVA['jruby.home'], "bin", "jrubyc")
  end
  def test_basic
    begin
      output = `#{@jrubyc_command} #{__FILE__}`

      assert_equal(
        "Compiling #{__FILE__} to class ruby/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("ruby/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
  
  def test_target
    begin
      output = `#{@jrubyc_command} -t /tmp #{__FILE__}`

      assert_equal(
        "Compiling #{__FILE__} to class ruby/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("ruby/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
  
  def test_bad_target
    output = `#{@jrubyc_command} -t does_not_exist #{__FILE__}`

    assert_equal(
      "Target dir not found: does_not_exist\n",
      output)
    assert_equal(1, $?.exitstatus)
  end
  
  def test_prefix
    begin
      output = `#{@jrubyc_command} -p foo #{__FILE__}`

      assert_equal(
        "Compiling #{__FILE__} to class foo/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("foo/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
end