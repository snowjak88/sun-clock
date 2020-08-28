/**
 * 
 */
package org.snowjak.sunclock;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author snowjak88
 *
 */
public class Util {

	public static double window(double v, double min, double max) {
		
		final double width = max - min;
		
		while (v < min)
			v += width;
		while (v > max)
			v -= width;
		
		return v;
	}

	public static double degreesToRadians(double degrees) {
		
		return degrees / 180d * PI;
	}

	public static double radiansToDegrees(double radians) {
		
		return radians * 180d / PI;
	}

	public static double clamp(double v, double min, double max) {
		
		return max(min(v, max), min);
	}
	
}
