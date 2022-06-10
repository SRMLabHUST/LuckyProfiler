function [ZhixinMin,ZhixinMax]=StaRegin1(Mean,Std,Min)

	V=2*Std;
	ZhixinMin=Mean-V;
	ZhixinMax=Mean+V;
    
    if ZhixinMin<Min
        ZhixinMin=Min;
    end
    
end