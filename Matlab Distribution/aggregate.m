function ALoc=aggregate(Loc,Skel) 
    ALoc=Loc; % x,y
    for k=1:size(Loc,1)
        x=Loc(k,1);
        y=Loc(k,2);
        L(1,1:2)=[x-1,y-1];
        L(2,1:2)=[x-1,y];
        L(3,1:2)=[x-1,y+1];
        L(4,1:2)=[x,y-1];
        L(5,1:2)=[x,y+1];
        L(6,1:2)=[x+1,y-1];
        L(7,1:2)=[x+1,y];
        L(8,1:2)=[x+1,y+1];
        for i=1:8
            jud= ALoc(:,1)==L(i,1) &ALoc(:,2)==L(i,2);
            if Skel(L(i,1),L(i,2))==1 && sum(jud)==0
                ALoc=[ALoc;L(i,:)];
            end
        end
    end
end