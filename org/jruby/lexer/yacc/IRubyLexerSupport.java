/*
 * IRubyLexerSupport.java - description
 * Created on 07.03.2002, 17:46:37
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.lexer.yacc;

import org.ablaf.common.*;
import org.ablaf.lexer.*;

/** Add some special Ruby  stuff.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IRubyLexerSupport extends ILexerSupport {
    public void setBuffer(String buffer, ISourcePosition startPosition);
}
