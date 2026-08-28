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

    expect(obj.yield_array0(&:inspect)).to eq([].inspect)
    expect(obj.yield_array1(&:inspect)).to eq([0].inspect)
    expect(obj.yield_array2(&:inspect)).to eq([0,1].inspect)
    expect(obj.yield_array3(&:inspect)).to eq([0,1,2].inspect)
    expect(obj.yield_array4(&:inspect)).to eq([0,1,2,3].inspect)

    expect(obj.yield_array_and_arg(&:[])).to eq(10)
  end
end