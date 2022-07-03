#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#include <stdlib.h>
#include <stdio.h>
#include "kernel.h"

#define SQR(x) (x)*(x)
#define PI 3.14159265358979323846

double* angs;
long* RelatedLocIndex,* RCadi;
int *d_jy, *d_jx;
int *d_dNE, *d_dNW, *d_dSE, *d_dSW;

__global__ void adjangle1(double* angs, long* Locx, long* Locy, long* SkelImwithout1, int nx, int SkelNum, int imgW) {
	int x = blockDim.x * blockIdx.x + threadIdx.x;
	int y = blockDim.y * blockIdx.y + threadIdx.y;

	int tid = y * nx + x;
	if (tid < SkelNum) {
		int G1 = SkelImwithout1[Locx[tid] * imgW + Locy[tid] + 1];
		int G2 = SkelImwithout1[(Locx[tid] - 1) * imgW + Locy[tid] + 1];
		int G3 = SkelImwithout1[(Locx[tid] - 1) * imgW + Locy[tid]];
		int G4 = SkelImwithout1[(Locx[tid] - 1) * imgW + Locy[tid] - 1];
		int G5 = SkelImwithout1[Locx[tid] * imgW + Locy[tid] - 1];
		int G6 = SkelImwithout1[(Locx[tid] + 1) * imgW + Locy[tid] - 1];
		int G7 = SkelImwithout1[(Locx[tid] + 1) * imgW + Locy[tid]];
		int G8 = SkelImwithout1[(Locx[tid] + 1) * imgW + Locy[tid] + 1];
		int G9 = SkelImwithout1[(Locx[tid] + 1) * imgW + Locy[tid] + 2];
		int G10 = SkelImwithout1[Locx[tid] * imgW + Locy[tid] + 2];
		int G11 = SkelImwithout1[(Locx[tid] - 1) * imgW + Locy[tid] + 2];
		int G12 = SkelImwithout1[(Locx[tid] - 2) * imgW + Locy[tid] + 2];
		int G13 = SkelImwithout1[(Locx[tid] - 2) * imgW + Locy[tid] + 1];
		int G14 = SkelImwithout1[(Locx[tid] - 2) * imgW + Locy[tid]];
		int G15 = SkelImwithout1[(Locx[tid] - 2) * imgW + Locy[tid] - 1];
		int G16 = SkelImwithout1[(Locx[tid] - 2) * imgW + Locy[tid] - 2];
		int G17 = SkelImwithout1[(Locx[tid] - 1) * imgW + Locy[tid] - 2];
		int G18 = SkelImwithout1[Locx[tid] * imgW + Locy[tid] - 2];
		int G19 = SkelImwithout1[(Locx[tid] + 1) * imgW + Locy[tid] - 2];
		int G20 = SkelImwithout1[(Locx[tid] + 2) * imgW + Locy[tid] - 2];
		int G21 = SkelImwithout1[(Locx[tid] + 2) * imgW + Locy[tid] - 1];
		int G22 = SkelImwithout1[(Locx[tid] + 2) * imgW + Locy[tid]];
		int G23 = SkelImwithout1[(Locx[tid] + 2) * imgW + Locy[tid] + 1];
		int G24 = SkelImwithout1[(Locx[tid] + 2) * imgW + Locy[tid] + 2];

		int G[5][5] = { {G16, G15, G14, G13, G12}, {G17, G4, G3, G2, G11}, {G18, G5, 1, G1, G10}, {G19, G6, G7, G8, G9}, {G20, G21, G22, G23, G24} };

		int N = 0;
		int temp1 = 0;
		double meanx = 0;
		double meany = 0;
		int temp3 = 0;
		int cnt = 0;

#pragma unrolling
		for (int i = 1; i <= 5; i++) {
#pragma unrolling
			for (int j = 1; j <= 5; j++) {
				if (G[i - 1][j - 1] == 1) {
					N++;
					temp1 += i * j;
					meanx += i;
					meany += j;
					temp3 += i * i;
					cnt++;
				}
			}

		}

		meanx /= cnt;
		meany /= cnt;

		double temp2 = N * meanx * meany;
		double temp4 = N * meanx * meanx;

		double k = (temp1 - temp2) / (temp3 - temp4);

		double ang;

		if (isnan(k)) {
			ang = PI / 2;
		}
		else {
			ang = atan(k);
		}

		angs[tid] = ang;
	}
}

__global__ void PreR(long* RCadi, long* RelatedLocIndex, int* SkelImwithout1, int* SingleResult, int* rr, int PreProjectionNum, int imgH, int imgW, int nx, int skelNum, int* loc2Index) {
	int x = blockDim.x * blockIdx.x + threadIdx.x;
	int y = blockDim.y * blockIdx.y + threadIdx.y;

	int tid = y * nx + x;
	if (tid < skelNum) {
		int* Loc = (int*)malloc(sizeof(int) * (PreProjectionNum + 7));
		Loc[0] = SingleResult[tid];

		int size = 1;

#pragma unrolling
		for (int k = 0; k < 1000; k++) {
			int presize = size;
			for (int K = 0; K < presize; K++) {
				int l = Loc[K];
				int neighbors[8] = { l - imgW - 1, l - imgW, l - imgW + 1 , l - 1, l + 1, l + imgW - 1, l + imgW , l + imgW + 1 };
#pragma unrolling                   
				for (int i = 0; i < 8; i++) {
					if (SkelImwithout1[neighbors[i]] == 1){
						bool jud = false;
						for (int j = 0; j < size; j++) {
							if (Loc[j] == neighbors[i]) {
								jud = true;
								break;
							}
						}			

						if (!jud) {
							Loc[size] = neighbors[i];
							size++;
						}
					}
				}
			}

			if (size >= PreProjectionNum) {
				for (int j = 0; j < PreProjectionNum; j++) {
					int ind = loc2Index[Loc[j]];
					RelatedLocIndex[tid * PreProjectionNum + j] = ind;
					RCadi[tid * PreProjectionNum + j] = rr[ind];
				}
				break;
			}
		}
		free(Loc);
	}
}

__global__ void computeDistance(int *jy, int *jx, int *dNE, int *dNW, int *dSE, int *dSW, int i, int j, int njunc) {
	int x = blockDim.x * blockIdx.x + threadIdx.x;

	if (x < njunc) {
		dNE[x] = SQR(i - jy[x]) + SQR(j - jx[x]);
		dNW[x] = SQR(i - jy[x]) + SQR(j + 1 - jx[x]);
		dSE[x] = SQR(i + 1 - jy[x]) + SQR(j - jx[x]);
		dSW[x] = SQR(i + 1 - jy[x]) + SQR(j + 1 - jx[x]);
	}
}

void getAngs(long* locx, long* locy, long* SkelImwithout1, int SkelNum, int imgH, int imgW) {
	int dev = 0;
	cudaSetDevice(dev);

	double* d_angs;
	long* d_locx, *d_locy, *d_SkelImwithout1;

	angs = (double*)malloc(sizeof(double) * SkelNum);

	cudaMalloc((double**)&d_angs, sizeof(double) * SkelNum);
	cudaMalloc((long**)&d_locx, sizeof(long) * SkelNum);
	cudaMalloc((long**)&d_locy, sizeof(long) * SkelNum);
	cudaMalloc((long**)&d_SkelImwithout1, sizeof(long) * imgH * imgW);

	cudaMemcpy(d_locx, locx, sizeof(long) * SkelNum, cudaMemcpyHostToDevice);//
	cudaMemcpy(d_locy, locy, sizeof(long) * SkelNum, cudaMemcpyHostToDevice);
	cudaMemcpy(d_SkelImwithout1, SkelImwithout1, sizeof(long) * imgH * imgW, cudaMemcpyHostToDevice);

	int nx = (int)ceil(sqrt(SkelNum));
	int ny = (int)ceil(sqrt(SkelNum));

	dim3 block(32, 32);//the size of a block must not bigger than 1024
	dim3 grid((nx + block.x - 1) / block.x, (ny + block.y - 1) / block.y);

	adjangle1 << <grid, block >> > (d_angs, d_locx, d_locy, d_SkelImwithout1, nx, SkelNum, imgW);

	cudaDeviceSynchronize();

	cudaMemcpy(angs, d_angs, sizeof(double) * SkelNum, cudaMemcpyDeviceToHost);

	cudaError_t err = cudaGetLastError();
	if (err != cudaSuccess) {
		fprintf(stderr, "Got error %s at %s:%d\n", cudaGetErrorString(err), \
			__FILE__, __LINE__); \
			// Possibly: exit(-1) if program cannot continue....
	}

	cudaFree(d_angs);
	cudaFree(d_locx);
	cudaFree(d_locy);
	cudaFree(d_SkelImwithout1);

	cudaDeviceReset();
}

void getRCadiAndRelatedLocIndex(long* SkelImwithout1, long* SingleResult, long* rr, int PreProjectionNum, int imgH, int imgW, int skelNum, long* loc2Index) {
	int dev = 0;
	cudaSetDevice(dev);

	long* d_RCadi, * d_RelatedLocIndex;
	int* d_SingleResult, *d_rr,*d_SkelImwithout1, *d_loc2Index;

	RCadi = (long*)malloc(sizeof(long) * (skelNum * PreProjectionNum));
	RelatedLocIndex = (long*)malloc(sizeof(long) * (skelNum * PreProjectionNum));
	memset(RCadi, 0, sizeof(long) * (skelNum * PreProjectionNum));
	memset(RelatedLocIndex, 0, sizeof(long) * (skelNum * PreProjectionNum));

	cudaMalloc((long**)&d_RCadi, sizeof(long) * (skelNum * PreProjectionNum));
	cudaMalloc((long**)&d_RelatedLocIndex, sizeof(long) * (skelNum * PreProjectionNum));
	cudaMalloc((int**)&d_SkelImwithout1, sizeof(int) * imgH * imgW);
	cudaMalloc((int**)&d_SingleResult, sizeof(int) * skelNum);
	cudaMalloc((int**)&d_rr, sizeof(int) * skelNum);
	cudaMalloc((int**)&d_loc2Index, sizeof(int) * imgH * imgW);

	cudaMemcpy(d_RCadi, RCadi, sizeof(long) * (skelNum * PreProjectionNum), cudaMemcpyHostToDevice);//
	cudaMemcpy(d_RelatedLocIndex, RelatedLocIndex, sizeof(long) * (skelNum * PreProjectionNum), cudaMemcpyHostToDevice);
	cudaMemcpy(d_SkelImwithout1, SkelImwithout1, sizeof(int) * imgH * imgW, cudaMemcpyHostToDevice);
	cudaMemcpy(d_SingleResult, SingleResult, sizeof(int) * skelNum, cudaMemcpyHostToDevice);
	cudaMemcpy(d_rr, rr, sizeof(int) * skelNum, cudaMemcpyHostToDevice);
	cudaMemcpy(d_loc2Index, loc2Index, sizeof(int) * imgH * imgW, cudaMemcpyHostToDevice);

	size_t neededHeapSize = (PreProjectionNum + 7) * skelNum * sizeof(int);

	cudaDeviceSetLimit(cudaLimitMallocHeapSize, (neededHeapSize / (1024 * 1024) + 1) * 1024 * 1024);

	int nx = (int)ceil(sqrt(skelNum));
	int ny = (int)ceil(sqrt(skelNum));

	dim3 block(32, 32);//the size of a block must not bigger than 1024
	dim3 grid((nx + block.x - 1) / block.x, (ny + block.y - 1) / block.y);

	PreR << <grid, block >> > (d_RCadi, d_RelatedLocIndex, d_SkelImwithout1, d_SingleResult, d_rr, PreProjectionNum, imgH, imgW, nx, skelNum, d_loc2Index);

	cudaDeviceSynchronize();

	cudaError_t err = cudaGetLastError();
	if (err != cudaSuccess) {
		fprintf(stderr, "Got error %s at %s:%d\n", cudaGetErrorString(err), \
			__FILE__, __LINE__); \
			// Possibly: exit(-1) if program cannot continue....
	}

	cudaMemcpy(RCadi, d_RCadi, sizeof(long) * (skelNum * PreProjectionNum), cudaMemcpyDeviceToHost);
	cudaMemcpy(RelatedLocIndex, d_RelatedLocIndex, sizeof(long) * (skelNum * PreProjectionNum), cudaMemcpyDeviceToHost);

	cudaFree(d_RCadi);
	cudaFree(d_RelatedLocIndex);
	cudaFree(d_SkelImwithout1);
	cudaFree(d_SingleResult);
	cudaFree(d_rr);

	cudaDeviceReset();
}

void init_deviceMem(int *jy, int *jx, int njunc) {
	cudaMalloc((int**)&d_jy, njunc * sizeof(int));
	cudaMalloc((int**)&d_jx, njunc * sizeof(int));
	cudaMalloc((int**)&d_dNE, njunc * sizeof(int));
	cudaMalloc((int**)&d_dNW, njunc * sizeof(int));
	cudaMalloc((int**)&d_dSE, njunc * sizeof(int));
	cudaMalloc((int**)&d_dSW, njunc * sizeof(int));

	cudaMemcpy(d_jy, jy, njunc * sizeof(int), cudaMemcpyHostToDevice);
	cudaMemcpy(d_jx, jx, njunc * sizeof(int), cudaMemcpyHostToDevice);
}

void free_jy_jx() {
	cudaFree(d_jy);
	cudaFree(d_jx);
	cudaFree(d_dNE);
	cudaFree(d_dNW);
	cudaFree(d_dSE);
	cudaFree(d_dSW);
}

void getDistance(int i, int j, int *dNE, int *dNW, int *dSE, int *dSW, int njunc) {
	dim3 block(256);
	dim3 grid((njunc + block.x - 1) / block.x);

	computeDistance << <grid, block >> > (d_jy, d_jx, d_dNE, d_dNW, d_dSE, d_dSW, i, j, njunc);

	cudaDeviceSynchronize();

	cudaMemcpy(dNE, d_dNE, njunc * sizeof(int), cudaMemcpyDeviceToHost);
	cudaMemcpy(dNW, d_dNW, njunc * sizeof(int), cudaMemcpyDeviceToHost);
	cudaMemcpy(dSE, d_dSE, njunc * sizeof(int), cudaMemcpyDeviceToHost);
	cudaMemcpy(dSW, d_dSW, njunc * sizeof(int), cudaMemcpyDeviceToHost);
}
