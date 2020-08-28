/**
 * 
 */
package org.snowjak.sunclock.pool;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * @author snowjak88
 *
 */
public class Pools {
	
	private static final HashMap<Class<? extends Poolable>, Pool<?>> pools = new HashMap<>();
	
	@SuppressWarnings("unchecked")
	public static <T extends Poolable, P extends Pool<T>> P getPool(Class<T> poolableType) {
		
		synchronized (pools) {
			if (!pools.containsKey(poolableType)) {
				try {
					final Constructor<T> defaultConstructor = poolableType.getConstructor();
					pools.put(poolableType, new Pool<T>(() -> {
						try {
							return defaultConstructor.newInstance();
						} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
							throw new PoolException("Cannot create an instance of the given type ("
									+ poolableType.getName() + ") -- cannot invoke default constructor!", e);
						}
					}));
				} catch (NoSuchMethodException e) {
					throw new PoolException("Cannot create a Pool for the given type (" + poolableType.getName()
							+ ") -- no default constructor available!", e);
				}
			}
			
			return (P) pools.get(poolableType);
		}
	}
}
