require File.expand_path('../spec_helper', __FILE__)
require File.expand_path('../../../core/thread/shared/wakeup', __FILE__)

load_extension("thread")

class Thread
  def self.capi_thread_specs=(t)
    @@capi_thread_specs = t
  end

  def call_capi_rb_thread_wakeup
    @@capi_thread_specs.rb_thread_wakeup(self)
  end
end

describe "C-API Thread function" do
  before :each do
    @t = CApiThreadSpecs.new
    ScratchPad.clear
    Thread.capi_thread_specs = @t
  end

  describe "rb_thread_select" do
    ruby_version_is ""..."2.2" do
      it "returns true if an fd is ready to read" do
        read, write = IO.pipe

        @t.rb_thread_select_fd(read.to_i, 0).should == false
        write << "1"
        @t.rb_thread_select_fd(read.to_i, 0).should == true
      end

      it "does not block all threads" do
        t = Thread.new do
          sleep 0.25
          ScratchPad.record :inner
        end
        Thread.pass while t.status and t.status != "sleep"

        @t.rb_thread_select(500_000)

        t.alive?.should be_false
        ScratchPad.recorded.should == :inner

        t.join
      end
    end

  end

  describe "rb_thread_wait_for" do
    it "sleeps the current thread for the give ammount of time" do
      start = Time.now
      @t.rb_thread_wait_for(0, 100_000)
      (Time.now - start).should be_close(0.1, 0.2)
    end
  end

  describe "rb_thread_alone" do
    it "returns true if there is only one thread" do
      pred = Thread.list.size == 1
      @t.rb_thread_alone.should == pred
    end
  end

  describe "rb_thread_current" do
    it "equals Thread.current" do
      @t.rb_thread_current.should == Thread.current
    end
  end

  describe "rb_thread_local_aref" do
    it "returns the value of a thread-local variable" do
      thr = Thread.current
      sym = :thread_capi_specs_aref
      thr[sym] = 1
      @t.rb_thread_local_aref(thr, sym).should == 1
    end

    it "returns nil if the value has not been set" do
      @t.rb_thread_local_aref(Thread.current, :thread_capi_specs_undefined).should be_nil
    end
  end

  describe "rb_thread_local_aset" do
    it "sets the value of a thread-local variable" do
      thr = Thread.current
      sym = :thread_capi_specs_aset
      @t.rb_thread_local_aset(thr, sym, 2).should == 2
      thr[sym].should == 2
    end
  end

  describe "rb_thread_wakeup" do
    it_behaves_like :thread_wakeup, :call_capi_rb_thread_wakeup
  end

  describe "rb_thread_create" do
    it "creates a new thread" do
      obj = Object.new
      proc = lambda { |x| ScratchPad.record x }
      thr = @t.rb_thread_create(proc, obj)
      thr.should be_kind_of(Thread)
      thr.join
      ScratchPad.recorded.should == obj
    end

    it "handles throwing an exception in the thread" do
      proc = lambda { |x| raise NotImplementedError }
      thr = @t.rb_thread_create(proc, nil)
      thr.should be_kind_of(Thread)

      lambda { thr.join }.should raise_error(NotImplementedError)
    end
  end

end

describe :rb_thread_blocking_region, :shared => true do
  before :each do
    @t = CApiThreadSpecs.new
    ScratchPad.clear
  end

  it "runs a C function with the global lock unlocked" do
    thr = Thread.new do
      @t.send(@method)
    end

    # Wait until it's blocking...
    sleep 1

    # Wake it up, causing the unblock function to be run.
    thr.wakeup

    # Make sure it stopped
    thr.join(1).should_not be_nil

    # And we got a proper value
    thr.value.should be_true
  end
end

describe "C-API Thread function" do
  describe "rb_thread_blocking_region" do
    extended_on :rubinius do
      it_behaves_like :rb_thread_blocking_region, :rb_thread_blocking_region_with_ubf_io
      it_behaves_like :rb_thread_blocking_region, :rb_thread_blocking_region
    end

    ruby_version_is "1.9"..."2.2" do
      it_behaves_like :rb_thread_blocking_region, :rb_thread_blocking_region_with_ubf_io
      it_behaves_like :rb_thread_blocking_region, :rb_thread_blocking_region
    end

    it_behaves_like :rb_thread_blocking_region, :rb_thread_call_without_gvl
    it_behaves_like :rb_thread_blocking_region, :rb_thread_call_without_gvl2
  end
end

