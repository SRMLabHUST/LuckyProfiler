package utility;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class MyCurveFitter extends AbstractCurveFitter {
    private final ParametricUnivariateFunction function;
    private final double[] initialGuess;
    private final int maxIter;

    private MyCurveFitter(ParametricUnivariateFunction function, double[] initialGuess, int maxIter) {
        this.function = function;
        this.initialGuess = initialGuess;
        this.maxIter = maxIter;
    }

    public static MyCurveFitter create(ParametricUnivariateFunction f, double[] start) {
        return new MyCurveFitter(f, start, 2147483647);
    }

    @Override
    protected LeastSquaresOptimizer getOptimizer() {
        return new LevenbergMarquardtOptimizer().withParameterRelativeTolerance(1.0E-3D);
    }

    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> observations) {
        int len = observations.size();
        double[] target = new double[len];
        double[] weights = new double[len];
        int count = 0;

        for (Iterator<WeightedObservedPoint> i$ = observations.iterator(); i$.hasNext(); ++count) {
            WeightedObservedPoint obs = i$.next();
            target[count] = obs.getY();
            weights[count] = obs.getWeight();
        }

        TheoreticalValuesFunction model = new TheoreticalValuesFunction(this.function, observations);
        return (new LeastSquaresBuilder()).maxEvaluations(2147483647).maxIterations(this.maxIter).start(this.initialGuess).target(target).weight(new DiagonalMatrix(weights)).model(model.getModelFunction(), model.getModelFunctionJacobian()).build();
    }

    public static Result widthSolvedFWHM1D(double[] L, double px) {
        double xright = (L.length - 1) / 2f;
        List<Double> xx_spand_list = new ArrayList<>();
        for (double elem = -xright; elem <= xright; elem += 0.1) {
            xx_spand_list.add(elem);
        }

        double[] xx_spand = xx_spand_list.stream().mapToDouble(t -> t).toArray();

        List<Double> xx0_list = new ArrayList<>();
        for (double elem = -xright; elem <= xright; elem += 1) {
            xx0_list.add(elem);
        }
        double[] xx0 = xx0_list.stream().mapToDouble(t -> t).toArray();

        double[] L_expand = interp1(xx0, L, xx_spand);
        int NmaxTemp = getThreshCount(L);

        int Nmax = (int) Math.ceil(NmaxTemp / 2f);

        double[] WidthFit_r = new double[Nmax];
        double[] WidthFit_s = new double[Nmax];
        double[] WidthFit_e = new double[Nmax];

        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < L.length; i++) {
            obs.add(px * xx0_list.get(i), L[i]);
        }

        double[] gaussfit = GaussianCurveFitter.create().fit(obs.toList());

        double[] xx_spand2fit = new double[xx_spand.length];
        for (int i = 0; i < xx_spand.length; i++) {
            xx_spand2fit[i] = xx_spand[i] * px;
        }

        double minWidthFit_e = Double.MAX_VALUE;
        for (int i = 0; i < Nmax; i++) {
            ParametricUnivariateFunction myFunction = new MyFunction(xx_spand2fit);
            double[] guess = new double[]{gaussfit[2] * 1.414 - px, i * px, 0, 0};
            MyCurveFitter fitter = MyCurveFitter.create(myFunction, guess);
            WeightedObservedPoints obs2 = new WeightedObservedPoints();
            for (int j = 0; j < xx_spand2fit.length; j++) {
                obs2.add(xx_spand2fit[j], L_expand[j]);
            }
            double[] cfun = fitter.fit(obs2.toList());
            WidthFit_r[i] = cfun[1];
            WidthFit_s[i] = cfun[0];
            double[] norg = WithWidth1D.withWidth(cfun[0], cfun[1], cfun[2], cfun[3], xx_spand2fit);
            double[] FitResultTemp = new double[norg.length];
            for (int j = 0; j < norg.length; j++) {
                FitResultTemp[j] = Math.pow(norg[j] - L_expand[j], 2);
            }
            WidthFit_e[i] = Math.log(Arrays.stream(FitResultTemp).sum() / FitResultTemp.length);
            minWidthFit_e = Math.min(minWidthFit_e, WidthFit_e[i]);
        }

        List<Integer> Index = new ArrayList<>();
        for (int i = 0; i < WidthFit_e.length; i++) {
            if (WidthFit_e[i] == minWidthFit_e) Index.add(i);
        }

        double WidthFit_r_sum = 0, WidthFit_s_sum = 0;
        for (Integer index : Index) {
            WidthFit_r_sum += WidthFit_r[index];
            WidthFit_s_sum += WidthFit_s[index];
        }

        double WithWidthFit_r = WidthFit_r_sum / Index.size(), WithWidthFit_s = WidthFit_s_sum / Index.size();
        WithWidthFit_s = Math.abs(2.3548 * WithWidthFit_s);

        double[][] Y = new double[1][];
        Y[0] = WithWidth1D.withWidth(WithWidthFit_s / 2.3548, WithWidthFit_r, 0, 1, xx_spand2fit);
        Y = Arithmetic.Normalized(Y);
        double[] X = new double[xx_spand.length];
        for (int i = 0; i < X.length; i++) {
            X[i] = (xx_spand[i] + xright + 1) * px;
        }

        return new Result(WithWidthFit_s, X, Y[0]);
    }

    public static class Result {
        public double WithWidthFit_s;
        public double[] X;
        public double[] Y;

        public Result(double withWidthFit_s, double[] x, double[] y) {
            WithWidthFit_s = withWidthFit_s;
            X = x;
            Y = y;
        }
    }

    static int getThreshCount(double[] l) {
        //normalize to 255
        int[] normL = new int[l.length];
        for (int i = 0; i < l.length; i++) {
            normL[i] = (int) Math.round(l[i] * 255);
        }

        Mat matL = new Mat(1, l.length, CvType.CV_8UC1);
        for (int i = 0; i < l.length; i++) {
            matL.put(0, i, normL[i]);
        }

        Imgproc.threshold(matL, matL, 0, 255, Imgproc.THRESH_OTSU);
        return Core.countNonZero(matL);
    }

    private static double[] interp1(double[] xx0, double[] l, double[] xx_spand) {
        AkimaSplineInterpolator akimaSplineInterpolator = new AkimaSplineInterpolator();
        PolynomialSplineFunction interpolate = akimaSplineInterpolator.interpolate(xx0, l);
        double[] L_expand = new double[xx_spand.length];
        for (int i = 0; i < xx_spand.length; i++) {
            L_expand[i] = interpolate.value(xx_spand[i]);
        }
        return L_expand;
    }

//    public static void main(String[] args) throws IOException {
//        double[] L = new double[]{0, 0, 0.0066, 0.0589, 0.0499, 0.0173, 0.0138, 0.0371, 0.1542, 0.3346, 0.6553, 0.9677, 0.9902, 1, 0.5891, 0.3935, 0.1684, 0.1203, 0.0334, 0.0006, 0, 0, 0, 0, 0};
//        AkimaSplineInterpolator akimaSplineInterpolator = new AkimaSplineInterpolator();
//        double[] x = new double[]{-12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
//        PolynomialSplineFunction interpolate = akimaSplineInterpolator.interpolate(x, L);
//        double[] interped = new double[241];
//        for (int i = 0; i < 241; i++) {
//            interped[i] = interpolate.value(-12 + i * 0.1);
//        }
//
//        MatFileReader L_expand_mat = new MatFileReader("L_expand.mat");
//        MLDouble L_expandML = (MLDouble) L_expand_mat.getMLArray("L_expand");
//        double[] L_expand = L_expandML.getArray()[0];
//
//        LineChartDemo0 err = new LineChartDemo0("ERR", x, interped, L_expand);
//        err.pack();
//        err.setVisible(true);
//    }
}

//class LineChartDemo0 extends ApplicationFrame {
//    public LineChartDemo0(String titile, double[] x, double[] interped, double[] L_expand) {
//        super(titile);
//        CategoryDataset dataset = createDataset(x, interped, L_expand);
//        JFreeChart chart = createChart(dataset);
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new Dimension(800, 500));
//        setContentPane(chartPanel);
//    }
//
//    private CategoryDataset createDataset(double[] x, double[] interped, double[] L_expand) {
//        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
//        for (int i = 0; i < x.length; i++) {
//            dataset.addValue((Number) Math.abs(interped[i] - L_expand[i]), "err", i);
//        }
//        return dataset;
//    }
//
//    private JFreeChart createChart(CategoryDataset dataset) {
//        return ChartFactory.createLineChart(
//                "Line Chart Demo",
//                "Category Axis",
//                "Value Axis",
//                dataset,
//                PlotOrientation.VERTICAL,
//                false,
//                true,
//                false
//        );
//    }
//}

class MyFunction implements ParametricUnivariateFunction {
    private final WithWidth1D withWidth1D;

    MyFunction(double[] x) {
        this.withWidth1D = new WithWidth1D(x);
    }

    public double value(double x, double... parameters) {
        double sigma = parameters[0];
        double rBead = parameters[1];
        double b = parameters[2];
        double A = parameters[3];
        if (!withWidth1D.samePara(sigma, rBead, b, A)) {
            withWidth1D.setPara(sigma, rBead, b, A);
            withWidth1D.upgrade();
        }

        return withWidth1D.getY(x);
    }

    public double[] gradient(double x, double... parameters) {
        double sigma = parameters[0];
        double rBead = parameters[1];
        double b = parameters[2];
        double A = parameters[3];
        if (!withWidth1D.samePara(sigma, rBead, b, A)) {
            withWidth1D.setPara(sigma, rBead, b, A);
            withWidth1D.upgrade();
        }
        double[] gradients = new double[4];
        WithWidth1D withWidth1D_sigma = new WithWidth1D(sigma + 0.0001, rBead, b, A, this.withWidth1D.getAllX());
        gradients[0] = (withWidth1D_sigma.getY(x) - this.withWidth1D.getY(x)) / 0.0001;

        WithWidth1D withWidth1D_rBead = new WithWidth1D(sigma, rBead + 0.0001, b, A, this.withWidth1D.getAllX());
        gradients[1] = (withWidth1D_rBead.getY(x) - this.withWidth1D.getY(x)) / 0.0001;

        WithWidth1D withWidth1D_b = new WithWidth1D(sigma, rBead, b + 0.0001, A, this.withWidth1D.getAllX());
        gradients[2] = (withWidth1D_b.getY(x) - this.withWidth1D.getY(x)) / 0.0001;

        WithWidth1D withWidth1D_A = new WithWidth1D(sigma, rBead, b, A + 0.0001, this.withWidth1D.getAllX());
        gradients[3] = (withWidth1D_A.getY(x) - this.withWidth1D.getY(x)) / 0.0001;
        return gradients;
    }
}