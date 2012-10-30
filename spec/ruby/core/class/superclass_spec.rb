require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Class#superclass" do
  ruby_version_is ""..."1.9" do
    it "returns the superclass of self" do
      Object.superclass.should == nil
      Class.superclass.should == Module
      Class.new.superclass.should == Object
      Class.new(String).superclass.should == String
      Class.new(Fixnum).superclass.should == Fixnum
    end
  end

  ruby_version_is "1.9" do
    it "returns the superclass of self" do
      BasicObject.superclass.should be_nil
      Object.superclass.should == BasicObject
      Class.superclass.should == Module
      Class.new.superclass.should == Object
      Class.new(String).superclass.should == String
      Class.new(Fixnum).superclass.should == Fixnum
    end
  end

  ruby_bug "redmine:567", "1.8.7" do
    describe "for a singleton class" do
      it "of an object returns the class of the object" do
        a = CoreClassSpecs::A.new
        sc = class << a; self; end
        sc.superclass.should == CoreClassSpecs::A
      end

      it "of a class returns the singleton class of its superclass" do # sorry, can't find a simpler way to express this...
        sc = class << CoreClassSpecs::H; self; end
        sc.superclass.should == class << CoreClassSpecs::A; self; end
      end
    end
  end
end
