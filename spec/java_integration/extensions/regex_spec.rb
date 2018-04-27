require File.dirname(__FILE__) + "/../spec_helper"

describe 'java.util.regex.Pattern' do

  it 'returns match start on match using =~' do
    i = java.util.regex.Pattern::CASE_INSENSITIVE
    p = java.util.regex.Pattern.compile("ab", i)
    expect( p =~ '0123a56b89abbab0ab' ).to eq 10
    expect( p =~ '0123a56b89' ).to be nil
  end

  it 'reports match with === as boolean' do
    p = java.util.regex.Pattern.compile("abb")
    expect( p === '0123a56b89abbab0ab' ).to be true
    expect( p === '0123a56b89' ).to be false
  end

  it 'returns matcher on match' do
    i = java.util.regex.Pattern::CASE_INSENSITIVE
    p = java.util.regex.Pattern.compile("ab", i)
    expect( p.match '0123a56b89abbab0AB' ).to be_a java.util.regex.Matcher
    expect( p.match '0123a56b89' ).to be nil
  end

  it 'reports casefold?' do
    i = java.util.regex.Pattern::CASE_INSENSITIVE
    p = java.util.regex.Pattern.compile("bar", i)
    expect( p.casefold? ).to be true
    p = java.util.regex.Pattern.compile("bar")
    expect( p.casefold? ).to be false
  end

end

describe 'java.util.regex.Matcher' do

  it 'returns regexp pattern' do
    p = java.util.regex.Pattern.compile("[a-f]")
    m = p.match('abcdef')
    expect( m.regexp ).to be p
  end

  it 'reports group begin/end and offset' do
    p = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
    m = p.match('THX1138.')
    expect( m.begin(0) ).to eq 1
    expect( m.begin(2) ).to eq 2
    expect( m.end(0) ).to eq 7
    expect( m.end(2) ).to eq 3
    expect( m.offset(0) ).to eq [1, 7]
    expect( m.offset(4) ).to eq [6, 7]

    p = java.util.regex.Pattern.compile("(?<foo>.)(.)(?<bar>.)")
    m = p.match('hoge')

    expect( m.begin(:foo) ).to eq 0
    expect( m.begin(:bar) ).to eq 2
    expect( m.end(:foo) ).to eq 1
    expect( m.end(:bar) ).to eq 3
    expect( m.offset(:bar) ).to eq [2, 3]
  end

  it 'reports size and string' do
    p = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
    m = p.match('THX1138.')
    expect( m.length ).to eq 5
    expect( m.size ).to eq 5
  end

  it 'returns matched string' do
    p = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
    m = p.match('THX1138.')

    # expect( m.string ).to eq 'HX1138' # different from Ruby
    expect( m.string ).to eq 'THX1138.'
  end

  it 'return captures as an array' do
    p = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
    m = p.match('THX1138.')

    expect( m.to_a ).to eq ['HX1138', 'H', 'X', '113', '8']
    expect( m.captures ).to eq ['H', 'X', '113', '8']

    # /([a-c]+)(b|d)?.*(\d+)/.match 'Xaaaeb111Z'
    # #<MatchData "aaaeb111" 1:"aaa" 2:nil 3:"1">
    p = java.util.regex.Pattern.compile("([a-c]+)(b|d)?.*?(\\d+)")
    m = p.match('Xaaaeb111Z')

    expect( m.to_a ).to eq ['aaaeb111', 'aaa', nil, '111']
    expect( m.captures ).to eq ['aaa', nil, '111']
  end

  it 'returns groups using []' do
    p = java.util.regex.Pattern.compile("(.)(.)(\\d+)(\\d)")
    m = p.match('THX1138.')

    expect( m[0] ).to eq 'HX1138'
    expect( m[1, 2] ).to eq ['H', 'X']
    expect( m[1..3] ).to eq ['H', 'X', '113']
  end

  it 'returns named groups using []' do
    p = java.util.regex.Pattern.compile("(?<foo>a+)b")
    m = p.match('ccaaab')

    expect( m[:foo] ).to eq 'aaa'
    expect( m['foo'] ).to eq 'aaa'
  end

  it "end does not blow stack" do
    # See https://jira.codehaus.org/browse/JRUBY-6571

    s = 'hello world'
    r = java.util.regex.Pattern.compile("[eo]")
    m = r.matcher(s)

    while m.find()
      expect{ m.end(0) }.not_to raise_error
    end
  end

end