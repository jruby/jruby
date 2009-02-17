###############################################################################
# tc_etc.rb
#
# Tests for the Etc module. This test case goes out of its way to avoid using
# the Test::Helper module because that module uses Etc internally. Thus, I
# want to avoid using Etc to test itself. :)
#
# Note that these tests should not be run on MS Windows.
#
# TODO: Need some group member tests.
###############################################################################
require 'test/unit'
require 'rbconfig'
require 'etc'
include Config

class TC_Etc < Test::Unit::TestCase
   def setup
      @login = `whoami`.chomp
      @name  = CONFIG['host_os'] =~ /darwin|osx/i ? 'mailman' : @login
      @group = IO.readlines('/etc/group').grep(/sys:/i).first.chomp.split(':')
      @user  = IO.readlines('/etc/passwd').grep(/#{@name}/i).first.chomp.split(':')

      @pwent  = nil
      @pwent2 = nil
      @grent  = nil
      @grent2 = nil

      # Some platforms use a double colon for the first separator, so we need
      # to delete any empty elements that appear as a result of this.
      @group.delete_if{ |e| e == "" || e == "*" }
      @user.delete_if{ |e| e == "" || e == "*" }

      @group_name = @group[0]
      @group_id   = @group[1].to_i
      @group_mem  = @group[2]

      @user_name = @user[0]
      @user_id   = @user[1].to_i
   end

   def test_endgrent
      assert_respond_to(Etc, :endgrent)
      assert_nothing_raised{ Etc.endgrent }
   end

   def test_endpwent
      assert_respond_to(Etc, :endpwent)
      assert_nothing_raised{ Etc.endpwent }
   end

   def test_getgrent
      assert_respond_to(Etc, :getgrent)
      assert_nothing_raised{ @grent = Etc.getgrent }
      assert_kind_of(Struct::Group, @grent)
   end

   # Buggy
   def test_getgrgid
      msg = '-Known issue on some platforms-'
      assert_nothing_raised{ @grent = Etc.getgrgid(@group_id) }
      assert_equal(@group_name, @grent.name, msg)
      assert_equal(@group_id, @grent.gid, msg)
   end

   def test_getgrnam
      assert_nothing_raised{ @grent = Etc.getgrnam('sys') }
      assert_equal(@group_name, @grent.name)
      assert_equal(@group_id, @grent.gid)
   end

   def test_getlogin
      assert_respond_to(Etc, :getlogin)
      assert_nothing_raised{ Etc.getlogin }
      assert_equal(@login, Etc.getlogin)
      assert_kind_of(String, Etc.getlogin)
   end

   def test_getpwent
      assert_respond_to(Etc, :getpwent)
      assert_nothing_raised{ Etc.getpwent }
      assert_kind_of(Struct::Passwd, Etc.getpwent)
   end

   def test_getpwnam
      assert_respond_to(Etc, :getpwnam)
      assert_nothing_raised{ @pwent = Etc.getpwnam(@login) }
      assert_kind_of(Struct::Passwd, Etc.getpwnam(@login))
      assert_equal(@login, @pwent.name)
   end

   def test_getpwuid
      assert_respond_to(Etc, :getpwuid)
      assert_nothing_raised{ @pwent = Etc.getpwuid(@user_id) }
      assert_kind_of(Struct::Passwd, Etc.getpwuid(@user_id))
#      assert_equal(@name, @pwent.name)
   end

   # Without a block the pointer increments one entry on each call, so the
   # first and subsequent reads should never be identical.
   #
   def test_group_no_block
      assert_respond_to(Etc, :group)
      assert_nothing_raised{ @grent = Etc.group }
      assert_kind_of(Struct::Group, @grent)
      assert_nothing_raised{ @grent2 = Etc.group }
      assert(@grent.name != @grent2.name)
   end

   # The block form resets the pointer, so the first read should be identical
   # each time we call it in block form.
   #
   def test_group_with_block
      assert_respond_to(Etc, :group)
      assert_nothing_raised{ Etc.group{ |g| @grent = g; break } }
      assert_kind_of(Struct::Group, @grent)
      assert_nothing_raised{ Etc.group{ |g| @grent2 = g; break } }
      assert(@grent.name == @grent2.name)
   end

   # The same comment for test_group_no_block applies here.
   #
   def test_passwd_no_block
      assert_respond_to(Etc, :passwd)
      assert_nothing_raised{ @pwent = Etc.passwd }
      assert_kind_of(Struct::Passwd, @pwent)
      assert_nothing_raised{ @pwent2 = Etc.group }
      assert(@pwent.name != @pwent2.name)
   end

   # The same comment for test_group_with_block applies here.
   #
   def test_passwd_with_block
      assert_respond_to(Etc, :passwd)
      assert_nothing_raised{ Etc.passwd{ |u| @pwent = u; break } }
      assert_kind_of(Struct::Passwd, @pwent)
      assert_nothing_raised{ Etc.group{ |u| @pwent2 = u; break } }
      assert(@pwent.name == @pwent2.name)
   end

   def test_setgrent
      assert_respond_to(Etc, :setgrent)
      assert_nothing_raised{ Etc.setgrent }
      assert_nil(Etc.setgrent)
   end

   def test_setpwent
      assert_respond_to(Etc, :setpwent)
      assert_nothing_raised{ Etc.setpwent }
   end

   # Test the struct members I'm fairly certain exist on all flavors of Unix.
   #
   def test_struct_passwd_basic
      assert_nothing_raised{ @pwent = Etc.getpwent }
      assert_respond_to(@pwent, :name)
      assert_respond_to(@pwent, :uid)
      assert_respond_to(@pwent, :gid)
      assert_respond_to(@pwent, :dir)
      assert_respond_to(@pwent, :shell)
   end

   # Because we don't know the exact values for any given passwd entry,
   # we can only check the basic types of data returned.
   #
   def test_struct_passwd
      assert_nothing_raised{ @pwent = Etc.getpwent }
      assert_kind_of(String, @pwent.name)
      assert_kind_of(Integer, @pwent.uid)
      assert_kind_of(Integer, @pwent.gid)
      assert_kind_of(String, @pwent.dir)
      assert_kind_of(String, @pwent.shell)
   end

   def test_struct_group_basic
      assert_nothing_raised{ @grent = Etc.getgrent }
      assert_respond_to(@grent, :name)
      assert_respond_to(@grent, :gid)
      assert_respond_to(@grent, :mem)
   end

   def test_struct_group
      assert_nothing_raised{ @grent = Etc.getgrent }
      assert_kind_of(String, @grent.name)
      assert_kind_of(Integer, @grent.gid)
      assert_kind_of(Array, @grent.mem)
   end

   def teardown
      @login  = nil
      @name   = nil
      @pwent  = nil
      @pwent2 = nil
      @grent  = nil
      @grent2 = nil
      @group  = nil
      @user  = nil
   end
end
