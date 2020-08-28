/**
 * 
 */
package org.snowjak.sunclock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.snowjak.sunclock.projection.Projection;

/**
 * @author snowjak88
 *
 */
public class EquirectangularProjectionTest {
	
	@Test
	public void reflexive() {
		
		final DoublePair latLong = new DoublePair(37, -137);
		
		final DoublePair result = Projection.EQUIRECTANGULAR
				.transformXY_LatLong(Projection.EQUIRECTANGULAR.transformLatLong_XY(latLong));
		
		assertNotNull("EQUIRECTANGULAR reflexive -- shouldn't be null", result);
		assertEquals("EQUIRECTANGULAR reflexive -- (X)", 37d, result.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR reflexive -- (Y)", -137d, result.getY(), 1e-2);
	}
	
	@Test
	public void toXY_centered() {
		
		final DoublePair latLong = new DoublePair(0, 0);
		
		final DoublePair xy = Projection.EQUIRECTANGULAR.transformLatLong_XY(latLong);
		
		assertNotNull("EQUIRECTANGULAR Lat-Long -> XY -- shouldn't be null", xy);
		assertEquals("EQUIRECTANGULAR Lat-Long -> XY -- 0/0 should map to 0.5/0.5 (X)", 0.5d, xy.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR Lat-Long -> XY -- 0/0 should map to 0.5/0.5 (Y)", 0.5d, xy.getY(), 1e-2);
	}
	
	@Test
	public void toXY_westEquator() {
		
		final DoublePair latLong = new DoublePair(0, 181);
		
		final DoublePair xy = Projection.EQUIRECTANGULAR.transformLatLong_XY(latLong);
		
		assertNotNull("EQUIRECTANGULAR Lat-Long -> XY -- shouldn't be null", xy);
		assertEquals("EQUIRECTANGULAR Lat-Long -> XY -- 0/181 should map to 0/0.5 (X)", 0d, xy.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR Lat-Long -> XY -- 0/181 should map to 0/0.5 (Y)", 0.5d, xy.getY(), 1e-2);
	}
	
	@Test
	public void toXY_eastEquator() {
		
		final DoublePair latLong = new DoublePair(0, 179);
		
		final DoublePair xy = Projection.EQUIRECTANGULAR.transformLatLong_XY(latLong);
		
		assertNotNull("EQUIRECTANGULAR Lat-Long -> XY -- shouldn't be null", xy);
		assertEquals("EQUIRECTANGULAR Lat-Long -> XY -- 0/179 should map to 1/0.5 (X)", 1d, xy.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR Lat-Long -> XY -- 0/179 should map to 0/0.5 (Y)", 0.5d, xy.getY(), 1e-2);
	}
	
	@Test
	public void toLatLong_centered() {
		
		final DoublePair xy = new DoublePair(0.5, 0.5);
		
		final DoublePair latLong = Projection.EQUIRECTANGULAR.transformXY_LatLong(xy);
		
		assertNotNull("EQUIRECTANGULAR XY -> Lat-Long -- shouldn't be null", latLong);
		assertEquals("EQUIRECTANGULAR XY -> Lat-Long -- 0.5/0.5 should map to 0/0 (lat)", 0d, latLong.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR XY -> Lat-Long -- 0.5/0.5 should map to 0/0 (long)", 0d, latLong.getY(), 1e-2);
	}
	
	@Test
	public void toLatLong_westEquator() {
		
		final DoublePair xy = new DoublePair(0, 0.5);
		
		final DoublePair latLong = Projection.EQUIRECTANGULAR.transformXY_LatLong(xy);
		
		assertNotNull("EQUIRECTANGULAR XY -> Lat-Long -- shouldn't be null", latLong);
		assertEquals("EQUIRECTANGULAR XY -> Lat-Long -- 0/0.5 should map to 0/-180 (lat)", 0d, latLong.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR XY -> Lat-Long -- 0/0.5 should map to 0/-180 (long)", -180d, latLong.getY(), 1e-2);
	}
	
	@Test
	public void toLatLong_eastEquator() {
		
		final DoublePair xy = new DoublePair(1, 0.5);
		
		final DoublePair latLong = Projection.EQUIRECTANGULAR.transformXY_LatLong(xy);
		
		assertNotNull("EQUIRECTANGULAR XY -> Lat-Long -- shouldn't be null", latLong);
		assertEquals("EQUIRECTANGULAR XY -> Lat-Long -- 1/0.5 should map to 0/180 (lat)", 0d, latLong.getX(), 1e-2);
		assertEquals("EQUIRECTANGULAR XY -> Lat-Long -- 1/0.5 should map to 0/180 (long)", 180d, latLong.getY(), 1e-2);
	}
}
