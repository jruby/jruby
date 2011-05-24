##
# Channel is a FIFO, thread-aware value passing class that can hold any number
# of objects similar to Queue.  Use #send to add objects to the channel and
# #receive to remove objects from the channel.
#
# If Channel#receive is called on an empty channel, the VM puts the current
# Thread (t1) to sleep.  At some future time, when Channel#send is called on
# that same thread, the VM wakes up t1 and the value passed to #send is
# returned.  This allows us to implement all manner of Thread semantics, such
# as Mutex.
#
# Channel is used heavily by Scheduler, to allow ruby code to interact with
# the outside world in a thread aware manner.

module Rubinius
  class Channel

    ##
    # Returns nil if nothing is waiting, or a List object which contains all
    # Thread objects waiting on this Channel.

    attr_reader :waiting

    ##
    # Returns nil if there are no values, otherwise a List object containing all
    # values the Channel contains.

    attr_reader :value

    ##
    # Creates a new Channel and registers it with the VM.

    def self.new
      Ruby.primitive :channel_new
      raise PrimitiveFailure, "Channel.new primitive failed"
    end

    # We must be sure a Channel is always created properly, so handle
    # this the same as new.
    def self.allocate
      Ruby.primitive :channel_new
      raise PrimitiveFailure, "Channel.new primitive failed"
    end

    ##
    # Puts +obj+ in the Channel.  If there are waiting threads the first thread
    # will be woken up and handed +obj+.

    def send(obj)
      Ruby.primitive :channel_send
      raise PrimitiveFailure, "Channel#send primitive failed"
    end

    alias_method :<<, :send

    ##
    # Removes and returns the first value from the Channel.  If the channel
    # is empty, Thread.current is put to sleep until #send is called.

    def receive
      Ruby.primitive :channel_receive
      raise PrimitiveFailure, "Channel#receive primitive failed"
    end

    def receive_timeout(duration)
      Ruby.primitive :channel_receive_timeout
      raise PrimitiveFailure, "Channel#receive_timeout primitive failed"
    end

    def try_receive
      Ruby.primitive :channel_try_receive
      raise PrimitiveFailure, "Channel#try_receive primitive failed"
    end

    ##
    # Converts +obj+ into a Channel using #to_channel.

    def self.convert_to_channel(obj)
      return obj if Channel === obj
      begin
        o2 = obj.to_channel
        unless Channel === o2
          raise ArgumentError, "to_channel on #{obj.inspect} did not return a Channel"
        end
        return o2
      rescue NoMethodError
         raise ArgumentError, "Unable to convert #{obj.inspect} into a channel"
      end
    end

    ##
    # Legacy API. To be removed.

    def self.receive(obj) # :nodoc:
      return convert_to_channel(obj).receive
    end

    ##
    # API compliance, returns self.

    def to_channel
      self
    end

  end
end
