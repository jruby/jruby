require 'rspec'

describe 'Date#inspect' do
  subject { Time.new(2013,4,22,14,18,05,'-05:00') }

  it 'returns correct offset' do
    expect(subject.inspect).to eq("2013-04-22 14:18:05 -0500")
  end

end
