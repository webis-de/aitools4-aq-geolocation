package de.aitools.aq.geolocating.timezones;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Based on
 * <a href="https://www.mkompf.com/java/tzdata.html">https://www.mkompf.com/java/tzdata.html</a>
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class CoordinatesMap {
  
  private final GeometryFactory geometryFactory;
  
  private final FilterFactory2 filterFactory;

  private final SimpleFeatureSource featureSource;
  
  private final String polygonProperty;

  public CoordinatesMap(final SimpleFeatureSource featureSource,
      final String polygonProperty)
  throws NullPointerException {
    if (featureSource == null) { throw new NullPointerException(); }
    if (polygonProperty == null) { throw new NullPointerException(); }
    this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
    this.filterFactory =
        CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints()); 
    this.featureSource = featureSource;
    this.polygonProperty = polygonProperty;
  }
  
  protected GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }
  
  protected SimpleFeatureSource getFeatureSource() {
    return this.featureSource;
  }
  
  @Override
  public String toString() {
    final SimpleFeatureType schema = this.featureSource.getSchema();
    return schema.getTypeName() + ": " + DataUtilities.encodeType(schema);
  }
  
  public FeatureIterator<SimpleFeature> find(
      final double longitude, final double latitude, final double dwithin)
  throws IOException {
    return this.find(new Coordinate(longitude, latitude), dwithin);
  }
  
  public FeatureIterator<SimpleFeature> find(
      final Coordinate coordinate, final double dwithin)
  throws IOException {
    final Point point = this.geometryFactory.createPoint(coordinate);
    return this.find(point, dwithin);
  }
  
  public FeatureIterator<SimpleFeature> find(
      final Point point, final double dwithin)
  throws IOException {
    final Filter toleranceFilter = this.filterFactory.dwithin(
        this.filterFactory.property(this.polygonProperty),
        this.filterFactory.literal(point),
        dwithin, "");
    final SimpleFeatureCollection filtered =
        this.featureSource.getFeatures(toleranceFilter);
    
    final FeatureIterator<SimpleFeature> iterator = filtered.features();
    return iterator;
  }
  
  public static SimpleFeatureSource loadShapeFile(final URL url)
  throws IOException {
    final ShapefileDataStoreFactory dataStoreFactory =
        new ShapefileDataStoreFactory();
    final Map<String, Serializable> parameters = new HashMap<>();
    parameters.put(
        ShapefileDataStoreFactory.URLP.key, url);
    parameters.put(
        ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
    
    final ShapefileDataStore dataStore = (ShapefileDataStore)
        dataStoreFactory.createDataStore(parameters);

    final SimpleFeatureSource featureSource = dataStore.getFeatureSource();
    return featureSource;
  }

}
