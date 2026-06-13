package com.wappscorp.wpvw.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    const val BANNER_AD_ID = "ca-app-pub-1892613718311718/1805140488"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-1892613718311718/7907514402"
    const val REWARDED_AD_ID = "ca-app-pub-1892613718311718/2424424290"

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
