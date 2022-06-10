%load data that calculated after How2FWHM2Auto
function Im2Show=ShowROI(Result,SizeIm1,SizeIm2)
for i=1:size(Result,1)
    Im2Show=zeros(SizeIm1,SizeIm2);
    Info=Result{i,2};
    Loc=Info(:,2:3);
    for j=1:size(Loc,1)
        x=Loc(j,1);
        y=Loc(j,2);
        Im2Show(x,y)=1;
    end
    
end
end