require 'test/minirunit'
test_check "Test Symbol:"

# Should not be able to dup, clone or create a symbol
test_exception() {
  :hej.dup
}
test_exception() {
  :hej.clone
}
test_exception(NameError) {
  Symbol.new
}

# Tainting or freezing a symbol is ignored

s = :hej
test_ok(! s.tainted?)
test_equal(s, s.taint)
test_ok(! s.tainted?)

test_ok(! s.frozen?)
test_equal(s, s.freeze)
test_ok(! s.frozen?)

# Symbol#id2name, Fixnum#id2name

s = :alpha
test_ok("alpha", s.id2name)
id = s.to_i
test_ok("alpha", id.id2name)


x = :froboz
test_exception(TypeError) {
  def x.foo(other)
    "fools"
  end
}
