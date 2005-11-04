/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.exceptions;

/**
 * This class should be used for performance reasons if the 
 * Exception don't need a stack trace.
 * 
 * @author jpetersen
 */
public class JumpException extends RuntimeException {
    private static final long serialVersionUID = -228162532535826617L;

    public static final class JumpType {
		public static final JumpType BreakJump = new JumpType(0);
		public static final JumpType NextJump = new JumpType(1);
		public static final JumpType RedoJump = new JumpType(2);
		public static final JumpType RetryJump = new JumpType(3);
		public static final JumpType ReturnJump = new JumpType(4);
		public static final JumpType ThrowJump = new JumpType(5);
		public static final JumpType RaiseJump = new JumpType(6);
		
		private final int typeId;
		private JumpType(int typeId) {
			this.typeId = typeId;
		}
		public int getTypeId() {
			return typeId;
		}
	}
	
	private JumpType jumpType;
	private Object primaryData;
	private Object secondaryData;
	private Object tertiaryData;

    /**
     * Constructor for JumpException.
     */
    public JumpException(JumpType jumpType) {
        super();
        this.jumpType = jumpType;
    }

    /**
     * Constructor for JumpException.
     * @param msg
     */
    public JumpException(String msg, JumpType jumpType) {
        super(msg);
        this.jumpType = jumpType;
    }

    public JumpException(String msg, Throwable cause, JumpType jumpType) {
        super(msg, cause);
        this.jumpType = jumpType;
    }
    
    /** This method don't do anything for performance reasons.
     * 
     * @see Throwable#fillInStackTrace()
     */
    public Throwable fillInStackTrace() {
        return this;
    }    

    protected Throwable originalFillInStackTrace() {
        return super.fillInStackTrace();
    }
    
    public JumpType getJumpType() {
    	return jumpType;
    }
    
	/**
	 * @return Returns the data.
	 */
	public Object getPrimaryData() {
		return primaryData;
	}
	
	/**
	 * @param data The data to set.
	 */
	public void setPrimaryData(Object data) {
		this.primaryData = data;
	}
	
	/**
	 * @return Returns the secondaryData.
	 */
	public Object getSecondaryData() {
		return secondaryData;
	}
	
	/**
	 * @param secondaryData The secondaryData to set.
	 */
	public void setSecondaryData(Object secondaryData) {
		this.secondaryData = secondaryData;
	}
	
	/**
	 * @return Returns the tertiaryData.
	 */
	public Object getTertiaryData() {
		return tertiaryData;
	}
	
	/**
	 * @param tertiaryData The tertiaryData to set.
	 */
	public void setTertiaryData(Object tertiaryData) {
		this.tertiaryData = tertiaryData;
	}
}
