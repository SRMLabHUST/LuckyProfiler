function [Result,SingleResult,FinalTro10Id,PreProjectionNum]=How2FWHM2Auto(RenderedImg,px,SaveLabel,Method)
%% This code is to calculate the full image and Point where has the reliable resolution belong several FWHM method
%Input: 
%   RenderedIm--Img;
%   px--pixle size
%Output:
%   SingelResult:[SkelID,Locx,Locy,angle,rr,SingleProfile]
%   Result:{SkelID,Loc,ProjectionProfile,CalculatedResolution,SolvedWidth,Fitresult,Fitgood}
%% Prepare for whole image 
     %str=['E:\Your save path\',SaveLabel];

        RenderedImg=rgb2gray(RenderedImg);
    end
    SizeIm1=size(RenderedImg,1);
    SizeIm2=size(RenderedImg,2);
    Offset=[RenderedImg(1,:),RenderedImg(SizeIm1,:),RenderedImg(:,1)',RenderedImg(:,SizeIm2)'];
    II=RenderedImg-mean(Offset,2);
    n=ceil(20/px);
    se=strel('disk',n);
    Im=DenoiseFunc(II,px); 
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
    

%% Calcualte each location to get r and lineprofile    
    [LineProfile,ang,rr]=EachProfile(Locx,Locy,SkelNum,SkelImwithout1,Im2select,II);
    rrmax=max(rr(:))+1;
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
    
     SingleResult=zeros(SkelNum,2*rrmax+1+5);
     for j=1:SkelNum         
        SingleResult(j,1)=j;
        index1=Locx(j);
        index2=Locy(j);
        SingleResult(j,2:3)=[index1,index2];
        SingleResult(j,4)=ang(j);
        SingleResult(j,5)=rr(j);
        if rr(j)>0
        rradd=rrmax-rr(j);
        SingleResult(j,6:size(SingleResult,2))=[zeros(1,rradd),LineProfile{j}(:)',zeros(1,rradd)];
        end
     end
   
%% According r selecte ROI      
     %Pre-thickness setting (Projection Num)
     if SkelNum>100
         randnum=100;
     else
         randnum=SkelNum;
     end
     [PreProjectionNum,intenum]=PreProNum(randnum,SkelImwithout1,SkelNum,SingleResult,px,SizeIm1,SizeIm2);
    
    % pre-r for each Region
    parfor i=1:SkelNum  
        [RCadi(i,:),RelatedLocIndex(i,:)]=PreR(SkelImwithout1,SingleResult,rr,i,PreProjectionNum,SizeIm1,SizeIm2);       
    end
    
    % According r choosing and calculate resolution
    %start-add on 20220405
    for i=1:size(RCadi,1)
        RCadiTemp=RCadi(i,:);
        if sum(RCadiTemp~=0)
        RCadiMean(i)=mean(RCadiTemp(RCadiTemp~=0));
        else
            RCadiMean(i)=0;
        end
    end
    [R,ind]=sort(RCadiMean);
 %end-add on 20220405
 

    Index=find(R~=0);
    N=ceil(0.04*SkelNum); %changed by 20220407
    SkelId=ind(Index(1):Index(N));
    for i=1:N
        SI=SkelId(i);
        LocIndex=RelatedLocIndex(SI,:);
        for j=1:size(LocIndex,2)
            LP(j,:)=SingleResult(LocIndex(j),6:size(SingleResult,2));
         end
        L=Normalized(sum(LP));
        switch Method
            case 'Size'
            [WithWidthFit_s,WithWidthFit_r,h]=WidthSolvedFWHM1D(L',px);
            Result{i,1}=SI;
            Result{i,2}=[LocIndex',SingleResult(LocIndex,2:3)];
            Result{i,3}=L;
            Result{i,4}=WithWidthFit_s;
            Result{i,5}=WithWidthFit_r;
            case 'Gauss'
                Lsize=px*(1:length(L));
            [fitresult,gof]=fit(Lsize',L','Gauss1','Lower',[1 0 0],'Upper',[1 Inf Inf]);
            Result{i,1}=SI;
            Result{i,2}=[LocIndex',SingleResult(LocIndex,2:3)];
            Result{i,3}=L;
            Result{i,4}=fitresult.c1*1.665;
            Result{i,5}=gof.rsquare;
        end
    close all
    end
    [FinalTro10Id]=ChoosOne(Result);
    disp(['Final ID Through ',num2str(N),' Candidate is ',num2str(FinalTro10Id)]);
    Im2Show=ShowROI(Result(FinalTro10Id,:),SizeIm1,SizeIm2);
    figure;imshow(Im2Show);
    Lfinal=Result{FinalTro10Id,3};
    xlength=1:length(Lfinal);
    xlength=xlength*px;
    [WithWidthFit_s,WithWidthFit_r,h]=WidthSolvedFWHM1D(Lfinal',px);
    fitresult=fit(xlength',Lfinal','Gauss1','Lower',[1 0 0],'Upper',[1 Inf Inf]);
    figure(h);
    plot(fitresult,xlength',Lfinal);
    ylabel('Normalized Intensity','FontSize',16,'FontWeight','bold');
    xlabel('Distance (nm)','FontSize',16,'FontWeight','bold');
    set(gca,'FontSize',16,'LineWidth',2); 
    box off;
    legend('Deconvolution','Raw','Gaussian');
%     save([str,'.mat']);
end

function [LineProfile,ang,rr]=EachProfile(Locx,Locy,SkelNum,SkelImwithout1,Im2select,II)
    rr=zeros(1,SkelNum);
    LineProfile=cell(1,SkelNum);
    ang=zeros(1,SkelNum);
    for i= 1:SkelNum 
%        below judges angle through 8 ajdent pixels
       [angle,G]=adjangle1(Locx,Locy,i,SkelImwithout1);
       ang(i)=angle;
       BoundCount=0;
       LineProfile_r=[];
       LineProfile_l=[];
       XX1=0;YY1=0;XX2=0;YY2=0;rep=0;
       for r=1:1000
            X1=Locx(i)-r*sin(angle); %suit with new angle
           Y1=Locy(i)+r*cos(angle);
           X1=round(X1);
           Y1=round(Y1);
           X2=Locx(i)-r*sin(pi+angle); %suit with new angle
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
                LineProfile_r=[LineProfile_r,II(X1,Y1)];
                LineProfile_l=[II(X2,Y2),LineProfile_l];
               if BoundCount>=2
                rr(i)=r-rep;
                    break
               end
           end
       end
       LineProfile{i}=[LineProfile_l,II(index1,index2),LineProfile_r];
    end
end

function [PreProjectionNum,intenum]=PreProNum(randnum,SkelImwithout1,SkelNum,SingleResult,px,SizeIm1,SizeIm2)
     PreIndex1=round(SkelNum*rand(1,randnum));
     aTry=SingleResult(:,6:size(SingleResult,2));
     Cannot=find(sum(aTry,2)==0);
     Cannum=0;
     for i=1:randnum
         if sum(PreIndex1(i)==Cannot)
             Cannum=Cannum+1;
         else
             PreIndex(i-Cannum)=PreIndex1(i);
         end
     end
     randnum=length(PreIndex);
     intenum=zeros(1,randnum);
    parfor i=1:randnum
        Index=PreIndex(i);
        XLoc=SingleResult(Index,2);
        YLoc=SingleResult(Index,3);
        Loc=[XLoc,YLoc];
        prefitg=0;
        LP=[];
        L=SingleResult(Index,6:size(SingleResult,2));
        L=Normalized(L);
        Lsize=1:length(L);
        Lsize=Lsize*px;
        [fitresult,fitgood]=fit(Lsize',L','Gauss1');        
        prefitg=fitresult.c1*1.665;
      if XLoc-1>=1 && YLoc-1>=1 && XLoc+1<=SizeIm1 && YLoc+1<=SizeIm2 
        for k=1:1000    
            Loc=aggregate(Loc,SkelImwithout1);          
            for j=1:size(Loc,1)
                Ind=find(SingleResult(:,2)==Loc(j,1) & SingleResult(:,3)==Loc(j,2));
                LP(j,:)=SingleResult(Ind,6:size(SingleResult,2));
            end
            L=Normalized(sum(LP,1));
            [fitresult,fitgood]=fit(Lsize',L','Gauss1');
            fg=fitgood.rsquare;
            sig=fitresult.c1*1.665;
            if fg>0.8 && abs(sig-prefitg)<0.01
                intenum(i)=size(Loc,1);
                break
            else
                prefitg=sig;
            end
        end
      end
    end
    PreProjectionNum=ceil(sum(intenum)/randnum); % changed
end

function  [RCadi,Index]=PreR(SkelImwithout1,SingleResult,rr,i,PreProjectionNum,SizeIm1,SizeIm2)
        XLoc=SingleResult(i,2);
        YLoc=SingleResult(i,3);
        Loc=[XLoc,YLoc];
      if XLoc-1>=1 && YLoc-1>=1 && XLoc+1<=SizeIm1 && YLoc+1<=SizeIm2 
        for k=1:1000    
            Loc=aggregate(Loc,SkelImwithout1);
            if size(Loc,1)>=PreProjectionNum
                 for j=1:PreProjectionNum
                    Index(j)=find(SingleResult(:,2)==Loc(j,1)&SingleResult(:,3)==Loc(j,2));
                    RCadi(j)=rr(Index(j));
                end
                break
            else
                Index=zeros(1,PreProjectionNum);
                RCadi=zeros(1,PreProjectionNum);
            end
        end
       else
          Index=zeros(1,PreProjectionNum);
          RCadi=zeros(1,PreProjectionNum);
      end
end

function [FinalTro10Id]=ChoosOne(Result) %changed by 20220608
    Index=cell2mat(Result(:,5))>0.95;
    Num=find(Index);
    A=Result(Index,4);
    A=cell2mat(A);
    [~,Id]=sort(A);
    FinalTro10Id=Num(Id(1));
end