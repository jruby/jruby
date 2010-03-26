require 'test/unit'

class TestDirWithPlusses < Test::Unit::TestCase
  def test_loaded_FILE_in_dir_with_plusses
    begin
      load 'test/dir_with_plusses_+++/required.rb'
      assert_equal(
        File.join(File.dirname(File.expand_path(__FILE__)), 'dir_with_plusses_+++', 'required.rb'),
        $dir_with_plusses_FILE
      )
    ensure
      # reset global
      $dir_with_plusses_FILE = nil
    end
  end

  def test_required_FILE_in_dir_with_plusses
    begin
      require 'test/dir_with_plusses_+++/required.rb'
      assert_equal(
        File.join(File.dirname(File.expand_path(__FILE__)), 'dir_with_plusses_+++', 'required.rb'),
        $dir_with_plusses_FILE
      )
    ensure
      # remove entry from loaded features and reset global
      $".pop
      $dir_with_plusses_FILE = nil
    end
  end
end
