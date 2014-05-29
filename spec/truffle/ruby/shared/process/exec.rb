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

  it "raises Errno::EACCES when the file does not have execute permissions" do
    File.executable?(__FILE__).should == false
    lambda { @object.exec __FILE__ }.should raise_error(Errno::EACCES)
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

  describe "with a single argument" do
    before(:each) do
      @path = tmp("tmp")
      touch @path
    end

    after(:each) do
      rm_r @path
    end

    it "subjects the specified command to shell expansion" do
      ruby_exe('exec "echo *"', :escape => true, :dir => tmp("")).should == "#{File.basename(@path)}\n"
    end

    it "creates an argument array with shell parsing semantics for whitespace" do
      ruby_exe('exec "echo a b  c 	d"', :escape => true).should == "a b c d\n"
    end
  end

  describe "with multiple arguments" do
    it "does not subject the arguments to shell expansion" do
      ruby_exe('exec "echo", "*"', :escape => true).should == "*\n"
    end
  end

  ruby_version_is "1.9.2" do
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

    it "sets the current directory when given the :chdir option" do
      tmpdir = tmp("")[0..-2]
      ruby_exe("exec(\"pwd\", :chdir => #{tmpdir.inspect})", :escape => true).should == "#{tmpdir}\n"
    end
  end

  describe "with a command array" do
    it "uses the first element as the command name and the second as the argv[0] value" do
      ruby_exe('exec(["/bin/sh", "argv_zero"], "-c", "echo $0")', :escape => true).should == "argv_zero\n"
    end

    it "coerces the argument using to_ary" do
      ruby_exe('o = Object.new; def o.to_ary; ["/bin/sh", "argv_zero"]; end; exec(o, "-c", "echo $0")', :escape => true).should == "argv_zero\n"
    end

    it "raises Argument error if the Array does not have exactly two elements" do
      lambda { @object.exec([]) }.should raise_error(ArgumentError)
      lambda { @object.exec([:a]) }.should raise_error(ArgumentError)
      lambda { @object.exec([:a, :b, :c]) }.should raise_error(ArgumentError)
    end
  end
end
