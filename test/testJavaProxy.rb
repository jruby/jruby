require 'java'
require 'test/minirunit'
test_check "Test JavaProxy:"

class RoomTest
  include_package 'org.jruby.javasupport.test'
  include_package 'java.lang'

  java_alias :JString, :String
  
  def testObject
	  room1 = Room.new("Bedroom")
	  room2 = Room.new("Bedroom")
	  room3 = Room.new("Bathroom")
	  
	  test_ok(room1 == room2);
	  test_ok(room1 == room2.java_object);
	  test_ok(room1.java_object == room2.java_object)
	  test_ok(room1.java_object == room2)
	  
	  test_ok(room1 != room3)
	  test_ok(room1 != room3.java_object)
	  test_ok(room1.java_object != room3.java_object)
	  test_ok(room1.java_object != room3)
	  test_ok(room1.java_object != "Bedroom")
	  
	  test_ok("Bedroom" == room1.to_s)
	  test_ok(room1.to_s == "Bedroom")
	  
	  test_ok(room1.equal?(room1))
	  test_ok(!room1.equal?(room2))
	  
	  test_ok(JString.new("Bedroom").hashCode() == room1.hash())
	  test_ok(JString.new("Bathroom").hashCode() == room3.hash())
	  test_ok(room1.hash() != room3.hash())

      roomArray = Room[].new(1)
      roomArray[0] = room1
      test_equal(room1, roomArray[0])
      test_equal(1, roomArray.length)
    end
end

RoomTest.new.testObject
