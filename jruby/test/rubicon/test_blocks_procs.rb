require 'test/unit'

class TestBlocksProcs < Test::Unit::TestCase

  def testBasicProc
    proc = proc {|i| i}
    assert_equal(2, proc.call(2))
    assert_equal(3, proc.call(3))
    
    proc = proc {|i| i*2}
    assert_equal(4, proc.call(2))
    assert_equal(6, proc.call(3))
  end

  def testProcScoping
    my_proc1 = nil
    my_proc2 = nil
    x = 0
    proc { 
      iii=5	
      my_proc1 = proc{|i|
        iii = i
      }
      my_proc2 = proc {
        x = iii			# nested variables shared by procs
      }
      # scope of nested variables
      assert_not_nil(defined?(iii))
    }.call
    assert(!defined?(iii))		# out of scope

    my_proc1.call(5)
    my_proc2.call
    assert_equal(5, x)
  end

  def testYield
    o = "dummy object"
    def o.f; yield nil; end;       o.f {|a| assert_nil(a)}
    def o.f; yield 1; end;         o.f {|a| assert_equal(1, a)}
    def o.f; yield []; end;        o.f {|a| assert_equal([], a)}
    def o.f; yield [1]; end;       o.f {|a| assert_equal([1], a)}
    def o.f; yield [nil]; end;     o.f {|a| assert_equal([nil], a)}
    def o.f; yield [[]]; end;      o.f {|a| assert_equal([[]], a)}
    def o.f; yield [*[]]; end;     o.f {|a| assert_equal([], a)}
    def o.f; yield [*[1]]; end;    o.f {|a| assert_equal([1], a)}
    def o.f; yield [*[1,2]]; end;  o.f {|a| assert_equal([1,2], a)}
    
    def o.f; yield *nil; end;      o.f {|a| assert_nil(a)}
    def o.f; yield *1; end;        o.f {|a| assert_equal(1, a)}
    def o.f; yield *[]; end;       o.f {|a| assert_nil(a)}
    def o.f; yield *[1]; end;      o.f {|a| assert_equal(1, a)}
    def o.f; yield *[nil]; end;    o.f {|a| assert_nil(a)}
    def o.f; yield *[[]]; end;     o.f {|a| assert_equal([], a)}
    def o.f; yield *[*[]]; end;    o.f {|a| assert_nil(a)}
    def o.f; yield *[*[1]]; end;   o.f {|a| assert_equal(1, a)}
    def o.f; yield *[*[1,2]]; end; o.f {|a| assert_equal([1,2], a)}
    
    def o.f; yield nil; end;       o.f {|*a| assert_equal([nil], a)}
    def o.f; yield 1; end;         o.f {|*a| assert_equal([1], a)}
    def o.f; yield []; end;        o.f {|*a| assert_equal([[]], a)}
    def o.f; yield [1]; end;       o.f {|*a| assert_equal([[1]], a)}
    def o.f; yield [nil]; end;     o.f {|*a| assert_equal([[nil]], a)}
    def o.f; yield [[]]; end;      o.f {|*a| assert_equal([[[]]], a)}
    def o.f; yield [*[]]; end;     o.f {|*a| assert_equal([[]], a)}
    def o.f; yield [*[1]]; end;    o.f {|*a| assert_equal([[1]], a)}
    def o.f; yield [*[1,2]]; end;  o.f {|*a| assert_equal([[1,2]], a)}
    
    def o.f; yield *nil; end;      o.f {|*a| assert_equal([nil], a)}
    def o.f; yield *1; end;        o.f {|*a| assert_equal([1], a)}
    def o.f; yield *[]; end;       o.f {|*a| assert_equal([], a)}
    def o.f; yield *[1]; end;      o.f {|*a| assert_equal([1], a)}
    def o.f; yield *[nil]; end;    o.f {|*a| assert_equal([nil], a)}
  	def o.f; yield *[[]]; end;     o.f {|*a| assert_equal([[]], a)}
    def o.f; yield *[*[]]; end;    o.f {|*a| assert_equal([], a)}
    def o.f; yield *[*[1]]; end;   o.f {|*a| assert_equal([1], a)}
    def o.f; yield *[*[1,2]]; end; o.f {|*a| assert_equal([1,2], a)}
    
    def o.f; yield nil; end;       o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield 1; end;         o.f {|a,b,*c| assert([a,b,c] == [1, nil, []])}
    def o.f; yield []; end;        o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield [1]; end;       o.f {|a,b,*c| assert([a,b,c] == [1, nil, []])}
    def o.f; yield [nil]; end;     o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield [[]]; end;      o.f {|a,b,*c| assert([a,b,c] == [[], nil, []])}
    def o.f; yield [*[]]; end;     o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield [*[1]]; end;    o.f {|a,b,*c| assert([a,b,c] == [1, nil, []])}
    def o.f; yield [*[1,2]]; end;  o.f {|a,b,*c| assert([a,b,c] == [1, 2, []])}
    
    def o.f; yield *nil; end;      o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield *1; end;        o.f {|a,b,*c| assert([a,b,c] == [1, nil, []])}
    def o.f; yield *[]; end;       o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield *[1]; end;      o.f {|a,b,*c| assert([a,b,c] == [1, nil, []])}
    def o.f; yield *[nil]; end;    o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield *[[]]; end;     o.f {|a,b,*c| assert_equal([[], nil, []], [a,b,c])}
    def o.f; yield *[*[]]; end;    o.f {|a,b,*c| assert([a,b,c] == [nil, nil, []])}
    def o.f; yield *[*[1]]; end;   o.f {|a,b,*c| assert([a,b,c] == [1, nil, []])}
    def o.f; yield *[*[1,2]]; end; o.f {|a,b,*c| assert([a,b,c] == [1, 2, []])}

  end

end
