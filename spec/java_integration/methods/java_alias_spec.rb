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
    expect do
      class java::util::ArrayList
        java_alias :blah, :toString
      end
    end.not_to raise_error
  end

  it "allows wiring a no-arg method with name plus empty array" do
    expect do
      class java::util::ArrayList
        java_alias :blah, :toString, []
      end
    end.not_to raise_error
  end
  
  it "allows calling a specific overload" do
    @list.add_int_object 0, 'foo'
    expect(@list.to_s).to eq("[foo]")
  end
  
  it "raises NameError if the method can't be found" do
    expect do
      class java::util::ArrayList
        java_alias :blah, :foobar
      end
    end.to raise_error(NameError)
  end
  
  it "raises ArgumentError if type count doesn't match arg count" do
    expect do
      @list.add_int_object 0, 'foo', 'bar'
    end.to raise_error(ArgumentError)
  end
end