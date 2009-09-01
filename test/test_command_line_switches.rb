require 'test/unit'
require 'test/test_helper'

require 'fileutils'

class TestCommandLineSwitches < Test::Unit::TestCase
  include TestHelper

  # FIXME: currently fails on Windows
  if (!WINDOWS)
    def test_dash_0_splits_records
      output = jruby_with_pipe("echo '1,2,3'", %Q{ -054 -n -e 'puts $_ + " "'})
      assert_equal "1, ,2, ,3\n ,", output
    end
  end

  def test_dash_little_c_checks_syntax
    with_jruby_shell_spawning do
      with_temp_script("bad : code") do |s|
        assert_match /SyntaxError/, jruby("-c #{s.path} 2>&1")
      end
    end
  end

  def test_dash_little_c_checks_syntax_only
    with_jruby_shell_spawning do
      with_temp_script(%q{ puts "a" }) do |s|
        assert_match /Syntax OK/, jruby(" -c #{s.path} 2>&1").chomp
      end
    end
  end
  
  # TODO -l: no idea what line ending processing is
  def test_dash_little_n_wraps_script_with_while_gets
    # FIXME: currently fails on windows and IBM JDK
    unless WINDOWS || IBM_JVM
      with_temp_script(%q{ puts "#{$_}#{$_}" }) do |s|
        output = IO.popen("echo \"a\nb\" | #{RUBY} -n #{s.path}", "r") { |p| p.read }
        assert_equal "a\na\nb\nb\n", output
      end
    end
  end

  def test_dash_little_p_wraps_script_with_while_gets_and_prints
    # FIXME: currently fails on Windows and IBM JDK
    unless WINDOWS || IBM_JVM
      with_temp_script(%q{ puts "#{$_}#{$_}" }) do |s|
        output = IO.popen("echo \"a\nb\" | #{RUBY} -p #{s.path}", "r") { |p| p.read }
        assert_equal "a\na\na\nb\nb\nb\n", output
      end
    end
  end

  # two args passed in which we can see as globals.  We also can see that
  # ARGV has removed those args from its list.  Also an improperly formatted
  # -s option (-g-a=123) is passed and is ignored.
  def test_dash_little_s
    with_temp_script(%q{puts $g, $v, $foo, *ARGV}) do |s|
      assert_equal "nil\n123\nbar\n4\n5\n6", `#{RUBY} -s #{s.path} -g-a=123 -v=123 -foo=bar 4 5 6`.chomp
    end
  end

  def test_dash_little_s_options_must_come_after_script
    with_temp_script(%q{puts $v, *ARGV}) do |s|
      assert_equal "nil\na\n-v=123\nb\nc", `#{RUBY} -s #{s.path} a -v=123 b c`.chomp
    end
  end

  # JRUBY-2693
  def test_dash_little_r_provides_program_name_to_loaded_library
    with_temp_script(%q{puts $0; puts $PROGRAM_NAME}) do |s|
      begin
        # tempfile does not put the .rb extension at the end, so -r does not find it
        path = s.path + ".rb"
        FileUtils.cp(s.path, path)
        assert_equal("#{path}\n#{path}\n#{path}\n#{path}\n",
                     jruby("-r#{path} #{path}"))
      ensure
        File.unlink(path) rescue nil
      end
    end
  end

  # This test is difficult to indicate meaning with. I am calling 
  # jgem, as it should not exist outside the jruby.bin directory.
  def test_dash_big_S_executes_script_in_jruby_bin_dir
    assert_match /^\d+\.\d+\.\d+/, `#{RUBY} -S jgem --version`
  end

  def test_dash_little_v_version_verbose_T_taint_d_debug_K_kcode_r_require_b_benchmarks_a_splitsinput_I_loadpath_C_cwd_F_delimeter_J_javaprop
    e_line = 'puts $VERBOSE, $SAFE, $DEBUG, $KCODE, $F.join(59.chr), $LOAD_PATH.join(44.chr), Dir.pwd, Java::java::lang::System.getProperty(:foo.to_s)'
    args = " -J-Dfoo=bar -v -T3 -d -Ku -b -a -n -Ihello -C .. -F, -e #{q + e_line + q}"
    lines = jruby_with_pipe("echo 1,2,3", args).split("\n")
    parent_dir = Dir.chdir('..') { Dir.pwd }

    assert_match /ruby \d+\.\d+\.\d+/, lines[0]
    assert_match /true$/, lines[1]
    assert_equal "3", lines[2]
    assert_equal "true", lines[3]
    assert_equal "UTF8", lines[4]
    assert_equal "1;2;3", lines[5].rstrip
    assert_match /^hello/, lines[6]
    # The gsub is for windows
    assert_equal "#{parent_dir}", lines[7].gsub('\\', '/')
    assert_equal "bar", lines[8]
    assert_match /Runtime: \d+ ms/, lines[9]

    e_line = 'puts Gem'
    args = " -rrubygems -e #{q + e_line + q}"
    lines = jruby_with_pipe("echo 1,2,3", args).split("\n")

    assert_equal "Gem", lines[0]
  end
  
  def test_dash_little_w_turns_warnings_on
    with_jruby_shell_spawning do
      assert_match /warning/, `#{RUBY} -v -e "defined? true" 2>&1`
    end
  end

  def test_dash_big_w_sets_warning_level
    with_jruby_shell_spawning do
      with_temp_script("defined? true") do |s|
        assert_equal "", jruby("-W1 #{s.path} 2>&1")
        assert_match /warning/, jruby("-W2 #{s.path} 2>&1")
      end
    end    
  end

  def test_dash_big_x_sets_extended_options
    # turn on ObjectSpace
    with_temp_script("ObjectSpace.each_object(Fixnum) {|o| puts o.inspect}") do |s|
      assert_no_match /ObjectSpace is disabled/, jruby("-X+O #{s.path} 2>&1")
    end
  end

  def test_dash_dash_copyright_displays_copyright
     assert_match /Copyright \(C\) 2001-2.../, `#{RUBY} --copyright`
  end

  # TODO --debug: cannot figure out how to test

  # TODO --jdb: cannot figure out how to test

  def test_dash_dash_properties_shows_list_of_properties
    assert_match /^These properties can be used/, `#{RUBY} --properties`
  end

  def test_dash_dash_version_shows_version
    version_string = `#{RUBY} --version`
    assert_match /ruby \d+\.\d+\.\d+/, version_string
    assert_match /jruby \d+\.\d+\.\d+/, version_string
  end

  # JRUBY-2648 [Note: jre6 on windows does not ship server VM - use jdk]
  def test_server_vm_option
    # server VM when explicitly set --server
    result = jruby(%Q{--server -rjava \
      -e "print java.lang.management.ManagementFactory.getCompilationMXBean.name"})
    assert_match /(tiered|server|j9jit24|j9jit23|bea jrockit\(r\) optimizing compiler)/, result.downcase
  end

  # JRUBY-2648 [Note: Originally these tests had tests for default vm and
  # also for -J options in addition to jruby options (-J-client versus 
  # --client).  In other tests we test that -J works and passes thru and
  # we should not assume to know what versions of Java will have as their
  # default VM.
  def test_client_vm_option
    arch = java.lang.System.getProperty('sun.arch.data.model')
    if (arch == nil || arch == '64')
      # Either non-Sun JVM, or x64 JVM (which doesn't have client VM)
      return
    end

    # client VM when explicitly set via --client
    result = jruby(%Q{--client -rjava \
      -e "print java.lang.management.ManagementFactory.getCompilationMXBean.name"})
    assert_match /client|j9jit24|j9jit23|bea jrockit\(r\) optimizing compiler/, result.downcase
  end
  
  # JRUBY-2821
  def test_with_interesting_file_names
    names = ["test-q", "test-d", "test--", "test-_", "test_U", "test_S_", "___D_",
             "test__", "test_U_D", "_P_U_S_D"]
    rgxes = [/test-q/, /test-d/, /test--/, /test-_/, /test_U/, /test_S_/, /___D_/,
             /test__/, /test_U_D/, /_P_U_S_D/]

    names.each_with_index do |name, idx|
      with_jruby_shell_spawning do
        with_temp_script('print __FILE__', name) do |s|
          assert_match rgxes[idx], jruby("#{s.path}")
        end
      end
    end
  end

  # JRUBY-3467
  def test_blank_arg_ends_arg_processing
    config = org.jruby.RubyInstanceConfig.new
    config.process_arguments(["-v", "", "-d"].to_java :string)
    # -v argument should be processed
    assert config.verbose?
    # -d argument should not be processed as an interpreter arg
    assert !config.debug?
  end
end
