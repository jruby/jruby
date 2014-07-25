require 'test/unit'
require 'yaml'

class TestVietnameseCharset < Test::Unit::TestCase
    def test_eej
        nguyen = "nguy\341\273\205n"
        assert_equal nguyen, YAML::load(nguyen.to_yaml) 
    end
end
