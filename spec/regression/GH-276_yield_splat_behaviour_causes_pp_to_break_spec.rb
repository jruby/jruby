if RUBY_VERSION >= "1.9"
  require 'rspec'
  
  def call_one
    yield(["a"])
  end
  
  def call_two
    yield(["a","b"])
  end
  
  def call_three
    yield(["a", "b", "c"])
  end
  
  def yield_with_splat(method_name = 'call_two')
    send(method_name) { |*a| yield(*a) }
  end
  
  describe 'yield splat' do
    it 'yields an array when block has only one argument' do
      value = nil
      yield_with_splat("call_one") { |a| value = a }
      value.should == ["a"]
    end
  
    it 'yields an array when block as one argument and passed two' do
      value = nil
      yield_with_splat("call_two") { |a| value = a }
      value.should == ["a", "b"]
    end
  
    it 'yields one value when block has two arguments and passed one' do
      first_value = nil
      second_value = nil
      yield_with_splat("call_one") { |a,b| first_value = a; second_value = b }
      first_value.should == "a"
      second_value.should == nil
    end
  
    it 'yields two values when block has two arguments and passed two' do
      first_value = nil
      second_value = nil
      yield_with_splat { |a,b| first_value = a; second_value = b }
      first_value.should == "a"
      second_value.should == "b"
    end
  
    it 'yields two values when block has two arguments and passed three' do
      first_value = nil
      second_value = nil
      yield_with_splat("call_three") { |a,b| first_value = a; second_value = b }
      first_value.should == "a"
      second_value.should == "b"
    end
  end
end
