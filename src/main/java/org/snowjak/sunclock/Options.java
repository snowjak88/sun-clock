/**
 * 
 */
package org.snowjak.sunclock;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.snowjak.sunclock.projection.Projection;

/**
 * Encapsulates application options.
 * 
 * @author snowjak88
 *
 */
public class Options {
	
	private static final Logger LOG = LogManager.getLogger(Options.class);
	
	/**
	 * The filename of the application's properties file.
	 */
	public static final String PROPERTIES_FILENAME = "sun-clock.properties";
	
	/**
	 * Defines those options available to the application.
	 * 
	 * @author snowjak88
	 *
	 */
	public enum DefinedOption {
		
		PROJECTION(new Option<Projection>((proj, prop) -> {
			if (proj == null)
				prop.setProperty("projections.name", "");
			else
				prop.setProperty("projections.name", proj.name());
		}, (prop) -> {
			if (!prop.containsKey("projections.name"))
				return null;
			final String name = prop.getProperty("projections.name").trim();
			if (name.isBlank() || name.equalsIgnoreCase("null"))
				return null;
			try {
				return Projection.valueOf(name);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}, Projection.EQUIRECTANGULAR)),
		/**
		 * Defines the resolution of the light-map.
		 * <p>
		 * The light-map is calculated as a square grid, consisting of at most N squares
		 * on each side (with the lower bound set by the fact that each square must be
		 * at least 1 pixel in size).
		 * </p>
		 */
		LIGHT_RESOLUTION(new Option<Integer>((res, prop) -> {
			if (res == null || res <= 0)
				prop.setProperty("light-map.resolution", "");
			else
				prop.setProperty("light-map.resolution", res.toString());
		}, (prop) -> {
			if (!prop.containsKey("light-map.resolution"))
				return null;
			try {
				final Integer val = Integer.parseInt(prop.getProperty("light-map.resolution"));
				if (val <= 0)
					return null;
				return val;
			} catch (NumberFormatException e) {
				return null;
			}
		}, Integer.valueOf(128)));
		
		private final Option<?> option;
		
		private DefinedOption(Option<?> option) {
			
			this.option = option;
		}
		
		private Option<?> getOption() {
			
			return option;
		}
	}
	
	/**
	 * Get the value associated with the given {@link DefinedOption}.
	 * 
	 * @param <T>
	 * @param option
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(DefinedOption option) {
		
		return (T) option.getOption().getValue();
	}
	
	/**
	 * Set the given {@link DefinedOption} to have the given value.
	 * 
	 * @param <T>
	 * @param option
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public static <T> void setValue(DefinedOption option, T value) {
		
		((Option<T>) (option.getOption())).setValue(value);
	}
	
	/**
	 * Associate the given {@link OptionUpdateListener} with the given
	 * {@link DefinedOption}.
	 * 
	 * @param <T>
	 * @param option
	 * @param listener
	 */
	@SuppressWarnings("unchecked")
	public static <T> void addUpdateListener(DefinedOption option, OptionUpdateListener<T> listener) {
		
		((Option<T>) option.getOption()).addUpdateListener(listener);
	}
	
	/**
	 * De-aAssociate the given {@link OptionUpdateListener} with the given
	 * {@link DefinedOption}.
	 * 
	 * @param <T>
	 * @param option
	 * @param listener
	 */
	@SuppressWarnings("unchecked")
	public static <T> void removeUpdateListener(DefinedOption option, OptionUpdateListener<T> listener) {
		
		((Option<T>) option.getOption()).removeUpdateListener(listener);
	}
	
	/**
	 * Attempt to read in application options from the properties file (i.e.,
	 * {@link #PROPERTIES_FILENAME}).
	 * 
	 * @throws FileNotFoundException
	 *             if the given properties file doesn't exist
	 */
	public static void readFromProperties() throws FileNotFoundException {
		
		try (InputStream propertiesStream = new FileInputStream(PROPERTIES_FILENAME)) {
			final Properties properties = new Properties();
			properties.load(propertiesStream);
			
			for (DefinedOption opt : DefinedOption.values())
				opt.getOption().populateFromProperties(properties);
		} catch (IOException e) {
			LOG.error("Encountered an unexpected exception while reading the properties file.", e);
		}
	}
	
	/**
	 * Attempt to write the application options to the properties file (i.e.,
	 * {@link #PROPERTIES_FILENAME}).
	 */
	public static void writeToProperties() {
		
		try (OutputStream propertiesStream = new FileOutputStream(PROPERTIES_FILENAME)) {
			
			final Properties properties = new Properties();
			
			for (DefinedOption opt : DefinedOption.values())
				opt.getOption().writeToProperties(properties);
			
			final ZonedDateTime now = ZonedDateTime.now();
			properties.store(propertiesStream, "sun-clock application properties");
			
		} catch (IOException e) {
			LOG.error("Cannot save properties to [" + PROPERTIES_FILENAME + "]", e);
		}
	}
	
	/**
	 * This constructor is off-limites -- you're not meant to instantiate an
	 * instance of this class.
	 */
	private Options() {
		
	}
	
	private static class Option<T> {
		
		private final BiConsumer<T, Properties> propertyWriter;
		private final Function<Properties, T> propertyReader;
		private final Set<OptionUpdateListener<T>> listeners = new LinkedHashSet<>();
		
		private T value;
		
		/**
		 * Create a new Option, with no (i.e., {@code null}) initial value.
		 * 
		 * @param type
		 *            the explicit type this Option is valued in
		 * @param propertyWriter
		 *            a method for converting an instance of this option into one or
		 *            more values in the given {@link Properties}
		 * @param propertyReader
		 *            a method for converting Properties into an instance of this option
		 */
		public Option(BiConsumer<T, Properties> propertyWriter, Function<Properties, T> propertyReader) {
			
			this(propertyWriter, propertyReader, null);
		}
		
		/**
		 * Create a new Option.
		 * 
		 * @param type
		 *            the explicit type this Option is valued in
		 * @param propertyWriter
		 *            a method for converting an instance of this option into one or
		 *            more values in the given {@link Properties}
		 * @param propertyReader
		 *            a method for converting Properties into an instance of this option
		 * @param initialValue
		 *            the initial value of this option, or {@code null} if none
		 */
		public Option(BiConsumer<T, Properties> propertyWriter, Function<Properties, T> propertyReader,
				T initialValue) {
			
			assert (propertyWriter != null);
			assert (propertyReader != null);
			
			this.propertyWriter = propertyWriter;
			this.propertyReader = propertyReader;
			this.value = initialValue;
		}
		
		public T getValue() {
			
			return value;
		}
		
		public void setValue(T value) {
			
			final T oldValue = value;
			this.value = value;
			synchronized (listeners) {
				listeners.forEach(l -> l.receiveUpdatedOption(oldValue, value));
			}
		}
		
		public void populateFromProperties(Properties properties) {
			
			final T readValue = propertyReader.apply(properties);
			if (readValue != null)
				setValue(readValue);
		}
		
		public void writeToProperties(Properties properties) {
			
			propertyWriter.accept(value, properties);
		}
		
		public void addUpdateListener(OptionUpdateListener<T> listener) {
			
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
		
		public void removeUpdateListener(OptionUpdateListener<T> listener) {
			
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
	}
	
	@FunctionalInterface
	public interface OptionUpdateListener<T> {
		
		public void receiveUpdatedOption(T oldValue, T newValue);
	}
}
