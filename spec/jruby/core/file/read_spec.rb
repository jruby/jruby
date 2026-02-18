#https://github.com/jruby/jruby/issues/1726
describe 'File#read' do
  it 'raises an Errno::ENOENT when the filepath is an empty string' do
    expect { File.read("") }.to raise_error(Errno::ENOENT)
  end
end
