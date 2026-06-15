package com.wappscorp.wpvw.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.wappscorp.wpvw.BuildConfig

object AdManager {
    private const val BANNER_REAL = "ca-app-pub-1892613718311718/1805140488"
    private const val INTERSTITIAL_REAL = "ca-app-pub-1892613718311718/7907514402"
    private const val REWARDED_REAL = "ca-app-pub-1892613718311718/2424424290"

    private const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_TEST = "ca-app-pub-3940256099942544/5224354917"

    val BANNER_AD_ID: String get() = if (BuildConfig.DEBUG) BANNER_TEST else BANNER_REAL
    val INTERSTITIAL_AD_ID: String get() = if (BuildConfig.DEBUG) INTERSTITIAL_TEST else INTERSTITIAL_REAL
    val REWARDED_AD_ID: String get() = if (BuildConfig.DEBUG) REWARDED_TEST else REWARDED_REAL

    fun createBannerAd(context: Context): AdView {
        return AdView(context).apply {
            adUnitId = BANNER_AD_ID
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
    }

    fun loadInterstitialAd(context: Context, onLoaded: (InterstitialAd) -> Unit) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    onLoaded(interstitialAd)
                }
            }
        )
    }

    fun loadRewardedAd(context: Context, onLoaded: (RewardedAd) -> Unit) {
        RewardedAd.load(
            context,
            REWARDED_AD_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    onLoaded(rewardedAd)
                }
            }
        )
    }
}
