function [WithWidthFit_s,WithWidthFit_r,h]=WidthSolvedFWHM1D(Line2fit,px)
    matfit=fittype('WithWidth1D(sigma,r,b,A,x)','independent', 'x','coefficients',{'sigma','r','b','A'});    
    xright=(length(Line2fit)-1)/2;
    if size(Line2fit,2)==1
        L=Line2fit';
    else
       L=Line2fit;
    end
    xx_spand=-xright:0.1:xright;
    xx0=-xright:xright;
    L_expand=interp1(xx0,L,xx_spand,'pchip');
    NmaxTemp=sum(L>graythresh(L));
    Nmax=ceil(NmaxTemp/2);
    WidthFit_r=zeros(1,Nmax);
    WidthFit_s=zeros(1,Nmax);
    WidthFit_e=zeros(1,Nmax);
    gaussfit=fit(px*xx0',L','Gauss1','Lower',[1 0 0],'Upper',[1 Inf Inf]);
%     rstart=[0,0.5:1:Nmax];
%     xx_spand=xx_spand*px;
    for nn=1:Nmax
        [cfun,gof]=fit(px*xx_spand',L_expand',matfit,'StartPoint',[gaussfit.c1/1.414-nn*px,nn*px,0,0],'Lower',[0 0 0 0 ],'Upper',[gaussfit.c1/1.414,(Nmax+1)*px,1,Inf],'Normalize','off'); % changed 20220418
        WidthFit_r(nn)=cfun.r;
        WidthFit_s(nn)=cfun.sigma;
        FitResultTemp=(WithWidth1D(cfun.sigma,cfun.r,cfun.b,cfun.A,xx_spand*px)-L_expand).^2;
        WidthFit_e(nn)=log(sum(FitResultTemp)/length(FitResultTemp(:)));
    end
        Index=find(WidthFit_e==min(WidthFit_e(:)));
        WithWidthFit_r=mean(WidthFit_r(Index));
        WithWidthFit_s=mean(WidthFit_s(Index));
        WithWidthFit_s=abs(2.3548*WithWidthFit_s);
    %--
    h=figure;
    Y=WithWidth1D(WithWidthFit_s/2.3548,WithWidthFit_r,0,1,xx_spand*px);
    Y=Normalized(Y);
    E=  exp(-(xx_spand*px).^2/(2*WithWidthFit_s/2.3548*WithWidthFit_s/2.3548));
    X=(xx_spand+xright+1)*px;
    plot(X,Y,'Color','g','MarkerSize',3,'LineWidth',2);hold on
    plot(X,E,'Color','y','MarkerSize',3,'LineWidth',2);
    ylabel('Normalized Intensity','FontSize',16,'FontWeight','bold');
    xlabel('Distance (nm)','FontSize',16,'FontWeight','bold');
    set(gca,'FontSize',16,'LineWidth',2); 
    box off;
    hold on
    %--
end