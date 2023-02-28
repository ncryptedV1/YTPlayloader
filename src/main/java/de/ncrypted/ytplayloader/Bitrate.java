package de.ncrypted.ytplayloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * @author ncrypted
 */
public enum Bitrate {
  KB128(128), KB160(160), KB192(192), KB256(256), KB320(320), VBR5(130), VBR4(165), VBR3(175), VBR2(
      190), VBR1(225), VBR0(245);

  @Getter
  private static final List<Bitrate> values = new ArrayList<>();
  private static final Map<Integer, Bitrate> rateToValue = new HashMap<>();

  static {
    values.add(KB128);
    values.add(KB160);
    values.add(KB192);
    values.add(KB256);
    values.add(KB320);
    values.add(VBR5);
    values.add(VBR4);
    values.add(VBR3);
    values.add(VBR2);
    values.add(VBR1);
    values.add(VBR0);
    for (Bitrate bitrate : getValues()) {
      rateToValue.put(bitrate.getRate(), bitrate);
    }
  }

  @Getter
  private final int rate;

  Bitrate(int rate) {
    this.rate = rate;
  }

  private static Bitrate getByRate(int rate) {
    return rateToValue.get(rate);
  }

  public static Bitrate getByReadable(String readable) {
    if (readable.startsWith("VBR")) {
      return valueOf(readable.split(" ")[0]);
    } else {
      return getByRate(Integer.valueOf(readable.split(" ")[0]));
    }
  }

  public String getReadable() {
    if (isVBR()) {
      return name() + " ~" + getRate() + " kbps";
    } else {
      return getRate() + " kbps";
    }
  }

  public String getFfmpegArg() {
    if (isVBR()) {
      return name().substring(3);
    } else {
      return getRate() + "k";
    }
  }

  public boolean isVBR() {
    return name().startsWith("VBR");
  }
}
