require 'test/unit'

# FIXME: This has platform-specific bits in it
class TestKernel < Test::Unit::TestCase

  def test_EQUAL # '=='
    o1 = Object.new
    o2 = Object.new
    assert(o1 == o1)
    assert(o2 == o2)
    assert(o1 != o2)
    assert(!(o1 == o2))
  end

  def test_MATCH # '=~'
    o1 = Object.new
    o2 = Object.new
    assert(!(o1 =~ o1))
    assert(!(o2 =~ o2))
    assert(o1 !~ o2)
    assert(!(o1 =~ o2))
  end

  def test_VERY_EQUAL # '==='
    o1 = Object.new
    o2 = Object.new
    assert(o1 === o1)
    assert(o2 === o2)
    assert(!(o1 === o2))
  end

  def test___id__
    # naive test - no 2 ids the same
    objs = []
    ObjectSpace.each_object { |obj| objs << obj.__id__ }
    objs.sort!
    0.upto(objs.size-2) {|i| assert(objs[i] != objs[i+1]) }

    assert_equal(1.__id__, (3-2).__id__)
    assert_instance_of(Fixnum, 1.__id__)
  end

  class SendTest
    def send_test1
      "send1"
    end
    
    def send_test2(a, b)
      a + b
    end
  end

  def test___send__
    t = SendTest.new
    assert_equal("send1", t.__send__(:send_test1))
    assert_equal(99,      t.__send__("send_test2", 44, 55))
  end

  def test_class
    assert_instance_of(Class, 1.class)
    assert_equal(Fixnum, 1.class)
    assert_equal(Class, TestKernel.class)
    assert_equal(Class, TestKernel.class.class)
    assert_equal(Module, Enumerable.class)
  end

  class CloneTest
    attr_accessor :str
  end

  def test_clone
    s1 = CloneTest.new
    s1.str = "hello"
    s2 = s1.clone
    assert(s1.str        == s2.str)
    assert(s1.str.__id__ == s2.str.__id__)
    assert(s1.__id__     != s2.__id__)

    foo = Object.new
    def foo.test
      "test"
    end
    bar = foo.clone
    def bar.test2
      "test2"
    end

    assert_equal("test2", bar.test2)
    assert_equal("test",  bar.test)
    assert_equal("test",  foo.test)
    Version.less_than("1.7") do
      assert_raise(NameError) { foo.test2 }
    end
    Version.greater_or_equal("1.7") do
      assert_raise(NoMethodError) { foo.test2 }
    end
  end

  class DisplayTest
    attr :val
    def write(obj)
      @val = "!#{obj.to_s}!"
    end
  end

  def test_display
    dt = DisplayTest.new
    assert_nil("hello".display(dt))
    assert_equal("!hello!", dt.val)

    save = $>
    begin
      $> = dt
      assert_nil("hello".display)
      assert_equal("!hello!", dt.val)
    ensure
      $> = save
    end
  end

  def test_dup
    s1 = CloneTest.new
    s1.str = "hello"
    s2 = s1.dup
    assert(s1.str        == s2.str)
    assert(s1.str.__id__ == s2.str.__id__)
    assert(s1.__id__     != s2.__id__)
  end

  def test_eql?
    o1 = Object.new
    o2 = Object.new
    assert(o1.eql?(o1))
    assert(o2.eql?(o2))
    assert(!(o1.eql?(o2)))
  end

  def test_equal?
    o1 = Object.new
    o2 = Object.new
    assert(o1.equal?(o1))
    assert(o2.equal?(o2))
    assert(!(o1.equal?(o2)))
  end

  module ExtendTest1
    def et1
      "et1"
    end
    def et3
      "et3.1"
    end
  end

  module ExtendTest2
    def et2
      "et2"
    end
    def et3
      "et3.2"
    end
  end

  def test_extend
    s = "hello"
    assert(!defined?(s.et1))
    s.extend(ExtendTest1)
    assert_not_nil(defined?(s.et1))
    assert(!defined?(s.et2))
    assert_equal("et1", s.et1)
    assert_equal("et3.1", s.et3)

    s.extend(ExtendTest2)
    assert_not_nil(defined?(s.et2))
    assert_equal("et1", s.et1)
    assert_equal("et2", s.et2)
    assert_equal("et3.2", s.et3)

    t = "goodbye"
    t.extend(ExtendTest1)
    t.extend(ExtendTest2)
    assert_equal("et1", t.et1)
    assert_equal("et2", t.et2)
    assert_equal("et3.2", t.et3)

    t = "goodbye"
    t.extend(ExtendTest2)
    t.extend(ExtendTest1)
    assert_equal("et1", t.et1)
    assert_equal("et2", t.et2)
    assert_equal("et3.1", t.et3)
  end

  def test_freeze
    s = "hello"
    s[3] = "x"
    eval %q{ def s.m1() "m1" end }
    assert_equal("m1", s.m1)
    s.freeze
    assert(s.frozen?)
    assert_raise(TypeError) { s[3] = 'y' }
    assert_raise(TypeError) { eval %q{ def s.m1() "m1" end } }
    assert_equal("helxo", s)
  end

  def test_frozen?
    s = "hello"
    assert(!s.frozen?)
    assert(!self.frozen?)
    s.freeze
    assert(s.frozen?)
  end

  def test_hash
    assert_instance_of(Fixnum, "hello".hash)
    s1 = "hello"
    s2 = "hello"
    assert(s1.__id__ != s2.__id__)
    assert(s1.eql?(s2))
    assert(s1.hash == s2.hash)
    s1[2] = 'x'
    assert(s1.hash != s2.hash)
  end

  def test_id
    objs = []
    ObjectSpace.each_object { |obj| objs << obj.__id__ }
    s1 = objs.size
    assert_equal(s1, objs.uniq.size)

    assert_equal(1.__id__, (3-2).__id__)
    assert_instance_of(Fixnum, 1.__id__)
  end

  def test_inspect
    assert_instance_of(String, 1.inspect)
    assert_instance_of(String, /a/.inspect)
    assert_instance_of(String, "hello".inspect)
    assert_instance_of(String, self.inspect)
  end

  def test_instance_eval
    s = "hello"
    assert_equal(s, s.instance_eval { self } ) 
    assert_equal("HELLO", s.instance_eval("upcase"))
  end

  def test_instance_of?
    s = "hello"
    assert(s.instance_of?(String))
    assert(!s.instance_of?(Object))
    assert(!s.instance_of?(Class))
    assert(self.instance_of?(TestKernel))
  end

  class IVTest1
  end
  class IVTest2
    def initialize
      @var1 = 1
      @var2 = 2
    end
  end
    
  def test_instance_variables
    o = IVTest1.new
    assert_equal([], o.instance_variables)
    o = IVTest2.new
    assert_bag_equal(%w(@var1 @var2), o.instance_variables)
  end

  def test_is_a?
    s = "hello"
    assert(s.is_a?(String))
    assert(s.is_a?(Object))
    assert(!s.is_a?(Class))
    assert(self.is_a?(TestKernel))
    assert(TestKernel.is_a?(Class))
    assert(TestKernel.is_a?(Module))
    assert(TestKernel.is_a?(Object))

    a = []
    assert(a.is_a?(Array))
    assert(a.is_a?(Enumerable))
  end

  def test_kind_of?
    s = "hello"
    assert(s.kind_of?(String))
    assert(s.kind_of?(Object))
    assert(!s.kind_of?(Class))
    assert(self.kind_of?(TestKernel))
    assert(TestKernel.kind_of?(Class))
    assert(TestKernel.kind_of?(Module))
    assert(TestKernel.kind_of?(Object))

    a = []
    assert(a.kind_of?(Array))
    assert(a.kind_of?(Enumerable))
  end

  def MethodTest1
    "mt1"
  end
  def MethodTest2(a, b, c)
    a + b + c
  end

  def test_method
    assert_raise(NameError) { self.method(:wombat) }
    m = self.method("MethodTest1")
    assert_instance_of(Method, m)
    assert_equal("mt1", m.call)
    assert_raise(ArgumentError) { m.call(1, 2, 3) }

    m = self.method("MethodTest2")
    assert_instance_of(Method, m)
    assert_equal(6, m.call(1, *[2, 3]))
    assert_raise(ArgumentError) { m.call(1, 3) }
  end

  class MethodMissing
    def method_missing(m, *a)
      return [m, a]
    end
    def mm
      return "mm"
    end
  end
      
  def test_method_missing
    mm = MethodMissing.new
    assert_equal("mm", mm.mm)
    assert_equal([ :dave, []], mm.dave)
    assert_equal([ :dave, [1, 2, 3]], mm.dave(1, *[2, 3]))
  end

  class MethodsTest
    def MethodsTest.singleton
    end
    def one
    end
    def two
    end
    def three
    end
    def four
    end
    private :two
    protected :three
  end

  def test_methods
    assert_bag_equal(TestKernel.instance_methods(true), self.methods)
    assert_bag_equal(%w(one three four)  + Object.instance_methods(true), 
        MethodsTest.new.methods)
  end

  def test_nil?
    assert(!self.nil?)
    assert(nil.nil?)
    a = []
    assert(a[99].nil?)
  end

  class PrivateMethods < MethodsTest
    def five
    end
    def six
    end
    def seven
    end
    private :six
    protected :seven
    end

  def test_private_methods
    assert_bag_equal(%w(two six) + Object.new.private_methods,
                  PrivateMethods.new.private_methods)
  end

  def test_protected_methods
    assert_bag_equal(%w(three seven) + Object.new.protected_methods,
                  PrivateMethods.new.protected_methods)
  end

  def test_public_methods
    assert_bag_equal(TestKernel.instance_methods(true), self.public_methods)
    assert_bag_equal(%w(one four)  + Object.instance_methods(true), 
        MethodsTest.new.public_methods)
  end

  def test_respond_to?
    assert(self.respond_to?(:test_respond_to?))
    assert(!self.respond_to?(:TEST_respond_to?))
           
    mt = PrivateMethods.new
    # public
    assert(mt.respond_to?("five"))
    assert(mt.respond_to?(:one))

    assert(mt.respond_to?("five", true))
    assert(mt.respond_to?(:one, false))
    # protected
    assert(mt.respond_to?("seven"))
    assert(mt.respond_to?(:three))

    assert(mt.respond_to?("seven", true))
    assert(mt.respond_to?(:three, false))
    #private
    assert(!mt.respond_to?(:two))
    assert(!mt.respond_to?("six"))
 
    assert(mt.respond_to?(:two, true))
    assert(mt.respond_to?("six", true))
  end

  def test_send
    t = SendTest.new
    assert_equal("send1", t.send(:send_test1))
    assert_equal(99,      t.send("send_test2", 44, 55))
  end

  def test_singleton_methods
    assert_equal(%w(singleton), MethodsTest.singleton_methods)
    assert_equal(%w(singleton), PrivateMethods.singleton_methods)
    
    mt = MethodsTest.new
    assert_equal([], mt.singleton_methods)
    eval "def mt.wombat() end"
    assert_equal(%w(wombat), mt.singleton_methods)
  end

  def test_taint
    a = "hello"
    assert(!a.tainted?)
    assert_equal(a, a.taint)
    assert(a.tainted?)
  end

  def test_tainted?
    a = "hello"
    assert(!a.tainted?)
    assert_equal(a, a.taint)
    assert(a.tainted?)
  end

  def test_to_a
    Version.less_than("1.7.2") do
      o = Object.new
      assert_equal([o], o.to_a)   # rest tested in individual classes
    end
  end

  def test_to_s
    o = Object.new
    assert_match(/^#<Object:0x[0-9a-f]+>/, o.to_s)
  end

  def test_type
    assert_instance_of(Class, self.class)
    assert_equal(TestKernel, self.class)
    assert_equal(String, "hello".class)
    assert_equal(Bignum, (10**40).class)
  end

  def test_untaint
    a = "hello"
    assert(!a.tainted?)
    assert_equal(a, a.taint)
    assert(a.tainted?)
    assert_equal(a, a.untaint)
    assert(!a.tainted?)
    
    a = "hello"
    assert(!a.tainted?)
    assert_equal(a, a.untaint)
    assert(!a.tainted?)
  end

  class Caster
    def to_a
      [4, 5, 6]
    end
    def to_f
      2.34
    end
    def to_i
      99
    end
    def to_s
      "Hello"
    end
  end

  def test_s_Array
    Version.less_than("1.7.2") do
      assert_equal([], Array(nil))
    end
    assert_equal([1, 2, 3], Array([1, 2, 3]))
    assert_equal([1, 2, 3], Array(1..3))
    assert_equal([4, 5, 6], Array(Caster.new))
  end

  def test_s_Float
    assert_instance_of(Float, Float(1))
    assert_flequal(1, Float(1))
    assert_flequal(1.23, Float('1.23'))
    assert_flequal(2.34, Float(Caster.new))
  end

  def test_s_Integer
    assert_instance_of(Fixnum, Integer(1))
    assert_instance_of(Bignum, Integer(10**30))
    assert_equal(123,    Integer(123.99))
    assert_equal(-123,   Integer(-123.99))
    a = Integer(1.0e30) - 10**30
    assert(a.abs < 10**20)
    assert_equal(99, Integer(Caster.new))
  end

  def test_s_String
    assert_instance_of(String, String(123))
    assert_equal("123", String(123))
    assert_equal("123.45", String(123.45))
    assert_equal("123",    String([1, 2, 3]))
    assert_equal("Hello",  String(Caster.new))
  end

  def test_s_BACKTICK
    assert_equal("hello\n", `echo hello`)
    assert_equal(0, $?)
    
    MsWin32.dont do
      assert_equal("", `not_a_valid_command 2>/dev/null`)
    end
    MsWin32.only do
      assert_equal("", `not_a_valid_command 2>nul`)
    end
    assert($? != 0)

    assert_equal("hello\n", %x{echo hello})
    assert_equal(0, $?)
    
    MsWin32.dont do
      assert_equal("", %x{not_a_valid_command 2>/dev/null})
    end
    MsWin32.only do
      assert_equal("", %x{not_a_valid_command 2>nul})
    end
    assert($? != 0)
  end

  def test_s_abort
    p = IO.popen("#$interpreter -e 'abort;exit 99'")
    p.close
    assert_equal(1<<8, $?)
  end

  def test_s_at_exit
    script = %{at_exit {puts "world"};at_exit {puts "cruel"};puts "goodbye";exit 99}

    p = IO.popen("#$interpreter -e '#{script}'")

    begin
      assert_equal("goodbye\n", p.gets)
      assert_equal("cruel\n", p.gets)
      assert_equal("world\n", p.gets)
    ensure
      p.close
      assert_equal(99<<8, $?)
    end
  end

  def test_s_autoload
    File.open("_dummy.rb", "w") do |f|
     f.print <<-EOM
      module Module_Test
        VAL = 123
      end
      EOM
    end
    assert(!defined? Module_Test)
    autoload(:Module_Test, "./_dummy.rb")
    assert_not_nil(defined? Module_Test::VAL)
    assert_equal(123, Module_Test::VAL)
    assert($".include?("./_dummy.rb"))#"
    File.delete("./_dummy.rb")
  end

  def bindproc(val)
    return binding
  end

  def test_s_binding
    val = 123
    b = binding
    assert_instance_of(Binding, b)
    assert_equal(123, eval("val", b))
    b = bindproc(321)
    assert_equal(321, eval("val", b))
  end

  def blocker1
    return block_given?
  end

  def blocker2(&p)
    return block_given?
  end

  def test_s_block_given?
    assert(!blocker1())
    assert(!blocker2())
    assert(blocker1() { 1 })
    assert(blocker2() { 1 })
  end

  def callcc_test(n)
    cont = nil
    callcc { |cont|
      return [cont, n]
    }
    n -= 1
    return [n.zero? ? nil : cont, n ]
  end

  def test_s_callcc
    res = nil
    cont = nil
    assert_equal(2, 
                 callcc { |cont|
                   res = 1
                   res = 2
                 })
    assert_equal(2, res)

    res = nil 
    assert_equal(99, 
                 callcc { |cont|
                   res = 1
                   cont.call 99
                   res = 2
                 })
    assert_equal(1, res)

    # Test gotos!
    callcc { |cont| res = 0 }
    res += 1
    cont.call if res < 4
    assert_equal(4, res)
    
    # Test reactivating procedures
    n = 4
    cont, res = callcc_test(4)
    assert_equal(n, res)
    n -= 1
    puts cont.call if cont
  end

  def caller_test
    return caller
  end

  def test_s_caller
    c = caller_test
    assert_match(%r{TestKernel.rb:#{__LINE__-1}:in `test_s_caller'}, c[0])
  end #`

  def catch_test
    throw :c, 456;
    789;
  end

  def test_s_catch
    assert_equal(123, catch(:c) { a = 1; 123 })
    assert_equal(321, catch(:c) { a = 1; throw :c, 321; 123 })
    assert_equal(456, catch(:c) { a = 1; catch_test; 123 })

    assert_equal(456, catch(:c) { catch(:d) { catch_test; 123 } } )
  end

  def test_s_chomp
    $_ = "hello"
    assert_equal("hello", chomp)
    assert_equal("hello", chomp('aaa'))
    assert_equal("he",    chomp('llo'))
    assert_equal("he",    $_)

    a  = "hello\n"
    $_ = a
    assert_equal("hello",   chomp)
    assert_equal("hello",   $_)
    assert_equal("hello\n", a)
  end

  def test_s_chomp!
    $_ = "hello"
    assert_equal(nil,     chomp!)
    assert_equal(nil,     chomp!('aaa'))
    assert_equal("he",    chomp!('llo'))
    assert_equal("he",    $_)

    a  = "hello\n"
    $_ = a
    assert_equal("hello", chomp!)
    assert_equal("hello", $_)
    assert_equal("hello", a)
  end

  def test_s_chop
    a  = "hi"
    $_ = a
    assert_equal("h",  chop)
    assert_equal("h",  $_)
    assert_equal("",   chop)
    assert_equal("",   $_)
    assert_equal("",   chop)
    assert_equal("hi", a)

    $_ = "hi\n"
    assert_equal("hi", chop)

    $_ = "hi\r\n"
    assert_equal("hi", chop)
    $_ = "hi\n\r"
    assert_equal("hi\n", chop)
  end

  def test_s_chop!
    a  = "hi"
    $_ = a
    assert_equal("h",  chop!)
    assert_equal("h",  $_)
    assert_equal("",   chop!)
    assert_equal("",   $_)
    assert_equal(nil,   chop!)
    assert_equal("",   $_)
    assert_equal("", a)

    $_ = "hi\n"
    assert_equal("hi", chop!)

    $_ = "hi\r\n"
    assert_equal("hi", chop!)
    $_ = "hi\n\r"
    assert_equal("hi\n", chop!)
  end

  def test_s_eval
    assert_equal(123, eval("100 + 20 + 3"))
    val = 123
    assert_equal(123, eval("val", binding))
    assert_equal(321, eval("val", bindproc(321)))
    skipping("Check of eval with file name")
    begin
      eval "1
            burble", binding, "gumby", 321
    rescue Exception => detail
    end
  end

  def test_s_exec
    open("xyzzy.dat", "w") { |f| f.puts "stuff" }
    tf = Tempfile.new("tf")
    begin

      # all in one string - wildcards get expanded
      tf.puts 'exec("echo xy*y.dat")'
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |p|
        assert_equal("xyzzy.dat\n", p.gets)
      end

      # with two parameters, the '*' doesn't get expanded
      tf.open
      tf.puts 'exec("echo", "xy*y.dat")'
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |p|
        assert_equal("xy*y.dat\n", p.gets)
      end

    ensure
      tf.close(true)
      File.unlink "xyzzy.dat" if p
    end

  end

  def test_s_exit
    begin
      exit
      fail("No exception raised")
    rescue SystemExit
      assert(true)
    rescue Exception
      fail("Bad exception: #$!")
    end

    p = IO.popen("#$interpreter -e 'exit'")
    p.close
    assert_equal(0, $?)

    p = IO.popen("#$interpreter -e 'exit 123'")
    p.close
    assert_equal(123 << 8, $?)
  end

  def test_s_exit!
    tf = Tempfile.new("tf")
    tf.puts %{
      begin
	exit! 99
	exit 1
      rescue SystemExit
	exit 2
      rescue Exception
	exit 3
      end
      exit 4}
    
    tf.close
    IO.popen("#$interpreter #{tf.path}").close
    assert_equal(99<<8, $?)
    
    IO.popen("#$interpreter -e 'exit!'").close
    Version.less_than("1.8.1") do
      assert_equal(0xff << 8, $?)
    end
    Version.greater_or_equal("1.8.1") do
      assert_equal(1 << 8, $?)
    end

    IO.popen("#$interpreter -e 'exit! 123'").close
    assert_equal(123 << 8, $?)
  ensure
    tf.close(true)
  end


  def test_s_fail
    begin
      fail
    rescue StandardError
      assert(true)
    rescue Exception
      fail("Wrong exception raised")
    end

    begin
      fail "Wombat"
    rescue StandardError => detail
      assert_equal("Wombat", detail.message)
    rescue Exception
      fail("Wrong exception raised")
    end

    assert_raise(NotImplementedError) { fail NotImplementedError }

    begin
      fail "Wombat"
      fail("No exception")
    rescue StandardError => detail
      assert_equal("Wombat", detail.message)
    rescue Exception
      fail("Wrong exception raised")
    end

    begin
      fail NotImplementedError, "Wombat"
      fail("No exception")
    rescue NotImplementedError => detail
      assert_equal("Wombat", detail.message)
    rescue Exception
      fail("Wrong exception raised")
    end

    bt = %w(one two three)
    begin
      fail NotImplementedError, "Wombat", bt
      fail("No exception")
    rescue NotImplementedError => detail
      assert_equal("Wombat", detail.message)
      assert_equal(bt, detail.backtrace)
    rescue Exception
      fail("Wrong exception raised")
    end

  end

  MsWin32.dont do
    def test_s_fork
      f = fork
      if f.nil?
	File.open("_pid", "w") {|f| f.puts $$}
	exit 99
      end
      begin
	Process.wait
	assert_equal(99<<8, $?)
	File.open("_pid") do |file|
	  assert_equal(file.gets.to_i, f)
	end
      ensure
	File.delete("_pid")
      end
      
      f = fork do
	File.open("_pid", "w") {|f| f.puts $$}
      end
      begin
	Process.wait
	assert_equal(0<<8, $?)
	File.open("_pid") do |file|
	  assert_equal(file.gets.to_i, f)
	end
      ensure
	File.delete("_pid")
      end
    end
  end


  def test_s_format
    assert_equal("00123", format("%05d", 123))
    assert_equal("123  |00000001", format("%-5s|%08x", 123, 1))
    x = format("%3s %-4s%%foo %.0s%5d %#x%c%3.1f %b %x %X %#b %#x %#X",
      "hi",
      123,
      "never seen",
      456,
      0,
      ?A,
      3.0999,
      11,
      171,
      171,
      11,
      171,
      171)

    assert_equal(' hi 123 %foo   456 0x0A3.1 1011 ab AB 0b1011 0xab 0XAB', x)
  end

  def setupFiles
    setupTestDir
    File.open("_test/_file1", "w") do |f|
      f.puts "0: Line 1"
      f.puts "1: Line 2"
    end
    File.open("_test/_file2", "w") do |f|
      f.puts "2: Line 1"
      f.puts "3: Line 2"
    end
    ARGV.replace ["_test/_file1", "_test/_file2" ]
  end

  def teardownFiles
    teardownTestDir
  end

  def test_s_gets1
    setupFiles
    begin
      count = 0
      while gets
        num = $_[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(4, count)
    ensure
      teardownFiles
    end
  end

  def test_s_gets2      
    setupFiles
    begin
      count = 0
      while gets(nil)
        split(/\n/).each do |line|
          num = line[0..1].to_i
          assert_equal(count, num)
          count += 1
        end
      end
      assert_equal(4, count)
    ensure
      teardownFiles
    end
  end

  def test_s_gets3
    setupFiles
    begin
      count = 0
      while gets(' ')
        count += 1
      end
      assert_equal(10, count)
    ensure
      teardownFiles
    end
  end

  def test_s_global_variables
    g1 = global_variables
    assert_instance_of(Array, g1)
    assert_instance_of(String, g1[0])
    assert(!g1.include?("$fred"))
    eval "$fred = 1"
    g2 = global_variables
    assert(g2.include?("$fred"))
    assert_equal(["$fred"], g2 - g1)
  end

  def test_s_gsub
    $_ = "hello"
    assert_equal("h*ll*", gsub(/[aeiou]/, '*'))
    assert_equal("h*ll*", $_)

    $_ = "hello"
    assert_equal("h<e>ll<o>", gsub(/([aeiou])/, '<\1>'))
    assert_equal("h<e>ll<o>", $_)

    $_ = "hello"
    assert_equal("104 101 108 108 111 ", gsub(/./) {
                   |s| s[0].to_s+' '})
    assert_equal("104 101 108 108 111 ", $_)

    $_ = "hello"
    assert_equal("HELL-o", gsub(/(hell)(.)/) {
                   |s| $1.upcase + '-' + $2
                   })
    assert_equal("HELL-o", $_)

    $_ = "hello"
    $_.taint
    assert_equal(true, (gsub(/./,'X').tainted?))
    assert_equal(true, $_.tainted?)
  end

  def test_s_gsub!
    $_ = "hello"
    assert_equal("h*ll*", gsub!(/[aeiou]/, '*'))
    assert_equal("h*ll*", $_)

    $_ = "hello"
    assert_equal("h<e>ll<o>", gsub!(/([aeiou])/, '<\1>'))
    assert_equal("h<e>ll<o>", $_)

    $_ = "hello"
    assert_equal("104 101 108 108 111 ", gsub!(/./) {
                   |s| s[0].to_s+' '})
    assert_equal("104 101 108 108 111 ", $_)

    $_ = "hello"
    assert_equal("HELL-o", gsub!(/(hell)(.)/) {
                   |s| $1.upcase + '-' + $2
                   })
    assert_equal("HELL-o", $_)

    $_ = "hello"
    assert_equal(nil, gsub!(/x/, 'y'))
    assert_equal("hello", $_)

    $_ = "hello"
    $_.taint
    assert_equal(true, (gsub!(/./,'X').tainted?))
    assert_equal(true, $_.tainted?)
  end

  def iterator_test(&b)
    return iterator?
  end
    
  def test_s_iterator?
    assert(iterator_test { 1 })
    assert(!iterator_test)
  end

  def test_s_lambda
    a = lambda { "hello" }
    assert_equal("hello", a.call)
    a = lambda { |s| "there " + s  }
    assert_equal("there Dave", a.call("Dave"))
  end

  def test_s_load
    File.open("_dummy_load.rb", "w") do |f|
     f.print <<-EOM
      module Module_Load
        VAL = 234
      end
      EOM
    end
    assert(!defined? Module_Load)
    load("./_dummy_load.rb")
    assert_not_nil(defined? Module_Load::VAL)
    assert_equal(234, Module_Load::VAL)
    assert(!$".include?("./_dummy_load.rb"))#"

    # Prove it's reloaded
    File.open("_dummy_load.rb", "w") do |f|
     f.print <<-EOM
      module Module_Load
        VAL1 = 456
      end
      EOM
    end
    load("./_dummy_load.rb")
    assert_equal(456, Module_Load::VAL1)

    # check that the sandbox works
    File.open("_dummy_load.rb", "w") do |f|
     f.print <<-EOM
      GLOBAL_VAL = 789
      EOM
    end
    load("./_dummy_load.rb", true)
    assert(!defined? GLOBAL_VAL)
    load("./_dummy_load.rb", false)
    assert_not_nil(defined? GLOBAL_VAL)
    assert_equal(789, GLOBAL_VAL)
    File.delete("./_dummy_load.rb")
  end

  def local_variable_test(c)
    d = 2
    local_variables
  end

  def test_s_local_variables
    assert_bag_equal(%w(a), local_variables)
    eval "b = 1"
    assert_bag_equal(%w(a b), local_variables)
    assert_bag_equal(%w(c d), local_variable_test(1))
    a = 1
  end

  # This is a lame test--can we do better?
  def test_s_loop
    a = 0
    loop do
      a += 1
      break if a > 4
    end
    assert_equal(5, a)
  end

  # regular files
  def test_s_open1
    setupTestDir
    begin
      file1 = "_test/_file1"
      
      assert_raise(Errno::ENOENT) { File.open("_gumby") }
      
      # test block/non block forms
      
      f = open(file1)
      begin
        assert_instance_of(File, f)
      ensure
        f.close
      end
      
      assert_nil(open(file1) { |f| assert_equal(File, f.class)})
      
      # test modes
      
      modes = [
        %w( r w r+ w+ a a+ ),
        [ File::RDONLY, 
          File::WRONLY | File::CREAT,
          File::RDWR,
          File::RDWR   + File::TRUNC + File::CREAT,
          File::WRONLY + File::APPEND + File::CREAT,
          File::RDWR   + File::APPEND + File::CREAT
        ]]

      for modeset in modes
        sys("rm -f #{file1}")
        sys("touch #{file1}")
        
        mode = modeset.shift      # "r"
        
        # file: empty
        open(file1, mode) { |f| 
          assert_nil(f.gets)
          assert_raise(IOError) { f.puts "wombat" }
        }
        
        mode = modeset.shift      # "w"
        
        # file: empty
        open(file1, mode) { |f| 
          assert_nil(f.puts("wombat"))
          assert_raise(IOError) { f.gets }
        }
        
        mode = modeset.shift      # "r+"
        
        # file: wombat
        open(file1, mode) { |f| 
          assert_equal("wombat\n", f.gets)
          assert_nil(f.puts("koala"))
          f.rewind
          assert_equal("wombat\n", f.gets)
          assert_equal("koala\n", f.gets)
        }
        
        mode = modeset.shift      # "w+"
        
        # file: wombat/koala
        open(file1, mode) { |f| 
          assert_nil(f.gets)
          assert_nil(f.puts("koala"))
          f.rewind
          assert_equal("koala\n", f.gets)
        }
        
        mode = modeset.shift      # "a"
        
        # file: koala
        open(file1, mode) { |f| 
          assert_nil(f.puts("wombat"))
          assert_raise(IOError) { f.gets }
        }
        
        mode = modeset.shift      # "a+"
        
        # file: koala/wombat
        open(file1, mode) { |f| 
          assert_nil(f.puts("wallaby"))
          f.rewind
          assert_equal("koala\n", f.gets)
          assert_equal("wombat\n", f.gets)
          assert_equal("wallaby\n", f.gets)
        }
        
      end
      
      # Now try creating files
      
      filen = "_test/_filen"
      
      open(filen, "w") {}
      begin
        assert(File.exists?(filen))
      ensure
        File.delete(filen)
      end

      Version.greater_or_equal("1.7") do
        open(filen, "w", 0444) {}
        begin
          assert(File.exists?(filen))
          Cygwin.known_problem do
            assert_equal(0444 & ~File.umask, File.stat(filen).mode & 0777)
          end
        ensure
          WindowsNative.or_variant do
            # to be able to delete the file on Windows
            File.chmod(0666, filen)
          end
          File.delete(filen)
        end
      end

    ensure
      teardownTestDir           # also does a chdir
    end
  end

  def setup_s_open2
    setupTestDir
    @file  = "_test/_10lines"
    File.open(@file, "w") do |f|
      10.times { |i| f.printf "%02d: This is a line\n", i }
    end
  end

  # pipes
  def test_s_open2
    setup_s_open2

    begin
      assert_nil(open("| echo hello") do |f|
                   assert_equal("hello\n", f.gets)
                 end)

      # READ
      p = open("|#$interpreter -e 'puts readlines' <#@file")
      begin
        count = 0
        p.each do |line|
          num = line[0..1].to_i
          assert_equal(count, num)
          count += 1
        end
        assert_equal(10, count)
      ensure
        p.close
      end


      # READ with block
      res = open("|#$interpreter -e 'puts readlines' <#@file") do |p|
        count = 0
        p.each do |line|
          num = line[0..1].to_i
          assert_equal(count, num)
          count += 1
        end
        assert_equal(10, count)
      end
      assert_nil(res)

      # WRITE
      p = open("|#$interpreter -e 'puts readlines' >#@file", "w")
      begin
        5.times { |i| p.printf "Line %d\n", i }
      ensure
        p.close
      end
      
      count = 0
      IO.foreach(@file) do |line|
        num = line.chomp[-1,1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(5, count)

      # Spawn an interpreter
      MsWin32.dont do
	parent = $$
	p = open("|-")
	if p
	  begin
	    assert_equal(parent, $$)
	    assert_equal("Hello\n", p.gets)
	  ensure
	    p.close
	  end
	else
	  assert_equal(parent, Process.ppid)
	  puts "Hello"
	  exit
	end
      end

      # Spawn an interpreter - WRITE
      MsWin32.dont do
	parent = $$
	pipe = open("|-", "w")
	
	if pipe
	  begin
	    assert_equal(parent, $$)
	    pipe.puts "12"
	    Process.wait
	    assert_equal(12, $?>>8)
	  ensure
	    pipe.close
	  end
	else
	  buff = $stdin.gets
	  exit buff.to_i
	end
      end

      # Spawn an interpreter - READWRITE
      MsWin32.dont do
	parent = $$
	p = open("|-", "w+")
	
	if p
	  begin
	    assert_equal(parent, $$)
	    p.puts "Hello\n"
	    assert_equal("Goodbye\n", p.gets)
	    Process.wait
	  ensure
	    p.close
	  end
	else
	  puts "Goodbye" if $stdin.gets == "Hello\n"
	  exit
	end
      end
    ensure
      teardownTestDir
    end
  end

  MacOS.only do
    undef test_s_open2
  end

    
  def test_s_p
    tf = Tempfile.new("tf")
    begin
      tf.puts %{
	class PTest2
	  def inspect
	    "ptest2"
	  end
	end
        p 1
        p PTest2.new
        exit}
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |pipe|
	assert_equal("1\n", pipe.gets)
        assert_equal("ptest2\n", pipe.gets)
      end
    ensure
        tf.close(true)
    end
  end

  def test_s_print
    tf = Tempfile.new("tf")
    begin
      tf.puts %{
	class PrintTest
	  def to_s
	    "printtest"
	  end
	end
	print 1
	print PrintTest.new
	print "\n"
	$, = ":"
      print 1, "cat", PrintTest.new, "\n"
	exit}
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |pipe|
	assert_equal("1printtest\n", pipe.gets)
	assert_equal("1:cat:printtest:\n", pipe.gets)
      end
    ensure
      tf.close(true)
    end
  end

  def test_s_printf
    tf = Tempfile.new("tf")
    begin
      tf.puts %{
        printf("%05d\n", 123)
        printf("%-5s|%08x\n", 123, 1)
        printf("%3s %-4s%%foo %.0s%5d %#x%c%3.1f %b %x %X %#b %#x %#X\n",
                "hi",
                123,
                "never seen",
                456,
                0,
                ?A,
                3.0999,
                11,
                171,
                171,
                11,
                171,
                171)
        exit}
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |pipe|
	assert_equal("00123\n", pipe.gets)
	assert_equal("123  |00000001\n", pipe.gets)
	assert_equal(" hi 123 %foo   456 0x0A3.1 1011 ab AB 0b1011 0xab 0XAB\n",
                     pipe.gets)
      end
    ensure
        tf.close(true)
    end
  end


  def test_s_proc
    a = proc { "hello" }
    assert_equal("hello", a.call)
    a = proc { |s| "there " + s  }
    assert_equal("there Dave", a.call("Dave"))
  end

  def test_s_putc
    setupTestDir
    fname = "_test/_op"
    begin
      File.open(fname, "wb") do |file|
        file.putc "A"
        0.upto(255) { |ch| file.putc ch }
      end
      
      File.open(fname, "rb") do |file|
        assert_equal(?A, file.getc)
        0.upto(255) { |ch| assert_equal(ch, file.getc) }
      end
    ensure
      teardownTestDir
    end
  end

  class PrintTest
    def to_s
      "printtest"
    end
  end

  def test_s_puts
    setupTestDir
    fname = "_test/_op"
    begin
      File.open(fname, "w") do |file|
        file.puts "line 1", "line 2"
        file.puts PrintTest.new
        file.puts 4
      end
      
      File.open(fname) do |file|
        assert_equal("line 1\n",  file.gets)
        assert_equal("line 2\n",  file.gets)
        assert_equal("printtest\n",  file.gets)
        assert_equal("4\n",  file.gets)
      end
    ensure
      teardownTestDir
    end
  end

  def test_s_raise
    begin
      raise
    rescue StandardError
      assert(true)
    rescue Exception
      fail("Wrong exception raised")
    end

    begin
      raise "Wombat"
    rescue StandardError => detail
      assert_equal("Wombat", detail.message)
    rescue Exception
      fail("Wrong exception raised")
    end

    assert_raise(NotImplementedError) { raise NotImplementedError }

    begin
      raise "Wombat"
      fail("No exception")
    rescue StandardError => detail
      assert_equal("Wombat", detail.message)
    rescue Exception
      fail("Wrong exception")
    end

    begin
      raise NotImplementedError, "Wombat"
      fail("No exception")
    rescue NotImplementedError => detail
      assert_equal("Wombat", detail.message)
    rescue Exception
      raise
    end

    bt = %w(one two three)
    begin
      raise NotImplementedError, "Wombat", bt
      fail("No exception")
    rescue NotImplementedError => detail
      assert_equal("Wombat", detail.message)
      assert_equal(bt, detail.backtrace)
    rescue Exception
      raise
    end

    if defined? Process.kill
      x = 0
      trap "SIGINT", proc {|sig| x = 2}
      Process.kill "SIGINT", $$
      sleep 0.1
      assert_equal(2, x)
      
      trap "SIGINT", proc {raise "Interrupt"}
      
      x = nil
      begin
        Process.kill "SIGINT", $$
        sleep 0.1
      rescue
        x = $!
      end
      assert_not_nil(x)
      assert_not_nil(/Interrupt/ =~ x.to_s)
    end
    
  end

  def rand_test(limit, result_type, bucket_scale, bucket_max, average)

    n = rand(limit)
    assert_instance_of(result_type, n)

    repeat = 10000
    sum = 0
    min = 2**30
    max = -1
    buckets = [0] * bucket_max
    sumstep = 0

    repeat.times do
      last, n = n, rand(limit)
      sumstep += (n-last).abs
      min = n if min > n
      max = n if max < n
      sum += n
      buckets[bucket_scale.call(n)] += 1
    end
    
    # Normalize the limit
    limit = limit.to_i
    limit = 1 if limit == 0

    # Check the mean is about right
    assert(min >= 0.0)
    assert(max < limit)
    avg = Float(sum) / Float(repeat)

    if (avg < Float(average)*0.95 or avg > Float(average)*1.05) 
      $stderr.puts "
         I am about to fail a test, but the failure may be purely
         a statistical coincidence. Try a difference see and see if
         if still happens."
      fail("Average out of range (got #{avg}, expected #{average}")
    end

    # Now do the same for the average difference
    avgstep = Float(sumstep) / Float(repeat)
    expstep = Float(limit)/3.0

    if (avgstep < Float(expstep)*0.95 or avgstep > Float(expstep)*1.05) 
      $stderr.puts "
         I am about to fail a test, but the failure may be purely
         a statistical coincidence. Try a difference see and see if
         if still happens."
      fail("Avg. step out of range (got #{avgstep}, expected #{expstep}")
    end

    # check that no bucket has les than repeats/100/2 or more than
    # repeats/100*1.5

    expected = repeat/bucket_max
    low = expected/2
    high = (expected*3)/2

    buckets.each_with_index do |item, index|
      assert(item > low && item < high, 
             "Bucket[#{index}] has #{item} entries. Expected roughly #{expected}")
    end
  end
  
  # Now the same, but with integers
  def test_s_rand
    rand_test(  0, Float,  proc {|n| (100*n).to_i }, 100,   0.5)
    rand_test(100, Fixnum, proc {|n| n },            100,  49.5)
    rand_test( 50, Fixnum, proc {|n| n },             50,  24.5)
    rand_test(500, Fixnum, proc {|n| n/5 },          100, 249)
  end

  def test_s_readline1
    setupFiles
    begin
      count = 0
      4.times do |count|
        line = readline
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_raise(EOFError) { readline }
    ensure
      teardownFiles
    end
  end

  def test_s_readline2
    setupFiles
    begin
      count = 0
      contents = readline(nil)
      contents.split(/\n/).each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(2, count)

      contents = readline(nil)
      contents.split(/\n/).each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(4, count)
      assert_raise(EOFError) { readline }
    ensure
      teardownFiles
    end
  end

  def test_s_readline3
    setupFiles
    begin
      count = 0
      10.times do |count|
        thing = readline(' ')
        count += 1
      end
      assert_raise(EOFError) { readline }
    ensure
      teardownFiles
    end
  end
  
  def test_s_readlines1
    setupFiles
    begin
      lines = readlines
      assert_equal(4, lines.size)
    ensure
      teardownFiles
    end
  end

  def test_s_readlines2
    setupFiles
    begin
      lines = readlines(nil)
      assert_equal(2, lines.size)
    ensure
      teardownFiles
    end
  end

  def test_s_require
    assert_raise(LoadError) { require("gumby") }

    File.open("_dummy_req.rb", "w") do |f|
     f.print <<-EOM
      module Module_Require
        VAL = 234
      end
      EOM
    end
    assert(!defined? Module_Require)
    assert_not_nil(require("./_dummy_req.rb"))
    assert_not_nil(defined? Module_Require::VAL)
    assert_equal(234, Module_Require::VAL)
    assert($".include?("./_dummy_req.rb"))#"

    # Prove it isn;t reloaded
    File.open("_dummy_req.rb", "w") do |f|
     f.print <<-EOM
      module Module_Require
        VAL1 = 456
      end
      EOM
    end
    assert(!require("./_dummy_req.rb"))
    assert(!defined? Module_Require::VAL1)
    File.delete("./_dummy_req.rb")
  end

  def make_file(dirname, filename, number, &block)
    if FileTest.directory?(dirname)
      File.delete(*Dir[File.join(dirname, "**")])
      Dir.rmdir(dirname) 
    end
    Dir.mkdir(dirname)
    File.open(File.join(dirname, filename), "w") do |f|
     f.print <<-EOM
      module Module_Require
        VAL1 = #{number}
      end
      EOM
    end
    block.call
  ensure
    File.delete(*Dir[File.join(dirname, "**")])
    Dir.rmdir(dirname)
  end
  def test_s_require_changedpath
    old = $:.clone
    begin
      dirname1 = "_dummy_dir1" 
      dirname2 = "_dummy_dir2" 
      filename = 'a.rb'
      make_file(dirname1, filename, 1) do
        make_file(dirname2, filename, 2) do
          assert(!$".include?(filename))
          $:.unshift(dirname1)
          assert(require(filename))
          # the next line proves that Ruby excludes files, even though
          # we have changed the path, and the originally loaded file no
          # longer is first in the path.
          $:.unshift(dirname2)
          assert(!require(filename))
        end
      end
    ensure
      $:.clear; $:.concat(old) # restore path
    end
  end

  def test_s_scan
    $_ = "cruel world"
    assert_equal(["cruel","world"],          scan(/\w+/))
    assert_equal(["cru", "el ","wor"],       scan(/.../))
    assert_equal([["cru"], ["el "],["wor"]], scan(/(...)/))

    res=[]
    assert_equal($_, scan(/\w+/) { |w| res << w })
    assert_equal(["cruel","world"], res)

    res=[]
    assert_equal($_, scan(/.../) { |w| res << w })
    assert_equal(["cru", "el ","wor"], res)

    res=[]
    assert_equal($_, scan(/(...)/) { |w| res << w })
    assert_equal([["cru"], ["el "],["wor"]], res)
    
    assert_equal("cruel world", $_)
  end

  # Need a better test here
  def test_s_select
    assert_nil(select(nil, nil, nil, 0))
    assert_raise(ArgumentError) { select(nil, nil, nil, -1) }
    
    tf = Tempfile.new("tf")
    tf.close
    begin
      File.open(tf.path) do |file|
	res = select([file], [$stdout, $stderr], [], 1)
	assert_equal([[file], [$stdout, $stderr], []], res)
      end
    ensure
      tf.close(true)
    end
  end

  def trace_func(*params)
    params.slice!(4)     # Can't check the binding
    @res << params
  end

  def trace_func_test(file, line)
    __LINE__
  end

  def test_s_set_trace_func
    @res = []
    line = __LINE__
    set_trace_func(proc {|*a| trace_func(*a) })
    innerLine = trace_func_test(__FILE__, __LINE__)
    set_trace_func(nil)
    assert_equal(["line", __FILE__, line+2, :test_s_set_trace_func, TestKernel],
                 @res.shift)
    Version.less_than("1.8.0") do
      if defined? Object.allocate
	assert_equal(["c-call", __FILE__, line+2, :allocate, String],
		     @res.shift)
	assert_equal(["c-return", __FILE__, line+2, :allocate, String],
		     @res.shift)
      end
    end
    assert_equal(["call", __FILE__, innerLine-1, :trace_func_test, TestKernel],
                 @res.shift)
    assert_equal(["line", __FILE__, innerLine, :trace_func_test, TestKernel],
                 @res.shift)
    assert_equal("return", @res.shift[0])
  end

  class SMATest
    @@sms = []
    def SMATest.singleton_method_added(id)
      @@sms << id.id2name
    end
    def getsms() @@sms end
    def SMATest.b() end
  end
  def SMATest.c() end

  def test_s_singleton_method_added
    assert_bag_equal(%w(singleton_method_added b c), SMATest.new.getsms)
  end

  def test_s_sleep
    s1 = Time.now
    11.times { sleep 0.1 }
    s2 = Time.now
    assert((s2-s1) >= 1)
    assert((s2-s1) <= 3)

    duration = sleep(0.1)
    assert_instance_of(Fixnum, duration)
    assert(duration >= 0 && duration < 2)

    # Does Thread.run interrupt a sleep
    pThread = Thread.current
    Thread.new { sleep 1; pThread.run }
    s1 = Time.now
    duration = sleep 999
    s2 = Time.now
    assert(duration >= 0 && duration <= 2, "#{duration} not in 0..2")
    assert((s2-s1) >= 0)
    assert((s2-s1) <= 3)
  end

  def test_s_split
    assert_equal(nil,$;)
    $_ =  " a   b\t c "
    assert_equal(["a", "b", "c"], split)
    assert_equal(["a", "b", "c"], split(" "))

    $_ = " a | b | c "
    assert_equal([" a "," b "," c "], split("|"))

    $_ =  "aXXbXXcXX"
    assert_equal(["a", "b", "c"], split(/X./))

    $_ = "abc"
    assert_equal(["a", "b", "c"], split(//))

    $_ = "a|b|c"
    assert_equal(["a|b|c"],            split('|',1))
    assert_equal(["a", "b|c"],         split('|',2))
    assert_equal(["a","b","c"],        split('|',3))

    $_ = "a|b|c|"
    assert_equal(["a","b","c",""],     split('|',-1))

    $_ = "a|b|c||"
    assert_equal(["a","b","c","",""],  split('|',-1))

    $_ = "a||b|c|"
    assert_equal(["a","", "b", "c"],    split('|'))
    assert_equal(["a","", "b", "c",""], split('|',-1))
  end

  def test_s_sprintf
    assert_equal("00123", sprintf("%05d", 123))
    assert_equal("123  |00000001", sprintf("%-5s|%08x", 123, 1))
    x = sprintf("%3s %-4s%%foo %.0s%5d %#x%c%3.1f %b %x %X %#b %#x %#X",
                "hi",
                123,
                "never seen",
                456,
                0,
                ?A,
                3.0999,
                11,
                171,
                171,
                11,
                171,
                171)

    assert_equal(' hi 123 %foo   456 0x0A3.1 1011 ab AB 0b1011 0xab 0XAB', x)
  end

  def test_s_srand
    # Check that srand with an argument always returns the same value
    [ 0, 123, 45678980].each do |seed|
      srand(seed)
      expected = rand(2**30)
      5.times do
        assert_equal(seed, srand(seed))
        assert_equal(expected, rand(2**30))
      end
    end

    # Now check that the seed is random if called with no argument
    keys = {}
    values = {}
    dups = 0
    100.times do
      oldSeed = srand
      dups += 1 if keys[oldSeed]
      keys[oldSeed] = 1
      value = rand(2**30)
      dups += 1 if values[value]
      values[value] = 1
    end

    # this is a crap shoot, but more than 2 dups is suspicious
    assert(dups <= 2, "srand may not be randomized.")

    # and check that the seed is randomized for different runs of Ruby
    values = {}
    5.times do
      val = `#$interpreter -e "puts rand(2**30)"`
      assert($? == 0)
      val = val.to_i
      assert_nil(values[val])
      values[val] = 1
    end
  end

  def test_s_sub
    $_ = "hello"
    assert_equal("hello", sub(/x/,'*'))
    assert_equal("hello", $_)

    $_ = "hello"
    assert_equal("h*llo", sub(/[aeiou]/,'*'))
    assert_equal("h*llo", $_)

    $_ = "hello"
    assert_equal("h<e>llo", sub(/([aeiou])/,'<\1>'))
    assert_equal("h<e>llo", $_)

    $_ = "hello"
    assert_equal("104 ello", sub(/./) { |s| s[0].to_s+' '})
    assert_equal("104 ello", $_)

    $_ = "hello"
    assert_equal("HELL-o", sub(/(hell)(.)/) {|s| $1.upcase + '-' + $2})
    assert_equal("HELL-o", $_)

    $_ = "hello".taint
    assert(sub(/./,'X').tainted?)
  end

  def test_s_sub!
    $_ = "hello"
    assert_equal(nil,     sub!(/x/,'*'))
    assert_equal("hello", $_)

    $_ = "hello"
    assert_equal("h*llo", sub!(/[aeiou]/,'*'))
    assert_equal("h*llo", $_)

    $_ = "hello"
    assert_equal("h<e>llo", sub!(/([aeiou])/,'<\1>'))
    assert_equal("h<e>llo", $_)

    $_ = "hello"
    assert_equal("104 ello", sub!(/./) { |s| s[0].to_s+' '})
    assert_equal("104 ello", $_)

    $_ = "hello"
    assert_equal("HELL-o", sub!(/(hell)(.)/) {|s| $1.upcase + '-' + $2})
    assert_equal("HELL-o", $_)

    $_ = "hello".taint
    assert(sub!(/./,'X').tainted?)
  end

  def test_s_syscall
    skipping("platform specific")
  end

  def test_s_system
    open("xyzzy.dat", "w") { |f| f.puts "stuff" }
    tf = Tempfile.new("tf")
    begin

      # all in one string - wildcards get expanded
      tf.puts 'system("echo xy*y.dat")'
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |p|
        assert_equal("xyzzy.dat\n", p.gets)
      end

      # with two parameters, the '*' doesn't get expanded
      tf.open
      tf.puts 'system("echo", "xy*y.dat")'
      tf.close
      IO.popen("#$interpreter #{tf.path}") do |p|
        assert_equal("xy*y.dat\n", p.gets)
      end

    ensure
      tf.close(true)
      File.unlink "xyzzy.dat" if p
    end

    Version.less_than("1.9.0") do
      system("____this_is_a_bad command____")
      assert($? != 0)
    end
    Version.greater_or_equal("1.9.0") do
      assert_raise(Errno::ENOENT) {
        system("____this_is_a_bad command____")
      }
    end
  end

#  def test_s_test
#    # in TestKernelTest
#  end

  def test_s_throw
    # tested by test_s_catch
  end

  def test_s_trace_var
    val = nil
    p = proc { |val| }
    trace_var(:$_, p)
    $_ = "123"
    assert_equal($_, val)
    $_ = nil
    assert_equal($_, val)
    untrace_var("$_")
    $_ = "123"
    assert_equal(nil, val)
    assert_equal("123", $_)
  end

  def test_s_trap

    res = nil
    lastProc = proc { res = 1 }

    # 1. Check that an exception is thrown if we wait for a child and
    # there is no child.

    # "IGNORE" discards child termination status (but apparently not
    # under Cygwin/W2k

    Unix.only do
      trap "CHLD", "IGNORE"
      pid = fork
      exit unless pid
      sleep 1                     # ensure child has exited (ish)
      assert_raise(Errno::ECHILD) { Process.wait }
    end

    # 2. check that we run a proc as a handler when a child
    # terminates

    MsWin32.dont do
      trap("SIGCHLD", lastProc)
      fork { ; }
      Process.wait
      assert_equal(1, res)
    end

    # 3. Reset the signal handler (checking it returns the previous
    # value) and ensure that the proc doesn't get called

    MsWin32.dont do
      assert_equal(lastProc, trap("SIGCHLD", "DEFAULT"))
      res = nil
      fork { ; }
      Process.wait
      assert_nil(res)
    end

    # 4. test EXIT handling
    IO.popen(%{#$interpreter -e 'trap "EXIT", "exit! 123";exit 99'}).close
    assert_equal(123<<8, $?)
  end

  def test_s_untrace_var1
    trace_var(:$_, "puts 99")
    p = proc { |val| }
    trace_var("$_", p)
    assert_bag_equal(["puts 99", p], untrace_var(:$_))
    assert_equal([], untrace_var(:$_))
  end

  def test_s_untrace_var2
    trace_var(:$_, "puts 99")
    p = proc { |val| }
    trace_var("$_", p)
    assert_bag_equal(["puts 99", p], untrace_var(:$_))
  end

  def test_gvar_alias
    $foo = 3
    eval "alias $bar $foo"
    assert_equal(3, $bar)

    $bar = 4
    assert_equal(4, $foo)
    
  end

  def test_svar_alias
    eval "alias $foo $_"
    $_ = 1
    assert_equal(1, $foo)
    $foo = 2
    assert_equal(2, $_)
  end

end
