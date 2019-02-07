
/*
 File: MotionSynchronizer.m
 Abstract: Synchronizes motion samples with media samples
 Version: 2.2
 
 Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
 Inc. ("Apple") in consideration of your agreement to the following
 terms, and your use, installation, modification or redistribution of
 this Apple software constitutes acceptance of these terms.  If you do
 not agree with these terms, please do not use, install, modify or
 redistribute this Apple software.
 
 In consideration of your agreement to abide by the following terms, and
 subject to these terms, Apple grants you a personal, non-exclusive
 license, under Apple's copyrights in this original Apple software (the
 "Apple Software"), to use, reproduce, modify and redistribute the Apple
 Software, with or without modifications, in source and/or binary forms;
 provided that if you redistribute the Apple Software in its entirety and
 without modifications, you must retain this notice and the following
 text and disclaimers in all such redistributions of the Apple Software.
 Neither the name, trademarks, service marks or logos of Apple Inc. may
 be used to endorse or promote products derived from the Apple Software
 without specific prior written permission from Apple.  Except as
 expressly stated in this notice, no other rights or licenses, express or
 implied, are granted by Apple herein, including but not limited to any
 patent rights that may be infringed by your derivative works or by other
 works in which the Apple Software may be incorporated.
 
 The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
 MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
 OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
 
 IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
 MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
 AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
 STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 
 Copyright (C) 2014 Apple Inc. All Rights Reserved.
 
 */

#import "VideoTimeConverter.h"
#import <CoreMotion/CoreMotion.h>

CFStringRef const VIDEOSNAKE_REMAPPED_PTS = CFSTR("RemappedPTS");


@interface VideoTimeConverter () {

}

@property(nonatomic, retain) __attribute__((NSObject)) CMClockRef motionClock;

@end

@implementation VideoTimeConverter

- (id)init
{
    self = [super init];
    if (self != nil) {
        _motionClock = CMClockGetHostTimeClock();
        if (_motionClock)
            CFRetain(_motionClock);
    }
    
    return self;
}

- (void)dealloc
{
    if (_sampleBufferClock)
        CFRelease(_sampleBufferClock);
    if (_motionClock)
        CFRelease(_motionClock);
}

- (void)checkStatus
{
    if ( self.sampleBufferClock == NULL ) {
        @throw [NSException exceptionWithName:NSInternalInconsistencyException reason:@"No sample buffer clock. Please set one before calling start." userInfo:nil];
        return;
    }
}

- (void)convertSampleBufferTimeToMotionClock:(CMSampleBufferRef)sampleBuffer
{
    CMTime originalPTS = CMSampleBufferGetPresentationTimeStamp(sampleBuffer);
    CMTime remappedPTS = originalPTS;
    if ( self.sampleBufferClock && self.motionClock ) {
        if ( !CFEqual(self.sampleBufferClock, self.motionClock) ) {
            remappedPTS = CMSyncConvertTime(originalPTS, self.sampleBufferClock, self.motionClock);
        }
    }
    // Attach the remapped timestamp to the buffer for use in -sync
    CFDictionaryRef remappedPTSDict = CMTimeCopyAsDictionary(remappedPTS, kCFAllocatorDefault);
    CMSetAttachment(sampleBuffer, VIDEOSNAKE_REMAPPED_PTS, remappedPTSDict, kCMAttachmentMode_ShouldPropagate);
    
    CFRelease(remappedPTSDict);
}

@end

CMTime getAttachmentTime(CMSampleBufferRef mediaSample)
{
    CFDictionaryRef mediaTimeDict = CMGetAttachment(mediaSample, VIDEOSNAKE_REMAPPED_PTS, NULL);
    CMTime mediaTime = (mediaTimeDict) ? CMTimeMakeFromDictionary(mediaTimeDict) : CMSampleBufferGetPresentationTimeStamp(mediaSample);
//    Float64 floatTime = CMTimeGetSeconds(mediaTime);
//    if (mediaTimeDict) {
//        NSLog(@"%@ is used to get timestamp %.6f", VIDEOSNAKE_REMAPPED_PTS, floatTime);
//    } else {
//        NSLog(@"%@ is NOT used to get timestamp %.6f", VIDEOSNAKE_REMAPPED_PTS, floatTime);
//    }
    return mediaTime;
}