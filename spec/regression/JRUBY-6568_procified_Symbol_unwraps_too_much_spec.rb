require 'rspec'

describe 'A procified Symbol' do
  it 'receives the array when yielded an array' do
    obj = Object.new
    def obj.yield_array0
      yield([])
    end
    def obj.yield_array1
      yield([0])
    end
    def obj.yield_array2
      yield([0, 1])
    end
    def obj.yield_array3
      yield([0, 1, 2])
    end
    def obj.yield_array4
      yield([0, 1, 2, 3])
    end
    def obj.yield_array_and_arg
      yield([10], 0)
    end

    obj.yield_array0(&:inspect).should == [].inspect
    obj.yield_array1(&:inspect).should == [0].inspect
    obj.yield_array2(&:inspect).should == [0,1].inspect
    obj.yield_array3(&:inspect).should == [0,1,2].inspect
    obj.yield_array4(&:inspect).should == [0,1,2,3].inspect

    obj.yield_array_and_arg(&:[]).should == 10
  end
end