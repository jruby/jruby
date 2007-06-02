require 'test/unit'

class TestBackquote < Test::Unit::TestCase
  def test_backquote_special_commands
    if File.exists?("/bin/echo")
      output = `/bin/echo hello`
      assert_equal("hello\n", output)
    end
  end

  def test_system_special_commands
    if File.exists?("/bin/true")
      assert(system("/bin/true"))
      assert_equal(0, $?.exitstatus)
    end

    if File.exists?("/bin/false")
      assert(! system("/bin/false"))
      assert($?.exitstatus > 0)
    end
  end
end
