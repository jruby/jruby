require 'rspec'

describe 'Array#delete_if' do
  it 'updates the array at each step of the iteration' do
    a = [ 1, 2, 3, 4, 5 ]
    a.delete_if do |e|
      if e <= 2
        expect(a.size).to eq(5)
        expect(a).to match_array([ 1, 2, 3, 4, 5 ])
      end

      if e > 2
        expect(a.size).to eq(4)
        expect(a).to match_array([ 1, 3, 4, 5 ])
      end

      e == 2
    end
  end
end