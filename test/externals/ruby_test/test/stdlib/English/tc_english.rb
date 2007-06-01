######################################################################
# tc_english.rb
#
# Test case for the English library. 
######################################################################
require 'test/unit'
require 'English'

class TC_English < Test::Unit::TestCase
   def setup
      $\ = "\n"
      $; = "--"
      $, = "++"
   end

   def test_english_argv
      assert_not_nil($ARGV)
      assert_equal($*, $ARGV)
   end

   # TODO: fork here to force $? to be set?
   def test_english_child_status
      if $?
         assert_not_nil($CHILD_STATUS)
         assert_equal($?, $CHILD_STATUS)
      end
   end

   def test_english_default_input
      assert_not_nil($DEFAULT_INPUT)
      assert_equal($<, $DEFAULT_INPUT)
   end

   def test_english_default_output
      assert_not_nil($DEFAULT_OUTPUT)
      assert_equal($>, $DEFAULT_OUTPUT)
   end

   def test_english_error_info
      if $!
         assert_not_nil($ERROR_INFO)
         assert_equal($!, $ERROR_INFO)
      end
   end

   def test_english_error_position
      if $@
         assert_not_nil($ERROR_POSITION)
         assert_equal($@, $ERROR_POSITION)
      end
   end

   def test_english_field_separator
      assert_not_nil($FS)
      assert_not_nil($FIELD_SEPARATOR)
      assert_equal($;, $FIELD_SEPARATOR)
   end

   def test_english_output_field_separator
      assert_not_nil($OFS)
      assert_not_nil($OUTPUT_FIELD_SEPARATOR)
      assert_equal($,, $OUTPUT_FIELD_SEPARATOR)
   end

   def test_english_ignore_case
      assert_not_nil($IGNORECASE)
      assert_equal($=, $IGNORECASE)
   end

   def test_english_input_line_number
      assert_not_nil($NR)
      assert_not_nil($INPUT_LINE_NUMBER)
      assert_equal($., $INPUT_LINE_NUMBER)
   end

   def test_english_input_record_separator
      assert_not_nil($RS)
      assert_not_nil($INPUT_RECORD_SEPARATOR)
      assert_equal($/, $INPUT_RECORD_SEPARATOR)
   end

   def test_english_output_record_separator
      assert_not_nil($ORS)
      assert_not_nil($OUTPUT_RECORD_SEPARATOR)
      assert_equal($\, $INPUT_RECORD_SEPARATOR)
   end

   def test_english_last_match_info
      "foo" =~ /foo/
      assert_not_nil($LAST_MATCH_INFO)
      assert_equal($~, $LAST_MATCH_INFO)
   end

   def test_english_last_paren_match
      "foo" =~ /(.*)/
      assert_not_nil($LAST_PAREN_MATCH)
      assert_equal($+, $LAST_PAREN_MATCH)
   end

   # TODO: improve this
   def test_english_last_read_line
      if $_
         assert_not_nil($LAST_READ_LINE)
         assert_equal($_, $LAST_READ_LINE)
      end
   end

   def test_english_loaded_features
      assert_not_nil($LOADED_FEATURES)
      assert_equal($", $LOADED_FEATURES)
   end

   def test_english_match
      "foo" =~ /foo/
      assert_not_nil($MATCH)
      assert_equal($&, $MATCH)
   end

   def test_english_pid
      assert_not_nil($PID)
      assert_not_nil($PROCESS_ID)
      assert_equal($$, $PID)
   end

   def test_english_postmatch
      "foo" =~ /foo/i
      assert_not_nil($POSTMATCH)
      assert_equal($', $POSTMATCH)
   end

   def test_english_prematch
      "foo" =~ /foo/i
      assert_not_nil($PREMATCH)
      assert_equal($`, $PREMATCH)
   end

   def test_english_program_name
      assert_not_nil($PROGRAM_NAME)
      assert_equal($0, $PROGRAM_NAME)
   end

   def teardown
      $\ = nil
      $; = nil
      $, = nil
   end
end
