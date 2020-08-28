/**
 * 
 */
package org.snowjak.sunclock.pool;

/**
 * @author snowjak88
 *
 */
public class PoolException extends RuntimeException {
	
	private static final long serialVersionUID = 1061799812844951163L;
	
	/**
	 * 
	 */
	public PoolException() {
		
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param message
	 * @param cause
	 */
	public PoolException(String message, Throwable cause) {
		
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param message
	 */
	public PoolException(String message) {
		
		super(message);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @param cause
	 */
	public PoolException(Throwable cause) {
		
		super(cause);
		// TODO Auto-generated constructor stub
	}
	
}
