require File.dirname(__FILE__) + '/../spec_helper'

# class methods
# atime, basename, blockdev?, chardev?, chmod, chown, ctime, delete,
# directory?, dirname, executable?, executable_real?, exist?,
# exists?, expand_path, extname, file?, fnmatch, fnmatch?, ftype,
# grpowned?, identical?, join, lchmod, lchown, link, lstat, mtime,
# new, owned?, pipe?, readable?, readable_real?, readlink, rename,
# setgid?, setuid?, size, size?, socket?, split, stat, sticky?,
# symlink, symlink?, truncate, umask, unlink, utime, writable?,
# writable_real?, zero?

# atime, chmod, chown, ctime, flock, lstat, mtime, path, truncate

warn 'File specs are extremely lacking'

context "File class method" do
  specify "dirname should return all the components of filename except the last one" do
    File.dirname('/home/jason').should == '/home'
    File.dirname('/home/jason/poot.txt').should == '/home/jason'
    File.dirname('poot.txt').should == '.'
    File.dirname('/holy///schnikies//w00t.bin').should == '/holy///schnikies'
    File.dirname('').should == '.'
    File.dirname('/').should == '/'
    File.dirname('/////').should == '/'
  end

  specify "basename should return the last component of the filename" do
    File.basename('a').should == 'a'
    File.basename('/a').should == 'a'
    File.basename('/a/b').should == 'b'
    File.basename('/ab/ba/bag').should == 'bag'
    File.basename('/ab/ba/bag.txt').should == 'bag.txt'
    File.basename('/').should == '/'
    File.basename('/foo/bar/baz.rb', '.rb').should == 'baz'
    File.basename('baz.rb', 'z.rb').should == 'ba'
  end
end

context "File instance method" do
end
