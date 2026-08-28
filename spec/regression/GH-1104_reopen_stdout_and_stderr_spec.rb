
require 'rspec'
require 'tempfile'

describe 'reopen $stdout and $stderr' do
  it 'preserve file mode after reopen' do
    [$stdout, $stderr].each do |stream|
      begin
        original_fd = stream.dup # backup fd
        tmp = Tempfile.new(File.basename(__FILE__))
        stream.reopen(tmp, 'r+')
        stream.puts('test')
        stream.rewind
        expect(stream.read(5)).to eq("test\n")
      ensure
        stream.reopen(original_fd) # restore stdout/stderr
        tmp.close!
      end
    end
  end
end
