####################################################################
# tc_pathname.rb
#
# Complete test suite for the Pathname class.
#
# Taken from the pathname.rb source code, by Akira Tanaka.
####################################################################
require "test/unit"
require "pathname"

class PathnameTest < Test::Unit::TestCase
   def test_initialize
      p1 = Pathname.new('a')
      assert_equal('a', p1.to_s)
      p2 = Pathname.new(p1)
      assert_equal(p1, p2)
   end
   
   class AnotherStringLike
      def initialize(s) @s = s end
      def to_str() @s end
      def ==(other) @s == other end
   end
   
   def test_equality
      obj = Pathname.new("a")
      str = "a"
      sym = :a
      ano = AnotherStringLike.new("a")
      assert_equal(false, obj == str)
      assert_equal(false, str == obj)
      assert_equal(false, obj == ano)
      assert_equal(false, ano == obj)
      assert_equal(false, obj == sym)
      assert_equal(false, sym == obj)
      
      obj2 = Pathname.new("a")
      assert_equal(true, obj == obj2)
      assert_equal(true, obj === obj2)
      assert_equal(true, obj.eql?(obj2))
   end
   
   def test_hashkey
      h = {}
      h[Pathname.new("a")] = 1
      h[Pathname.new("a")] = 2
      assert_equal(1, h.size)
   end
   
   def assert_pathname_cmp(e, s1, s2)
      p1 = Pathname.new(s1)
      p2 = Pathname.new(s2)
      r = p1 <=> p2
      assert(e == r,
        "#{p1.inspect} <=> #{p2.inspect}: <#{e}> expected but was <#{r}>")
   end
   def test_comparison
      assert_pathname_cmp( 0, "a", "a")
      assert_pathname_cmp( 1, "b", "a")
      assert_pathname_cmp(-1, "a", "b")
      ss = %w(
        a
        a/
        a/b
        a.
        a0
      )
      s1 = ss.shift
      ss.each {|s2|
         assert_pathname_cmp(-1, s1, s2)
         s1 = s2
      }
   end
   
   def test_comparison_string
      assert_equal(nil, Pathname.new("a") <=> "a")
      assert_equal(nil, "a" <=> Pathname.new("a"))
   end
   
   def test_syntactical
      assert_equal(true, Pathname.new("/").root?)
      assert_equal(true, Pathname.new("//").root?)
      assert_equal(true, Pathname.new("///").root?)
      assert_equal(false, Pathname.new("").root?)
      assert_equal(false, Pathname.new("a").root?)
   end
   
   def test_cleanpath
      assert_equal('/', Pathname.new('/').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('//').cleanpath(true).to_s)
#      assert_equal('', Pathname.new('').cleanpath(true).to_s)
      
      assert_equal('.', Pathname.new('.').cleanpath(true).to_s)
      assert_equal('..', Pathname.new('..').cleanpath(true).to_s)
      assert_equal('a', Pathname.new('a').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('/.').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('/..').cleanpath(true).to_s)
      assert_equal('/a', Pathname.new('/a').cleanpath(true).to_s)
      assert_equal('.', Pathname.new('./').cleanpath(true).to_s)
      assert_equal('..', Pathname.new('../').cleanpath(true).to_s)
      assert_equal('a/', Pathname.new('a/').cleanpath(true).to_s)
      
      assert_equal('a/b', Pathname.new('a//b').cleanpath(true).to_s)
      assert_equal('a/.', Pathname.new('a/.').cleanpath(true).to_s)
      assert_equal('a/.', Pathname.new('a/./').cleanpath(true).to_s)
      assert_equal('a/..', Pathname.new('a/../').cleanpath(true).to_s)
      assert_equal('/a/.', Pathname.new('/a/.').cleanpath(true).to_s)
      assert_equal('..', Pathname.new('./..').cleanpath(true).to_s)
      assert_equal('..', Pathname.new('../.').cleanpath(true).to_s)
      assert_equal('..', Pathname.new('./../').cleanpath(true).to_s)
      assert_equal('..', Pathname.new('.././').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('/./..').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('/../.').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('/./../').cleanpath(true).to_s)
      assert_equal('/', Pathname.new('/.././').cleanpath(true).to_s)
      
      assert_equal('a/b/c', Pathname.new('a/b/c').cleanpath(true).to_s)
      assert_equal('b/c', Pathname.new('./b/c').cleanpath(true).to_s)
      assert_equal('a/c', Pathname.new('a/./c').cleanpath(true).to_s)
      assert_equal('a/b/.', Pathname.new('a/b/.').cleanpath(true).to_s)
      assert_equal('a/..', Pathname.new('a/../.').cleanpath(true).to_s)
      
      assert_equal('/a', Pathname.new('/../.././../a').cleanpath(true).to_s)
      assert_equal('a/b/../../../../c/../d',
      Pathname.new('a/b/../../../../c/../d').cleanpath(true).to_s)
   end
   
   def test_cleanpath_no_symlink
      assert_equal('/', Pathname.new('/').cleanpath.to_s)
      assert_equal('/', Pathname.new('//').cleanpath.to_s)
#      assert_equal('', Pathname.new('').cleanpath.to_s)
      
      assert_equal('.', Pathname.new('.').cleanpath.to_s)
      assert_equal('..', Pathname.new('..').cleanpath.to_s)
      assert_equal('a', Pathname.new('a').cleanpath.to_s)
      assert_equal('/', Pathname.new('/.').cleanpath.to_s)
      assert_equal('/', Pathname.new('/..').cleanpath.to_s)
      assert_equal('/a', Pathname.new('/a').cleanpath.to_s)
      assert_equal('.', Pathname.new('./').cleanpath.to_s)
      assert_equal('..', Pathname.new('../').cleanpath.to_s)
      assert_equal('a', Pathname.new('a/').cleanpath.to_s)
      
      assert_equal('a/b', Pathname.new('a//b').cleanpath.to_s)
      assert_equal('a', Pathname.new('a/.').cleanpath.to_s)
      assert_equal('a', Pathname.new('a/./').cleanpath.to_s)
      assert_equal('.', Pathname.new('a/../').cleanpath.to_s)
      assert_equal('/a', Pathname.new('/a/.').cleanpath.to_s)
      assert_equal('..', Pathname.new('./..').cleanpath.to_s)
      assert_equal('..', Pathname.new('../.').cleanpath.to_s)
      assert_equal('..', Pathname.new('./../').cleanpath.to_s)
      assert_equal('..', Pathname.new('.././').cleanpath.to_s)
      assert_equal('/', Pathname.new('/./..').cleanpath.to_s)
      assert_equal('/', Pathname.new('/../.').cleanpath.to_s)
      assert_equal('/', Pathname.new('/./../').cleanpath.to_s)
      assert_equal('/', Pathname.new('/.././').cleanpath.to_s)
      
      assert_equal('a/b/c', Pathname.new('a/b/c').cleanpath.to_s)
      assert_equal('b/c', Pathname.new('./b/c').cleanpath.to_s)
      assert_equal('a/c', Pathname.new('a/./c').cleanpath.to_s)
      assert_equal('a/b', Pathname.new('a/b/.').cleanpath.to_s)
      assert_equal('.', Pathname.new('a/../.').cleanpath.to_s)
      
      assert_equal('/a', Pathname.new('/../.././../a').cleanpath.to_s)
      assert_equal('../../d', Pathname.new('a/b/../../../../c/../d').cleanpath.to_s)
   end
   
   def test_destructive_update
      path = Pathname.new("a")
      path.to_s.replace "b"
      assert_equal(Pathname.new("a"), path)
   end
   
   def test_null_character
      assert_raise(ArgumentError) { Pathname.new("\0") }
   end
   
   def assert_relpath(result, dest, base)
      assert_equal(Pathname.new(result),
      Pathname.new(dest).relative_path_from(Pathname.new(base)))
   end
   
   def assert_relpath_err(dest, base)
      assert_raise(ArgumentError) {
         Pathname.new(dest).relative_path_from(Pathname.new(base))
      }
   end
   
   def test_relative_path_from
      assert_relpath("../a", "a", "b")
      assert_relpath("../a", "a", "b/")
      assert_relpath("../a", "a/", "b")
      assert_relpath("../a", "a/", "b/")
      assert_relpath("../a", "/a", "/b")
      assert_relpath("../a", "/a", "/b/")
      assert_relpath("../a", "/a/", "/b")
      assert_relpath("../a", "/a/", "/b/")
      
      assert_relpath("../b", "a/b", "a/c")
      assert_relpath("../a", "../a", "../b")
      
      assert_relpath("a", "a", ".")
      assert_relpath("..", ".", "a")
      
      assert_relpath(".", ".", ".")
      assert_relpath(".", "..", "..")
      assert_relpath("..", "..", ".")
      
      assert_relpath("c/d", "/a/b/c/d", "/a/b")
      assert_relpath("../..", "/a/b", "/a/b/c/d")
      assert_relpath("../../../../e", "/e", "/a/b/c/d")
      assert_relpath("../b/c", "a/b/c", "a/d")
      
      assert_relpath("../a", "/../a", "/b")
      assert_relpath("../../a", "../a", "b")
      assert_relpath(".", "/a/../../b", "/b")
      assert_relpath("..", "a/..", "a")
      assert_relpath(".", "a/../b", "b")
      
      assert_relpath("a", "a", "b/..")
      assert_relpath("b/c", "b/c", "b/..")
      
      assert_relpath_err("/", ".")
      assert_relpath_err(".", "/")
      assert_relpath_err("a", "..")
      assert_relpath_err(".", "..")
   end
   
   def assert_pathname_plus(a, b, c)
      a = Pathname.new(a)
      b = Pathname.new(b)
      c = Pathname.new(c)
      d = b + c
      assert(a == d,
        "#{b.inspect} + #{c.inspect}: #{a.inspect} expected but was #{d.inspect}")
   end
   
   def test_plus
      assert_pathname_plus('a/b', 'a', 'b')
      assert_pathname_plus('a', 'a', '.')
      assert_pathname_plus('b', '.', 'b')
      assert_pathname_plus('.', '.', '.')
      assert_pathname_plus('/b', 'a', '/b')
      
      assert_pathname_plus('/', '/', '..')
      assert_pathname_plus('.', 'a', '..')
      assert_pathname_plus('a', 'a/b', '..')
      assert_pathname_plus('../..', '..', '..')
      assert_pathname_plus('/c', '/', '../c')
      assert_pathname_plus('c', 'a', '../c')
      assert_pathname_plus('a/c', 'a/b', '../c')
      assert_pathname_plus('../../c', '..', '../c')
   end
   
   def test_taint
      obj = Pathname.new("a"); assert_same(obj, obj.taint)
      obj = Pathname.new("a"); assert_same(obj, obj.untaint)
      
      assert_equal(false, Pathname.new("a"      )           .tainted?)
      assert_equal(false, Pathname.new("a"      )      .to_s.tainted?)
      assert_equal(true,  Pathname.new("a"      ).taint     .tainted?)
      assert_equal(true,  Pathname.new("a"      ).taint.to_s.tainted?)
      assert_equal(true,  Pathname.new("a".taint)           .tainted?)
      assert_equal(true,  Pathname.new("a".taint)      .to_s.tainted?)
      assert_equal(true,  Pathname.new("a".taint).taint     .tainted?)
      assert_equal(true,  Pathname.new("a".taint).taint.to_s.tainted?)
      
      str = "a"
      path = Pathname.new(str)
      str.taint
      assert_equal(false, path     .tainted?)
      assert_equal(false, path.to_s.tainted?)
   end
   
   def test_untaint
      obj = Pathname.new("a"); assert_same(obj, obj.untaint)
      
      assert_equal(false, Pathname.new("a").taint.untaint     .tainted?)
      assert_equal(false, Pathname.new("a").taint.untaint.to_s.tainted?)
      
      str = "a".taint
      path = Pathname.new(str)
      str.untaint
      assert_equal(true, path     .tainted?)
      assert_equal(true, path.to_s.tainted?)
   end
   
   def test_freeze
      obj = Pathname.new("a"); assert_same(obj, obj.freeze)
      
      assert_equal(false, Pathname.new("a"       )            .frozen?)
      assert_equal(false, Pathname.new("a".freeze)            .frozen?)
      assert_equal(true,  Pathname.new("a"       ).freeze     .frozen?)
      assert_equal(true,  Pathname.new("a".freeze).freeze     .frozen?)
      assert_equal(false, Pathname.new("a"       )       .to_s.frozen?)
      assert_equal(false, Pathname.new("a".freeze)       .to_s.frozen?)
      assert_equal(false, Pathname.new("a"       ).freeze.to_s.frozen?)
      assert_equal(false, Pathname.new("a".freeze).freeze.to_s.frozen?)
   end
   
   def test_to_s
      str = "a"
      obj = Pathname.new(str)
      assert_equal(str, obj.to_s)
      assert_not_same(str, obj.to_s)
      assert_not_same(obj.to_s, obj.to_s)
   end
   
   def test_kernel_open
      count = 0
      stat1 = File.stat(__FILE__)
      result = Kernel.open(Pathname.new(__FILE__)) {|f|
         stat2 = f.stat
         assert_equal(stat1.dev, stat2.dev)
         assert_equal(stat1.ino, stat2.ino)
         assert_equal(stat1.size, stat2.size)
         count += 1
         2
      }
      assert_equal(1, count)
      assert_equal(2, result)
   end
end