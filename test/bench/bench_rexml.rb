require 'rexml/document'
require 'benchmark'
require 'rexml/parsers/baseparser'
require 'rexml/parsers/streamparser'

class DummyListener
  def xmldecl(a, b, c); end;
  def text(a); end;
  def tag_start(a, b = nil); end
  def tag_end(a); end
  def comment(a); end
  def cdata(a); end
end

content = File.read('build.xml')

puts "read content from stream, no DOM"
5.times {
  puts Benchmark.measure {
    5.times {
      REXML::Parsers::StreamParser.new(File.open('build.xml'), DummyListener.new).parse
    }
  }
}

puts "read content once, no DOM"
5.times {
  puts Benchmark.measure {
    5.times {
      REXML::Parsers::StreamParser.new(content, DummyListener.new).parse
    }
  }
}

puts "read content from stream, build DOM"
5.times {
  puts Benchmark.measure {
    5.times {
      doc = REXML::Document.new(File.open("build.xml"))
    }
  }
}

puts "read content once, build DOM"
5.times {
  puts Benchmark.measure {
    5.times {
      doc = REXML::Document.new(content)
    }
  }
}

begin
  gem 'jrexml'
  require 'jrexml'

  puts "read content from stream, no DOM, JREXML"
  5.times {
    puts Benchmark.measure {
      5.times {
        REXML::Parsers::StreamParser.new(File.open('build.xml'), DummyListener.new).parse
      }
    }
  }

  puts "read content once, no DOM, JREXML"
  5.times {
    puts Benchmark.measure {
      5.times {
        REXML::Parsers::StreamParser.new(content, DummyListener.new).parse
      }
    }
  }

  puts "read content from stream, build DOM, JREXML"
  5.times {
    puts Benchmark.measure {
      5.times {
        doc = REXML::Document.new(File.open("build.xml"))
      }
    }
  }

  puts "read content once, build DOM, REXML"
  5.times {
    puts Benchmark.measure {
      5.times {
        doc = REXML::Document.new(content)
      }
    }
  }
rescue Exception
  puts "no JREXML installed, skipping JREXML tests"
end