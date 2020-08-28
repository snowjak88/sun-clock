/**
 * 
 */
package org.snowjak.sunclock.pool;

/**
 * @author snowjak88
 *
 */
public interface Poolable {
	
	/**
	 * Reset this object so that it can safely be handed off to a Pool.
	 */
	public void reset();
}
