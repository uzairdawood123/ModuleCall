#ifdef __OBJC__
#import <UIKit/UIKit.h>
#else
#ifndef FOUNDATION_EXPORT
#if defined(__cplusplus)
#define FOUNDATION_EXPORT extern "C"
#else
#define FOUNDATION_EXPORT extern
#endif
#endif
#endif

#import "ArCoreReactNative-Bridging-Header.h"
#import "ModuleWithEmitter.h"
#import "Pods-ArCoreReactNative-umbrella.h"
#import "SnapKit-umbrella.h"

FOUNDATION_EXPORT double ar_core_react_nativeVersionNumber;
FOUNDATION_EXPORT const unsigned char ar_core_react_nativeVersionString[];

