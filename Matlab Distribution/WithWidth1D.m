function norg=WithWidth1D(sigma,rBead,b,A,x)
    g1= A*exp(-x.^2/(2*sigma*sigma));
    g2 =sigmoid1D(x+rBead)-sigmoid1D(x-rBead);
    g =conv(g2,g1,'same');
    norg =g+b;
end