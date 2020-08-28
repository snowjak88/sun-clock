/**
 * 
 */
package org.snowjak.sunclock.pool;

import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * @author snowjak88
 *
 */
public class Pool<T extends Poolable> {
	
	private final Supplier<T> constructor;
	private final LinkedList<T> availableInstances = new LinkedList<>();
	
	/**
	 * Construct a new Pool, which will use the given constructor to create new
	 * object-instances on demand.
	 * 
	 * @param constructor
	 */
	public Pool(Supplier<T> constructor) {
		
		this.constructor = constructor;
	}
	
	/**
	 * Attempt to get an instance from the Pool. If the Pool has no instances ready,
	 * then the constructor (specified at construction-time) is used to create a new
	 * instance.
	 * 
	 * @return
	 */
	public T getInstance() {
		
		synchronized (availableInstances) {
			if (!availableInstances.isEmpty())
				return availableInstances.removeFirst();
		}
		
		return constructor.get();
	}
	
	/**
	 * Retire the given instance to the Pool. {@link Poolable#reset() reset()} is
	 * invoked on the instance automatically.
	 * 
	 * @param instance
	 */
	public void retireInstance(T instance) {
		
		instance.reset();
		synchronized (availableInstances) {
			availableInstances.add(instance);
		}
	}
}
