require 'rspec'

describe 'time add precision' do
  it 'handles BC time correctly' do
    if RUBY_VERSION =~ /1\.8/
      time = Time.utc(-1, 1, 1, 0, 0, 0)
      (time + -1).should eq(Time.utc(-2, 12, 31, 23, 59, 59))
    else
      time = Time.new(0)
      (time + -1).should eq(Time.new(-1, 12, 31, 23, 59, 59))
    end
  end
end

describe 'time minus precision' do
  it 'handles BC time correctly' do
    if RUBY_VERSION =~ /1\.8/
      time = Time.utc(-1, 1, 1, 0, 0, 0)
      other_time = Time.utc(2012, 5, 23, 12, 0, 0)
      (time - other_time).should eq(-63536529600.0)
    else
      time = Time.new(0)
      other_time = Time.new(2012, 5, 23, 12, 0, 0)
      (time - other_time).should eq(-63504988500.0)
    end
  end
end