require 'rspec'

# JRuby has separate codepaths for 1 char and >1 char.
describe "A Range of strings >1 char long" do
  it "works" do
    expect(('01'..'10').include?('01')).to be true
    expect(('01'..'10').include?('02')).to be true
    expect(('01'..'10').include?('10')).to be true
    expect(('01'..'10').include?('test')).to be false
  end
end
