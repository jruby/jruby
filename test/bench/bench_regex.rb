require 'benchmark'

def bench_regexp(regex, string, amount)
  puts "REGEXP: #{regex} =~ #{string} for #{amount} times"
  puts Benchmark.realtime { amount.times { regex =~ string } }
end

amount = 10_000_000

bench_regexp(/a/, " a", 4*amount)
bench_regexp(/.*?=/, "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/", amount)
bench_regexp(/^(.*?)=(.*?);/, "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/", amount)
bench_regexp(/.*_p/, "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/", 4*amount)
bench_regexp(/.*=/, "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/", 4*amount)
bench_regexp(/a.*?[b-z]{2,4}aaaaaa/, "afdgdsgderaabxxaaaaaaaaaaaaaaaaaaaaaaaa", amount)
bench_regexp(/:\/\//, "/shop/viewCategory.shtml?category=DOGS", amount)
bench_regexp(/^\w+\:\/\/[^\/]+(\/.*|$)$/, "/shop/viewCategory.shtml?category=DOGS", amount)
bench_regexp(/\A\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A\/shop\/signonForm\.shtml\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A\/shop\/newAccountForm\.shtml\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A\/shop\/newAccount\.shtml\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A\/shop\/viewCart\.shtml\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A\/shop\/index\.shtml\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A\/shop\/viewCategory\.shtml\/?\Z/, "/shop/viewCategory.shtml", amount)
bench_regexp(/\A(?:::)?([A-Z]\w*(?:::[A-Z]\w*)*)\z/, "CategoriesController", amount)
bench_regexp(/\Ainsert/, "SELECT * FROM sessions WHERE (session_id = '1b341ffe23b5298676d535fcabd3d0d7')  LIMIT 1", amount)
bench_regexp(/\A\(?\s*(select|show)/, "SELECT * FROM sessions WHERE (session_id = '1b341ffe23b5298676d535fcabd3d0d7')  LIMIT 1", amount)
bench_regexp(/.*?\n/, "1b341ffe23b5298676d535fcabd3d0d7", amount)
bench_regexp(/^find_(all_by|by)_([_a-zA-Z]\w*)$/, "find_by_string_id", amount)
bench_regexp(/\.rjs$/, "categories/show.rhtml", amount)
bench_regexp(/^[-a-z]+:\/\//, "petstore.css", amount)
bench_regexp(/^get$/, "", amount)
bench_regexp(/^post$/, "", amount)
bench_regexp(/^[^:]+/, "www.example.com", amount)
bench_regexp(/(=|\?|_before_type_cast)$/, "updated_on", amount)
bench_regexp(/^(.*?)=(.*?);/, "_petstore_session_id=1b341ffe23b5298676d535fcabd3d0d7; path=/", amount)
