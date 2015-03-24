require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Array#|" do
  it "returns an array of elements that appear in either array (union)" do
    ([] | []).should == []
    ([1, 2] | []).should == [1, 2]
    ([] | [1, 2]).should == [1, 2]
    ([ 1, 2, 3, 4 ] | [ 3, 4, 5 ]).should == [1, 2, 3, 4, 5]
  end

  it "creates an array with no duplicates" do
    ([ 1, 2, 3, 1, 4, 5 ] | [ 1, 3, 4, 5, 3, 6 ]).should == [1, 2, 3, 4, 5, 6]
  end

  it "creates an array with elements in order they are first encountered" do
    ([ 1, 2, 3, 1 ] | [ 1, 3, 4, 5 ]).should == [1, 2, 3, 4, 5]
  end

  it "properly handles recursive arrays" do
    empty = ArraySpecs.empty_recursive_array
    (empty | empty).should == empty

    array = ArraySpecs.recursive_array
    (array | []).should == [1, 'two', 3.0, array]
    ([] | array).should == [1, 'two', 3.0, array]
    (array | array).should == [1, 'two', 3.0, array]
    (array | empty).should == [1, 'two', 3.0, array, empty]
  end

  it "tries to convert the passed argument to an Array using #to_ary" do
    obj = mock('[1,2,3]')
    obj.should_receive(:to_ary).and_return([1, 2, 3])
    ([0] | obj).should == ([0] | [1, 2, 3])
  end

  # MRI follows hashing semantics here, so doesn't actually call eql?/hash for Fixnum/Symbol
  it "acts as if using an intermediate hash to collect values" do
    ([5.0, 4.0] | [5, 4]).should == [5.0, 4.0, 5, 4]
    str = "x"
    ([str] | [str.dup]).should == [str]

    obj1 = mock('1')
    obj2 = mock('2')
    def obj1.hash; 0; end
    def obj2.hash; 0; end
    def obj1.eql? a; true; end
    def obj2.eql? a; true; end

    ([obj1] | [obj2]).should == [obj1]

    def obj1.eql? a; false; end
    def obj2.eql? a; false; end

    ([obj1] | [obj2]).should == [obj1, obj2]
  end

  it "does not return subclass instances for Array subclasses" do
    (ArraySpecs::MyArray[1, 2, 3] | []).should be_an_instance_of(Array)
    (ArraySpecs::MyArray[1, 2, 3] | ArraySpecs::MyArray[1, 2, 3]).should be_an_instance_of(Array)
    ([] | ArraySpecs::MyArray[1, 2, 3]).should be_an_instance_of(Array)
  end

  it "does not call to_ary on array subclasses" do
    ([1, 2] | ArraySpecs::ToAryArray[5, 6]).should == [1, 2, 5, 6]
  end
end
