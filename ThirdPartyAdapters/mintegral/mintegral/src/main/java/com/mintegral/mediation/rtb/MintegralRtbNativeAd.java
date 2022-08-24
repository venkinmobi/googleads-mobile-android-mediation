package com.mintegral.mediation.rtb;

import static com.mbridge.msdk.MBridgeConstans.NATIVE_VIDEO_SUPPORT;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.MediaView;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.Frame;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.NativeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MintegralRtbNativeAd extends UnifiedNativeAdMapper implements NativeListener.NativeAdListener {
  private final String TAG = MintegralMediationAdapter.class.getSimpleName();
  private Campaign campaign;
  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> adLoadCallback;
  private MediationNativeAdCallback nativeCallback;

  private MBBidNativeHandler mbBidNativeHandler;

  public MintegralRtbNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
                              @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
    adConfiguration = mediationNativeAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void loadAd() {
    String unitId = adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
    Map<String, Object> properties = MBBidNativeHandler.getNativeProperties(placementId, unitId);
    properties.put(NATIVE_VIDEO_SUPPORT, true);
    properties.put(MBridgeConstans.PROPERTIES_AD_NUM, 1);
    mbBidNativeHandler = new MBBidNativeHandler(properties, adConfiguration.getContext());
    String token = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(token)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_BID_RESPONSE, "Failed to load rewarded ad from MIntegral. Missing or invalid bid response.");
      adLoadCallback.onFailure(error);
      return;
    }
    mbBidNativeHandler.setAdListener(this);
    mbBidNativeHandler.bidLoad(token);
  }

  private void mapNativeAd(Campaign ad) {
    campaign = ad;
    setHeadline(campaign.getAppName());
    setBody(campaign.getAppDesc());
    setCallToAction(campaign.getAdCall());
    setStarRating(campaign.getRating());
    setStore(campaign.getPackageName());
    setIcon(new MBridgeNativeMappedImage(null, Uri.parse(ad.getIconUrl()),
            1.0));
    List<Image> imagesList = new ArrayList<Image>();
    imagesList.add(new MBridgeNativeMappedImage(null, Uri.parse(ad.getImageUrl()),
            1.0));
    setImages(imagesList);
    setOverrideClickHandling(true);
    setOverrideImpressionRecording(false);
  }


  @Override
  public void recordImpression() {
  }


  @Override
  public void handleClick(View view) {
    super.handleClick(view);
  }


  @Override
  public void trackViews(@NonNull View view, @NonNull Map<String, View> map, @NonNull Map<String, View> map1) {
    if (view instanceof ViewGroup) {
      if (mbBidNativeHandler != null) {
        mbBidNativeHandler.registerView(view, traversalView(view), campaign);
      }
    } else if (view instanceof View) {
      if (mbBidNativeHandler != null) {
        mbBidNativeHandler.registerView(view, campaign);
      }
    }
    super.trackViews(view, map, map1);
  }


  @Override
  public void untrackView(View view) {
    super.untrackView(view);
  }


  private List traversalView(View view) {
    List<View> viewList = new ArrayList<View>();
    if (null == view) {
      return viewList;
    }
    if (view instanceof MediaView) {
      viewList.add(view);
    } else if (view instanceof ViewGroup) {
      ViewGroup viewGroup = (ViewGroup) view;
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        if (viewGroup.getChildAt(i) instanceof ViewGroup) {
          viewList.addAll(traversalView(viewGroup.getChildAt(i)));
        } else {
          viewList.add(viewGroup.getChildAt(i));
        }
      }
    } else if (view instanceof View) {
      viewList.add(view);
    }
    return viewList;
  }

  @Override
  public void onAdLoaded(List<Campaign> list, int i) {
    if (list == null || list.size() == 0) {
      AdError adError = MintegralConstants.createAdapterError(MintegralConstants.ERROR_CODE_NO_FILL, "response is empty");
      Log.w(TAG, adError.toString());
      adLoadCallback.onFailure(adError);
      return;
    }
    mapNativeAd(list.get(0));
    nativeCallback = adLoadCallback.onSuccess(MintegralRtbNativeAd.this);
  }

  @Override
  public void onAdLoadError(String s) {
    AdError adError = MintegralConstants.createAdapterError(MintegralConstants.ERROR_SDK_INTER_ERROR, s);
    Log.w(TAG, adError.toString());
    adLoadCallback.onFailure(adError);
  }

  @Override
  public void onAdClick(Campaign campaign) {
    if (nativeCallback != null) {
      nativeCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdFramesLoaded(List<Frame> list) {
    //No-op, the method is deprecated
  }

  @Override
  public void onLoggingImpression(int i) {
    if (nativeCallback != null) {
      nativeCallback.reportAdImpression();
    }
  }

  public class MBridgeNativeMappedImage extends Image {
    private Drawable mDrawable;
    private final Uri mImageUri;
    private final double mScale;

    public MBridgeNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
      mDrawable = drawable;
      mImageUri = imageUri;
      mScale = scale;
    }

    @Override
    public Drawable getDrawable() {
      return mDrawable;
    }

    @Override
    public Uri getUri() {
      return mImageUri;
    }

    @Override
    public double getScale() {
      return mScale;
    }


    void setDrawable(Drawable mDrawable) {
      this.mDrawable = mDrawable;
    }
  }

}
