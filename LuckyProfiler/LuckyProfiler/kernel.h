#pragma once

extern double* angs;
extern long* RelatedLocIndex,* RCadi;

void getAngs(long* locx, long* locy, long* SkelImwithout1, int SkelNum, int imgH, int imgW);
void getRCadiAndRelatedLocIndex(long* SkelImwithout1, long* SingleResult, long* rr, int PreProjectionNum, int imH, int imgW, int skelNum, long* col2Ind);
void init_deviceMem(int *jy, int *jx, int njunc);
void getDistance(int i, int j, int *dNE, int *dNW, int *dSE, int *dSW, int njunc);
void free_jy_jx();