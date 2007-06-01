require 'test/unit'

class TestKanjiIndex < Test::Unit::TestCase
  def do_match(c)
    assert_equal(c.a_pos, Regexp.new('\G'+c.a,nil,c.kcode) =~ c.s)
    assert_equal(c.a_pos, Regexp.new('\A'+c.a,nil,c.kcode) =~ c.s)
    assert_equal(c.i_pos, Regexp.new(c.i,nil,c.kcode) =~ c.s)
    assert_equal(c.u_pos, Regexp.new(c.u,nil,c.kcode) =~ c.s)
    assert_equal(c.e_pos, Regexp.new(c.e,nil,c.kcode) =~ c.s)
  end
  def do_index_kanji(c)
    assert_equal(c.u_pos, c.s.index(Regexp.new('\G'+c.u,nil,c.kcode), c.u_pos))
    assert_equal(c.U, $&)
    assert_equal(c.u_pos, c.s.index(Regexp.new('\G'+c.u,nil,c.kcode), c.i_pos+1))
    assert_equal(c.U, $&)
    assert_equal(c.u_pos, c.s.index(Regexp.new(c.u,nil,c.kcode), c.i_pos+1))
    assert_nil(c.s.index(Regexp.new('\G'+c.u,nil,c.kcode), c.u_pos+1))
    assert_nil(c.s.index(Regexp.new(c.u,nil,c.kcode), c.u_pos+1))
  end
  def do_index_mixed(c)
    assert_equal(c.e_pos, c.s.index(Regexp.new('\G'+c.e,nil,c.kcode), c.e_pos))
    assert_equal(c.E, $&)
    assert_equal(c.e_pos-1, c.s.index(Regexp.new('\G.'+c.e,nil,c.kcode), c.u_pos+1))
    assert_equal('x'+c.E, $&)
    assert_equal(c.e_pos, c.s.index(Regexp.new(c.e,nil,c.kcode), c.u_pos))
    assert_equal(c.E, $&)
    assert_equal(c.e_pos, c.s.index(Regexp.new(c.e,nil,c.kcode), c.u_pos+1))
    assert_equal(c.E, $&)
    if c.U.size > 2
      assert_equal(c.e_pos, c.s.index(Regexp.new(c.e,nil,c.kcode), c.u_pos+2))
      assert_equal(c.E, $&)
    end
    assert_nil(c.s.index(Regexp.new('\G'+c.e,nil,c.kcode), c.o_pos))
    assert_nil(c.s.index(Regexp.new(c.e,nil,c.kcode), c.o_pos))
  end
  def do_rindex_kanji(c)
    assert_equal(c.u_pos, c.s.rindex(Regexp.new('\G'+c.u,nil,c.kcode), c.u_pos))
    assert_equal(c.U, $&)
    if c.U.size > 1
      assert_equal(c.u_pos, c.s.rindex(Regexp.new('\G'+c.u,nil,c.kcode), c.u_pos+1))
      assert_equal(c.U, $&)
    end
    assert_equal(c.u_pos, c.s.rindex(Regexp.new(c.u,nil,c.kcode), c.u_pos+1))
    assert_equal(c.U, $&)
    assert_equal(c.u_pos, c.s.rindex(Regexp.new(c.u,nil,c.kcode), c.e_pos))
    assert_equal(c.U, $&)
    if c.I.size > 1
      assert_nil(c.s.rindex(Regexp.new('\G'+c.u,nil,c.kcode), c.i_pos+1))
      assert_nil(c.s.rindex(Regexp.new(c.u,nil,c.kcode), c.i_pos+1))
    end
  end
  def do_rindex_mixed(c)
    assert_equal(c.e_pos, c.s.rindex(Regexp.new('\G'+c.e,nil,c.kcode), c.e_pos))
    assert_equal(c.E, $&)
    if c.E.size > 1
      assert_equal(c.e_pos, c.s.rindex(Regexp.new('\G'+c.e,nil,c.kcode), c.e_pos+1))
      assert_equal(c.E, $&)
    end
    assert_equal(c.e_pos-1, c.s.rindex(Regexp.new('\G.'+c.e,nil,c.kcode), c.e_pos-1))
    assert_equal('x'+c.E, $&)
    assert_equal(c.e_pos, c.s.rindex(Regexp.new(c.e,nil,c.kcode), c.e_pos+1))
    assert_equal(c.E, $&)
    assert_equal(c.e_pos, c.s.rindex(Regexp.new(c.e,nil,c.kcode), c.o_pos))
    assert_equal(c.E, $&)
    assert_equal(c.u_pos, c.s.rindex(Regexp.new(c.u,nil,c.kcode), c.o_pos))
    assert_equal(c.U, $&)
    assert_equal(c.u_pos, c.s.rindex(Regexp.new(c.u,nil,c.kcode), c.o_pos-1))
    assert_equal(c.U, $&)
    if c.E.size > 2
      assert_equal(c.e_pos, c.s.rindex(Regexp.new(c.e,nil,c.kcode), c.o_pos-1))
      assert_equal(c.E, $&)
    end
    assert_nil(c.s.rindex(Regexp.new('\G'+c.e,nil,c.kcode), c.e_pos-1))
    assert_nil(c.s.rindex(Regexp.new(c.e,nil,c.kcode), c.e_pos-1))
  end

  class Charset
    attr_reader :s, :A, :I, :U, :E, :O, :a, :i, :u, :e, :o
    attr_reader :a_pos, :i_pos, :u_pos, :e_pos, :o_pos, :kcode

    def initialize(k,a,i,u,e,o)
      @A,@I,@U,@E,@O=a.freeze,i.freeze,u.freeze,e.freeze,o.freeze
      @kcode = k.freeze
      @s = [@A,@I,@U,"x",@E,@O].join.freeze
      @a, @i, @u, @e, @o = [@A,@I,@U,@E,@O].collect {|s| Regexp.quote(s).freeze}
      @a_pos = 0
      @i_pos = @A.size
      @u_pos = @i_pos + @I.size
      @e_pos = @u_pos + @U.size + 1
      @o_pos = @e_pos + @E.size
    end
  end

  BINARY = Charset.new('NONE',"A","I","U","E","O")
  EUC = Charset.new('EUC',"\244\242","\244\244","\244\246","\244\250","\244\252")
  SJIS = Charset.new('SJIS',"\202\240","\202\242","\202\244","\202\246","\202\250")
  UTF8 = Charset.new('UTF8',"\343\201\202","\343\201\204","\343\201\206","\343\201\210","\343\201\212")
  tests = instance_methods.grep(/^do_/)
  %w[BINARY EUC SJIS UTF8].each do |c|
    tests.each do |t|
      eval "def test_#{c}_#{t.split('_',2)[1]};#{t}(#{c});end"
    end
  end
end