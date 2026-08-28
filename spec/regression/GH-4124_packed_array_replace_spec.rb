describe '#4124 Packed arrays' do
  it 'replaces properly' do
    # try a range of sizes, since we may expand packed arrays
    arys = 1.upto(10).map {|i| (1..i).to_a }

    arys.each do |ary|
      plus_one = ary.map {|i| i+1}
      ary.replace plus_one

      expect(ary).to eq plus_one
    end
  end
end
