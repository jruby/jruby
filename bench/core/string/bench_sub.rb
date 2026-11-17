str1 = 'white chocolate'
str2 = 'a1'
str3 = 'dog'

regex2 = /\d/
regex3 = /\w+/

benchmark 'sub-string' do
  r1 = str1.sub('white', 'dark')
end

benchmark "sub-regex" do
  r2 = str2.sub(regex2, '2')
end

benchmark "sub-regex-block" do
  r3 = str3.sub(regex3) { |animal| animal == 'dog' ? 'cat' : 'dog' }
end
