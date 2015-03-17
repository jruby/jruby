describe :process_spawn, :shared => true do
  before :each do
    @name = tmp("kernel_spawn.txt")
  end

  after :each do
    rm_r @name
  end

  it "executes the given command" do
    lambda { Process.wait @object.spawn("echo spawn") }.should output_to_fd("spawn\n")
  end

  it "returns the process ID of the new process as a Fixnum" do
    pid = @object.spawn(ruby_cmd("exit"))
    Process.wait pid
    pid.should be_an_instance_of(Fixnum)
  end

  it "returns immediately" do
    start = Time.now
    pid = @object.spawn(ruby_cmd("sleep 10"))
    (Time.now - start).should < 5
    Process.kill :KILL, pid
    Process.wait pid
  end

  # argv processing

  describe "with a single argument" do
    it "subjects the specified command to shell expansion" do
      lambda { Process.wait @object.spawn("echo *") }.should_not output_to_fd("*\n")
    end

    it "creates an argument array with shell parsing semantics for whitespace" do
      lambda { Process.wait @object.spawn("echo a b  c   d") }.should output_to_fd("a b c d\n")
    end

    it "calls #to_str to convert the argument to a String" do
      o = mock("to_str")
      o.should_receive(:to_str).and_return("echo foo")
      lambda { Process.wait @object.spawn(o) }.should output_to_fd("foo\n")
    end

    it "raises an ArgumentError if the command includes a null byte" do
      lambda { @object.spawn "\000" }.should raise_error(ArgumentError)
    end

    it "raises a TypeError if the argument does not respond to #to_str" do
      lambda { @object.spawn :echo }.should raise_error(TypeError)
    end
  end

  describe "with multiple arguments" do
    it "does not subject the arguments to shell expansion" do
      lambda { Process.wait @object.spawn("echo", "*") }.should output_to_fd("*\n")
    end

    it "preserves whitespace in passed arguments" do
      lambda { Process.wait @object.spawn("echo", "a b  c   d") }.should output_to_fd("a b  c   d\n")
    end

    it "calls #to_str to convert the arguments to Strings" do
      o = mock("to_str")
      o.should_receive(:to_str).and_return("foo")
      lambda { Process.wait @object.spawn("echo", o) }.should output_to_fd("foo\n")
    end

    it "raises an ArgumentError if an argument includes a null byte" do
      lambda { @object.spawn "echo", "\000" }.should raise_error(ArgumentError)
    end

    it "raises a TypeError if an argument does not respond to #to_str" do
      lambda { @object.spawn "echo", :foo }.should raise_error(TypeError)
    end
  end

  describe "with a command array" do
    it "uses the first element as the command name and the second as the argv[0] value" do
      lambda { Process.wait @object.spawn(["/bin/sh", "argv_zero"], "-c", "echo $0") }.should output_to_fd("argv_zero\n")
    end

    it "does not subject the arguments to shell expansion" do
      lambda { Process.wait @object.spawn(["echo", "echo"], "*") }.should output_to_fd("*\n")
    end

    it "preserves whitespace in passed arguments" do
      lambda { Process.wait @object.spawn(["echo", "echo"], "a b  c   d") }.should output_to_fd("a b  c   d\n")
    end

    it "calls #to_ary to convert the argument to an Array" do
      o = mock("to_ary")
      o.should_receive(:to_ary).and_return(["/bin/sh", "argv_zero"])
      lambda { Process.wait @object.spawn(o, "-c", "echo $0") }.should output_to_fd("argv_zero\n")
    end

    it "calls #to_str to convert the first element to a String" do
      o = mock("to_str")
      o.should_receive(:to_str).and_return("echo")
      lambda { Process.wait @object.spawn([o, "echo"], "foo") }.should output_to_fd("foo\n")
    end

    it "calls #to_str to convert the second element to a String" do
      o = mock("to_str")
      o.should_receive(:to_str).and_return("echo")
      lambda { Process.wait @object.spawn(["echo", o], "foo") }.should output_to_fd("foo\n")
    end

    it "raises an ArgumentError if the Array does not have exactly two elements" do
      lambda { @object.spawn([]) }.should raise_error(ArgumentError)
      lambda { @object.spawn([:a]) }.should raise_error(ArgumentError)
      lambda { @object.spawn([:a, :b, :c]) }.should raise_error(ArgumentError)
    end

    it "raises an ArgumentError if the Strings in the Array include a null byte" do
      lambda { @object.spawn ["\000", "echo"] }.should raise_error(ArgumentError)
      lambda { @object.spawn ["echo", "\000"] }.should raise_error(ArgumentError)
    end

    it "raises a TypeError if an element in the Array does not respond to #to_str" do
      lambda { @object.spawn ["echo", :echo] }.should raise_error(TypeError)
      lambda { @object.spawn [:echo, "echo"] }.should raise_error(TypeError)
    end
  end

  # env handling

  after :each do
    ENV.delete("FOO")
  end

  it "sets environment variables in the child environment" do
    lambda do
      Process.wait @object.spawn({"FOO" => "BAR"}, ruby_cmd('print ENV["FOO"]'))
    end.should output_to_fd("BAR")
  end

  it "unsets environment variables whose value is nil" do
    ENV["FOO"] = "BAR"
    lambda do
      Process.wait @object.spawn({"FOO" => nil}, ruby_cmd('print ENV["FOO"]'))
    end.should output_to_fd("")
  end

  it "calls #to_hash to convert the environment" do
    o = mock("to_hash")
    o.should_receive(:to_hash).and_return({"FOO" => "BAR"})
    lambda do
      Process.wait @object.spawn(o, ruby_cmd('print ENV["FOO"]'))
    end.should output_to_fd("BAR")
  end

  it "calls #to_str to convert the environment keys" do
    o = mock("to_str")
    o.should_receive(:to_str).and_return("FOO")
    lambda do
      Process.wait @object.spawn({o => "BAR"}, ruby_cmd('print ENV["FOO"]'))
    end.should output_to_fd("BAR")
  end

  it "calls #to_str to convert the environment values" do
    o = mock("to_str")
    o.should_receive(:to_str).and_return("BAR")
    lambda do
      Process.wait @object.spawn({"FOO" => o}, ruby_cmd('print ENV["FOO"]'))
    end.should output_to_fd("BAR")
  end

  it "raises an ArgumentError if an environment key includes an equals sign" do
    lambda do
      @object.spawn({"FOO=" => "BAR"}, ruby_cmd('print ENV["FOO"]'))
    end.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if an environment key includes a null byte" do
    lambda do
      @object.spawn({"\000" => "BAR"}, ruby_cmd('print ENV["FOO"]'))
    end.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if an environment value includes a null byte" do
    lambda do
      @object.spawn({"FOO" => "\000"}, ruby_cmd('print ENV["FOO"]'))
    end.should raise_error(ArgumentError)
  end

  # :unsetenv_others

  it "unsets other environment variables when given a true :unsetenv_others option" do
    ENV["FOO"] = "BAR"
    lambda do
      Process.wait @object.spawn(ruby_cmd('print ENV["FOO"]'), :unsetenv_others => true)
    end.should output_to_fd("")
  end

  it "unsets other environment variables when given a non-false :unsetenv_others option" do
    ENV["FOO"] = "BAR"
    lambda do
      Process.wait @object.spawn(ruby_cmd('print ENV["FOO"]'), :unsetenv_others => :true)
    end.should output_to_fd("")
  end

  it "does not unset other environment variables when given a false :unsetenv_others option" do
    ENV["FOO"] = "BAR"
    lambda do
      Process.wait @object.spawn(ruby_cmd('print ENV["FOO"]'), :unsetenv_others => false)
    end.should output_to_fd("BAR")
  end

  it "does not unset other environment variables when given a nil :unsetenv_others option" do
    ENV["FOO"] = "BAR"
    lambda do
      Process.wait @object.spawn(ruby_cmd('print ENV["FOO"]'), :unsetenv_others => nil)
    end.should output_to_fd("BAR")
  end

  it "does not unset environment variables included in the environment hash" do
    lambda do
      Process.wait @object.spawn({"FOO" => "BAR"}, ruby_cmd('print ENV["FOO"]'), :unsetenv_others => true)
    end.should output_to_fd("BAR")
  end

  # :pgroup

  platform_is_not :windows do
    it "joins the current process group by default" do
      lambda do
        Process.wait @object.spawn(ruby_cmd("print Process.getpgid(Process.pid)"))
      end.should output_to_fd(Process.getpgid(Process.pid).to_s)
    end

    it "joins the current process if :pgroup => false" do
      lambda do
        Process.wait @object.spawn(ruby_cmd("print Process.getpgid(Process.pid)"), :pgroup => false)
      end.should output_to_fd(Process.getpgid(Process.pid).to_s)
    end

    it "joins the current process if :pgroup => nil" do
      lambda do
        Process.wait @object.spawn(ruby_cmd("print Process.getpgid(Process.pid)"), :pgroup => nil)
      end.should output_to_fd(Process.getpgid(Process.pid).to_s)
    end

    it "joins a new process group if :pgroup => true" do
      process = lambda do
        Process.wait @object.spawn(ruby_cmd("print Process.getpgid(Process.pid)"), :pgroup => true)
      end

      process.should_not output_to_fd(Process.getpgid(Process.pid).to_s)
      process.should output_to_fd(/\d+/)
    end

    it "joins a new process group if :pgroup => 0" do
      process = lambda do
        Process.wait @object.spawn(ruby_cmd("print Process.getpgid(Process.pid)"), :pgroup => 0)
      end

      process.should_not output_to_fd(Process.getpgid(Process.pid).to_s)
      process.should output_to_fd(/\d+/)
    end

    it "joins the specified process group if :pgroup => pgid" do
      lambda do
        Process.wait @object.spawn(ruby_cmd("print Process.getpgid(Process.pid)"), :pgroup => 123)
      end.should_not output_to_fd("123")
    end

    it "raises an ArgumentError if given a negative :pgroup option" do
      lambda { @object.spawn("echo", :pgroup => -1) }.should raise_error(ArgumentError)
    end

    it "raises a TypeError if given a symbol as :pgroup option" do
      lambda { @object.spawn("echo", :pgroup => :true) }.should raise_error(TypeError)
    end
  end

  platform_is :windows do
    it "raises an ArgumentError if given :pgroup option" do
      lambda { @object.spawn("echo", :pgroup => false) }.should raise_error(ArgumentError)
    end
  end

  # :rlimit_core
  # :rlimit_cpu
  # :rlimit_data

  # :chdir

  it "uses the current working directory as its working directory" do
    lambda do
      Process.wait @object.spawn(ruby_cmd("print Dir.pwd"))
    end.should output_to_fd(Dir.pwd)
  end

  describe "when passed :chdir" do
    before do
      @dir = tmp("spawn_chdir", false)
      Dir.mkdir @dir
    end

    after do
      rm_r @dir
    end

    it "changes to the directory passed for :chdir" do
      lambda do
        Process.wait @object.spawn(ruby_cmd("print Dir.pwd"), :chdir => @dir)
      end.should output_to_fd(@dir)
    end

    it "calls #to_path to convert the :chdir value" do
      dir = mock("spawn_to_path")
      dir.should_receive(:to_path).and_return(@dir)

      lambda do
        Process.wait @object.spawn(ruby_cmd("print Dir.pwd"), :chdir => dir)
      end.should output_to_fd(@dir)
    end
  end

  # :umask

  it "uses the current umask by default" do
    lambda do
      Process.wait @object.spawn(ruby_cmd("print File.umask"))
    end.should output_to_fd(File.umask.to_s)
  end

  it "sets the umask if given the :umask option" do
    lambda do
      Process.wait @object.spawn(ruby_cmd("print File.umask"), :umask => 146)
    end.should output_to_fd("146")
  end

  # redirection

  it "redirects STDOUT to the given file descriptior if :out => Fixnum" do
    File.open(@name, 'w') do |file|
      lambda do
        Process.wait @object.spawn(ruby_cmd("print :glark"), :out => file.fileno)
      end.should output_to_fd("glark", file)
    end
  end

  it "redirects STDOUT to the given file if :out => IO" do
    File.open(@name, 'w') do |file|
      lambda do
        Process.wait @object.spawn(ruby_cmd("print :glark"), :out => file)
      end.should output_to_fd("glark", file)
    end
  end

  it "redirects STDOUT to the given file if :out => String" do
    Process.wait @object.spawn(ruby_cmd("print :glark"), :out => @name)
    @name.should have_data("glark")
  end

  it "redirects STDERR to the given file descriptior if :err => Fixnum" do
    File.open(@name, 'w') do |file|
      lambda do
        Process.wait @object.spawn(ruby_cmd("STDERR.print :glark"), :err => file.fileno)
      end.should output_to_fd("glark", file)
    end
  end

  it "redirects STDERR to the given file descriptor if :err => IO" do
    File.open(@name, 'w') do |file|
      lambda do
        Process.wait @object.spawn(ruby_cmd("STDERR.print :glark"), :err => file)
      end.should output_to_fd("glark", file)
    end
  end

  it "redirects STDERR to the given file if :err => String" do
    Process.wait @object.spawn(ruby_cmd("STDERR.print :glark"), :err => @name)
    @name.should have_data("glark")
  end

  it "redirects both STDERR and STDOUT to the given file descriptior" do
    File.open(@name, 'w') do |file|
      lambda do
        Process.wait @object.spawn(ruby_cmd("print(:glark); STDOUT.flush; STDERR.print(:bang)"),
                                   [:out, :err] => file.fileno)
      end.should output_to_fd("glarkbang", file)
    end
  end

  it "redirects both STDERR and STDOUT to the given IO" do
    File.open(@name, 'w') do |file|
      lambda do
        Process.wait @object.spawn(ruby_cmd("print(:glark); STDOUT.flush; STDERR.print(:bang)"),
                                   [:out, :err] => file)
      end.should output_to_fd("glarkbang", file)
    end
  end

  it "does NOT redirect both STDERR and STDOUT at the time to the given name" do
    # this behavior is not guaranteed; it may be changed after 1.9.3 or later.  [ruby-dev:41433]
    touch @name
    Process.wait @object.spawn(ruby_cmd("print(:glark); STDOUT.flush; STDERR.print(:bang)"),
                               [:out, :err] => @name)
    @name.should have_data("")
  end

  # :close_others

  it "closes file descriptors >= 3 in the child process" do
    IO.pipe do |r, w|
      begin
        pid = @object.spawn(ruby_cmd(""))
        w.close
        lambda { r.read_nonblock(1) }.should raise_error(EOFError)
      ensure
        Process.kill(:TERM, pid)
        Process.wait(pid)
      end
    end
  end

  it "closes file descriptors >= 3 in the child process even if given a false :close_others option because they are set close_on_exec" do
    IO.pipe do |r, w|
      begin
        pid = @object.spawn(ruby_cmd(""), :close_others => false)
        w.close
        lambda { r.read_nonblock(1) }.should raise_error(EOFError)
      ensure
        Process.kill(:TERM, pid)
        Process.wait(pid)
      end
    end
  end

  it "does not close file descriptors >= 3 in the child process when given a false :close_others option and fds are set close_on_exec=false" do
    IO.pipe do |r, w|
      r.close_on_exec = false
      w.close_on_exec = false
      begin
        pid = @object.spawn(ruby_cmd(""), :close_others => false)
        w.close
        lambda { r.read_nonblock(1) }.should raise_error(Errno::EAGAIN)
      ensure
        Process.kill(:TERM, pid)
        Process.wait(pid)
      end
    end
  end

  # error handling

  it "raises an ArgumentError if passed no command arguments" do
    lambda { @object.spawn }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if passed env or options but no command arguments" do
    lambda { @object.spawn({}) }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if passed env and options but no command arguments" do
    lambda { @object.spawn({}, {}) }.should raise_error(ArgumentError)
  end

  it "raises an Errno::ENOENT for an empty string" do
    lambda { @object.spawn "" }.should raise_error(Errno::ENOENT)
  end

  it "raises an Errno::ENOENT if the command does not exist" do
    lambda { @object.spawn "nonesuch" }.should raise_error(Errno::ENOENT)
  end

  unless File.executable?(__FILE__) # Some FS (e.g. vboxfs) locate all files executable
    it "raises an Errno::EACCES when the file does not have execute permissions" do
      lambda { @object.spawn __FILE__ }.should raise_error(Errno::EACCES)
    end
  end

  it "raises an Errno::EACCES when passed a directory" do
    lambda { @object.spawn File.dirname(__FILE__) }.should raise_error(Errno::EACCES)
  end

  it "raises an ArgumentError when passed a string key in options" do
    lambda { @object.spawn("echo", "chdir" => Dir.pwd) }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError when passed an unknown option key" do
    lambda { @object.spawn("echo", :nonesuch => :foo) }.should raise_error(ArgumentError)
  end

  describe "with Integer option keys" do
    before :each do
      @name = tmp("spawn_fd_map.txt")
      @io = new_io @name, "w+"
      @io.sync = true
    end

    after :each do
      @io.close unless @io.closed?
      rm_r @name
    end

    it "maps the key to a file descriptor in the child that inherits the file descriptor from the parent specified by the value" do
      child_fd = @io.fileno + 1
      args = [RUBY_EXE, fixture(__FILE__, "map_fd.rb"), child_fd.to_s]
      pid = @object.spawn *args, { child_fd => @io }
      Process.waitpid pid
      @io.rewind

      @io.read.should == "writing to fd: #{child_fd}"
    end
  end
end
