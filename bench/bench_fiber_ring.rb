# Adapted from http://people.equars.com/2008/5/22/ruby-fiber-ring-benchmark

require 'benchmark'

class Ring
   attr_reader :id
   attr_accessor :attach

   def initialize(id)
      @id = id
      @fiber = Fiber.new do
         pass_message
      end
   end

   def |(other)
      other.attach = self if !other.nil?
      other
   end

   def resume
      @fiber.resume
    end

   def pass_message
      while message = message_in
         message_out(message)
      end
   end

   def message_in
      @attach.resume if !@attach.nil?
   end

   def message_out(message)
      Fiber.yield(message)
   end

end

class RingStart < Ring
   attr_accessor :message
   def initialize(n, m, message)
      @m = m
      @message = message
      super(n)
   end

   def pass_message
      loop { message_out(@message) }
   end

end


def create_chain_r(i, chain)
   # recursive version
   return chain if i<=0
   r = chain.nil? ? Ring.new(i) :  chain | Ring.new(i)
   create_chain(i-1, r)
end

def create_chain(n, chain)
   # loop version
   # needed to avoid stack overflow for high n
   n.downto(0) {
      chain = chain | Ring.new(n)
   }
   chain
end

tms = (ARGV[0] || 5).to_i
n = (ARGV[1] || 10000).to_i
m = (ARGV[2] || 100).to_i
mess = ARGV[3] || :hello

tms.times do
  puts "#{n} fibers / #{m} passes: " + Benchmark.measure {
    ringu = RingStart.new(0, m, mess)
    chain = create_chain(n, ringu)
    m.times { ringu.message = chain.resume }
  }.to_s
end
