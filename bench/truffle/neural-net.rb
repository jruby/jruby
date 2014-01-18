# Copyright (c) Alex Gaynor and individual contributors.
# All rights reserved.

# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:

#     1. Redistributions of source code must retain the above copyright notice,
#        this list of conditions and the following disclaimer.

#     2. Redistributions in binary form must reproduce the above copyright
#        notice, this list of conditions and the following disclaimer in the
#        documentation and/or other materials provided with the distribution.

#     3. Neither the name of topaz nor the names of its contributors may be used
#        to endorse or promote products derived from this software without
#        specific prior written permission.

# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Synapse
  attr_accessor :weight, :prev_weight
  attr_accessor :source_neuron, :dest_neuron

  def initialize(source_neuron, dest_neuron, prng)
    self.source_neuron = source_neuron
    self.dest_neuron = dest_neuron
    self.prev_weight = self.weight = prng.rand * 2 - 1
  end
end

class Neuron

  LEARNING_RATE = 1.0
  MOMENTUM = 0.3

  attr_accessor :synapses_in, :synapses_out
  attr_accessor :threshold, :prev_threshold, :error
  attr_accessor :output

  def initialize(prng)
    self.prev_threshold = self.threshold = prng.rand * 2 - 1
    self.synapses_in = []
    self.synapses_out = []
  end

  def calculate_output
    activation = synapses_in.inject(0.0) do |sum, synapse|
      sum + synapse.weight * synapse.source_neuron.output
    end
    activation -= threshold

    self.output = 1.0 / (1.0 + Math.exp(-activation))
  end

  def derivative
    output * (1 - output)
  end

  def output_train(rate, target)
    self.error = (target - output) * derivative
    update_weights(rate)
  end

  def hidden_train(rate)
    self.error = synapses_out.inject(0.0) do |sum, synapse|
      sum + synapse.prev_weight * synapse.dest_neuron.error
    end * derivative
    update_weights(rate)
  end

  def update_weights(rate)
    synapses_in.each do |synapse|
      temp_weight = synapse.weight
      synapse.weight += (rate * LEARNING_RATE * error * synapse.source_neuron.output) + (MOMENTUM * ( synapse.weight - synapse.prev_weight))
      synapse.prev_weight = temp_weight
    end
    temp_threshold = threshold
    self.threshold += (rate * LEARNING_RATE * error * -1) + (MOMENTUM * (threshold - prev_threshold))
    self.prev_threshold = temp_threshold
  end
end

class NeuralNetwork
  attr_accessor :prng

  def initialize(inputs, hidden, outputs)
    self.prng = DeterministicRandom.new

    @input_layer = (1..inputs).map do
        Neuron.new(prng)
    end
    @hidden_layer = (1..hidden).map do
        Neuron.new(prng)
    end
    @output_layer = (1..outputs).map do
        Neuron.new(prng)
    end

    @input_layer.product(@hidden_layer).each do |source, dest|
      synapse = Synapse.new(source, dest, prng)
      source.synapses_out << synapse
      dest.synapses_in << synapse
    end
    @hidden_layer.product(@output_layer).each do |source, dest|
      synapse = Synapse.new(source, dest, prng)
      source.synapses_out << synapse
      dest.synapses_in << synapse
    end
  end

  def train(inputs, targets)
    feed_forward(inputs)

    @output_layer.zip(targets).each do |neuron, target|
      neuron.output_train(0.3, target)
    end
    @hidden_layer.each do |neuron|
        neuron.hidden_train(0.3)
    end
  end

  def feed_forward(inputs)
    @input_layer.zip(inputs).each do |neuron, input|
      neuron.output = input
    end
    @hidden_layer.each do|neuron|
        neuron.calculate_output
    end
    @output_layer.each do |neuron|
        neuron.calculate_output
    end
  end

  def current_outputs
    @output_layer.map do |neuron|
        neuron.output
    end
  end

end

def run(n)
  xor = NeuralNetwork.new(2, 10, 1)

  n.times do
    xor.train([0, 0], [0])
    xor.train([1, 0], [1])
    xor.train([0, 1], [1])
    xor.train([1, 1], [0])
  end

  checksum = 0

  n.times do
    xor.feed_forward([0, 0])
    checksum += xor.current_outputs[0]
    xor.feed_forward([0, 1])
    checksum += xor.current_outputs[0]
    xor.feed_forward([1, 0])
    checksum += xor.current_outputs[0]
    xor.feed_forward([1, 1])
    checksum += xor.current_outputs[0]
  end

  checksum
end

def warmup
  100000.times do
    run(1)
  end
end

def sample
  run(10000) - 20018.21456752439 < 0.00001
end

def name
  return "topaz-neural-net"
end
