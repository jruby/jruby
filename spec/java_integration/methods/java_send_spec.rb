require 'java'

describe "A Java object's java_send method" do
  before :each do
    @list = java.util.ArrayList.new
    @integer = java.lang.Integer.new(1)
  end
  
  it "works with name only for no-arg methods" do
    m = @list.java_send(:toString).should == "[]"
  end
  
  it "works with a signature" do
    @list.java_send :add, [Java::int, java.lang.Object], 0, 'foo'
    @list.to_s.should == "[foo]"
  end
  
  it "raises NameError if the method can't be found" do
    lambda do
      @list.java_send :foobar
    end.should raise_error(NameError)
    
    lambda do
      @list.java_send :add, [Java::long, java.lang.Object], 0, 'foo'
    end.should raise_error(NameError)
  end
  
  it "raises ArgumentError if type count doesn't match arg count" do
    lambda do
      @list.java_send :add, [Java::int, java.lang.Object], 0, 'foo', 'bar'
    end.should raise_error(ArgumentError)
  end
end