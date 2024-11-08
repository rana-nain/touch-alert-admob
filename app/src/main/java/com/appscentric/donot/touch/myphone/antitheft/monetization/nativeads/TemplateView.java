package com.appscentric.donot.touch.myphone.antitheft.monetization.nativeads;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.appscentric.donot.touch.myphone.antitheft.R;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

public class TemplateView extends FrameLayout {
   private int templateType;
   private NativeTemplateStyle styles;
   private NativeAd nativeAd;
   private NativeAdView nativeAdView;

   private TextView primaryView;
//   private TextView secondaryView;
   private TextView tertiaryView;
   private ImageView iconView;
   private MediaView mediaView;
   private Button callToActionView;
   private LinearLayout background;

   private static final String MEDIUM_TEMPLATE = "medium_template";
   private static final String SMALL_TEMPLATE = "small_template";

   public TemplateView(Context context) {
      super(context);
   }

   public TemplateView(Context context, @Nullable AttributeSet attrs) {
      super(context, attrs);
      initView(context, attrs);
   }

   public TemplateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      initView(context, attrs);
   }

   public TemplateView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
      super(context, attrs, defStyleAttr, defStyleRes);
      initView(context, attrs);
   }

   public void setStyles(NativeTemplateStyle styles) {
      this.styles = styles;
      this.applyStyles();
   }

   public NativeAdView getNativeAdView() {
      return nativeAdView;
   }

   private void applyStyles() {

      Drawable mainBackground = styles.getMainBackgroundColor();
      if (mainBackground != null) {
         background.setBackground(mainBackground);
         if (primaryView != null) {
            primaryView.setBackground(mainBackground);
         }
//         if (secondaryView != null) {
//            secondaryView.setBackground(mainBackground);
//         }
         if (tertiaryView != null) {
            tertiaryView.setBackground(mainBackground);
         }
      }

      Typeface primary = styles.getPrimaryTextTypeface();
      if (primary != null && primaryView != null) {
         primaryView.setTypeface(primary);
      }

//      Typeface secondary = styles.getSecondaryTextTypeface();
//      if (secondary != null && secondaryView != null) {
//         secondaryView.setTypeface(secondary);
//      }

      Typeface tertiary = styles.getTertiaryTextTypeface();
      if (tertiary != null && tertiaryView != null) {
         tertiaryView.setTypeface(tertiary);
      }

      Typeface ctaTypeface = styles.getCallToActionTextTypeface();
      if (ctaTypeface != null && callToActionView != null) {
         callToActionView.setTypeface(ctaTypeface);
      }

      if (styles.getPrimaryTextTypefaceColor() != null && primaryView != null) {
         primaryView.setTextColor(styles.getPrimaryTextTypefaceColor());
      }

//      if (styles.getSecondaryTextTypefaceColor() != null && secondaryView != null) {
//         secondaryView.setTextColor(styles.getSecondaryTextTypefaceColor());
//      }

      if (styles.getTertiaryTextTypefaceColor() != null && tertiaryView != null) {
         tertiaryView.setTextColor(styles.getTertiaryTextTypefaceColor());
      }

      if (styles.getCallToActionTypefaceColor() != null && callToActionView != null) {
         callToActionView.setTextColor(styles.getCallToActionTypefaceColor());
      }

      float ctaTextSize = styles.getCallToActionTextSize();
      if (ctaTextSize > 0 && callToActionView != null) {
         callToActionView.setTextSize(ctaTextSize);
      }

      float primaryTextSize = styles.getPrimaryTextSize();
      if (primaryTextSize > 0 && primaryView != null) {
         primaryView.setTextSize(primaryTextSize);
      }

//      float secondaryTextSize = styles.getSecondaryTextSize();
//      if (secondaryTextSize > 0 && secondaryView != null) {
//         secondaryView.setTextSize(secondaryTextSize);
//      }

      float tertiaryTextSize = styles.getTertiaryTextSize();
      if (tertiaryTextSize > 0 && tertiaryView != null) {
         tertiaryView.setTextSize(tertiaryTextSize);
      }

      Drawable ctaBackground = styles.getCallToActionBackgroundColor();
      if (ctaBackground != null && callToActionView != null) {
         callToActionView.setBackground(ctaBackground);
      }

      Drawable primaryBackground = styles.getPrimaryTextBackgroundColor();
      if (primaryBackground != null && primaryView != null) {
         primaryView.setBackground(primaryBackground);
      }

//      Drawable secondaryBackground = styles.getSecondaryTextBackgroundColor();
//      if (secondaryBackground != null && secondaryView != null) {
//         secondaryView.setBackground(secondaryBackground);
//      }

      Drawable tertiaryBackground = styles.getTertiaryTextBackgroundColor();
      if (tertiaryBackground != null && tertiaryView != null) {
         tertiaryView.setBackground(tertiaryBackground);
      }

      invalidate();
      requestLayout();
   }

   private boolean adHasOnlyStore(NativeAd nativeAd) {
      String store = nativeAd.getStore();
      String advertiser = nativeAd.getAdvertiser();
      return !TextUtils.isEmpty(store) && TextUtils.isEmpty(advertiser);
   }

   public void setNativeAd(NativeAd nativeAd) {
      this.nativeAd = nativeAd;

      if (nativeAd == null) {
         // Clear ad content here
         if (iconView != null) {
            iconView.setVisibility(GONE);
            iconView.setImageDrawable(null);
         }
         if (tertiaryView != null) {
            tertiaryView.setText(null);
         }
         primaryView.setText(null);
         callToActionView.setText(null);

         // Instead of calling setNativeAd(null), we clear the views
         nativeAdView.setCallToActionView(null);
         nativeAdView.setHeadlineView(null);
         nativeAdView.setMediaView(null);
         nativeAdView.setBodyView(null);
         return;
      }

      String store = nativeAd.getStore();
      String advertiser = nativeAd.getAdvertiser();
      String headline = nativeAd.getHeadline();
      String body = nativeAd.getBody();
      String cta = nativeAd.getCallToAction();
      Double starRating = nativeAd.getStarRating();
      NativeAd.Image icon = nativeAd.getIcon();

      String secondaryText;

      nativeAdView.setCallToActionView(callToActionView);
      nativeAdView.setHeadlineView(primaryView);
      nativeAdView.setMediaView(mediaView);

      primaryView.setText(headline);
      callToActionView.setText(cta);

      if (icon != null) {
         iconView.setVisibility(VISIBLE);
         iconView.setImageDrawable(icon.getDrawable());
      } else {
         iconView.setVisibility(GONE);
      }

      if (tertiaryView != null) {
         tertiaryView.setText(body);
         nativeAdView.setBodyView(tertiaryView);
      }

      nativeAdView.setNativeAd(nativeAd);
   }


   public void destroyNativeAd() {
      nativeAd.destroy();
   }

   public String getTemplateTypeName() {
      if (templateType == R.layout.gnt_medium_template_view) {
         return MEDIUM_TEMPLATE;
      } else if (templateType == R.layout.gnt_small_template_view) {
         return SMALL_TEMPLATE;
      }
      return "";
   }

   private void initView(Context context, AttributeSet attributeSet) {

      TypedArray attributes =
              context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.TemplateView, 0, 0);

      try {
         templateType =
                 attributes.getResourceId(
                         R.styleable.TemplateView_gnt_template_type, R.layout.gnt_medium_template_view);
      } finally {
         attributes.recycle();
      }
      LayoutInflater inflater =
              (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      inflater.inflate(templateType, this);
   }

   @Override
   public void onFinishInflate() {
      super.onFinishInflate();
      nativeAdView = findViewById(R.id.native_ad_view);
      primaryView = findViewById(R.id.ad_headline);
      tertiaryView = findViewById(R.id.ad_body);

      callToActionView = findViewById(R.id.ad_call_to_action);
      iconView = findViewById(R.id.ad_app_icon);
      mediaView = findViewById(R.id.ad_media);
      background = findViewById(R.id.background);
   }
}