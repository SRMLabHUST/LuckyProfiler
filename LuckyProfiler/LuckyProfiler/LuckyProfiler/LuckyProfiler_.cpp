#include "cuda.h"
#include <LuckyProfiler_.h>
#include "kernel.h"
#include "skeleton.h"

int skelNum;
int PreProjectionNum;

JNIEXPORT void JNICALL Java_LuckyProfiler_1_computeAngs(JNIEnv* env, jobject, jintArray locx, jintArray locy, jintArray SkelImwithout1, jint SkelNum, jint imgH, jint imgW) {
	jint* Locx = env->GetIntArrayElements(locx, NULL);
	jint* Locy = env->GetIntArrayElements(locy, NULL);
	jint* skel = env->GetIntArrayElements(SkelImwithout1, NULL);
	skelNum = SkelNum;

	getAngs(Locx, Locy, skel, SkelNum, imgH, imgW);

	env->ReleaseIntArrayElements(SkelImwithout1, skel, 0);
	env->ReleaseIntArrayElements(locx, Locx, 0);
	env->ReleaseIntArrayElements(locy, Locy, 0);
}

JNIEXPORT JNIEXPORT jintArray JNICALL Java_LuckyProfiler_1_getSkeleton(JNIEnv *env, jobject, jintArray JSkeletontemp, jint imgH, jint imgW) {
	jint* Skeletontemp = env->GetIntArrayElements(JSkeletontemp, NULL);
		
	long* skg = compute_skeleton_gradient(Skeletontemp, imgH, imgW);

	//int thres = 30;

	//bool* skr = (bool*)calloc(0, sizeof(bool) * imgH * imgW);
	//int idx = 0;
	//for (int j = 0; j < imgW; j++) {
	//	for (int i = 0; i < imgH; i++) {
	//		if (skg[idx] > thres) {
	//			skr[i * imgW + j] = 1;
	//		}
	//		idx++;
	//	}
	//}

	//bool *BoolSkelImwithout1 = algbwmorph(skr, imgH, imgW);
	//short *SkelImwithout1 = (short*)malloc(sizeof(short) * imgH * imgW);
	//for (int i = 0; i < imgH * imgW; i++) SkelImwithout1[i] = BoolSkelImwithout1[i];

	jintArray result = NULL;
	result = env->NewIntArray(imgH * imgW);
	env->SetIntArrayRegion(result, 0, imgH * imgW, skg);

	env->ReleaseIntArrayElements(JSkeletontemp, Skeletontemp, 0);

	free(skg);
	return result;
}

JNIEXPORT void JNICALL Java_LuckyProfiler_1_computeRCadiAndRelatedLocIndex
(JNIEnv* env, jobject, jintArray JSkelImwithout1, jintArray JSingleResult, jintArray Jrr, jint JPreProjectionNum, jint imH, jint imgW) {
	PreProjectionNum = JPreProjectionNum;
	jint* SkelImwithout1 = env->GetIntArrayElements(JSkelImwithout1, NULL);
	jint* SingleResult = env->GetIntArrayElements(JSingleResult, NULL);
	jint* rr = env->GetIntArrayElements(Jrr, NULL);

	getRCadiAndRelatedLocIndex(SkelImwithout1, SingleResult, rr, PreProjectionNum, imH, imgW, skelNum);

	env->ReleaseIntArrayElements(JSkelImwithout1, SkelImwithout1, 0);
	env->ReleaseIntArrayElements(JSingleResult, SingleResult, 0);
	env->ReleaseIntArrayElements(Jrr, rr, 0);
}

JNIEXPORT jintArray JNICALL Java_LuckyProfiler_1_getRCadi(JNIEnv* env, jobject) {
	jintArray result = NULL;
	result = env->NewIntArray(skelNum * PreProjectionNum);
	env->SetIntArrayRegion(result, 0, skelNum * PreProjectionNum, RCadi);
	free(RCadi);
	return result;
}

JNIEXPORT jintArray JNICALL Java_LuckyProfiler_1_getRelatedLocIndex(JNIEnv* env, jobject) {
	jintArray result = NULL;
	result = env->NewIntArray(skelNum * PreProjectionNum);
	env->SetIntArrayRegion(result, 0, skelNum * PreProjectionNum, RelatedLocIndex);
	free(RelatedLocIndex);
	return result;
}

JNIEXPORT jdoubleArray JNICALL Java_LuckyProfiler_1_getAngs(JNIEnv* env, jobject) {
	jdoubleArray result = NULL;
	result = env->NewDoubleArray(skelNum);
	env->SetDoubleArrayRegion(result, 0, skelNum, angs);
	free(angs);
	return result;
}