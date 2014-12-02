#https://github.com/jruby/jruby/issues/1726
describe 'File#read' do
  it 'raises an Errno::ENOENT when the filepath is an empty string' do
    lambda { File.read("") }.should raise_error(Errno::ENOENT)
  end
end
