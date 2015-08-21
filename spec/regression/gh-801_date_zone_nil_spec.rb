require 'rspec'

describe 'Date#zone' do
  subject { Time.new(2013,4,22,14,18,05,'-05:00') }

  it 'returns nil' do
    expect(subject.zone).to be_nil
  end
end