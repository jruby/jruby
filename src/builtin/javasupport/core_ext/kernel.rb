# Create convenience methods for top-level java packages so we do not need to prefix
# with 'Java::'.  We undef these methods within Package in case we run into 'com.foo.com'.
[:java, :javax, :com, :org].each do |meth|
 Java::Package.create_package(meth, meth.to_s, Kernel)
 Java::Package.send(:undef_method, meth)
end