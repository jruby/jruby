require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#crypt" do
  # Note: MRI's documentation just says that the C stdlib function crypt() is
  # called.
  #
  # I'm not sure if crypt() is guaranteed to produce the same result across
  # different platforms. It seems that there is one standard UNIX implementation
  # of crypt(), but that alternative implementations are possible. See
  # http://www.unix.org.ua/orelly/networking/puis/ch08_06.htm
  it "returns a cryptographic hash of self by applying the UNIX crypt algorithm with the specified salt" do
    "".crypt("aa").should == "aaQSqAReePlq6"
    "nutmeg".crypt("Mi").should == "MiqkFWCm1fNJI"
    "ellen1".crypt("ri").should == "ri79kNd7V6.Sk"
    "Sharon".crypt("./").should == "./UY9Q7TvYJDg"
    "norahs".crypt("am").should == "amfIADT2iqjA."
    "norahs".crypt("7a").should == "7azfT5tIdyh0I"

    # Only uses first 8 chars of string
    "01234567".crypt("aa").should == "aa4c4gpuvCkSE"
    "012345678".crypt("aa").should == "aa4c4gpuvCkSE"
    "0123456789".crypt("aa").should == "aa4c4gpuvCkSE"

    # Only uses first 2 chars of salt
    "hello world".crypt("aa").should == "aayPz4hyPS1wI"
    "hello world".crypt("aab").should == "aayPz4hyPS1wI"
    "hello world".crypt("aabc").should == "aayPz4hyPS1wI"
  end

  platform_is :java do
    it "returns NULL bytes prepended to the string when the salt contains NULL bytes" do
      "hello".crypt("\x00\x00").should == "\x00\x00dR0/E99ehpU"
      "hello".crypt("\x00a").should == "\x00aeipc4xPxhGY"
      "hello".crypt("a\x00").should == "a\x00GJVggM8eWwo"
    end
  end

  platform_is_not :java do
    platform_is :openbsd do
      it "returns empty string if the first byte of the salt" do
        "hello".crypt("\x00\x00").should == ""
        "hello".crypt("\x00a").should == ""
      end

      it "returns the same character prepended to the string for the salt if the second character of the salt is a NULL byte" do
        "hello".crypt("a\x00").should == "aaGJVggM8eWwo"
        "hello".crypt("b\x00").should == "bb.LIhrI2NKCo"
      end
    end

    platform_is :darwin, /netbsd[a-z]*[1-5]\./ do
      it "returns '.' prepended to the string for each NULL byte the salt contains" do
        "hello".crypt("\x00\x00").should == "..dR0/E99ehpU"
        "hello".crypt("\x00a").should == ".aeipc4xPxhGY"
        "hello".crypt("a\x00").should == "a.GJVggM8eWwo"
      end
    end

    platform_is /netbsd[a-z]*(?![1-5]\.)/ do
      it "returns '*0' when the salt contains NULL bytes" do
        "hello".crypt("\x00\x00").should == "*0"
        "hello".crypt("\x00a").should == "*0"
        "hello".crypt("a\x00").should == "*0"
      end
    end

    platform_is :freebsd do
      it "returns an empty string when the salt starts with NULL bytes" do
        "hello".crypt("\x00\x00").should == ""
        "hello".crypt("\x00a").should == ""
      end

      it "ignores trailing NULL bytes in the salt but counts them for the 2 character minimum" do
        "hello".crypt("a\x00").should == "aaGJVggM8eWwo"
      end
    end

    # These specs are quarantined because this behavior isn't consistent
    # across different linux distributions and highly dependent of the
    # exact distribution. It seems like in newer Glibc versions this now
    # throws an error:
    #
    # https://github.com/rubinius/rubinius/issues/2168
    quarantine! do
      platform_is :linux do
        it "returns an empty string when the salt starts with NULL bytes" do
          "hello".crypt("\x00\x00").should == ""
          "hello".crypt("\x00a").should == ""
        end

        it "ignores trailing NULL bytes in the salt but counts them for the 2 character minimum" do
          "hello".crypt("a\x00").should == "aa1dYAU.hgL3A"
        end
      end
    end
  end

  it "raises an ArgumentError when the salt is shorter than two characters" do
    lambda { "hello".crypt("")  }.should raise_error(ArgumentError)
    lambda { "hello".crypt("f") }.should raise_error(ArgumentError)
  end

  it "calls #to_str to converts the salt arg to a String" do
    obj = mock('aa')
    obj.should_receive(:to_str).and_return("aa")

    "".crypt(obj).should == "aaQSqAReePlq6"
  end

  it "raises a type error when the salt arg can't be converted to a string" do
    lambda { "".crypt(5)         }.should raise_error(TypeError)
    lambda { "".crypt(mock('x')) }.should raise_error(TypeError)
  end

  it "taints the result if either salt or self is tainted" do
    tainted_salt = "aa"
    tainted_str = "hello"

    tainted_salt.taint
    tainted_str.taint

    "hello".crypt("aa").tainted?.should == false
    tainted_str.crypt("aa").tainted?.should == true
    "hello".crypt(tainted_salt).tainted?.should == true
    tainted_str.crypt(tainted_salt).tainted?.should == true
  end

  it "doesn't return subclass instances" do
    StringSpecs::MyString.new("hello").crypt("aa").should be_kind_of(String)
    "hello".crypt(StringSpecs::MyString.new("aa")).should be_kind_of(String)
    StringSpecs::MyString.new("hello").crypt(StringSpecs::MyString.new("aa")).should be_kind_of(String)
  end
end
