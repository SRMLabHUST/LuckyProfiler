function [ oI ] = DenoiseFunc( II,pixelsize )
%UNTITLED Summary of this function goes here
%   Detailed explanation goes here
n=ceil(50/pixelsize);    
I=single(II);
h1=fspecial('average',n);
h3=fspecial('gaussian',n);
I1=medfilt2(I,[n,n]);
I2=filter2(h3,I1);
I3=filter2(h1,I2);

[Gx,Gy]=gradient(I3);
F=sqrt(Gx.^2+Gy.^2);
[GF1,GF2]=gradient(F);
GF=sqrt(GF1.^2+GF2.^2);
% T=otsu_thres(GF);
Ttemp1=mean(GF(:));
Ttemp2=std(GF(:));
if Ttemp1<Ttemp2
    T=mean(GF(:))-std(GF(:));%
else
    T=mean(GF(:))+std(GF(:));
end
Index=GF>T & I3> min(5*std(std(I3)),mean(mean(I3)));
oI1=I3.*single(Index);
oI2=medfilt2(oI1,[n,n]);
oI=filter2(h3,oI2);


end

