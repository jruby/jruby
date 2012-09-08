require 'rspec'

describe 'Enumerable#inject passed a symbol and called as a proc' do
  it 'correctly fills the array' do
    o = Object.new
    o.extend Enumerable
    def o.each(&block)
      [1,2,3,4].each {|x| block.call(x)}
    end

    o.inject([], :<<).should == [1,2,3,4]
  end
end
