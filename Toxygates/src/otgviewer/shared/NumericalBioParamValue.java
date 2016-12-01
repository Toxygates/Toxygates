package otgviewer.shared;

import static t.viewer.client.Utils.formatNumber;

import javax.annotation.Nullable;

/**
 * A numerical biological parameter with a pathological range and a healthy range.
 */
@SuppressWarnings("serial")
public class NumericalBioParamValue extends BioParamValue {

  protected @Nullable Double lowerBound, upperBound;
  protected double value;

  public NumericalBioParamValue() {}

  /**
   * @param id
   * @param label
   * @param lowerBound Lower bound on the healthy range, if any
   * @param upperBound Upper bound on the healthy range, if any
   * @param value Observed value
   */
  public NumericalBioParamValue(String id, String label, @Nullable String section,
      @Nullable Double lowerBound,
      @Nullable Double upperBound, double value) {
    super(id, label, section);
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    this.value = value;
  }

  public NumericalBioParamValue(String id, String label,
      @Nullable String section,
      @Nullable Double lowerBound, @Nullable Double upperBound, 
      String value) {
    super(id, label, section);
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
    try {
      this.value = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      this.value = Double.NaN;
    }
  }

  public double value() {
    return value;
  }

  @Override
  public boolean isPathological() {
    return value < lowerBound || value > upperBound;
  }

  @Override
  public String displayValue() {
    return formatNumber(value);
  }

  @Override
  public String tooltip() {
    if (lowerBound != null && upperBound != null) {
      return "Normal range: " + formatNumber(lowerBound) + " <= x <= " + formatNumber(upperBound);
    }
    if (lowerBound != null) {
      return "Normal range: " + formatNumber(lowerBound) + " <= x";
    }
    if (upperBound != null) {
      return "Normal range: x <= " + formatNumber(upperBound);
    }
    return displayValue();
  }


}
