require 'rspec'

describe 'File#utime' do
  it 'should raise appropriate errno on failure' do
    # no permission
    if File.exist?('/etc') && Process.uid != 0
      lambda do
        File.utime(0, 0, '/etc')
      end.should raise_error(Errno::EPERM)
    end

    # does not exist
    lambda do
      File.utime(0, 0, '/some_crazy_path')
    end.should raise_error(Errno::ENOENT)
  end
end
