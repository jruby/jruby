## Make sure that the chdir option to Kernel#spawn works.

require 'test/unit'
require 'rbconfig'
require 'open3'
require 'pathname'

class TestSpawnChdir < Test::Unit::TestCase
  PWD_EXPRESSION = 'print Dir.pwd'.freeze

  def test_basic
    capture_with_script(Pathname.new('simple-directory-name'))
  end

  def test_supports_directory_with_shell_metacharacters
    pend('chdir does not currently work with shell meta-characters') do
      capture_with_script(
        Pathname.new("';echo arbitrary code execution via chdir;:'"))
    end
  end

  def test_supports_command_with_spaces_in_argument
    pend('chdir does not currently work with spaces in arguments') do
      capture(Pathname.new('simple-directory-name'),
              [RbConfig.ruby, '-e', PWD_EXPRESSION])
    end
  end

  private

  def capture(dir, command_args)
    dir.mkpath
    out, status = Open3.capture2(*command_args, chdir: dir.to_s)
    assert_equal(dir.realpath.to_s, out)
    assert_equal(status.exitstatus, 0)
  ensure
    dir.rmtree
  end

  def capture_with_script(dir)
    # Write out a Ruby script that prints the current working directory. We do
    # this so that we can create a command line without spaces, because a
    # command line with spaces does not currently work with chdir. Note that
    # this will still fail if the user has shell metacharacters in the path to
    # the Ruby executable.
    script = Pathname.new('pwd.rb')
    script.write(PWD_EXPRESSION)
    begin
      capture(dir, [RbConfig.ruby, script.relative_path_from(dir).to_s])
    ensure
      script.delete
    end
  end
end
