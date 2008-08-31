require 'benchmark'
require 'java'

src = File.read(__FILE__)
StandardASMCompiler = org.jruby.compiler.impl.StandardASMCompiler
ASTCompiler = org.jruby.compiler.ASTCompiler
ASTInspector = org.jruby.compiler.ASTInspector

def parse(src)
  JRuby.parse(src, __FILE__, false)
end

def inspect(node)
  inspector = ASTInspector.new
  inspector.inspect(node)
  inspector
end

def compile(node, inspector)
  filename = node.position.file
  classname = filename.sub("/", ".").sub("\\", ".").sub(".rb", "")
  context = StandardASMCompiler.new(classname, filename)
  compiler = ASTCompiler.new
  compiler.compileRoot(node, context, inspector)
end

TIMES = (ARGV[0] || 5).to_i
TIMES.times {
  Benchmark.bm(30) {|bm|
    bm.report("1k * parse this file") {
      1000.times { parse(src) }
    }
    # this is a little misleading since inspect does not follow blocks, defs, classes, or modules
    bm.report("1k * inspect this file") {
      node = parse(src);
      1000.times { inspect(node) }
    }
    bm.report("1k * compile this file") {
      node = parse(src);
      inspector = inspect(node);
      1000.times { compile(node, inspector) }
    }
  }
}

# Dummy clas to make this more than a trivial compilation
class Dummy
  def dummy
    a,b,c,d,e,f,g,h,i,j = 1,2,3,4,5,6,7,8,9,0
    a,b,c,d = :foo, :bar, :baz, :quux
    dummy2
  end

  def dummy2
    a = 11111111111111111111111111111111111111111111111111111111111
    b = 11111111111111111111111111111111111111111111111111111111111
    c = 11111111111111111111111111111111111111111111111111111111111
    d = "fooooooooooooooooooooooooooo"
    e = "barrrrrrrrrrrrrrrrrrrrrrrrrr"
    dummy3
  end

  def dummy3
    if true;
      if true;
        if true;
          if true;
            if true;
              if true;
                if true;
                  if true;
                    if true;
                      if true;
                      else
                      end
                    else
                    end
                  else
                  end
                else
                end
              else
              end
            else
            end
          else
          end
        else
        end
      else
      end
    else
    end
    dummy3
  end

  def dummy4
    foo { foo { foo { foo { foo { foo { foo { foo { foo { foo }}}}}}}}}
  end
  def foo
    yield
    yield
    yield
    yield
    yield
    yield
    yield
  end

  def ==
    super
    super
    super
    super()
    super()
    super()
    super(1,2,3)
    super(1,2,3)
    super(1,2,3)
  end
end