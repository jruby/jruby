require 'jruby/profiler'
require 'json'

module JRuby::Profiler::SpecHelpers
  
  def profile_data
    @profile_data ||= JRuby::Profiler.profile_data
  end
    
  def top_invocation
    @top_invocation ||= profile_data.compute_results
  end
  
  def method_name(inv)
    profile_data.method_name(inv.method_serial_number)
  end
  
  def get_inv(name, inv=top_invocation)
    inv.children.values.detect {|inv| method_name(inv) == name}
  end
  
  def tree(inv, indent = 0)
    puts " "*indent + method_name(inv) + "(#{inv.method_serial_number}@#{inv.recursive_depth}) x#{inv.count}"
    inv.children.values.to_a.each do |c|
      tree(c, indent + 2)
    end
  end

  def graph_output
    data_output JRuby::Profiler::GraphProfilePrinter
  end

  def json_output
    raw_output = data_output(JRuby::Profiler::JsonProfilePrinter)
    JSON.parse(raw_output)
  end

  def flat_output
    data_output JRuby::Profiler::FlatProfilePrinter
  end

  def data_output(printer)
    output_stream = java.io.ByteArrayOutputStream.new
    print_stream = java.io.PrintStream.new(output_stream)
    printer.new(profile_data).printProfile(print_stream)
    output_stream.toString
  end
      
  def line_for(text, method)
    lines = lines_for(text, method)
    if lines.length == 0
      return nil
    elsif lines.length > 1
      raise "multiple lines matching #{method}"
    end
    lines.first
  end
  
  def main_line_for(text, method)
    lines = lines_for(text, method).select {|l| l[:main_row] == true}
    if lines.length == 0
      return nil
    elsif lines.length > 1
      raise "multiple main lines matching #{method}"
    end
    lines.first
  end
  
  def lines_for(text, method)
    lines = text.split("\n").select {|l| l.include?(method)}
    lines.map do |line|
      bits = line.split(" ")
      if bits.length == 7
        {:total_pc => bits[0], :self_pc => bits[1], :total => bits[2].to_f,
          :self => bits[3].to_f, :children => bits[4].to_f, :calls => bits[5].to_i, 
          :name => bits[6].strip, :main_row => true
        }
      elsif bits.length == 5
        {:total => bits[0].to_f,
          :self => bits[1].to_f, :children => bits[2].to_f, :calls => bits[3].to_i, 
          :name => bits[4].strip, :main_row => false
        }
      end
    end
  end
  
  def decode_main_row(line)
    bits = line.split(" ")
    {:total_pc => bits[0], :self_pc => bits[1], :total => bits[2].to_f,
      :self => bits[3].to_f, :children => bits[4].to_f, :calls => bits[5].to_i, 
      :name => bits[6].strip, :parents => nil, :children => []
    }
  end
  
  def decode_parent_or_child(line)
    bits = line.split(" ")
    {:total => bits[0].to_f,
      :self => bits[1].to_f, :children => bits[2].to_f, :calls => bits[3].split("/").map{|b| b.to_i}, 
      :name => bits[4].strip
    }
  end
  
  def decode_graph(text)
    methods = []
    current_method = nil
    parents = []
    text.split("\n").each do |line|
      if line =~ /^\s*-+\s*$/
        current_method = nil
        parents = []
      elsif line =~ /^\s+\d+%/ # main row
        current_method = decode_main_row(line)
        current_method[:parents] = parents
        methods << current_method
        parents = nil
      elsif line =~ /^\s+\d+\.\d+\s/ # parent or child
        if current_method # child
          current_method[:children] << decode_parent_or_child(line)
        else              # parent
          parents << decode_parent_or_child(line)
        end
      end
    end
    methods
  end
end


class ProfilerTest
  
  def wait(t)
    sleep t
  end
  
  def test_instance_method
  end
  
  def self.test_static_method
  end
  
  class << self
    def test_metaclass_method
    end
  end
  
  def level1
    level2
  end
  
  def level2
    level3
    level3
  end
  
  def level3
  end
  
  def recurse(x)
    if x > 0
      recurse(x-1)
    end
  end
  
  def recurse_wait(x, wait)
    if x > 0
      s = Time.now
      true until Time.now - s > wait
      recurse_wait(x-1, wait)
    end
  end
  
  def recurse_and_start_profiling(x)
    if x > 0
      recurse_and_start_profiling(x - 1)
    else
      JRuby::Profiler.start
    end
  end
  
  def start
    JRuby::Profiler.start
    test_instance_method
  end
  
  def stop
    ProfilerTest.test_static_method
    JRuby::Profiler.stop
  end
  
end
