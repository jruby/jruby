require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Class.inherited" do

  before :each do
    ::CoreClassSpecs::Record.called(nil)
  end

  it "is invoked with the child Class when self is subclassed" do
    begin
      top = Class.new do
        def self.inherited(cls)
          $child_class = cls
        end
      end

      child = Class.new(top)
      $child_class.should == child

      other_child = Class.new(top)
      $child_class.should == other_child
    ensure
      $child_class = nil
    end
  end

  it "is invoked only once per subclass" do
    expected = [
      [CoreClassSpecs::Inherited::A, CoreClassSpecs::Inherited::B],
      [CoreClassSpecs::Inherited::B, CoreClassSpecs::Inherited::C],
    ]

    CoreClassSpecs::Inherited::A::SUBCLASSES.should == expected
  end

  it "is called when marked as a private class method" do
    CoreClassSpecs::A.private_class_method :inherited
    CoreClassSpecs::Record.called?.should == nil
    module ::CoreClassSpecs; class B < A; end; end
    ::CoreClassSpecs::Record.called?.should == ::CoreClassSpecs::B
  end

  it "is called when marked as a protected class method" do
    class << ::CoreClassSpecs::A
      protected :inherited
    end
    ::CoreClassSpecs::Record.called?.should == nil
    module ::CoreClassSpecs; class C < A; end; end
    ::CoreClassSpecs::Record.called?.should == ::CoreClassSpecs::C
  end

  it "is called when marked as a public class method" do
    ::CoreClassSpecs::A.public_class_method :inherited
    ::CoreClassSpecs::Record.called?.should == nil
    module ::CoreClassSpecs; class D < A; end; end
    ::CoreClassSpecs::Record.called?.should == ::CoreClassSpecs::D
  end

  it "is called by super from a method provided by an included module" do
    ::CoreClassSpecs::Record.called?.should == nil
    module ::CoreClassSpecs; class E < F; end; end
    ::CoreClassSpecs::Record.called?.should == ::CoreClassSpecs::E
  end

  it "is called by super even when marked as a private class method" do
    ::CoreClassSpecs::Record.called?.should == nil
    ::CoreClassSpecs::H.private_class_method :inherited
    module ::CoreClassSpecs; class I < H; end; end
    ::CoreClassSpecs::Record.called?.should == ::CoreClassSpecs::I
  end

  it "will be invoked by child class regardless of visibility" do
    top = Class.new do
      class << self
        def inherited(cls); end
      end
    end

    class << top; private :inherited; end
    lambda { Class.new(top) }.should_not raise_error

    class << top; protected :inherited; end
    lambda { Class.new(top) }.should_not raise_error
  end

end

