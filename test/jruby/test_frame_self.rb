require 'test/unit'

# Test for issue JIRA-2418
# This fails with "protected method `protected_2' called for #<Issue2418:0xfa39bb> (NoMethodError)"
# in JRuby 1.1.1, works in MRI 1.8
# Author: steen.lehmann@jayway.dk

class TestFrameSelf < Test::Unit::TestCase
  
  class Issue2418
    
    def unprotected
      protected_1('current')
    end

    def method_missing(method, *args)
      false
    end
    
  protected

    def protected_1(span_class)
      # This line causes the issue
      classnames = Array[*span_class]

      protected_2
    end

    def protected_2
      true
    end

  end

  def test_frame_self
    assert Issue2418.new.unprotected,
      "Frame is set correctly, and method_missing not called"
  end
  
end
