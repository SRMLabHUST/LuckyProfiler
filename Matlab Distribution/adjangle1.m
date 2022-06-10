% Try for getting gradient
function [ang,G]=adjangle1(Locx,Locy,i,SkelImwithout1)
    G1=SkelImwithout1(Locx(i),Locy(i)+1);
    G2=SkelImwithout1(Locx(i)-1,Locy(i)+1);
    G3=SkelImwithout1(Locx(i)-1,Locy(i));
    G4=SkelImwithout1(Locx(i)-1,Locy(i)-1);
    G5=SkelImwithout1(Locx(i),Locy(i)-1);
    G6=SkelImwithout1(Locx(i)+1,Locy(i)-1);
    G7=SkelImwithout1(Locx(i)+1,Locy(i));
    G8=SkelImwithout1(Locx(i)+1,Locy(i)+1);
    G9=SkelImwithout1(Locx(i)+1,Locy(i)+2);
    G10=SkelImwithout1(Locx(i),Locy(i)+2);
    G11=SkelImwithout1(Locx(i)-1,Locy(i)+2);
    G12=SkelImwithout1(Locx(i)-2,Locy(i)+2);
    G13=SkelImwithout1(Locx(i)-2,Locy(i)+1);
    G14=SkelImwithout1(Locx(i)-2,Locy(i));
    G15=SkelImwithout1(Locx(i)-2,Locy(i)-1);
    G16=SkelImwithout1(Locx(i)-2,Locy(i)-2);
    G17=SkelImwithout1(Locx(i)-1,Locy(i)-2);
    G18=SkelImwithout1(Locx(i),Locy(i)-2);
    G19=SkelImwithout1(Locx(i)+1,Locy(i)-2);
    G20=SkelImwithout1(Locx(i)+2,Locy(i)-2);
    G21=SkelImwithout1(Locx(i)+2,Locy(i)-1);
    G22=SkelImwithout1(Locx(i)+2,Locy(i));
    G23=SkelImwithout1(Locx(i)+2,Locy(i)+1);
    G24=SkelImwithout1(Locx(i)+2,Locy(i)+2);
    G=[G16,G15,G14,G13,G12;G17,G4,G3,G2,G11;G18,G5,1,G1,G10;G19,G6,G7,G8,G9;G20,G21,G22,G23,G24];
    [x,y]=find(G==1);
    N=sum(G(:));
    temp1=sum(x.*y);
    temp2=N*mean(x)*mean(y);
    temp3=sum(x.*x);
    temp4=N*sum(mean(x)*mean(x));
    k=(temp1-temp2)/(temp3-temp4);
    if isnan(k)
        ang=pi/2;
    else
        ang=atan(k);
    end
%     ang=pi/2-ang; %try
end



%     angle=[];
%        if G1==1
%            angle=[angle,0];
%        end
%        if G2==1
%            angle=[angle,45];
%        end
%        if G3==1
%            angle=[angle,90];
%        end
%        if G4==1
%            angle=[angle,135];
%        end
%        if G5==1
%            angle=[angle,180];
%        end
%        if G6==1
%            angle=[angle,45];
%        end
%        if G7==1
%            angle=[angle,90];
%        end
%        if G8==1
%            angle=[angle,135];
%        end
%        if G9==1
%            angle=[angle,157.5];
%        end
%        if G10==1
%            angle=[angle,0];
%        end
%        if G11==1
%            angle=[angle,22.5];
%        end
%        if G12==1
%            angle=[angle,45];
%        end
%        if G13==1
%            angle=[angle,67.5];
%        end
%        if G14==1
%            angle=[angle,90];
%        end
%        if G15==1
%            angle=[angle,112.5];
%        end
%        if G16==1
%            angle=[angle,135];
%        end
%        if G17==1
%            angle=[angle,157.5];
%        end
%        if G18==1
%            angle=[angle,180];
%        end
%        if G19==1
%            angle=[angle,22.5];
%        end
%        if G20==1
%            angle=[angle,45];
%        end
%        if G21==1
%            angle=[angle,67.5];
%        end
%        if G22==1
%            angle=[angle,90];
%        end
%        if G23==1
%            angle=[angle,112.5];
%        end
%        if G24==1
%            angle=[angle,135];
%        end
%        if sum(G(:))==0
%            ang=0;
%        else
%            if sum(angle~=0 & angle~=180)==0
%                ang=0;
%                ang=pi/2-ang;
%            else
%               anglemean=mean(angle(angle~=0 & angle~=180));
%               if abs(anglemean)-0 == abs(anglemean)-180
%                   ang=mean(angle);
%               else
%                   if abs(anglemean-0) > abs(anglemean-180)
%                     angle(angle==0)=180;
%                     ang=mean(angle);
%                     ang=deg2rad(ang-90);
%                   else 
%                      angle(angle==180)=0;
%                      ang=mean(angle);
%                     ang=deg2rad(ang-90);
%                   end
%               end            
%            end
%        end
