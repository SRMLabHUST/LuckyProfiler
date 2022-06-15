#pragma once

extern double* angs;
extern long* RelatedLocIndex,* RCadi;

void getAngs(long* locx, long* locy, long* SkelImwithout1, int SkelNum, int imgH, int imgW);
void getRCadiAndRelatedLocIndex(long* SkelImwithout1, long* SingleResult, long* rr, int PreProjectionNum, int imH, int imgW, int skelNum);
//void bwlookup(int* bw, int i, int imgH, int imgW);