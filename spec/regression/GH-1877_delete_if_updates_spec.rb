require 'rspec'

describe 'Array#delete_if' do
  it 'updates the array at each step of the iteration' do
    a = [ 1, 2, 3, 4, 5 ]
    a.delete_if do |e|
      if e <= 2
        a.size.should == 5
        a.should match_array([ 1, 2, 3, 4, 5 ])
      end

      if e > 2
        a.size.should == 4
        a.should match_array([ 1, 3, 4, 5 ])
      end

      e == 2
    end
  end
end