
def constantize_full(camel_cased_word)
  unless /\A(?:::)?([A-Z]\w*(?:::[A-Z]\w*)*)\z/ =~ camel_cased_word
    raise NameError, "#{camel_cased_word.inspect} is not a valid constant name!"
  end

  Object.module_eval("::#{$1}", __FILE__, __LINE__)
end

def constantize_no_re(camel_cased_word)
  Object.module_eval("::#{camel_cased_word}", __FILE__, __LINE__)
end

def constantize_no_eval(camel_cased_word)
  unless /\A(?:::)?([A-Z]\w*(?:::[A-Z]\w*)*)\z/ =~ camel_cased_word
    raise NameError, "#{camel_cased_word.inspect} is not a valid constant name!"
  end
end

require 'benchmark'

Integer(ARGV[0] || 10).times do 
  Benchmark.bm(30) do |bm|
    bm.report("constantize_full(\"Hash\")") { 500_000.times { constantize_full("Hash") }}
    bm.report("constantize_no_re(\"Hash\")") { 500_000.times { constantize_no_re("Hash") }}
    bm.report("constantize_no_eval(\"Hash\")") { 500_000.times { constantize_no_eval("Hash") }}
  end
end
