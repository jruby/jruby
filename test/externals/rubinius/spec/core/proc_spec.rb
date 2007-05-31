require File.dirname(__FILE__) + '/../spec_helper'

context "A Proc instance" do
  setup do
    @prc = lambda { "Software is the source, not the binaries" }
  end

  specify "created using &nil should be nil" do
    def a(&prc)
      prc
    end

    a(&nil).should == nil
  end
  
  specify "should be created from an object responding to :to_proc" do
    class Symbol
      def to_proc
        lambda { self }
      end
    end

    def a(&prc)
      prc.call
    end

    a(&:to_s).should == :to_s
  end
  
  specify "should respond to :call" do
    @prc.respond_to?(:call).should == true
  end

  specify "should respond to :to_proc" do
    @prc.respond_to?(:to_proc).should == true
  end

  specify "to_proc should return self" do
    @prc.object_id.should == @prc.to_proc.object_id
  end
  
  specify "should be the same if passed across methods" do
    def a(&prc)
      b(&prc)
    end
    
    def b(&prc)
      c(&prc)
    end
    
    def c(&prc)
      prc.object_id
    end

    a(&@prc).should == @prc.object_id
  end
end

describe Proc do
  it "should support multiple arguments" do
    Proc.new {|*x| x.reverse }.call(1,2,3,4,5).should == [5,4,3,2,1]
  end
end
