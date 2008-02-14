require 'test/unit'
require 'stringio'
require 'java'

class TestJrubyc < Test::Unit::TestCase
  def setup
    @jruby_command = File.join(ENV_JAVA['jruby.home'], "bin", "jruby")
    @jrubyc_command = File.join(ENV_JAVA['jruby.home'], "bin", "jrubyc")
  end
  def test_basic
    begin
      output = `#{@jruby_command} #{@jrubyc_command} #{__FILE__}`

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
      output = `#{@jruby_command} #{@jrubyc_command} -t /tmp #{__FILE__}`

      assert_equal(
        "Compiling #{__FILE__} to class ruby/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("ruby/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
  
  def test_bad_target
    output = `#{@jruby_command} #{@jrubyc_command} -t does_not_exist #{__FILE__}`

    assert_equal(
      "Target dir not found: does_not_exist\n",
      output)
    assert_equal(1, $?.exitstatus)
  end
  
  def test_prefix
    begin
      output = `#{@jruby_command} #{@jrubyc_command} -p foo #{__FILE__}`

      assert_equal(
        "Compiling #{__FILE__} to class foo/test/compiler/test_jrubyc\n",
        output)

      assert(File.exist?("foo/test/compiler/test_jrubyc.class"))
    ensure
      File.delete("ruby") rescue nil
    end
  end
  
  def test_require
    $compile_test = false
    File.open("test_file1.rb", "w") {|file| file.write("$compile_test = true")}
    
    begin
      output = `#{@jruby_command} #{@jrubyc_command} test_file1.rb`
      
      assert_equal(
        "Compiling test_file1.rb to class ruby/test_file1\n",
        output)
      
      File.delete("test_file1.rb")
      
      assert_nothing_raised { require 'ruby/test_file1' }
      assert($compile_test)
    ensure
      File.delete("ruby") rescue nil
    end
  end
end