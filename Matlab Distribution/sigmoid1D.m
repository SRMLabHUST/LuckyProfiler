function u = sigmoid1D(x)
w=10000000;
fenmu = 1+exp(-w*x);
u=1./fenmu;
end