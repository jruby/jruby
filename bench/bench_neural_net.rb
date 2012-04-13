# from bryanbibat's gist: https://gist.github.com/2348802

class Synapse
  attr_accessor :weight, :prev_weight
  attr_accessor :source_neuron, :dest_neuron

  def initialize(source_neuron, dest_neuron, prng)
    self.source_neuron = source_neuron
    self.dest_neuron = dest_neuron
    self.prev_weight = self.weight = prng.rand(-1.0..1.0)
  end
end

class Neuron

  LEARNING_RATE = 1.0
  MOMENTUM = 0.3

  attr_accessor :synapses_in, :synapses_out
  attr_accessor :threshold, :prev_threshold, :error
  attr_accessor :output

  def initialize(prng)
    self.prev_threshold = self.threshold = prng.rand(-1.0..1.0)
    self.synapses_in = []
    self.synapses_out = []
  end

  def calculate_output
    # calculate output based on the previous layer
    # use logistic function

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
      synapse.weight += (rate * LEARNING_RATE * error * synapse.source_neuron.output) +
        (MOMENTUM * ( synapse.weight - synapse.prev_weight))
      synapse.prev_weight = temp_weight
    end
    temp_threshold = threshold
    self.threshold += (rate * LEARNING_RATE * error * -1) + 
      (MOMENTUM * (threshold - prev_threshold))
    self.prev_threshold = temp_threshold
  end
end

class NeuralNetwork
  attr_accessor :prng

  def initialize(inputs, hidden, outputs)
    self.prng = Random.new

    @input_layer = (1..inputs).map { Neuron.new(prng) }
    @hidden_layer = (1..hidden).map { Neuron.new(prng) }
    @output_layer = (1..outputs).map { Neuron.new(prng) }

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
    @hidden_layer.each { |neuron| neuron.hidden_train(0.3) }
  end

  def feed_forward(inputs)
    @input_layer.zip(inputs).each do |neuron, input|
      neuron.output = input
    end
    @hidden_layer.each { |neuron| neuron.calculate_output }
    @output_layer.each { |neuron| neuron.calculate_output }
  end

  def current_outputs
    @output_layer.map { |neuron| neuron.output }
  end

end

require 'benchmark'

(ARGV[0] || 5).to_i.times do
  x = Benchmark.measure do |x|
    xor = NeuralNetwork.new(2, 10, 1)
  
    10000.times do 
      xor.train([0, 0], [0])
      xor.train([1, 0], [1])
      xor.train([0, 1], [1])
      xor.train([1, 1], [0])
    end
  
    xor.feed_forward([0, 0])
    puts xor.current_outputs
    xor.feed_forward([0, 1])
    puts xor.current_outputs
    xor.feed_forward([1, 0])
    puts xor.current_outputs
    xor.feed_forward([1, 1])
    puts xor.current_outputs
  end
  puts x
end
