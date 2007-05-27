require File.dirname(__FILE__) + '/../../spec_helper'

context "A ruby environment" do
  class Foo
    def no_args
    end
    def args(x)
    end
  end
  
  class Bar < Foo
    def no_args
      defined?(super)
    end
    def args
      defined?( super() )
    end
  end
  
  specify "should return true when defined?(exit) is sent" do
    !!defined?(exit).should == true
  end

  specify "should return true when defined?(Kernel.puts) is sent (attribute)" do
    !!defined?(Kernel.puts).should == true
  end

  specify "should return false when defined?(DoesNotExist.puts) is sent" do
    !!defined?(DoesNotExist.puts).should == false
  end

  specify "should return true when defined?(x = 2) is sent" do
    !!defined?(x = 2).should == true
  end

  specify "should return true when defined?(Object) is sent" do
    !!defined?(Object).should == true
  end

  specify "should return true when @@x = 1; defined?(@@x) is sent" do
    @@x = 1
    !!defined?(@@x).should == true
  end

  specify "should return true when @x = 1; defined?(@x) is sent" do
    @x = 1
    !!defined?(@x).should == true
  end

  specify "should return true when $x = 1; defined?($x) is sent" do
    $x = 1
    !!defined?($x).should == true
  end

  specify "should return true when x = 1; defined?(x) is sent" do
    x = 1
    !!defined?(x).should == true
  end

  specify "should return true when defined? is called on a block var" do
    block = Proc.new { |x| defined?(x) }
    !!(block.call(1)).should == true
  end

  specify "should return true when defined?('foo = bar') is sent" do
    !!defined?('foo = bar').should == true
  end

  specify "should return true when defined?(self) is sent" do
    !!defined?(self).should == true
  end

  specify "should return true when defined?(nil) is sent" do
    !!defined?(nil).should == true
  end

  specify "should return true when defined?(true) is sent" do
    !!defined?(true).should == true
  end

  specify "should return true when defined?(false) is sent" do
    !!defined?(false).should == true
  end

  specify "should return false when defined?(x) is sent" do
    !!defined?(x).should == false
  end

  specify "should return true when Bar#no_args uses defined?" do
    (!!Bar.new.no_args).should == true
  end

  specify "should return true when Bar#args uses defined?" do
    (!!Bar.new.args).should == true
  end
end

