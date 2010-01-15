require 'java'

describe "A Java object's java_send method" do
  before :all do
    class java::util::ArrayList
      java_alias :add_int_object, :add, [Java::int, java.lang.Object]
    end
  end
  
  before :each do
    @list = java.util.ArrayList.new
  end

  it "allows wiring a no-arg method with just name" do
    lambda do
      class java::util::ArrayList
        java_alias :blah, :toString
      end
    end.should_not raise_error
  end

  it "allows wiring a no-arg method with name plus empty array" do
    lambda do
      class java::util::ArrayList
        java_alias :blah, :toString, []
      end
    end.should_not raise_error
  end
  
  it "allows calling a specific overload" do
    @list.add_int_object 0, 'foo'
    @list.to_s.should == "[foo]"
  end
  
  it "raises NameError if the method can't be found" do
    lambda do
      class java::util::ArrayList
        java_alias :blah, :foobar
      end
    end.should raise_error(NameError)
  end
  
  it "raises ArgumentError if type count doesn't match arg count" do
    lambda do
      @list.add_int_object 0, 'foo', 'bar'
    end.should raise_error(ArgumentError)
  end
end