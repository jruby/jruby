require File.expand_path('../../../spec_helper', __FILE__)

describe "File.join" do
  it "returns an empty string when given no arguments" do
    File.join.should == ""
  end

  it "when given a single argument returns an equal string" do
    File.join("").should == ""
    File.join("usr").should == "usr"
  end

  it "joins parts using File::SEPARATOR" do
    File.join('usr', 'bin').should == "usr/bin"
  end

  it "supports any number of arguments" do
    File.join("a", "b", "c", "d").should == "a/b/c/d"
  end

  platform_is :windows do
    it "joins parts using File::ALT_SEPARATOR on windows" do
      File.join("C:\\", 'windows').should == "C:\\windows"
      File.join("\\\\", "usr").should == "\\\\usr"
    end
  end

  it "flattens nested arrays" do
    File.join(["a", "b", "c"]).should == "a/b/c"
    File.join(["a", ["b", ["c"]]]).should == "a/b/c"
  end

  it "inserts the separator in between empty strings and arrays" do
    File.join("").should == ""
    File.join("", "").should == "/"
    File.join(["", ""]).should == "/"
    File.join("a", "").should == "a/"
    File.join("", "a").should == "/a"

    File.join([]).should == ""
    File.join([], []).should == "/"
    File.join([[], []]).should == "/"
    File.join("a", []).should == "a/"
    File.join([], "a").should == "/a"
  end

  it "handles leading parts edge cases" do
    File.join("/bin")     .should == "/bin"
    File.join("", "bin")  .should == "/bin"
    File.join("/", "bin") .should == "/bin"
    File.join("/", "/bin").should == "/bin"
  end

  it "handles trailing parts edge cases" do
    File.join("bin", "")  .should == "bin/"
    File.join("bin/")     .should == "bin/"
    File.join("bin/", "") .should == "bin/"
    File.join("bin", "/") .should == "bin/"
    File.join("bin/", "/").should == "bin/"
  end

  it "handles middle parts edge cases" do
    File.join("usr",   "", "bin") .should == "usr/bin"
    File.join("usr/",  "", "bin") .should == "usr/bin"
    File.join("usr",   "", "/bin").should == "usr/bin"
    File.join("usr/",  "", "/bin").should == "usr/bin"
  end

  it "gives priority to existing separators in the rightmost argument" do
    File.join("usr/",   "bin")   .should == "usr/bin"
    File.join("usr/",   "/bin")  .should == "usr/bin"
    File.join("usr//",  "/bin")  .should == "usr/bin"
    File.join("usr//",  "//bin") .should == "usr//bin"
    File.join("usr//",  "///bin").should == "usr///bin"
    File.join("usr///", "//bin") .should == "usr//bin"
  end

  # TODO: See MRI svn r23306. Add patchlevel when there is a release.
  ruby_bug "redmine #1418", "1.8.8" do
    it "raises an ArgumentError if passed a recursive array" do
      a = ["a"]
      a << a
      lambda { File.join a }.should raise_error(ArgumentError)
    end
  end

  it "doesn't remove File::SEPARATOR from the middle of arguments" do
    path = File.join "file://usr", "bin"
    path.should == "file://usr/bin"
  end

  it "raises a TypeError exception when args are nil" do
    lambda { File.join nil }.should raise_error(TypeError)
  end

  it "calls #to_str" do
    lambda { File.join(mock('x')) }.should raise_error(TypeError)

    bin = mock("bin")
    bin.should_receive(:to_str).exactly(:twice).and_return("bin")
    File.join(bin).should == "bin"
    File.join("usr", bin).should == "usr/bin"
  end

  it "doesn't mutate the object when calling #to_str" do
    usr = mock("usr")
    str = "usr"
    usr.should_receive(:to_str).and_return(str)
    File.join(usr, "bin").should == "usr/bin"
    str.should == "usr"
  end

  ruby_version_is "1.9" do
    it "calls #to_path" do
      lambda { File.join(mock('x')) }.should raise_error(TypeError)

      bin = mock("bin")
      bin.should_receive(:to_path).exactly(:twice).and_return("bin")
      File.join(bin).should == "bin"
      File.join("usr", bin).should == "usr/bin"
    end
  end
end
