package org.jruby.javasupport.test;

public class TestIntWrapperImpl
  implements TestIntWrapper
{
    private long n = 0;
        
    public TestIntWrapperImpl (long n) {
        this.n = n;
    }

    public long getInteger () {
        return n;
    }

    public String toString () {
        return "<java: " + n + ">";
    }

    public boolean equals (Object o) {
        if (o instanceof TestIntWrapper) {
            return this.getInteger() == ((TestIntWrapper)o).getInteger();
        }
        return false;
    }
}
