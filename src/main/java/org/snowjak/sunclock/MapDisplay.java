/**
 * 
 */
package org.snowjak.sunclock;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.snowjak.sunclock.Util.degreesToRadians;
import static org.snowjak.sunclock.Util.window;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.snowjak.sunclock.Options.DefinedOption;
import org.snowjak.sunclock.pool.Pool;
import org.snowjak.sunclock.pool.Pools;
import org.snowjak.sunclock.projection.Projection;

import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class MapDisplay extends Canvas {
	
	private static final long serialVersionUID = 378491777213729442L;
	private static final Logger LOG = LogManager.getLogger(MapDisplay.class);
	
	private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();
	private final ListeningExecutorService redrawExecutor = MoreExecutors
			.listeningDecorator(Executors.newCachedThreadPool());
	private ScheduledFuture<?> timerTask = null;
	
	private final Semaphore updateSemaphore = new Semaphore(1);
	
	private BufferedImage lightMap = null;
	
	private final JPopupMenu mapPopupMenu;
	private final ActionListener mapPopupActionListener;
	
	private Projection projection;
	private double resolution;
	private BufferedImage mapImage;
	private int mapWidth, mapHeight;
	private int mapOffsetX, mapOffsetY;
	
	public MapDisplay() {
		
		super();
		
		setProjection(Options.getValue(DefinedOption.PROJECTION));
		setLightMapResolution(Options.getValue(DefinedOption.LIGHT_RESOLUTION));
		Options.addUpdateListener(DefinedOption.PROJECTION, (oldProj, newProj) -> setProjection((Projection) newProj));
		Options.addUpdateListener(DefinedOption.LIGHT_RESOLUTION,
				(oldRes, newRes) -> setLightMapResolution((Integer) newRes));
		
		mapPopupActionListener = new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (e.getActionCommand().equals("save")) {
					
					if (mapImage == null || lightMap == null)
						return;
						
					//
					// Create the image to save.
					//
					
					final BufferedImage saveImage = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
					final Graphics2D saveGfx = saveImage.createGraphics();
					saveGfx.setColor(Color.black);
					saveGfx.clearRect(0, 0, mapWidth, mapHeight);
					saveGfx.drawImage(mapImage, 0, 0, mapWidth, mapHeight, null);
					saveGfx.drawImage(lightMap, 0, 0, mapWidth, mapHeight, null);
					
					try {
						saveImageWithDialog(saveImage);
					} catch (IllegalArgumentException ex) {
						
						LOG.error("Cannot save the current screen", ex);
						JOptionPane.showInternalMessageDialog(null,
								"Cannot save this image. For whatever reason, your Java environment doesn't have any compatible image-writers.",
								"Cannot Save Image", JOptionPane.ERROR_MESSAGE);
						
					} catch (IOException ex) {
						
						LOG.error("Cannot save the current screen", ex);
						JOptionPane.showInternalMessageDialog(null,
								"Cannot save this image -- the selected file could not be written. Consult the log for details.",
								"Cannot Save Image", JOptionPane.ERROR_MESSAGE);
						
					} finally {
						saveGfx.dispose();
					}
				}
			}
		};
		
		mapPopupMenu = new JPopupMenu();
		final JMenuItem saveAsItem = mapPopupMenu.add("Save as...");
		saveAsItem.setActionCommand("save");
		saveAsItem.addActionListener(mapPopupActionListener);
		
		addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				if (projection == null)
					return;
				
				final int mouseX = e.getPoint().x - mapOffsetX;
				final int mouseY = e.getPoint().y - mapOffsetY;
				
				if (mouseX < 0 || mouseY < 0)
					return;
				if (mouseX > mapWidth || mouseY > mapHeight)
					return;
				
				if (e.getButton() == MouseEvent.BUTTON1)
					logMapLatLong((double) mouseX / (double) mapWidth, 1d - (double) mouseY / (double) mapHeight);
				
				else if (e.getButton() == MouseEvent.BUTTON3)
					mapPopupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
			
		});
		addComponentListener(new ComponentAdapter() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				
				startTimer();
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				
				stopTimer();
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				
				try {
					updateSemaphore.acquire();
					
					if (mapImage == null)
						return;
					
					recalculateImageSize(e.getComponent().getWidth(), e.getComponent().getHeight());
					
					updateSemaphore.release();
				} catch (InterruptedException exp) {
					
				}
				
				startTimer();
				redrawLightMap();
			}
		});
	}
	
	private void recalculateImageSize(double windowWidth, double windowHeight) {
		
		if (mapImage == null)
			return;
		
		final double imageRatio = ((double) mapImage.getWidth()) / ((double) mapImage.getHeight());
		
		final double newRatio = windowWidth / windowHeight;
		
		if (imageRatio >= newRatio) {
			mapWidth = (int) Math.floor(windowWidth);
			mapHeight = (int) Math.floor(windowWidth / imageRatio);
		} else {
			mapHeight = (int) Math.floor(windowHeight);
			mapWidth = (int) Math.floor(windowHeight * imageRatio);
		}
		
		mapOffsetX = (int) ((windowWidth - (double) mapWidth) / 2d);
		mapOffsetY = (int) ((windowHeight - (double) mapHeight) / 2d);
		
		if (mapWidth > 0 && mapHeight > 0)
			if (lightMap == null || lightMap.getWidth() != mapWidth || lightMap.getHeight() != mapHeight)
				lightMap = new BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_INT_ARGB);
	}
	
	public void setProjection(Projection projection) {
		
		if (this.projection == projection)
			return;
		
		this.projection = projection;
		
		if (projection == null) {
			this.mapImage = null;
			return;
		}
		
		try (InputStream imageStream = MapDisplay.class.getClassLoader()
				.getResourceAsStream(projection.getImageName())) {
			
			mapImage = ImageIO.read(imageStream);
			
		} catch (IOException e) {
			LOG.error("Cannot open image {} associated with the Projection {}", projection.getImageName(),
					projection.toString());
			LOG.error("Received exception --", e);
		}
		
		recalculateImageSize(getWidth(), getHeight());
		redrawLightMap();
		repaint();
	}
	
	public void setLightMapResolution(int resolution) {
		
		if ((int) this.resolution == resolution)
			return;
		
		this.resolution = resolution;
		
		redrawLightMap();
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		
		try {
			updateSemaphore.acquire();
			
			if (mapImage != null)
				g.drawImage(mapImage, mapOffsetX, mapOffsetY, mapWidth, mapHeight, Color.BLACK, null);
			
			if (lightMap != null)
				g.drawImage(lightMap, mapOffsetX, mapOffsetY, null);
			
			updateSemaphore.release();
		} catch (InterruptedException e) {
			
		}
	}
	
	public void dispose() {
		
		timerExecutor.shutdown();
		redrawExecutor.shutdown();
	}
	
	private void redrawLightMap() {
		
		if (projection == null)
			return;
		if (lightMap == null)
			return;
		
		try {
			updateSemaphore.acquire();
			
			if (projection == null)
				return;
			if (lightMap == null)
				return;
			
			LOG.info("redrawing light-map ...");
			
			final Pool<DoublePair> pool = Pools.getPool(DoublePair.class);
			
			final ZonedDateTime now = ZonedDateTime.now();
			
			final int xSize = (int) max(floor((double) lightMap.getWidth() / resolution), 1d);
			final int ySize = (int) max(floor((double) lightMap.getHeight() / resolution), 1d);
			final int step = max(xSize, ySize);
			
			final List<ListenableFuture<?>> redrawFutures = new LinkedList<>();
			final CountDownLatch redrawComplete = new CountDownLatch(1);
			
			for (int x = 0; x < lightMap.getWidth(); x += step) {
				
				final double dx = (double) x / (double) lightMap.getWidth(),
						dx2 = (double) (x + step) / (double) lightMap.getWidth();
				
				for (int y = 0; y < lightMap.getHeight(); y += step) {
					
					final int px = x, py = y;
					
					redrawFutures.add(redrawExecutor.submit(() -> {
						
						final double dy = (double) py / (double) lightMap.getHeight(),
								dy2 = (double) (py + step) / (double) lightMap.getHeight();
						
						final DoublePair xy = pool.getInstance();
						xy.set((dx + dx2) / 2d, (dy + dy2) / 2d);
						final DoublePair latLong = projection.transformXY_LatLong(xy);
						
						final double exposure = 0.9d * sqrt(calculateSunExposure(latLong, now)) + 0.1d;
						
						final int exp = (int) (256d * exposure);
						final int invExp = 255 - exp;
						final int rgba = (invExp << 24) + (0xf << 16) + (0xf << 8) + (0xf);
						
						for (int tx = 0; tx < step; tx++)
							for (int ty = 0; ty < step; ty++) {
								if (px + tx >= lightMap.getWidth() || py + ty >= lightMap.getHeight())
									continue;
								lightMap.setRGB(px + tx, py + ty, rgba);
							}
						
						pool.retireInstance(xy);
						pool.retireInstance(latLong);
					}));
				}
			}
			
			Futures.whenAllComplete(redrawFutures).run(() -> redrawComplete.countDown(),
					MoreExecutors.directExecutor());
			redrawComplete.await();
			
			updateSemaphore.release();
		} catch (InterruptedException e) {
			
		}
	}
	
	public static double calculateSunExposure(DoublePair latLong, ZonedDateTime now) {
		
		//
		// Current epoch date/time (fractional) from UTC.
		//
		final double SECONDS_PER_DAY = 60d * 60d * 24d;
		final double MINUTES_PER_DAY = 60d * 24d;
		
		// final ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
		final ZonedDateTime utc = now.withZoneSameInstant(ZoneId.of("UTC"));
		final double nowUTC = (double) utc.getLong(ChronoField.DAY_OF_YEAR)
				+ (double) utc.getLong(ChronoField.SECOND_OF_DAY) / SECONDS_PER_DAY;
		
		//
		// Local date/time, given longitude.
		//
		final double latitude = degreesToRadians(window(latLong.getX(), -90d, +90d));
		final double longitude = degreesToRadians(window(latLong.getY(), -180d, +180d));
		final double nowLocal = nowUTC + (longitude / (2d * PI));
		final double nowLocalDay = floor(nowLocal);
		final double nowLocalFractional = nowLocal - nowLocalDay;
		
		final double longitudeCorrection = degreesToRadians((360d / 364d) * (nowLocal - 81d));
		// (minutes)
		final double equationOfTime = 9.87d * sin(2d * longitudeCorrection) - 7.53d * cos(longitudeCorrection)
				- 1.5d * sin(longitudeCorrection);
		
		final double localSolarNoonFractional = ((MINUTES_PER_DAY / 2d) - equationOfTime) / MINUTES_PER_DAY;
		final double localSolarNoon = nowLocalDay + localSolarNoonFractional;
		final double localSolarTime = (nowLocal * MINUTES_PER_DAY + equationOfTime) / MINUTES_PER_DAY;
		final double localTilSolarNoon = localSolarNoon - localSolarTime;
		
		//
		//
		//
		
		final double solarDeclination = degreesToRadians(23.45d)
				* sin(degreesToRadians(360d / 365d * (nowLocalDay - 81d)));
		final double solarAltitudeAtNoon = (PI / 2d) - latitude + solarDeclination;
		// final double airMassRatioAtNoon = abs(1d / sin(solarAltitudeAtNoon));
		
		final double solarHourAngle = 2d * PI * localTilSolarNoon;
		final double solarAltitudeNow = asin(
				cos(latitude) * cos(solarDeclination) * cos(solarHourAngle) + sin(latitude) * sin(solarDeclination));
		
		final double azimuth = asin(cos(solarDeclination) * sin(solarHourAngle) / cos(solarAltitudeNow));
		final double azimuthEN = PI - azimuth;
		final double azimuthWS = PI - azimuth - (2d * PI);
		
		// final double solarAzimuthAngle = (cos(solarAltitudeNow) >=
		// (tan(solarDeclination) / tan(latitude))) ? (azimuth)
		// : ((nowLocalFractional < 0.5d) ? (azimuthEN) : (azimuthWS));
		// final double airMassRatioNow = abs(1d / sin(solarAltitudeNow));
		
		final double cosAngleOfIncidence = // cos(solarAltitudeNow) * cos(solarAzimuthAngle) * sin(solarAltitudeNow);
				sin(solarAltitudeNow);
		
		return (cosAngleOfIncidence < 0d) ? 0d : cosAngleOfIncidence;
	}
	
	private void startTimer() {
		
		try {
			
			updateSemaphore.acquire();
			
			if (timerTask == null || timerTask.isDone()) {
				LOG.info("starting timer ...");
				timerTask = timerExecutor.scheduleWithFixedDelay(() -> redrawLightMap(), 0, 1, TimeUnit.MINUTES);
			}
			
			updateSemaphore.release();
			
		} catch (InterruptedException e) {
			
		}
	}
	
	private void stopTimer() {
		
		try {
			
			updateSemaphore.acquire();
			if (timerTask != null && !timerTask.isDone()) {
				LOG.info("stopping timer ...");
				timerTask.cancel(false);
			}
			updateSemaphore.release();
			
		} catch (InterruptedException e) {
			
		}
	}
	
	/**
	 * Given a point on the map, expressed as a fraction in [0,1] starting from
	 * bottom-left (i.e., south-west in most map-projections).
	 * 
	 * @param x
	 * @param y
	 */
	private void logMapLatLong(double x, double y) {
		
		final Pool<DoublePair> pool = Pools.getPool(DoublePair.class);
		final DoublePair xy = pool.getInstance();
		xy.set(x, y);
		final DoublePair latLong = projection.transformXY_LatLong(xy);
		
		final double exposure = calculateSunExposure(latLong, ZonedDateTime.now(Clock.systemUTC()));
		
		LOG.info("Click [" + xy.getX() + "," + xy.getY() + "] --> [" + latLong.getX() + "," + latLong.getY()
				+ "] -- exposure = " + exposure);
		pool.retireInstance(xy);
		pool.retireInstance(latLong);
	}
	
	/**
	 * Given a {@link BufferedImage}, perform the following:
	 * 
	 * <ul>
	 * <li>Determine which, if any, {@link ImageIO} writers can support writing this
	 * image</li>
	 * <li>Present a dialog so the user can select an appropriate file-name to save
	 * the image as</li>
	 * <li>If the user did not provide an extension, pick one from among the list of
	 * valid image-formats</li>
	 * <li>Save the image</li>
	 * </ul>
	 * 
	 * @param image
	 * @throws IllegalArgumentException
	 *             if the given image cannot be accepted by any known ImageWriters
	 * @throws IOException
	 *             if the image-file could not be written successfully
	 */
	private void saveImageWithDialog(BufferedImage image) throws IOException, IllegalArgumentException {
		
		//
		// Determine which image-writers, if any, are capable of writing our image.
		//
		final ImageTypeSpecifier imageTypeSpecifier = new ImageTypeSpecifier(image);
		
		final Set<ImageWriter> validImageWriters = new LinkedHashSet<>();
		for (String formatName : ImageIO.getWriterFormatNames())
			Iterators.addAll(validImageWriters, ImageIO.getImageWriters(imageTypeSpecifier, formatName));
		
		if (validImageWriters.isEmpty())
			throw new IllegalArgumentException("Cannot save given image: no ImageWriters can accept it.");
			
		//
		// Construct our list of image-format names and extensions.
		//
		final Set<String> extensions = new LinkedHashSet<>();
		final Map<String, Set<ImageWriter>> extensionsImageWriters = new HashMap<>();
		
		for (ImageWriter iw : validImageWriters) {
			for (String suffix : iw.getOriginatingProvider().getFileSuffixes()) {
				extensions.add(suffix);
				extensionsImageWriters.computeIfAbsent(suffix, (s) -> new HashSet<>()).add(iw);
			}
		}
		
		final StringBuilder description = new StringBuilder();
		for (String ext : extensions) {
			if (description.length() > 1)
				description.append(';');
			description.append("*.").append(ext);
		}
		description.append(')');
		
		//
		// Present the save-file picker
		//
		
		final JFileChooser saveFileChooser = new JFileChooser();
		saveFileChooser.setFileFilter(
				new FileNameExtensionFilter("Images (" + description.toString(), extensions.toArray(len -> {
					return new String[len];
				})));
		
		//
		// If the user did not choose "OK", then we don't need to go any further.
		//
		if (saveFileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
			
		//
		// Examine the selected file. See if it ends with one of our identified
		// suffixes.
		// At the same time, identify the format-name associated with the chosen
		// suffix (if any).
		//
		File saveFile = saveFileChooser.getSelectedFile();
		final String saveFileName = saveFile.getName();
		
		final String formatName;
		
		final String selectedExtension = extensions.stream().filter(ext -> saveFileName.toLowerCase().endsWith(ext))
				.findFirst().orElse(null);
		
		if (selectedExtension == null) {
			//
			// Apparently the user did not include an extension.
			// Therefore, we are free to choose any extension we please.
			//
			final ImageWriter anyImageWriter = validImageWriters.iterator().next();
			final String anyExtension = anyImageWriter.getOriginatingProvider().getFileSuffixes()[0];
			formatName = anyImageWriter.getOriginatingProvider().getFormatNames()[0];
			
			saveFile = new File(saveFile.getPath() + "." + anyExtension);
			
		} else {
			formatName = extensionsImageWriters.get(selectedExtension).iterator().next().getOriginatingProvider()
					.getFormatNames()[0];
		}
		
		//
		// Now all we have to do is save the image to the specified file.
		//
		ImageIO.write(image, formatName, saveFile);
	}
}