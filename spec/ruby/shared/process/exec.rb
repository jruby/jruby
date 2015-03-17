describe :process_exec, :shared => true do
  it "raises Errno::ENOENT for an empty string" do
    lambda { @object.exec "" }.should raise_error(Errno::ENOENT)
  end

  it "raises Errno::ENOENT for a command which does not exist" do
    lambda { @object.exec "bogus-noent-script.sh" }.should raise_error(Errno::ENOENT)
  end

  it "raises an ArgumentError if the command includes a null byte" do
    lambda { @object.exec "\000" }.should raise_error(ArgumentError)
  end

  unless File.executable?(__FILE__) # Some FS (e.g. vboxfs) locate all files executable
    it "raises Errno::EACCES when the file does not have execute permissions" do
      lambda { @object.exec __FILE__ }.should raise_error(Errno::EACCES)
    end
  end

  platform_is_not :openbsd do
    it "raises Errno::EACCES when passed a directory" do
      lambda { @object.exec File.dirname(__FILE__) }.should raise_error(Errno::EACCES)
    end
  end

  platform_is :openbsd do
    it "raises Errno::EISDIR when passed a directory" do
      lambda { @object.exec File.dirname(__FILE__) }.should raise_error(Errno::EISDIR)
    end
  end

  it "runs the specified command, replacing current process" do
    ruby_exe('exec "echo hello"; puts "fail"', :escape => true).should == "hello\n"
  end

  it "sets the current directory when given the :chdir option" do
    tmpdir = tmp("")[0..-2]
    ruby_exe("exec(\"pwd\", :chdir => #{tmpdir.inspect})", :escape => true).should == "#{tmpdir}\n"
  end

  it "flushes STDOUT upon exit when it's not set to sync" do
    ruby_exe("STDOUT.sync = false; STDOUT.write 'hello'").should == "hello"
  end

  it "flushes STDERR upon exit when it's not set to sync" do
    ruby_exe("STDERR.sync = false; STDERR.write 'hello'", :args => "2>&1").should == "hello"
  end

  describe "with a single argument" do
    before(:each) do
      @dir = tmp("exec_with_dir", false)
      Dir.mkdir @dir

      @name = "some_file"
      @path = tmp("exec_with_dir/#{@name}", false)
      touch @path
    end

    after(:each) do
      rm_r @path
      rm_r @dir
    end

    it "subjects the specified command to shell expansion" do
      result = ruby_exe('exec "echo *"', :escape => true, :dir => @dir)
      result.chomp.should == @name
    end

    it "creates an argument array with shell parsing semantics for whitespace" do
      ruby_exe('exec "echo a b  c   d"', :escape => true).should == "a b c d\n"
    end
  end

  describe "with multiple arguments" do
    it "does not subject the arguments to shell expansion" do
      ruby_exe('exec "echo", "*"', :escape => true).should == "*\n"
    end
  end

  describe "(environment variables)" do
    before(:each) do
      ENV["FOO"] = "FOO"
    end

    after(:each) do
      ENV["FOO"] = nil
    end

    it "sets environment variables in the child environment" do
      ruby_exe('exec({"FOO" => "BAR"}, "echo $FOO")', :escape => true).should == "BAR\n"
    end

    it "unsets environment variables whose value is nil" do
      ruby_exe('exec({"FOO" => nil}, "echo $FOO")', :escape => true).should == "\n"
    end

    it "coerces environment argument using to_hash" do
      ruby_exe('o = Object.new; def o.to_hash; {"FOO" => "BAR"}; end; exec(o, "echo $FOO")', :escape => true).should == "BAR\n"
    end

    it "unsets other environment variables when given a true :unsetenv_others option" do
      ruby_exe('exec("echo $FOO", :unsetenv_others => true)', :escape => true).should == "\n"
    end
  end

  describe "with a command array" do
    it "uses the first element as the command name and the second as the argv[0] value" do
      ruby_exe('exec(["/bin/sh", "argv_zero"], "-c", "echo $0")', :escape => true).should == "argv_zero\n"
    end

    it "coerces the argument using to_ary" do
      ruby_exe('o = Object.new; def o.to_ary; ["/bin/sh", "argv_zero"]; end; exec(o, "-c", "echo $0")', :escape => true).should == "argv_zero\n"
    end

    it "raises an ArgumentError if the Array does not have exactly two elements" do
      lambda { @object.exec([]) }.should raise_error(ArgumentError)
      lambda { @object.exec([:a]) }.should raise_error(ArgumentError)
      lambda { @object.exec([:a, :b, :c]) }.should raise_error(ArgumentError)
    end
  end

  describe "with an options Hash" do
    describe "with Integer option keys" do
      before :each do
        @name = tmp("exec_fd_map.txt")
        @io = tmp("exec_fd_map_parent.txt")
      end

      after :each do
        rm_r @name, @io
      end

      it "maps the key to a file descriptor in the child that inherits the file descriptor from the parent specified by the value" do
        cmd = <<-EOC
          f = File.open "#{@name}", "w+"
          child_fd = f.fileno + 1
          File.open("#{@io}", "w") { |io| io.print child_fd }
          exec "#{RUBY_EXE}", "#{fixture __FILE__, "map_fd.rb"}", child_fd.to_s, { child_fd => f }
          EOC

        ruby_exe(cmd, :escape => true)
        child_fd = IO.read(@io).to_i
        child_fd.to_i.should > STDERR.fileno

        @name.should have_data("writing to fd: #{child_fd}")
      end
    end
  end
end
