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

# add some 1.8 tests from Rubicon
test_equal(':"hello world"', 'hello world'.intern.inspect)
test_equal(':"with \" char"', 'with " char'.intern.inspect)
test_equal(':"with \\\\ \" chars"', 'with \ " chars'.intern.inspect)

test_equal(Array, Symbol.all_symbols.class)
Symbol.all_symbols.each do |sym|
  test_equal(Symbol, sym.class)
end

test_equal(Symbol.all_symbols, Symbol.all_symbols.uniq)

symbols1 = Symbol.all_symbols
s1 = "jruby_unique_symbol_1".intern
s2 = "jruby_unique_symbol_2".intern
symbols2 = Symbol.all_symbols

res = symbols2 - symbols1

test_equal(2, res.size)
test_ok(res.member?(s1))
test_ok(res.member?(s2))


# extra inspect tests for 1.8
pairs = []
pairs << ['$',    ':"$"'   ]
pairs << ['$$',   ':$$'    ]
pairs << ['$$$',  ':"$$$"' ]
pairs << ['$0',   ':$0'    ]
pairs << ['$00',  ':"$00"' ]
pairs << ['$1',   ':$1'    ]
pairs << ['$11',  ':$11'   ]
pairs << ['$1L',  ':"$1L"' ]
pairs << ['$-',   ':$-'    ]
pairs << ['$--',  ':"$--"' ]
pairs << ['$-_',  ':$-_'   ]
pairs << ['$-I',  ':$-I'   ]
pairs << ['$-II', ':"$-II"']
pairs << ['$@',   ':$@'    ]
pairs << ['$@@',  ':"$@@"' ]
pairs << ['@@',   ':"@@"'  ]
pairs << ['@@F',  ':@@F'   ]
pairs << ['@@Foo_Bar', ':@@Foo_Bar']
pairs << ['@',    ':"@"'   ]
pairs << ['@F',   ':@F'    ]
pairs << ['@Foo_Bar', ':@Foo_Bar']
pairs << ['<',    ':<'     ]
pairs << ['>',    ':>'     ]
pairs << ['<<',   ':<<'    ]
pairs << ['>>',   ':>>'    ]
pairs << ['<<<',  ':"<<<"' ]
pairs << ['>>>',  ':">>>"' ]
pairs << ['<=',   ':<='    ]
pairs << ['<==',  ':"<=="' ]
pairs << ['<=>',  ':<=>'   ]
pairs << ['=',    ':"="'   ]
pairs << ['==',   ':=='    ]
pairs << ['===',  ':==='   ]
pairs << ['====', ':"===="']
pairs << ['*',    ':*'     ]
pairs << ['**',   ':**'    ]
pairs << ['***',  ':"***"' ]
pairs << ['+',    ':+'     ]
pairs << ['-',    ':-'     ]
pairs << ['+@',   ':+@'    ]
pairs << ['-@',   ':-@'    ]
pairs << ['++',   ':"++"'  ]
pairs << ['|',    ':|'     ]
pairs << ['||',   ':"||"'  ]
pairs << ['[]',   ':[]'    ]
pairs << ['[]=',  ':[]='   ]
pairs << ['[[',   ':"[["'  ]
pairs << ['foobar', ':foobar']
pairs << ['foo bar', ':"foo bar"']
pairs << ['foobar!', ':foobar!']
pairs << ['Foobar!', ':"Foobar!"']


for pair in pairs
  str, insp = pair
  test_equal(str.intern.inspect, insp)
end
