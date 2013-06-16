require 'rspec'

if RUBY_VERSION > '1.9'
  describe 'Date#inspect' do
    subject { Time.new(2013,4,22,14,18,05,'-05:00') }

    it 'returns correct offset' do
      subject.inspect.should == "2013-04-22 04:18:05 -0500"
    end

  end
end