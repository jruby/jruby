module Kernel
  begin
    class RubySignalHandler
      include Java::sun.misc.SignalHandler
      def initialize(proc)
        @proc = proc
      end
      def handle(sig)
        @proc.call
      end
    end
    def trap(sig, &block)
      raise TypeError, "integer signal numbers not supported" unless sig.kind_of?(String)
      sig = sig.sub(/^SIG(.+)/,'\1')
      signal = Java::sun.misc.Signal
      signal.handle(signal.new(sig), RubySignalHandler.new(block))
    end
  rescue NameError
    def trap(sig, &block)
      raise "trap not supported by this VM"
    end
  end
end