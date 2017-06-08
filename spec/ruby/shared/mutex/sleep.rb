describe :mutex_sleep, :shared => true do
  it "sleeps timeout seconds" do
    m = Mutex.new
    m.lock
    expect(m.sleep(1)).to eq(1)
  end

  it "raises ThreadError if mutex was not locked by the current thread" do
    m = Mutex.new
    expect{m.sleep}.to raise_error(ThreadError)
  end

  it "checkes when the thread is next woken up, it will attempt to reacquire the lock." do
    m = Mutex.new
    m.lock
    m.sleep(1)
    expect {m.locked?}.to be_true
  end	
end	