package org.jruby.javasupport.util;

public class JavaClassConfiguration
{
	public boolean callInitialize = false;
	public boolean allMethods = true;
	public boolean javaConstructable = true;
	public Class<?>[][] extraCtors = null;
	
	// for java proxies
	public boolean allCtors  = false;
	public boolean rubyConstructable = true; // 
	public boolean splitSuper = true;
	public boolean IroCtors = true;
}
