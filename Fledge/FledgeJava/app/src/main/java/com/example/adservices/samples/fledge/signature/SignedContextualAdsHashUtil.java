package com.example.adservices.samples.fledge.signature;

import static com.example.adservices.samples.fledge.sampleapp.MainActivity.TAG;

import android.adservices.adselection.AdWithBid;
import android.adservices.adselection.SignedContextualAds;
import android.adservices.common.AdData;
import android.adservices.common.AdFilters;
import android.adservices.common.AppInstallFilters;
import android.adservices.common.FrequencyCapFilters;
import android.adservices.common.KeyedFrequencyCap;
import android.annotation.SuppressLint;
import android.os.ext.SdkExtensions;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Creates a hash by serializing {@link SignedContextualAds}.
 *
 * <p>This serialization should only used for signing {@link SignedContextualAds}. It's not suitable
 * for deserialization.
 *
 * <p>Serialization rules are described in {@link SignedContextualAds}. Ad tech and PPAPI should
 * perform the same serialization for signatures to be verified with no problem
 */
public class SignedContextualAdsHashUtil {
  private static final String FIELD_SEPARATOR = "|";
  private static final String ENUMERATOR_SEPARATOR = ",";
  private static final String BUYER = "buyer=";
  private static final String DECISION_LOGIC_URI = "decision_logic_uri=";
  private static final String ADS_WITH_BID = "ads_with_bid=";
  private static final String AD_DATA = "ad_data=";
  private static final String BID = "bid=";
  private static final String AD_COUNTER_KEYS = "ad_counter_keys=";
  private static final String AD_FILTERS = "ad_filters=";
  private static final String AD_RENDER_ID = "ad_render_id=";
  private static final String METADATA = "metadata=";
  private static final String RENDER_URI = "render_uri=";
  private static final String APP_INSTALL_FILTERS = "app_install_filters=";
  private static final String FREQUENCY_CAP_FILTERS = "frequency_cap_filters=";
  private static final String PACKAGE_NAMES = "package_names=";
  private static final String KEYED_FREQUENCY_CAPS_FOR_WIN_EVENTS =
      "keyed_frequency_caps_for_win_events=";
  private static final String KEYED_FREQUENCY_CAPS_FOR_IMPRESSION_EVENTS =
      "keyed_frequency_caps_for_impression_events=";
  private static final String KEYED_FREQUENCY_CAPS_FOR_VIEW_EVENTS =
      "keyed_frequency_caps_for_view_events=";
  private static final String KEYED_FREQUENCY_CAPS_FOR_CLICK_EVENTS =
      "keyed_frequency_caps_for_click_events=";
  private static final String AD_COUNTER_KEY = "ad_counter_key=";
  private static final String INTERVAL = "interval=";
  private static final String MAX_COUNT = "max_count=";
  private final ByteArrayOutputStream mByteArrayStream;

  public SignedContextualAdsHashUtil() {
    this.mByteArrayStream = new ByteArrayOutputStream();
  }

  /**
   * Serializes given {@link SignedContextualAds} object
   *
   * <p>See {@link SignedContextualAds} for serialization rules
   *
   * @param ads contextual ad to serializer
   * @return byte array representation of the serialization
   */
  public byte[] serialize(SignedContextualAds ads) {
    writeContextualAds(ads);
    return mByteArrayStream.toByteArray();
  }

  private void writeContextualAds(SignedContextualAds ads) {
    writeString(BUYER);
    writeField(ads.getBuyer().toString(), this::writeString);

    writeString(DECISION_LOGIC_URI);
    writeField(ads.getDecisionLogicUri().toString(), this::writeString);

    writeString(ADS_WITH_BID);
    writeList(ads.getAdsWithBid(), this::writeAdWithBid);
  }

  private void writeAdWithBid(AdWithBid adWithBid) {
    writeString(AD_DATA);
    writeField(adWithBid.getAdData(), this::writeAdData);

    writeString(BID);
    writeField(adWithBid.getBid(), this::writeDouble);
  }

  private void writeAdData(AdData adData) {
    if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 4) {
      writeString(AD_COUNTER_KEYS);
      writeSet(adData.getAdCounterKeys(), this::writeInt);

      if (!Objects.isNull(adData.getAdFilters())) {
        writeString(AD_FILTERS);
        writeField(adData.getAdFilters(), this::writeAdFilters);
      }

      if (!Objects.isNull(adData.getAdRenderId())) {
        writeString(AD_RENDER_ID);
        writeField(adData.getAdRenderId(), this::writeString);
      }

      writeString(METADATA);
      writeField(adData.getMetadata(), this::writeString);

      writeString(RENDER_URI);
      writeField(adData.getRenderUri().toString(), this::writeString);
    }
  }

  private void writeAdFilters(AdFilters adFilters) {
    if (!Objects.isNull(adFilters.getAppInstallFilters())) {
      writeString(APP_INSTALL_FILTERS);
      writeField(adFilters.getAppInstallFilters(), this::writeAppInstallFilter);
    }

    if (!Objects.isNull(adFilters.getFrequencyCapFilters())) {
      writeString(FREQUENCY_CAP_FILTERS);
      writeField(adFilters.getFrequencyCapFilters(), this::writeFrequencyCapFilters);
    }
  }

  private void writeAppInstallFilter(AppInstallFilters appInstallFilters) {
    writeString(PACKAGE_NAMES);
    writeSet(appInstallFilters.getPackageNames(), this::writeString);
  }

  private void writeFrequencyCapFilters(FrequencyCapFilters frequencyCapFilters) {
    writeString(KEYED_FREQUENCY_CAPS_FOR_CLICK_EVENTS);
    writeList(
        frequencyCapFilters.getKeyedFrequencyCapsForClickEvents(),
        this::writeKeyedFrequencyCap);

    writeString(KEYED_FREQUENCY_CAPS_FOR_IMPRESSION_EVENTS);
    writeList(
        frequencyCapFilters.getKeyedFrequencyCapsForImpressionEvents(),
        this::writeKeyedFrequencyCap);

    writeString(KEYED_FREQUENCY_CAPS_FOR_VIEW_EVENTS);
    writeList(
        frequencyCapFilters.getKeyedFrequencyCapsForViewEvents(),
        this::writeKeyedFrequencyCap);

    writeString(KEYED_FREQUENCY_CAPS_FOR_WIN_EVENTS);
    writeList(
        frequencyCapFilters.getKeyedFrequencyCapsForWinEvents(),
        this::writeKeyedFrequencyCap);
  }

  private void writeKeyedFrequencyCap(KeyedFrequencyCap keyedFrequencyCap) {
    writeString(AD_COUNTER_KEY);
    writeField(keyedFrequencyCap.getAdCounterKey(), this::writeInt);

    writeString(INTERVAL);
    writeField(keyedFrequencyCap.getInterval().toMillis(), this::writeLong);

    writeString(MAX_COUNT);
    writeField(keyedFrequencyCap.getMaxCount(), this::writeInt);
  }

  private <T> void writeField(T field, Consumer<T> writerConsumer) {
    Objects.requireNonNull(field);

    writerConsumer.accept(field);
    writeString(FIELD_SEPARATOR);
  }

  private <T> void writeList(List<T> list, Consumer<T> writerConsumer) {
    FirstElementStateHolder state = new FirstElementStateHolder();
    for (T t : list) {
      if (!state.isFirstThenSetFalse()) {
        writeString(ENUMERATOR_SEPARATOR);
      }
      writerConsumer.accept(t);
    }
    writeString(FIELD_SEPARATOR);
  }

  private <T> void writeSet(Set<T> set, Consumer<T> writerConsumer) {
    FirstElementStateHolder state = new FirstElementStateHolder();
    set.stream()
        .sorted()
        .forEach(
            (value) -> {
              if (!state.isFirstThenSetFalse()) {
                writeString(ENUMERATOR_SEPARATOR);
              }
              writerConsumer.accept(value);
            });
    writeString(FIELD_SEPARATOR);
  }

  void writeString(String value) {
    try {
      mByteArrayStream.write(value.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      String errMsg = String.format("Couldn't write bytes into stream for value: %s", value);
      Log.i(TAG, errMsg);
      throw new UncheckedIOException(errMsg, e);
    }
  }

  void writeLong(long value) {
    writeString(Long.toString(value));
  }

  @SuppressLint("DefaultLocale")
  void writeDouble(double value) {
    writeString(String.format("%.2f", value));
  }

  void writeInt(int value) {
    writeString(Integer.toString(value));
  }

  private static class FirstElementStateHolder {
    private boolean mIsFirst = true;

    public boolean isFirstThenSetFalse() {
      boolean initialValue = mIsFirst;
      mIsFirst = false;
      return initialValue;
    }
  }
}

