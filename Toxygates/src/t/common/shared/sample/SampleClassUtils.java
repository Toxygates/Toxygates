package t.common.shared.sample;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import t.common.shared.DataSchema;
import t.common.shared.HasClass;
import t.model.SampleClass;
import t.model.sample.Attribute;

public class SampleClassUtils {
  public static SampleClass asMacroClass(SampleClass sc, DataSchema schema) {
    return sc.copyOnly(Arrays.asList(schema.macroParameters()));
  }

  public static SampleClass asUnit(SampleClass sc, DataSchema schema) {
    List<Attribute> keys = new ArrayList<Attribute>();
    for (Attribute attribute : schema.macroParameters()) {
      keys.add(attribute);
    }
    keys.add(schema.majorParameter());
    keys.add(schema.mediumParameter());
    keys.add(schema.minorParameter());
    return sc.copyOnly(keys);
  }
  
  public static String label(SampleClass sc, DataSchema schema) {
    StringBuilder sb = new StringBuilder();
    for (Attribute attribute : schema.macroParameters()) {
      sb.append(sc.get(attribute)).append("/");
    }
    return sb.toString();
  }
  
  public static String tripleString(SampleClass sc, DataSchema schema) {
    String maj = sc.get(schema.majorParameter());
    String med = sc.get(schema.mediumParameter());
    String min = sc.get(schema.minorParameter());
    return maj + "/" + med + "/" + min;
  }

  public static Stream<String> collectInner(List<? extends HasClass> from, Attribute key) {
    return from.stream().map(hc -> hc.sampleClass().get(key)).
      filter(k -> k != null).distinct();
  }

  public static boolean strictCompatible(SampleClass sc, HasClass hc2) {
    return sc.strictCompatible(hc2.sampleClass());
  }

  public static List<SampleClass> classes(List<? extends HasClass> from) {
    List<SampleClass> r = new ArrayList<SampleClass>();
    for (HasClass hc : from) {
      r.add(hc.sampleClass());
    }
    return r;
  }

  public static <S extends Sample, HS extends HasSamples<S>> 
  Stream<String> getMajors(
      DataSchema schema, HS hasSamples) {
    return getMajors(schema, hasSamples, (SampleClass) null);
  }

  public static <S extends Sample, HS extends HasSamples<S>> 
  Stream<String> getMajors(
      DataSchema schema, HS hasSamples, @Nullable SampleClass sc) {
    List<S> sList = Arrays.asList(hasSamples.getSamples());
    List<S> filtered = (sc != null) ? filter(sc, sList) : sList;
    return collectInner(filtered, schema.majorParameter());
  }
  
  public static <T extends HasClass> List<T> filter(SampleClass sc, List<T> from) {
    List<T> r = new ArrayList<T>();
    for (T t : from) {
      if (sc.compatible(t.sampleClass())) {
        r.add(t);
      }
    }
    return r;
  }
}
