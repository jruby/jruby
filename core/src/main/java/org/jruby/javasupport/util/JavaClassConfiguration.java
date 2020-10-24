package org.jruby.javasupport.util;

public class JavaClassConfiguration
{
	public boolean callInitialize = false;
	public boolean allMethods = true;
	public boolean javaConstructable = true;
	
	// for java proxies
	public boolean allCtors  = false;
	public boolean rubyConstructable = true; // 
	public boolean splitSuper = true;
}
