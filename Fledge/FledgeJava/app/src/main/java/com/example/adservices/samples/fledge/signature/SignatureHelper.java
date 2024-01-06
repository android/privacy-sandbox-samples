package com.example.adservices.samples.fledge.signature;

import static com.example.adservices.samples.fledge.sampleapp.MainActivity.TAG;

import android.adservices.adselection.SignedContextualAds;
import android.util.Log;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SignatureHelper {
  /**
   * This key matches the key in the  {@code ProtectedAudienceSignatureManager} implementation
   * from the service side. This key and its public key pair is used if the enrollment is
   * disabled. This implementation uses this private key to sign the contextual ads to enable
   * testing.
   */
  public static final String PRIVATE_TEST_KEY_STRING =
      "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgECetqRr9eE9DKKjILR+hP66Y1niEw/bqPD/MNx"
          + "PTMvmhRANCAAT4TKjRM6WVbxqzalNPPFrBDnulcmIfXpURGAepxXX9ikAO3eKrr29uHBbwqyLczQ"
          + "avFpw66B78DCIztSHr5Xc8";

  /**
   * Signs the given {@link SignedContextualAds}. If signing fails then returns the unauthenticated
   * ads.
   * @param notSignedContextualAds ads to sign
   * @return signed contextual ads if signing is successful, otherwise returns the original bundle.
   */
  public static SignedContextualAds signContextualAdsOrReturn(SignedContextualAds notSignedContextualAds) {
    SignedContextualAds.Builder signedContextualAdsBuilder = notSignedContextualAds.cloneToBuilder();
    try {
      Signature ecdsaSigner = getECDSASignatureInstance();
      ecdsaSigner.update(new SignedContextualAdsHashUtil().serialize(notSignedContextualAds));
      signedContextualAdsBuilder.setSignature(ecdsaSigner.sign());
    } catch (Exception e) {
      String errMsg =
          String.format(
              "Something went wrong during signing a contextual ad bundle: %s. "
                  + "Ads will be disqualified due to invalid signatures", e);
      Log.i(TAG, errMsg);
    }
    return signedContextualAdsBuilder.build();
  }

  // public static SignedContextualAds generateSignedContextualAds(AdTechIdentifier buyer, Uri contextualLogicUri) {
  //   return generateSignedContextualAds(buyer, contextualLogicUri, new ArrayList<>());
  // }
  //
  // public static SignedContextualAds generateSignedContextualAds(AdTechIdentifier buyer, Uri contextualLogicUri, List<AdWithBid> adsWithBidList) {
  //   SignedContextualAds signedContextualAds = new SignedContextualAds.Builder()
  //       .setBuyer(buyer) //
  //       .setDecisionLogicUri(contextualLogicUri)
  //       .setAdsWithBid(adsWithBidList)
  //       .setSignature(new byte[] {})
  //       .build();
  //   try {
  //     Signature ecdsaSigner = getECDSASignatureInstance();
  //     ecdsaSigner.update(new SignedContextualAdsHashUtil().serialize(signedContextualAds));
  //     signedContextualAds =
  //         signedContextualAds.cloneToBuilder()
  //             .setSignature(ecdsaSigner.sign())
  //             .build();
  //   } catch (Exception e) {
  //     String errMsg =
  //         String.format(
  //             "Something went wrong during signing a contextual ad bundle: %s. "
  //                 + "Ads will be disqualified due to invalid signatures", e);
  //     Log.i(TAG, errMsg);
  //   }
  //   return signedContextualAds;
  // }

  private static Signature getECDSASignatureInstance() throws Exception {
    byte[] privateKeyBytes = Base64.getDecoder().decode( PRIVATE_TEST_KEY_STRING);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("EC");
    PrivateKey privateKey = keyFactory.generatePrivate(spec);
    Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
    ecdsaSign.initSign(privateKey);
    return ecdsaSign;
  }
}
