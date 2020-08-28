/**
 * 
 */
package org.snowjak.sunclock;

import java.util.function.DoubleUnaryOperator;

import org.snowjak.sunclock.pool.Poolable;

/**
 * Denotes a pair of (primitive) double values.
 * 
 * @author snowjak88
 *
 */
public class DoublePair implements Poolable {
	
	private double x, y;
	
	public DoublePair() {
		
		this(0, 0);
	}
	
	public DoublePair(double x, double y) {
		
		this.x = x;
		this.y = y;
	}
	
	public double getX() {
		
		return x;
	}
	
	public double getY() {
		
		return y;
	}
	
	public void set(double x, double y) {
		
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Apply the given {@link DoubleUnaryOperator operators} to X and Y. This method
	 * mutates this DoublePair's value.
	 * 
	 * @param xOperator
	 * @param yOperator
	 * @return this DoublePair, for method-chaining
	 */
	public DoublePair applyDirectly(DoubleUnaryOperator xOperator, DoubleUnaryOperator yOperator) {
		
		set(xOperator.applyAsDouble(x), yOperator.applyAsDouble(y));
		return this;
	}
	
	@Override
	public void reset() {
		
		x = 0;
		y = 0;
	}
}
