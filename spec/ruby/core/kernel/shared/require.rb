describe :kernel_require_basic, :shared => true do
  describe "(path resolution)" do
    it "loads an absolute path" do
      path = File.expand_path "load_fixture.rb", CODE_LOADING_DIR
      @object.send(@method, path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a non-canonical absolute path" do
      dir, file = File.split(File.expand_path("load_fixture.rb", CODE_LOADING_DIR))
      path = File.join dir, ["..", "code"], file
      @object.send(@method, path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a file defining many methods" do
      path = File.expand_path "methods_fixture.rb", CODE_LOADING_DIR
      @object.send(@method, path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "raises a LoadError if the file does not exist" do
      path = File.expand_path "nonexistent.rb", CODE_LOADING_DIR
      File.exist?(path).should be_false
      lambda { @object.send(@method, path) }.should raise_error(LoadError)
      ScratchPad.recorded.should == []
    end

    # Can't make a file unreadable on these platforms
    platform_is_not :os => [:windows, :cygwin] do
      describe "with an unreadable file" do
        before :each do
          @path = tmp("unreadable_file.rb")
          touch @path
          File.chmod 0000, @path
        end

        after :each do
          File.chmod 0666, @path
          rm_r @path
        end

        it "raises a LoadError" do
          File.exist?(@path).should be_true
          lambda { @object.send(@method, @path) }.should raise_error(LoadError)
        end
      end
    end

    it "calls #to_str on non-String objects" do
      path = File.expand_path "load_fixture.rb", CODE_LOADING_DIR
      name = mock("load_fixture.rb mock")
      name.should_receive(:to_str).and_return(path)
      @object.send(@method, name).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "raises a TypeError if passed nil" do
      lambda { @object.send(@method, nil) }.should raise_error(TypeError)
    end

    it "raises a TypeError if passed a Fixnum" do
      lambda { @object.send(@method, 42) }.should raise_error(TypeError)
    end

    it "raises a TypeError if passed an Array" do
      lambda { @object.send(@method, []) }.should raise_error(TypeError)
    end

    it "raises a TypeError if passed an object that does not provide #to_str" do
      lambda { @object.send(@method, mock("not a filename")) }.should raise_error(TypeError)
    end

    it "raises a TypeError if passed an object that has #to_s but not #to_str" do
      name = mock("load_fixture.rb mock")
      name.stub!(:to_s).and_return("load_fixture.rb")
      $LOAD_PATH << "."
      Dir.chdir CODE_LOADING_DIR do
        lambda { @object.send(@method, name) }.should raise_error(TypeError)
      end
    end

    it "raises a TypeError if #to_str does not return a String" do
      name = mock("#to_str returns nil")
      name.should_receive(:to_str).at_least(1).times.and_return(nil)
      lambda { @object.send(@method, name) }.should raise_error(TypeError)
    end

    it "calls #to_path on non-String objects" do
      name = mock("load_fixture.rb mock")
      name.stub!(:to_path).and_return("load_fixture.rb")
      $LOAD_PATH << "."
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, name).should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end

    it "calls #to_path on a String" do
      path = File.expand_path "load_fixture.rb", CODE_LOADING_DIR
      str = mock("load_fixture.rb mock")
      str.should_receive(:to_path).and_return(path)
      @object.send(@method, str).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "calls #to_str on non-String objects returned by #to_path" do
      path = File.expand_path "load_fixture.rb", CODE_LOADING_DIR
      name = mock("load_fixture.rb mock")
      to_path = mock("load_fixture_rb #to_path mock")
      name.should_receive(:to_path).and_return(to_path)
      to_path.should_receive(:to_str).and_return(path)
      @object.send(@method, name).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    # "http://redmine.ruby-lang.org/issues/show/2578"
    it "loads a ./ relative path from the current working directory with empty $LOAD_PATH" do
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "./load_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a ../ relative path from the current working directory with empty $LOAD_PATH" do
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "../code/load_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a ./ relative path from the current working directory with non-empty $LOAD_PATH" do
      $LOAD_PATH << "an_irrelevant_dir"
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "./load_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a ../ relative path from the current working directory with non-empty $LOAD_PATH" do
      $LOAD_PATH << "an_irrelevant_dir"
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "../code/load_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a non-canonical path from the current working directory with non-empty $LOAD_PATH" do
      $LOAD_PATH << "an_irrelevant_dir"
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, "../code/../code/load_fixture.rb").should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end

    it "resolves a filename against $LOAD_PATH entries" do
      $LOAD_PATH << CODE_LOADING_DIR
      @object.send(@method, "load_fixture.rb").should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "does not require file twice after $LOAD_PATH change" do
      $LOAD_PATH << CODE_LOADING_DIR
      @object.require("load_fixture.rb").should be_true
      $LOAD_PATH.unshift CODE_LOADING_DIR + "/gem"
      @object.require("load_fixture.rb").should be_false
      ScratchPad.recorded.should == [:loaded]
    end

    it "does not resolve a ./ relative path against $LOAD_PATH entries" do
      $LOAD_PATH << CODE_LOADING_DIR
      lambda do
        @object.send(@method, "./load_fixture.rb")
      end.should raise_error(LoadError)
      ScratchPad.recorded.should == []
    end

    it "does not resolve a ../ relative path against $LOAD_PATH entries" do
      $LOAD_PATH << CODE_LOADING_DIR
      lambda do
        @object.send(@method, "../code/load_fixture.rb")
      end.should raise_error(LoadError)
      ScratchPad.recorded.should == []
    end

    it "resolves a non-canonical path against $LOAD_PATH entries" do
      $LOAD_PATH << File.dirname(CODE_LOADING_DIR)
      @object.send(@method, "code/../code/load_fixture.rb").should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a path with duplicate path separators" do
      $LOAD_PATH << "."
      sep = File::Separator + File::Separator
      path = ["..", "code", "load_fixture.rb"].join(sep)
      Dir.chdir CODE_LOADING_DIR do
        @object.send(@method, path).should be_true
      end
      ScratchPad.recorded.should == [:loaded]
    end
  end
end

describe :kernel_require, :shared => true do
  describe "(path resolution)" do
    # For reference see [ruby-core:24155] in which matz confirms this feature is
    # intentional for security reasons.
    it "does not load a bare filename unless the current working directory is in $LOAD_PATH" do
      Dir.chdir CODE_LOADING_DIR do
        lambda { @object.require("load_fixture.rb") }.should raise_error(LoadError)
        ScratchPad.recorded.should == []
      end
    end

    it "does not load a relative path unless the current working directory is in $LOAD_PATH" do
      Dir.chdir File.dirname(CODE_LOADING_DIR) do
        lambda do
          @object.require("code/load_fixture.rb")
        end.should raise_error(LoadError)
        ScratchPad.recorded.should == []
      end
    end

    it "loads a file that recursively requires itself" do
      path = File.expand_path "recursive_require_fixture.rb", CODE_LOADING_DIR
      @object.require(path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end
  end

  describe "(non-extensioned path)" do
    before :each do
      a = File.expand_path "a", CODE_LOADING_DIR
      b = File.expand_path "b", CODE_LOADING_DIR
      $LOAD_PATH.replace [a, b]
    end

    it "loads a .rb extensioned file when a C-extension file exists on an earlier load path" do
      @object.require("load_fixture").should be_true
      ScratchPad.recorded.should == [:loaded]
    end
  end

  describe "(file extensions)" do
    it "loads a .rb extensioned file when passed a non-extensioned path" do
      path = File.expand_path "load_fixture", CODE_LOADING_DIR
      File.exist?(path).should be_true
      @object.require(path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a .rb extensioned file when a C-extension file of the same name is loaded" do
      $LOADED_FEATURES << File.expand_path("load_fixture.bundle", CODE_LOADING_DIR)
      $LOADED_FEATURES << File.expand_path("load_fixture.dylib", CODE_LOADING_DIR)
      $LOADED_FEATURES << File.expand_path("load_fixture.so", CODE_LOADING_DIR)
      $LOADED_FEATURES << File.expand_path("load_fixture.dll", CODE_LOADING_DIR)
      path = File.expand_path "load_fixture", CODE_LOADING_DIR
      @object.require(path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "does not load a C-extension file if a .rb extensioned file is already loaded" do
      $LOADED_FEATURES << File.expand_path("load_fixture.rb", CODE_LOADING_DIR)
      path = File.expand_path "load_fixture", CODE_LOADING_DIR
      @object.require(path).should be_false
      ScratchPad.recorded.should == []
    end

    it "loads a .rb extensioned file when passed a non-.rb extensioned path" do
      path = File.expand_path "load_fixture.ext", CODE_LOADING_DIR
      File.exist?(path).should be_true
      @object.require(path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "loads a .rb extensioned file when a complex-extensioned C-extension file of the same name is loaded" do
      $LOADED_FEATURES << File.expand_path("load_fixture.ext.bundle", CODE_LOADING_DIR)
      $LOADED_FEATURES << File.expand_path("load_fixture.ext.dylib", CODE_LOADING_DIR)
      $LOADED_FEATURES << File.expand_path("load_fixture.ext.so", CODE_LOADING_DIR)
      $LOADED_FEATURES << File.expand_path("load_fixture.ext.dll", CODE_LOADING_DIR)
      path = File.expand_path "load_fixture.ext", CODE_LOADING_DIR
      @object.require(path).should be_true
      ScratchPad.recorded.should == [:loaded]
    end

    it "does not load a C-extension file if a complex-extensioned .rb file is already loaded" do
      $LOADED_FEATURES << File.expand_path("load_fixture.ext.rb", CODE_LOADING_DIR)
      path = File.expand_path "load_fixture.ext", CODE_LOADING_DIR
      @object.require(path).should be_false
      ScratchPad.recorded.should == []
    end
  end

  describe "($LOAD_FEATURES)" do
    before :each do
      @path = File.expand_path("load_fixture.rb", CODE_LOADING_DIR)
    end

    it "stores an absolute path" do
      @object.require(@path).should be_true
      $LOADED_FEATURES.should == [@path]
    end

    it "does not store the path if the load fails" do
      $LOAD_PATH << CODE_LOADING_DIR
      lambda { @object.require("raise_fixture.rb") }.should raise_error(RuntimeError)
      $LOADED_FEATURES.should == []
    end

    it "does not load an absolute path that is already stored" do
      $LOADED_FEATURES << @path
      @object.require(@path).should be_false
      ScratchPad.recorded.should == []
    end

    it "does not load a ./ relative path that is already stored" do
      $LOADED_FEATURES << "./load_fixture.rb"
      Dir.chdir CODE_LOADING_DIR do
        @object.require("./load_fixture.rb").should be_false
      end
      ScratchPad.recorded.should == []
    end

    it "does not load a ../ relative path that is already stored" do
      $LOADED_FEATURES << "../load_fixture.rb"
      Dir.chdir CODE_LOADING_DIR do
        @object.require("../load_fixture.rb").should be_false
      end
      ScratchPad.recorded.should == []
    end

    it "does not load a non-canonical path that is already stored" do
      $LOADED_FEATURES << "code/../code/load_fixture.rb"
      $LOAD_PATH << File.dirname(CODE_LOADING_DIR)
      @object.require("code/../code/load_fixture.rb").should be_false
      ScratchPad.recorded.should == []
    end

    it "respects being replaced with a new array" do
      prev = $LOADED_FEATURES.dup

      @object.require(@path).should be_true
      $LOADED_FEATURES.should == [@path]

      $LOADED_FEATURES.replace(prev)

      @object.require(@path).should be_true
      $LOADED_FEATURES.should == [@path]
    end

    it "does not load twice the same file with and without extension" do
      $LOAD_PATH << CODE_LOADING_DIR
      @object.require("load_fixture.rb").should be_true
      @object.require("load_fixture").should be_false
    end

    describe "when a non-extensioned file is in $LOADED_FEATURES" do
      before :each do
        $LOADED_FEATURES << "load_fixture"
      end

      it "loads a .rb extensioned file when a non extensioned file is in $LOADED_FEATURES" do
        $LOAD_PATH << CODE_LOADING_DIR
        @object.require("load_fixture").should be_true
        ScratchPad.recorded.should == [:loaded]
      end

      it "loads a .rb extensioned file from a subdirectory" do
        $LOAD_PATH << File.dirname(CODE_LOADING_DIR)
        @object.require("code/load_fixture").should be_true
        ScratchPad.recorded.should == [:loaded]
      end

      it "returns false if the file is not found" do
        Dir.chdir File.dirname(CODE_LOADING_DIR) do
          @object.require("load_fixture").should be_false
          ScratchPad.recorded.should == []
        end
      end

      it "returns false when passed a path and the file is not found" do
        $LOADED_FEATURES << "code/load_fixture"
        Dir.chdir CODE_LOADING_DIR do
          @object.require("code/load_fixture").should be_false
          ScratchPad.recorded.should == []
        end
      end
    end

    it "stores ../ relative paths as absolute paths" do
      Dir.chdir CODE_LOADING_DIR do
        @object.require("../code/load_fixture.rb").should be_true
      end
      $LOADED_FEATURES.should == [@path]
    end

    it "stores ./ relative paths as absolute paths" do
      Dir.chdir CODE_LOADING_DIR do
        @object.require("./load_fixture.rb").should be_true
      end
      $LOADED_FEATURES.should == [@path]
    end

    it "collapses duplicate path separators" do
      $LOAD_PATH << "."
      sep = File::Separator + File::Separator
      path = ["..", "code", "load_fixture.rb"].join(sep)
      Dir.chdir CODE_LOADING_DIR do
        @object.require(path).should be_true
      end
      $LOADED_FEATURES.should == [@path]
    end

    it "canonicalizes non-unique absolute paths" do
      dir, file = File.split(File.expand_path("load_fixture.rb", CODE_LOADING_DIR))
      path = File.join dir, ["..", "code"], file
      @object.require(path).should be_true
      $LOADED_FEATURES.should == [@path]
    end

    it "adds the suffix of the resolved filename" do
      $LOAD_PATH << CODE_LOADING_DIR
      @object.require("load_fixture").should be_true
      $LOADED_FEATURES.should == [@path]
    end

    it "does not load a non-canonical path for a file already loaded" do
      $LOADED_FEATURES << @path
      $LOAD_PATH << File.dirname(CODE_LOADING_DIR)
      @object.require("code/../code/load_fixture.rb").should be_false
      ScratchPad.recorded.should == []
    end

    it "does not load a ./ relative path for a file already loaded" do
      $LOADED_FEATURES << @path
      $LOAD_PATH << "an_irrelevant_dir"
      Dir.chdir CODE_LOADING_DIR do
        @object.require("./load_fixture.rb").should be_false
      end
      ScratchPad.recorded.should == []
    end

    it "does not load a ../ relative path for a file already loaded" do
      $LOADED_FEATURES << @path
      $LOAD_PATH << "an_irrelevant_dir"
      Dir.chdir CODE_LOADING_DIR do
        @object.require("../code/load_fixture.rb").should be_false
      end
      ScratchPad.recorded.should == []
    end
  end

  describe "(shell expansion)" do
    before :all do
      @env_home = ENV["HOME"]
      ENV["HOME"] = CODE_LOADING_DIR
    end

    after :all do
      ENV["HOME"] = @env_home
    end

    # "#3171"
    it "performs tilde expansion on a .rb file before storing paths in $LOADED_FEATURES" do
      path = File.expand_path("load_fixture.rb", CODE_LOADING_DIR)
      @object.require("~/load_fixture.rb").should be_true
      $LOADED_FEATURES.should == [path]
    end

    it "performs tilde expansion on a non-extensioned file before storing paths in $LOADED_FEATURES" do
      path = File.expand_path("load_fixture.rb", CODE_LOADING_DIR)
      @object.require("~/load_fixture").should be_true
      $LOADED_FEATURES.should == [path]
    end
  end

  describe "(concurrently)" do
    before :each do
      ScratchPad.record []
      @path = File.expand_path "concurrent.rb", CODE_LOADING_DIR
      @path2 = File.expand_path "concurrent2.rb", CODE_LOADING_DIR
      @path3 = File.expand_path "concurrent3.rb", CODE_LOADING_DIR
    end

    after :each do
      ScratchPad.clear
      $LOADED_FEATURES.delete @path
      $LOADED_FEATURES.delete @path2
      $LOADED_FEATURES.delete @path3
    end

    # Quick note about these specs:
    #
    # You'll notice in concurrent.rb that there are some sleep calls. This seems
    # like a bad form for specs testing concurrency since using sleep to force
    # thread progression is a mega hack, there is currently no other way to spec
    # the behavior. Here is why:
    #
    # The behavior we're spec'ing requires that t2 enter #require, see t1 is
    # loading @path, grab a lock, and wait on it.
    #
    # We do make sure that t2 starts the require once t1 is in the middle
    # of concurrent.rb, but we then need to get t2 to get far enough into #require
    # to see t1's lock and try to lock it.
    #
    # Because #require is completely opapque, there is no other way to hold t1 until
    # t2 has progress that far other than just having t1 sleep for a little bit
    # and hope that t2 has progressed and is now holding the lock for @path.
    #
    # Sucks? Yep. But we haven't come up with a better solution.
    #
    it "blocks a second thread from returning while the 1st is still requiring" do
      start = false
      fin = false

      t1_res = nil
      t2_res = nil

      t1 = Thread.new do
        t1_res = @object.require(@path)
        Thread.pass until fin
        ScratchPad.recorded << :t1_post
      end

      t2 = Thread.new do
        Thread.pass until t1 && t1[:in_concurrent_rb]
        begin
          t2_res = @object.require(@path)
          ScratchPad.recorded << :t2_post
        ensure
          fin = true
        end
      end

      t1.join
      t2.join

      t1_res.should be_true
      t2_res.should be_false

      ScratchPad.recorded.should == [:con_pre, :con_post, :t2_post, :t1_post]
    end

    it "blocks based on the path" do
      start = false

      t1 = Thread.new do
        Thread.pass until start
        # Yes, using sleep for synchronization is broken and wrong. See the
        # comment above. Alternatively, fix Ruby.
        sleep 0.1
        @object.require(@path2).should be_true
        ScratchPad.recorded << :t1_post
      end

      t2 = Thread.new do
        start = true
        @object.require(@path3).should be_true
        ScratchPad.recorded << :t2_post
      end

      t1.join
      t2.join

      ScratchPad.recorded.should == [:con3_post, :t2_post, :con2_post, :t1_post]
    end

    it "allows a 2nd require if the 1st raised an exception" do
      start = false
      fin = false

      t2_res = nil

      t1 = Thread.new do
        Thread.current[:con_raise] = true

        lambda {
          @object.require(@path)
        }.should raise_error(RuntimeError)

        Thread.pass until fin
        ScratchPad.recorded << :t1_post
      end

      t2 = Thread.new do
        Thread.pass until t1 && t1[:in_concurrent_rb]
        begin
          t2_res = @object.require(@path)
          ScratchPad.recorded << :t2_post
        ensure
          fin = true
        end
      end

      t1.join
      t2.join

      t2_res.should be_true

      ScratchPad.recorded.should == [:con_pre, :con_pre, :con_post, :t2_post, :t1_post]
    end

    # "redmine #5754"
    it "blocks a 3rd require if the 1st raises an exception and the 2nd is still running" do
      start = false
      fin = false

      t1_res = nil
      t2_res = nil

      t1_running = false
      t2_running = false

      t2 = nil

      t1 = Thread.new do
        Thread.current[:con_raise] = true
        t1_running = true

        lambda {
          @object.require(@path)
        }.should raise_error(RuntimeError)

        # This hits the bug. Because MRI removes it's internal lock from a table
        # when the exception is raised, this #require doesn't see that t2 is
        # in the middle of requiring the file, so this #require runs when it should
        # not.
        #
        # Sometimes this raises a ThreadError also, but I'm not sure why.
        Thread.pass until t2_running && t2[:in_concurrent_rb] == true
        t1_res = @object.require(@path)

        Thread.pass until fin

        ScratchPad.recorded << :t1_post
      end

      t2 = Thread.new do
        t2_running = true

        Thread.pass until t1_running && t1[:in_concurrent_rb] == true

        begin
          t2_res = @object.require(@path)

          ScratchPad.recorded << :t2_post
        ensure
          fin = true
        end
      end

      t1.join
      t2.join

      t1_res.should be_false
      t2_res.should be_true

      ScratchPad.recorded.should == [:con_pre, :con_pre, :con_post, :t2_post, :t1_post]
    end
  end

  it "stores the missing path in a LoadError object" do
    path = "abcd1234"

    lambda {
      @object.send(@method, path)
    }.should(raise_error(LoadError) { |e|
      e.path.should == path
    })
  end
end
