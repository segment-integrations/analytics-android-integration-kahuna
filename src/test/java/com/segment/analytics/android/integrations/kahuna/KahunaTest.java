package com.segment.analytics.android.integrations.kahuna;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import com.kahuna.sdk.EmptyCredentialsException;
import com.kahuna.sdk.Kahuna;
import com.kahuna.sdk.KahunaCommon;
import com.kahuna.sdk.KahunaUserCredentials;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.AliasPayloadBuilder;
import com.segment.analytics.test.GroupPayloadBuilder;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.kahuna.sdk.KahunaUserCredentials.EMAIL_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.FACEBOOK_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.GOOGLE_PLUS_ID;
import static com.kahuna.sdk.KahunaUserCredentials.INSTALL_TOKEN_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.LINKEDIN_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.TWITTER_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.USERNAME_KEY;
import static com.kahuna.sdk.KahunaUserCredentials.USER_ID_KEY;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({ Kahuna.class, KahunaCommon.class })
@SuppressStaticInitializationFor("com.kahuna.sdk.Kahuna") //
public class KahunaTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  @Mock Kahuna kahuna;
  @Mock KahunaUserCredentials credentials;
  KahunaIntegration integration;

  @Before public void setUp() throws Exception {
    initMocks(this);

    integration = new KahunaIntegration(kahuna, Logger.with(VERBOSE), context, true, "foo", "bar");

    Mockito.reset(kahuna);
    PowerMockito.when(kahuna.createUserCredentials()).thenReturn(credentials);
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(kahuna).start();
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(kahuna).stop();
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());

    verify(kahuna).trackEvent("foo");
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackWithQuantityAndRevenue() {
    integration.track(new TrackPayloadBuilder().event("bar")
        .properties(new Properties().putValue("quantity", 3).putRevenue(10))
        .build());

    verify(kahuna).trackEvent("bar", 3, 1000);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackWithNoQuantityAndOnlyRevenue() {
    integration.track(new TrackPayloadBuilder() //
        .event("bar") //
        .properties(new Properties().putRevenue(10)) //
        .build());

    verify(kahuna).trackEvent("bar", -1, 1000);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductCategoryWithoutCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(
        new TrackPayloadBuilder().event("Viewed Product Category").build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.CATEGORIES_VIEWED, KahunaIntegration.NONE);
    expectedAttributes.put(KahunaIntegration.LAST_VIEWED_CATEGORY, KahunaIntegration.NONE);

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductCategoryWithCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder() //
        .event("Viewed Product Category") //
        .properties(new Properties().putCategory("foo")) //
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.CATEGORIES_VIEWED, "foo");
    expectedAttributes.put(KahunaIntegration.LAST_VIEWED_CATEGORY, "foo");

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductCategoryWithPreviouslyViewedCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(KahunaIntegration.CATEGORIES_VIEWED, KahunaIntegration.NONE);
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(
        new TrackPayloadBuilder().event("Viewed Product Category")
            .properties(new Properties().putCategory("foo"))
            .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.CATEGORIES_VIEWED, "None,foo");
    expectedAttributes.put(KahunaIntegration.LAST_VIEWED_CATEGORY, "foo");

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  static List<Integer> rangeOf(int start, int end) {
    List<Integer> list = new ArrayList<>(end - start + 1);
    for (int i = start; i <= end; i++) {
      list.add(i);
    }
    return list;
  }

  @Test public void trackViewedProductCategoryWithPreviouslyViewedCategoryMax() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(KahunaIntegration.CATEGORIES_VIEWED, TextUtils.join(",", rangeOf(1, 50)));
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackViewedProductCategory(new TrackPayloadBuilder() //
        .event("Viewed Product Category") //
        .properties(new Properties().putCategory("51")) //
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    // the '1' is removed
    expectedAttributes.put(KahunaIntegration.CATEGORIES_VIEWED,
        TextUtils.join(",", rangeOf(2, 51)));
    expectedAttributes.put(KahunaIntegration.LAST_VIEWED_CATEGORY, "51");

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackViewedProductWithoutName() {
    integration.trackViewedProduct(new TrackPayloadBuilder().event("Viewed Product").build());

    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void trackViewedProductWithName() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackViewedProduct(new TrackPayloadBuilder().event("Viewed Product")
        .properties(new Properties().putName("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.LAST_PRODUCT_VIEWED_NAME, "foo");

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackAddedProductWithoutName() {
    integration.trackViewedProduct(new TrackPayloadBuilder().event("Added Product").build());

    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void trackAddedProductWithName() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackAddedProduct(new TrackPayloadBuilder().event("Added Product")
        .properties(new Properties().putName("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.LAST_PRODUCT_ADDED_TO_CART_NAME, "foo");

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackAddedProductCategoryWithoutCategory() {
    integration.trackAddedProductCategory(new TrackPayloadBuilder().event("Added Product").build());

    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackAddedProductCategoryWithCategory() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackAddedProductCategory(new TrackPayloadBuilder().event("Added Product")
        .properties(new Properties().putCategory("foo"))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.LAST_PRODUCT_ADDED_TO_CART_CATEGORY, "foo");

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void trackCompletedOrder() {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.trackCompletedOrder(new TrackPayloadBuilder().event("Completed Order")
        .properties(new Properties().putDiscount(10))
        .build());

    Map<String, String> expectedAttributes = new LinkedHashMap<>();
    expectedAttributes.put(KahunaIntegration.LAST_PURCHASE_DISCOUNT, String.valueOf(10.0));

    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().build());
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreInteractions(Kahuna.class);
  }

  @Test public void identify() throws EmptyCredentialsException {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);

    integration.identify(new IdentifyPayloadBuilder().traits(createTraits("foo")).build());

    verify(credentials).add(USER_ID_KEY, "foo");
    verify(kahuna).createUserCredentials();
    verify(kahuna).getUserAttributes();
    verify(kahuna).login(credentials);
    verify(kahuna).setUserAttributes(map);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void identifyWithSocialAttributes() throws EmptyCredentialsException {
    Map<String, String> map = new LinkedHashMap<>();
    PowerMockito.when(kahuna.getUserAttributes()).thenReturn(map);
    Traits traits = new Traits() //
        .putUsername("foo")
        .putEmail("bar")
        .putValue("fbid", "baz")
        .putValue("twtr", "qux")
        .putValue("lnk", "quux")
        .putValue("install_token", "foobar")
        .putValue("gplus_id", "foobaz")
        .putValue("non_kahuna_credential", "foobarqazqux");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    Map<String, String> expectedAttributes = new ValueMap() //
        .putValue("non_kahuna_credential", "foobarqazqux").toStringMap();
    Assert.assertEquals(kahuna.getUserAttributes(), expectedAttributes);
    verify(kahuna, times(2)).getUserAttributes();
    verify(kahuna).setUserAttributes(expectedAttributes);
    verify(kahuna).setUserAttributes(expectedAttributes);

    verify(credentials).add(USERNAME_KEY, "foo");
    verify(credentials).add(EMAIL_KEY, "bar");
    verify(credentials).add(FACEBOOK_KEY, "baz");
    verify(credentials).add(TWITTER_KEY, "qux");
    verify(credentials).add(LINKEDIN_KEY, "quux");
    verify(credentials).add(INSTALL_TOKEN_KEY, "foobar");
    verify(credentials).add(GOOGLE_PLUS_ID, "foobaz");
    verify(kahuna).createUserCredentials();
    verify(kahuna).login(credentials);
    verifyNoMoreInteractions(kahuna);
  }

  @Test public void reset() {
    integration.reset();

    verify(kahuna).logout();
    verifyNoMoreInteractions(kahuna);
  }
}
