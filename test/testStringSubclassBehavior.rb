require 'test/minirunit'

class MyString < String
end

str = MyString.new("abcd")
str2 = MyString.new(" \\ndc\\nba\\n ")

test_equal(MyString, str.class)
test_equal(MyString, str2.class)
test_equal(MyString, str[0..2].class) # !!!
test_equal(MyString, str.split(/c/).first.class)
test_equal(MyString, str.split(/c/)[0].class)
str2.each_line do |line|
    test_equal(MyString, line.class) # !!
end

test_equal(MyString, str.upcase.class)
test_equal(MyString, str.downcase.class)
test_equal(MyString, str.capitalize.class)
test_equal(MyString, str.swapcase.class)

test_equal(String, str["bc"].class)
test_equal(MyString, str.strip.class)
test_equal(MyString, str.lstrip.class)
test_equal(MyString, str.rstrip.class)
test_equal(MyString, str.squeeze.class)
test_equal(MyString, str.tr("a","b").class)
test_equal(MyString, str.reverse.class)
test_equal(MyString, str.sub("a","b").class)
test_equal(MyString, str.sub(/a/,"b").class)
test_equal(MyString, str.gsub("a","b").class)
test_equal(MyString, str.gsub(/a/,"b").class)
test_equal(MyString, str.delete("bc").class)
test_equal(MyString, str2.chop.class)
test_equal(MyString, str2.chomp.class)
test_equal(MyString, str.dup.class)
test_equal(MyString, str.clone.class)
test_equal(MyString, str.center(20).class)
test_equal(MyString, str.ljust(20).class)
test_equal(MyString, str.rjust(20).class)
test_equal(MyString, str.scan(/.*/).first.class)
test_equal(MyString, str.slice(2,4).class)
test_equal(MyString, str.gsub('ab','dc').class)
test_equal(MyString, str.sub('sb','cd').class)
