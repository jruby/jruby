require 'rspec'

describe 'unmarshalling multiple strings with encodings' do
  # Generate input file via:
  # x = ['a', 'b'].map {|s| s.force_encoding('Shift_JIS')}
  # File.open('marshal-data', 'w') {|f| Marshal.dump(x, f)}
  # Any encoding besides UTF-8 or US-ASCII should trigger the problem

  it "does not throw an encoding error" do
    dump_path = __FILE__.chomp(File.extname(__FILE__)) + ".dump"

    unmarshalled = File.open(dump_path) do |f|
      Marshal.load(f)
    end

    unmarshalled.should include('a')
    unmarshalled.should include('b')
  end
end
