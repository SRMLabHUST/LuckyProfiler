function [Result]=How2FWHM2ROI(RenderedImg,ROISelected,px)
%% This code is to calculate the full image and Point where has the reliable resolution belong several FWHM method
%Input: 
%   RenderedIm--Img;
%   ROISelected -- Rectangle Loc (X0,Y0,Width,Height);Line is Ok for it can
%   also get related parameter
%   px--pixle size
%Output:
%   Result:{State,Loc,ProjectionThickness,CalculatedResolution,SolvedWidth,Distance,Fitresult,Fitgood}
%State:
    % 1--GaussianFitting
    % 2--WidthSolved     
    % 3--Two Point   
%% Start
ROINum =size(ROISelected,1); %[]
if size(RenderedImg,3)==3
   RenderedImg=rgb2gray(RenderedImg);
end
Offset=[RenderedImg(1,:),RenderedImg(size(RenderedImg,1),:),RenderedImg(:,1)',RenderedImg(:,size(RenderedImg,2))'];
for k=1:ROINum
%% Get LineProfile ROI
    XLoc=[];
    YLoc=[];
    XLoc=ROISelected(k,1):ROISelected(k,1)+ROISelected(k,3);
    YLoc=ROISelected(k,2):ROISelected(k,2)+ROISelected(k,4);
    ROI=RenderedImg(YLoc,XLoc);
    ROISubOffset=ROI-mean(Offset,2);
    
    
%%   Prepare  
    n=ceil(20/px);
    se=strel('disk',n);
    Im=DenoiseFunc(ROISubOffset,px); 
    RenderedImg2BS=imdilate(Im,se); 
    Bounderpre=imbinarize(RenderedImg2BS,'adaptive','Sensitivity',0.4); 
    h=fspecial('laplacian',0);
    Im2select=imfilter(Bounderpre,h,'same');
    se=strel('disk',1);
    Im2select=imdilate(Im2select,se); 
    thres=30;
    skeletontemp1=Normalized(Im);
    skeletontemp=imbinarize(skeletontemp1,'adaptive','Sensitivity',0.4);
    [skr,rad] = skeleton( skeletontemp);
    SkelImwithout1 = bwmorph(skr > thres,'skel',inf);
    SkelNum=sum(SkelImwithout1(:));
    [Locx,Locy]=find(SkelImwithout1);
    rr=zeros(1,SkelNum);
    LineProfile=cell(1,SkelNum);
    for i=1:SkelNum
% Above is using Gradient to calculate angle, below judges angle through 8 ajdent pixels
       [angle,G]=adjangle1(Locx,Locy,i,SkelImwithout1);
       BoundCount=0;
       LineProfile_r=[];
       LineProfile_l=[];
       XX1=0;YY1=0;XX2=0;YY2=0;rep=0;
       for r=1:1000
            X1=Locx(i)-r*sin(angle);
           Y1=Locy(i)+r*cos(angle);
           X1=round(X1);
           Y1=round(Y1);
           X2=Locx(i)-r*sin(pi+angle);
           Y2=Locy(i)+r*cos(pi+angle);
           X2=round(X2);
           Y2=round(Y2);
           index1=Locx(i);
           index2=Locy(i);
           if Y1>size(Im2select,2)|| Y1<1 || X1>size(Im2select,1)||X1<1||Y2>size(Im2select,2)|| Y2<1 || X2>size(Im2select,1)||X2<1
               break
           end
           if (X1==XX1 && Y1==YY1) || (X2==XX2 && Y2==YY2)
               rep=rep+1;
           else
                XX1=X1;YY1=Y1;XX2=X2;YY2=Y2;
                BoundCount=Im2select(X1,Y1)+Im2select(X2,Y2)+BoundCount;
                LineProfile_r=[LineProfile_r,ROI(X1,Y1)];
                LineProfile_l=[ROI(X2,Y2),LineProfile_l];
               if BoundCount>=2
                rr(i)=r-rep;
                    break
               end
           end
       end
       LineProfile{i}=[LineProfile_l,ROI(index1,index2),LineProfile_r]; 
    end
    rrmax=max(rr)+1;
    %start-add on 20220405
    rrnozero=rr(rr~=0);
    [a1,a2]=histcounts(rrnozero);
    a3=(a2(2:end)+a2(1:end-1))/2;
    fitresult=fit(a3',a1'/sum(a1),'Gauss1');
    T1=fitresult.b1+3*fitresult.c1;
    T2=fitresult.b1-3*fitresult.c1;
    Index1=rr>T1 | rr<T2;
    rr(Index1)=0;
    %end-add on 20220405
     
     LP=zeros(SkelNum,2*rrmax+1);
     for j=1:SkelNum
        if rr(j)>0
        rradd=rrmax-rr(j);
        LP(j,:)=[zeros(1,rradd),LineProfile{j}(:)',zeros(1,rradd)];
        end
     end
     L{k}=sum(LP);  

    
 %%  Line Profile
    NorL=Normalized(L{k}(:));
    maxL=max(L{k}(:));
    Lsize=1:length(L{k}(:));
    Lsize=Lsize*px;
    [WithWidthFit_s,WithWidthFit_r,h]=WidthSolvedFWHM1D(NorL',px);
    Result{k,1}=2;
    Result{k,2}=ROISelected;
    Result{k,3}=SkelNum;
    Result{k,4}=WithWidthFit_s;
    Result{k,5}=WithWidthFit_r;
    
    %-----------
    figure(h);
    for jj=1:SkelNum
         plot(Lsize,LP(jj,:)/maxL,':','LineWidth',1);hold on
    end
    plot(Lsize,NorL,'Color','k','MarkerSize',3,'LineWidth',2);
    ylabel('Normalized Intensity','FontSize',16,'FontWeight','bold');
    xlabel('Distance (nm)','FontSize',16,'FontWeight','bold');
    set(gca,'FontSize',16,'LineWidth',2); 
    box off;
    hold off
    %----------
%     savefig(['E:\Mine\LAB\Resolution\FWHM method for LM\ROI-Projection\ROI-20220405\ROI','-',num2str(k),'.fig']);
end
end


