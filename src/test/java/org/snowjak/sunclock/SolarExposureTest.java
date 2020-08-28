/**
 * 
 */
package org.snowjak.sunclock;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import org.junit.Test;
import org.snowjak.sunclock.pool.Pools;

/**
 * @author snowjak88
 *
 */
public class SolarExposureTest {
	
	@Test
	public void testCalculateSunExposure_1() {
		
		final ZonedDateTime now = ZonedDateTime.of(2020, 11, 14, 14, 34, 0, 0, TimeZone.getTimeZone("MST").toZoneId())
				.withZoneSameInstant(ZoneId.of("UTC"));
		final DoublePair latLong = Pools.getPool(DoublePair.class).getInstance();
		latLong.set(32.28, -106.75);
		
		final double exposure = MapDisplay.calculateSunExposure(latLong, now);
		
		assertEquals(.417, exposure, 5e-2);
	}
	
	@Test
	public void testCalculateSunExposure_2() {
		
		final ZonedDateTime now = ZonedDateTime.of(2020, 6, 6, 8, 23, 0, 0, TimeZone.getTimeZone("MST").toZoneId())
				.withZoneSameInstant(ZoneId.of("UTC"));
		final DoublePair latLong = Pools.getPool(DoublePair.class).getInstance();
		latLong.set(32.28, -106.75);
		
		final double exposure = MapDisplay.calculateSunExposure(latLong, now);
		
		assertEquals(.666, exposure, 5e-2);
	}
	
}
