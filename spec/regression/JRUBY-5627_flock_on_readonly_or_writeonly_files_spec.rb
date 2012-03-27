require 'rspec'

# JRUBY-5627 and the concern about changing an exclusive lock to a shared
# lock trumps JRUBY-1214's desire to emulate MRI behavior on platforms
# where you can lock read-only streams exclusively. This behavior varies
# across platforms, for one, and we also should never change an exclusive
# lock to a shared lock. So we reverted the fix, added an EBADF error to
# simulate the behavior on stricter platforms, and did the reverse logic
# for LOCK_SH below.
describe 'JRUBY-5627' do
  before :each do
    @filename = '__lock_test_2_'
    File.open(@filename, "w+") { }
  end
  
  after :each do
    File.delete(@filename)
  end
  
  describe 'a file opened only for read' do
    it 'raises EBADF when exclusively locked' do
      File.open(@filename, "r") do |file|
        begin
          proc {
            file.flock(File::LOCK_EX)
            }.should raise_error(Errno::EBADF)
        ensure
          file.flock(File::LOCK_UN)
        end
      end
    end
    
  describe 'a file opened only for write' do
    it 'raises EBADF when shared locked' do
      File.open(@filename, "w") do |file|
        begin
          proc {
            file.flock(File::LOCK_SH)
            }.should raise_error(Errno::EBADF)
          ensure
            file.flock(File::LOCK_UN)
          end
        end
      end
    end
  end
end
