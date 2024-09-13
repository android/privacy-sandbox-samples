package com.example.adservices.samples.fledge.sampleapp

import android.adservices.common.AdTechIdentifier
import android.annotation.SuppressLint
import android.net.Uri
import java.util.Objects

@SuppressLint("NewApi")
class ConfigUris(
    val baseUri: Uri,
    val auctionServerSellerSfeUri: Uri,
    val auctionServerSeller: AdTechIdentifier,
    val auctionServerBuyer: AdTechIdentifier,
    val coordinatorUri: Uri
) {
    val buyer: AdTechIdentifier
        get() = AdTechIdentifier.fromString(Objects.requireNonNull(baseUri.host).toString())

    val seller: AdTechIdentifier
        get() = AdTechIdentifier.fromString(Objects.requireNonNull(baseUri.host).toString())

    val isMaybeServerAuction: Boolean
        /**
         * Check if a server auction is likely given input.
         *
         * @return True if any of the server auction values have been passed.
         */
        get() = auctionServerBuyer !== EMPTY_AD_TECH && auctionServerSeller !== EMPTY_AD_TECH && auctionServerSellerSfeUri !== Uri.EMPTY

    companion object {
        private val EMPTY_AD_TECH = AdTechIdentifier.fromString("")
    }
}
