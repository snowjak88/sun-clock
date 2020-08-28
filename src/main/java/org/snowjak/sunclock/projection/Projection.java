/**
 * 
 */
package org.snowjak.sunclock.projection;

import static java.lang.Math.*;
import static java.lang.Math.atan;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.tan;
import static org.snowjak.sunclock.Util.clamp;
import static org.snowjak.sunclock.Util.degreesToRadians;
import static org.snowjak.sunclock.Util.radiansToDegrees;
import static org.snowjak.sunclock.Util.window;

import java.util.function.Function;

import org.snowjak.sunclock.DoublePair;
import org.snowjak.sunclock.pool.Pools;

/**
 * Encapsulates a projection of the globe onto a 2D plane. Provides:
 * <ul>
 * <li>Translation to/from X/Y and lat./long.</li>
 * <li>Name of the JPG (housed in /src/main/resources/) used to illustrate this
 * projection</li>
 * </ul>
 * 
 * @author snowjak88
 *
 */
public enum Projection {
	
	EQUIRECTANGULAR("projections/equirectangular.JPG", "Equirectangular", -90, +90, (latlng) -> {
		//
		// lat/long in [-90,+90] and [-180,180]
		// yields x/y in [0,1] and [0,1]
		//
		final double lat = degreesToRadians(window(latlng.getX(), -90, +90)),
				lng = degreesToRadians(window(latlng.getY(), -180, 180));
		final double x = lng, y = lat / PI;
		final DoublePair result = Pools.getPool(DoublePair.class).getInstance();
		result.set(x / (2d * PI) + 0.5d, y + 0.5d);
		return result;
	}, (xy) -> {
		//
		// x/y in [0,1] and [0,1]
		// yields lat/long in [-90,+90] and [-180,180]
		//
		final double x = (xy.getX() - 0.5d) * 2d * Math.PI, y = xy.getY() - 0.5d;
		final double lng = x, lat = y * PI;
		final DoublePair result = Pools.getPool(DoublePair.class).getInstance();
		result.set(window(radiansToDegrees(lat), -90, +90), window(radiansToDegrees(lng), -180, 180));
		return result;
	}),
	WEB_MERCATOR("projections/web-mercator.JPG", "Web Mercator", -85.051129, +85.051129, (latlng) -> {
		//
		// lat/long in [-85.051129,+85.051129] and [-180,180]
		// yields x/y in [0,1] and [0,1]
		//
		final double lat = degreesToRadians(clamp(window(latlng.getX(), -90, +90), -85.051129, +85.051129)),
				lng = degreesToRadians(window(latlng.getY(), -180, 180));
		
		final double x = lng, y = log(tan(PI / 4d + lat / 2d)) / (PI * 2d);
		final DoublePair result = Pools.getPool(DoublePair.class).getInstance();
		result.set(x / (2d * PI) + 0.5d, y + 0.5d);
		return result;
	}, (xy) -> {
		//
		// x/y in [0,1] and [0,1]
		// yields lat/long in [-85.051129,+85.051129] and [-180,180]
		//
		final double x = (xy.getX() - 0.5d) * 2d * Math.PI, y = xy.getY() - 0.5d;
		final double lng = x, lat = 2d * (atan(exp(PI * y * 2d)) - PI / 4d);
		final DoublePair result = Pools.getPool(DoublePair.class).getInstance();
		result.set(window(radiansToDegrees(lat), -90, +90), window(radiansToDegrees(lng), -180, 180));
		return result;
	}),
	CASSINI("projections/cassini.jpg", "Cassini", -90, +90, (latlng) -> {
		//
		// lat/long in [-90,+90] and [-180,180]
		// yields x/y in [0,1] and [0,1]
		//
		final double lat = degreesToRadians(window(latlng.getX(), -90, +90)),
				lng = degreesToRadians(window(latlng.getY(), -180, +180));
		final double x = asin(cos(lat) * sin(lng)), y = atan(tan(lat) / cos(lng));
		final DoublePair result = Pools.getPool(DoublePair.class).getInstance();
		result.set(x / (PI) + 0.5d, y / (2d * PI) + 0.5d);
		return result;
	}, (xy) -> {
		//
		// x/y in [0,1] and [0,1]
		// yields lat/long in [-90,+90] and [-180,180]
		//
		final double x = (xy.getX() - 0.5d) * Math.PI, y = (xy.getY() - 0.5d) * 2d * PI;
		final double lng = atan2(tan(x), cos(y)), lat = asin(sin(y) * cos(x));
		final DoublePair result = Pools.getPool(DoublePair.class).getInstance();
		result.set(window(radiansToDegrees(lat), -90, +90), window(radiansToDegrees(lng), -180, 180));
		return result;
	});
	
	private final String imageName, name;
	private final double minLatitude, maxLatitude;
	private final Function<DoublePair, DoublePair> toXyTransform, toLatLongTransform;
	
	Projection(String imageName, String name, double minLatitude, double maxLatitude,
			Function<DoublePair, DoublePair> toXyTransform, Function<DoublePair, DoublePair> toLatLongTransform) {
		
		this.imageName = imageName;
		this.name = name;
		this.minLatitude = minLatitude;
		this.maxLatitude = maxLatitude;
		this.toXyTransform = toXyTransform;
		this.toLatLongTransform = toLatLongTransform;
	}
	
	public String getImageName() {
		
		return imageName;
	}
	
	public String getName() {
		
		return name;
	}
	
	public double getMinLatitude() {
		
		return minLatitude;
	}
	
	public double getMaxLatitude() {
		
		return maxLatitude;
	}
	
	/**
	 * Given a pair of X/Y coordinates in [0,1], [0,1], transform these coordinates
	 * into latitude/longitude in [-90,+90], [0,360)
	 * 
	 * @param xy
	 * @return
	 */
	public DoublePair transformXY_LatLong(DoublePair xy) {
		
		return toLatLongTransform.apply(xy);
	}
	
	/**
	 * Given a pair of latitude/longitude coordinates in [-90,+90], [0,360),
	 * transform these coordinates into X/Y in [0,1], [0,1]
	 * 
	 * @param latLong
	 * @return
	 */
	public DoublePair transformLatLong_XY(DoublePair latLong) {
		
		return toXyTransform.apply(latLong);
	}
}
