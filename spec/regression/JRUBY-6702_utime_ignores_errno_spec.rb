require 'rspec'

describe 'File#utime' do
  it 'should raise appropriate errno on failure' do
    # no permission
    if File.exist?('/etc') && Process.uid != 0
      expect do
        File.utime(0, 0, '/etc')
      end.to raise_error(Errno::EPERM)
    end

    # does not exist
    expect do
      File.utime(0, 0, '/some_crazy_path')
    end.to raise_error(Errno::ENOENT)
  end
end
