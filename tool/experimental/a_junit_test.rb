require 'junit'

class MyJUnitTest
  include JUnit
  
  +Test()
  def test_something_broken
    assert "a" == "b"
  end

  +Test()
  def test_something_good
    assert "a" == "a"
  end
end

JUnit.run(MyJUnitTest)