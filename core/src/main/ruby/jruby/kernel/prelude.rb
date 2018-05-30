class Thread
  MUTEX_FOR_THREAD_EXCLUSIVE = Thread::Mutex.new # :nodoc:
  private_constant :MUTEX_FOR_THREAD_EXCLUSIVE

  # call-seq:
  #    Thread.exclusive { block }   => obj
  #
  # Wraps the block in a single, VM-global Mutex.synchronize, returning the
  # value of the block. A thread executing inside the exclusive section will
  # only block other threads which also use the Thread.exclusive mechanism.
  def self.exclusive(&block)
    warn "Thread.exclusive is deprecated, use Thread::Mutex", caller
    MUTEX_FOR_THREAD_EXCLUSIVE.synchronize(&block)
  end
end

# :stopdoc:
class Binding
  def irb
    require 'irb'
    irb
  end

  # suppress redefinition warning
  alias irb irb # :nodoc:
end

module Kernel
  def pp(*objs)
    require 'pp'
    pp(*objs)
  end

  # suppress redefinition warning
  alias pp pp # :nodoc:
end