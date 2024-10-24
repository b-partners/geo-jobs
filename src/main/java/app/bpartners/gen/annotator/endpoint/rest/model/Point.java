/*
 * Image Annotation API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: latest
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package app.bpartners.gen.annotator.endpoint.rest.model;

import app.bpartners.gen.annotator.endpoint.rest.OpenapiGenerated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringJoiner;

/** Point */
@JsonPropertyOrder({Point.JSON_PROPERTY_X, Point.JSON_PROPERTY_Y})
@OpenapiGenerated
@SuppressWarnings("all")
public class Point implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String JSON_PROPERTY_X = "x";
  private Double x;

  public static final String JSON_PROPERTY_Y = "y";
  private Double y;

  public Point() {}

  public Point x(Double x) {
    this.x = x;
    return this;
  }

  /**
   * Get x
   *
   * @return x
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_X)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Double getX() {
    return x;
  }

  @JsonProperty(JSON_PROPERTY_X)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setX(Double x) {
    this.x = x;
  }

  public Point y(Double y) {
    this.y = y;
    return this;
  }

  /**
   * Get y
   *
   * @return y
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_Y)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Double getY() {
    return y;
  }

  @JsonProperty(JSON_PROPERTY_Y)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setY(Double y) {
    this.y = y;
  }

  /** Return true if this Point object is equal to o. */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Point point = (Point) o;
    return Objects.equals(this.x, point.x) && Objects.equals(this.y, point.y);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Point {\n");
    sb.append("    x: ").append(toIndentedString(x)).append("\n");
    sb.append("    y: ").append(toIndentedString(y)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `x` to the URL query string
    if (getX() != null) {
      joiner.add(
          String.format(
              "%sx%s=%s",
              prefix,
              suffix,
              URLEncoder.encode(String.valueOf(getX()), StandardCharsets.UTF_8)
                  .replaceAll("\\+", "%20")));
    }

    // add `y` to the URL query string
    if (getY() != null) {
      joiner.add(
          String.format(
              "%sy%s=%s",
              prefix,
              suffix,
              URLEncoder.encode(String.valueOf(getY()), StandardCharsets.UTF_8)
                  .replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}
