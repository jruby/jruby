require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

# DO NOT PUT ANYTHING ABOVE THIS
describe "Kernel#caller" do
  before :each do
    def a(skip)
      caller(skip)
    end
    def b(skip)
      a(skip)
    end
    def c(skip)
      b(skip)
    end
  end

  it "is a private method" do
    Kernel.should have_private_instance_method(:caller)
  end

  it "returns the current call stack" do
    stack = c 0
    stack[0].should =~ /caller_spec.rb.*?8.*?`a'/
    stack[1].should =~ /caller_spec.rb.*?11.*?`b'/
    stack[2].should =~ /caller_spec.rb.*?14.*?`c'/
  end

  it "omits a number of frames corresponding to the parameter" do
    c(0)[1..-1].should == c(1)
    c(0)[2..-1].should == c(2)
    c(0)[3..-1].should == c(3)
  end

  it "defaults to omitting one frame" do
    caller.should == caller(1)
  end

  # The contents of the array returned by #caller depends on whether
  # the call is made from an instance_eval block or a <block>#call.
  # We purposely do not spec what happens if you request to omit
  # more entries than exist in the array returned.
end

describe "Kernel#caller in a Proc or eval" do
  ruby_version_is ""..."1.9" do
    it "returns the definition trace of a block when evaluated in a Proc binding" do
      stack = CallerFixture.caller_of(CallerFixture.block)
      stack[0].should =~ /caller_fixture1\.rb:4/
      stack[1].should =~ /caller_fixture1\.rb:4:in `.+'/
    end

    it "returns the definition trace of a Proc" do
      stack = CallerFixture.caller_of(CallerFixture.example_proc)
      stack[0].should =~ /caller_fixture1\.rb:14:in `example_proc'/
      stack[1].should =~ /caller_fixture1\.rb:14/
    end

    it "returns the correct caller line from a called Proc" do
      stack = CallerFixture.entry_point.call
      stack[0].should =~ /caller_fixture1\.rb:31:in `(block in )?third'/
      stack[1].should =~ /caller_spec\.rb:60/
    end

    it "returns the correct definition line for a complex Proc trace" do
      stack = CallerFixture.caller_of(CallerFixture.entry_point)
      stack[0].should =~ /caller_fixture1\.rb:29:in `third'/
      ruby_bug("http://redmine.ruby-lang.org/issues/show/146", "1.8.7") do
        stack[1].should =~ /caller_fixture1\.rb:25:in `second'/
      end
    end

    it "begins with (eval) for caller(0) in eval" do
      stack = CallerFixture.eval_caller(0)
      stack[0].should == "(eval):1:in `eval_caller'"
      stack[1].should =~ /caller_spec\.rb:74/
    end

    it "begins with the eval's sender's sender for caller(1) in eval" do
      stack = CallerFixture.eval_caller(1)
      stack[0].should =~ /caller_spec\.rb:80/
    end

    it "shows the current line in the calling block twice when evaled" do
      stack = CallerFixture.eval_caller(0)
      stack[0].should == "(eval):1:in `eval_caller'"
      stack[1].should =~/caller_spec\.rb:85/
      stack[2].should =~/caller_fixture2\.rb:23/
      stack[3].should =~/caller_spec\.rb:85/
    end
  end

  ruby_version_is "1.9" do
    it "returns the definition trace of a block when evaluated in a Proc binding" do
      stack = CallerFixture.caller_of(CallerFixture.block)
      stack[0].should =~ /caller_fixture1\.rb:4:in `<top \(required\)>'/
      stack[1].should =~ /caller_fixture2\.rb:18:in `eval'/
      stack[2].should =~ /caller_fixture2\.rb:18:in `caller_of'/
      stack[3].should =~ /caller_spec\.rb:95:in `block \(3 levels\) in <top \(required\)>'/
    end

    it "returns the definition trace of a Proc" do
      stack = CallerFixture.caller_of(CallerFixture.example_proc)
      stack[0].should =~ /caller_fixture1\.rb:14:in `example_proc'/
      stack[1].should =~ /caller_fixture2\.rb:18:in `eval'/
      stack[2].should =~ /caller_fixture2\.rb:18:in `caller_of'/
      stack[3].should =~ /caller_spec\.rb:103:in `block \(3 levels\) in <top \(required\)>'/
    end

    it "returns the correct caller line from a called Proc" do
      stack = CallerFixture.entry_point.call
      stack[0].should =~ /caller_fixture1\.rb:31:in `block in third'/
      stack[1].should =~ /caller_spec\.rb:111:in `call'/
      stack[2].should =~ /caller_spec\.rb:111:in `block \(3 levels\) in <top \(required\)>'/
    end

    # On 1.8 this expectation is marred by bug #146. I don't understand 1.9's
    # output to ascertain whether the same bug occurs here, and if so what is
    # the correct behaviour
    it "returns the correct definition line for a complex Proc trace" do
      stack = CallerFixture.caller_of(CallerFixture.entry_point)
      stack[0].should =~ /caller_fixture1\.rb:29:in `third'/
      stack[1].should =~ /caller_fixture2\.rb:18:in `eval'/
      stack[2].should =~ /caller_fixture2\.rb:18:in `caller_of'/
      stack[3].should =~ /caller_spec.rb:121:in `block \(3 levels\) in <top \(required\)>'/
    end

    it "begins with (eval) for caller(0) in eval" do
      stack = CallerFixture.eval_caller(0)
      stack[0].should == "(eval):1:in `eval_caller'"
      stack[1].should =~ /caller_fixture2\.rb:23:in `eval'/
      stack[2].should =~ /caller_fixture2\.rb:23:in `eval_caller'/
      stack[3].should =~ /caller_spec\.rb:129:in `block \(3 levels\) in <top \(required\)>'/
    end

    it "shows the current line in the calling block twice when evaled" do
      stack = CallerFixture.eval_caller(0); line = __LINE__
      stack[0].should == "(eval):1:in `eval_caller'"
      stack[1].should =~/caller_fixture2\.rb:23:in `eval'/
      stack[2].should =~/caller_fixture2\.rb:23:in `eval_caller'/
      stack[3].should =~/caller_spec\.rb:#{line}:in `block \(3 levels\) in <top \(required\)>/
    end
  end

  ruby_version_is "1.9"..."2.0" do
    it "begins with the eval's sender's sender for caller(4) in eval" do
      stack = CallerFixture.eval_caller(4); line = __LINE__
      stack[0].should =~ /caller_spec\.rb:#{line}:in `block \(3 levels\) in <top \(required\)>'/
    end
  end

  ruby_version_is "2.0" do
    it "begins with the eval's sender's sender for caller(4) in eval" do
      stack = CallerFixture.eval_caller(4)
      stack[0].should =~ /\.rb:\d+:in `instance_eval'/ # mspec.rb
    end
  end

  it "it returns one frame for new and one frame for initialize when creating objects" do
    stack = CallerFixture::InitializeRecorder.new(0).caller_on_initialize
    stack[0].should =~ /initialize/
    stack[1].should =~ /new/
  end
end

describe "Kernel.caller" do
  ruby_bug("redmine:3011", "1.8.7") do
    it "returns one entry per call, even for recursive methods" do
      two   = CallerSpecs::recurse(2)
      three = CallerSpecs::recurse(3)
      (three.size - two.size).should == 1
    end
  end
end
