require 'rbyaml/error'
require 'rbyaml/reader'
require 'rbyaml/scanner'
require 'rbyaml/parser'
require 'rbyaml/composer'
require 'rbyaml/constructor'
require 'rbyaml/detector'

class RbYAMLTester
  include RbYAML::Reader, RbYAML::Scanner, RbYAML::Parser
  
  def initialize(stream)
    initialize_reader(stream)
    initialize_scanner
    initialize_parser
  end
end

class RbYAMLTester2
  include RbYAML::Reader, RbYAML::Scanner, RbYAML::Parser, RbYAML::Composer

  def initialize(stream)
    initialize_reader(stream)
    initialize_scanner
    initialize_parser
    initialize_composer
  end
end

class RbYAMLTester3
  include RbYAML::Reader, RbYAML::Scanner, RbYAML::Parser, RbYAML::Composer, RbYAML::Constructor, RbYAML::Detector

  def initialize(stream)
    initialize_reader(stream)
    initialize_scanner
    initialize_parser
    initialize_composer
    initialize_constructor
  end
end

i=0
begin
  File.open(ARGV.shift) {|f|
    tester = RbYAMLTester3.new(f)
    tester.each_document {|doc|
      puts "#{doc.inspect}"
      #    i += 1
      #    if (i%10000) == 0
      #      puts "token ##{i}"
      #    end
    }
  }
rescue RbYAML::MarkedYAMLError => err
  puts "MarkedYAMLError: #{err}"
end
