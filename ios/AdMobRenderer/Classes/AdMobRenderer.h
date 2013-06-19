/*
 * AdMobRenderer
 *
 * Copyright (c) 2011年 FreeWheel Inc. All rights reserved.
 *
 * To use the AdMob renderer,
 *	- Add TouchJSON code to your project; 
 *	- Add AudioToolbox.framework, MessageUI.framework frameworks
 *	- Add libAdmob.a to your project 
 *
 */


/** 
 *	AdManager publishes notification of this topic when AdMob renderer will present full screen modal 
 */
FW_EXTERN NSString *const FW_NOTIFICATION_ADMOB_PRESENT_FULL_SCREEN_MODAL;

/** 
 *	AdManager publishes notification of this topic when AdMob renderer will dismiss full screen modal 
 */
FW_EXTERN NSString *const FW_NOTIFICATION_ADMOB_DISMISS_FULL_SCREEN_MODAL;

/**
 * Ad unit for adMob interstitial ad EventAppOpen
 *
 * See Also:
 *  - FW_ADUNIT_ADMOB_INTERSTITIAL_SCREEN_CHANGE
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_PREROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_POSTROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_OTHER
 */
FW_EXTERN NSString *const FW_ADUNIT_ADMOB_INTERSTITIAL_APP_OPEN;

/**
 * Ad unit for adMob interstitial ad EventScreenChange
 *
 * See Also:
 *  - FW_ADUNIT_ADMOB_INTERSTITIAL_APP_OPEN
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_PREROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_POSTROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_OTHER
 */
FW_EXTERN NSString *const FW_ADUNIT_ADMOB_INTERSTITIAL_SCREEN_CHANGE;

/**
 * Ad unit for adMob interstitial ad EventPreroll
 *
 * See Also:
 *  - FW_ADUNIT_ADMOB_INTERSTITIAL_APP_OPEN
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_SCREEN_CHANGE
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_POSTROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_OTHER
 */
FW_EXTERN NSString *const FW_ADUNIT_ADMOB_INTERSTITIAL_PREROLL;

/**
 * Ad unit for adMob interstitial ad EventPostroll
 *
 * See Also:
 *  - FW_ADUNIT_ADMOB_INTERSTITIAL_APP_OPEN
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_SCREEN_CHANGE
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_PREROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_OTHER
 */
FW_EXTERN NSString *const FW_ADUNIT_ADMOB_INTERSTITIAL_POSTROLL;

/**
 * Ad unit for adMob interstitial ad EventOther
 *
 * See Also:
 *  - FW_ADUNIT_ADMOB_INTERSTITIAL_APP_OPEN
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_SCREEN_CHANGE
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_PREROLL
 *	- FW_ADUNIT_ADMOB_INTERSTITIAL_POSTROLL
 */
FW_EXTERN NSString *const FW_ADUNIT_ADMOB_INTERSTITIAL_OTHER;

/**
 *  Specify the AdMob Interstitial Event type of AdMob InterstitialAd request.
 *  value is FW_ADUNIT_ADMOB_INTERSTITIAL_APP_OPEN or FW_ADUNIT_ADMOB_INTERSTITIAL_SCREEN_CHANGE
 *
 */
FW_EXTERN NSString *const FW_PARAMETER_ADMOB_INTERSTITIAL_TYPE;

/**
  *  Specify whether the AdMob renderer to use test mode
  *
  */
FW_EXTERN NSString *const FW_PARAMETER_ADMOB_TEST_MODE;

/**
  *  Specify the click actions when test AdMob Renderer
  *
  */
FW_EXTERN NSString *const FW_PARAMETER_ADMOB_TEST_ACTION;

/**
  *  White space separated string, each one indicate a device UDID
  *  e.g. @"28ab37c3902621dd572509110745071f0101b124 8cf09e81ef3ec5418c3450f7954e0e95db8ab200"
  */
FW_EXTERN NSString *const FW_PARAMETER_ADMOB_TEST_DEVICES;

/**
 *	The parameter pass as locationDescription to AdMob, It can be any free-form text such as
 *	@"Champs-Elysees Paris" or @"94041 US".
 */
FW_EXTERN NSString *const FW_PARAMETER_ADMOB_LOCATION_DESCRIPTION;
