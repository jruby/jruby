package org.jruby.compiler.ir;
import java.math.BigInteger;

public class Bignum extends Constant
{
	final public BigInteger _value;

	public Bignum(BigInteger val) { _value = val; }
}
