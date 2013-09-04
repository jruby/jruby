# A Fiber library in pure Ruby using Thread and SizedQueue

require 'weakref'
require 'thread'

class Fiber
  attr_accessor :thread, :data
  FIBER_KEY = :__fiber__
  ROOT_FIBER_KEY = :__root_fiber__
  FIBER_THREAD_KEY = :__fiber_thread__
  
  class FiberData
    attr_accessor :queue, :prev, :parent, :weak_fiber
    
    def initialize(queue, parent, fiber)
      @queue, @parent = queue, parent
      
      @weak_fiber = WeakRef.new(fiber)
    end
  end
  
  FINALIZER = proc {|data, thread, id| data.shutdown; thread.raise Interrupt}
  
  ##
  # Create a new Fiber with the given block as its body.
  #
  def initialize(root = false, &block)
    if root
      @data = FiberData.new(SizedQueue.new(1), nil, self)
      @thread = Thread.current
      Thread.current[FIBER_KEY] = WeakRef.new(self)
    elsif !block && !root
      raise ArgumentError, "tried to create Proc object without block"
    else
      @data = FiberData.new(SizedQueue.new(1), Fiber.__fiber_thread__(Thread.current), self)
      current_fiber = Fiber.__current__

      @thread = Fiber.__create_thread__(@data, current_fiber.data.queue, block)
      
      # set up finalizer to shut things down
      ObjectSpace.define_finalizer self,  Fiber::FINALIZER.curry[@data,@thread]

      current_fiber.data.queue.pop # wait for ready
    end
  end
  
  ##
  # Create the thread to pump this fiber. This is done in a class method to
  # avoid holding a hard reference to the Fiber object itself. When the Fiber
  # object is GCable, it will attempt to shutdown its queue and kill this
  # thread.
  def self.__create_thread__(data, queue, block)
    Thread.new do
      Thread.current[FIBER_KEY] = data.weak_fiber
      Thread.current[FIBER_THREAD_KEY] = data.parent
      
      queue.push nil # indicate we're ready
      
      init = data.queue.pop
      begin
        data.prev.data.queue.push block.call(*init)
      rescue Exception => e
        data.prev.thread.raise e if data.prev
      end
    end
  end
  
  ##
  # Resume execution of this fiber and wait for it (or a fiber to which it
  # transfers control (to yield back to us).
  #
  def resume(*val)
    raise FiberError, "double resume" if data.prev
    
    raise FiberError, "dead fiber called" if !__alive__
    
    current_fiber = Fiber.__current__
    
    val = val[0] if val.size == 1
    
    return val if self == current_fiber
    
    raise FiberError, "fiber called across threads" unless data.parent == Fiber.__fiber_thread__(current_fiber.thread)
    
    data.prev = current_fiber
    begin
      data.queue.push(val)
      result = current_fiber.data.queue.pop
      return result
    ensure
      data.prev = nil
    end
  end
  
  alias transfer resume
  
  ##
  # Yield control from this Fiber back to the Fiber or Thread that resumed it.
  #
  def self.yield(val = nil)
    current_fiber = Fiber.__current__
    
    raise FiberError, "can't yield from root fiber" unless current_fiber.data.parent
    
    prev_fiber = current_fiber.data.prev
    
    prev_fiber.data.queue.push(val)
    result = current_fiber.data.queue.pop
    return result
  end
  
  ##
  # Return true if this Fiber is alive; false otherwise.
  #
  def __alive__
    @thread && @thread.alive?
  end
  private :__alive__
  
  ##
  # Get the Fiber for the current thread. If this is a root thread (i.e. not a
  # Fiber thread) then the returned Fiber will be a root fiber.
  #
  def self.__current__
    weak_fiber = Thread.current[FIBER_KEY]
    
    if !weak_fiber || !(fiber = weak_fiber.__getobj__)
      root_fiber = Fiber.new(true)
      Thread.current[FIBER_KEY] = WeakRef.new(root_fiber)
      Thread.current[ROOT_FIBER_KEY] = root_fiber
      return root_fiber
    else
        return fiber
    end
  end
  
  ##
  # Get the root thread that goes with the given thread. If the given thread is
  # pumping a fiber, then use its root thread.
  #
  def self.__fiber_thread__(thread)
    thread[FIBER_THREAD_KEY] || thread
  end
end