str1 = 'white chocolate'
str2 = 'a1'
str3 = 'dog'

regex2 = /\d/
regex3 = /\w+/

benchmark 'gsub-string' do

  r1 = str1.gsub('white', 'dark')
end

benchmark "gsub-regex" do
  r2 = str2.gsub(regex2, '2')
end

benchmark "gsub-regex-block" do
  r3 = str3.gsub(regex3) { |animal| animal == 'dog' ? 'cat' : 'dog' }
end
