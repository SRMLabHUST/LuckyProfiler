#pragma once

extern double* angs;
extern short* RCadi;
extern long* RelatedLocIndex;

void getAngs(long* locx, long* locy, short* SkelImwithout1, int SkelNum, int imgH, int imgW);
void getRCadiAndRelatedLocIndex(short* SkelImwithout1, long* SingleResult, long* rr, int PreProjectionNum, int imH, int imgW, int skelNum);
//void bwlookup(int* bw, int i, int imgH, int imgW);