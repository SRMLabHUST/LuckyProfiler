package core;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import utility.MyCurveFitter;
import utility.Skeleton;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static core.Adjangle.adjangle1;
import static core.Aggregate.aggregate;
import static utility.Arithmetic.Normalized;
import static utility.Converter.*;
import static utility.Denoise.DenoiseFunc;
import static utility.Util.getEdges;

public class LuckyProfiler_ implements PlugInFilter {
    ImagePlus imp;
    ImageProcessor ip;
    ImageStack ims;
    int CalcType;//0:Roi;1:Auto
    int fitType;
    // Pixel Size
    double PixelSizeImage;
    // image properties
    int imageW, imageH;
    CheckboxGroup CalcTypeCbG, fitTypeCbg;
    TextField PixelSizeImageTF;
    JFrame jf;
    //frameListener1 mFL1;
    LuckyProfiler_ CUDADll;
    JPanel CalcTypeChoosePanel, SystemParPanel, DeconPanel;
    Checkbox deconBox, gaussBox;
    Label label02;
    JButton StartjButton;

    public native void computeAngs(int[] locx, int[] locy, int[] SkelImwithout1, int SkelNum, int imgH, int imgW);

    public native int[] getSkeleton(int[] skeletontemp, int imgH, int imgW);

    public native double[] getAngs();

    public native void computeRCadiAndRelatedLocIndex(int[] SkelImwithout1, int[] SingleResult, int[] rr, int PreProjectionNum, int imH, int imgW);

    public native int[] getRCadi();

    public native int[] getRelatedLocIndex();

    public LuckyProfiler_() {

        PixelSizeImage = 10;
        CalcType = 0;
    }

    public int setup(String arg, ImagePlus imp) {

        System.loadLibrary("opencv_java450");
        System.loadLibrary("core");
        this.imp = imp;
        return DOES_16 + DOES_8G + NO_CHANGES + SUPPORTS_MASKING;
    }

    public void run(ImageProcessor ip) {
        this.ip = ip;
        CUDADll = new LuckyProfiler_();

        imageW = imp.getWidth();// width of raw PALM image
        imageH = imp.getHeight();// height of raw PALM image
        ims = imp.getImageStack();

        // initialize LuckyProfile dialog
        jf = new JFrame("core");
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gbc;
        jf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        jf.setLayout(gridbag);

        // this panel will choose the resolution calculation(auto or roi) type
        CalcTypeChoosePanel = new JPanel();
        CalcTypeChoosePanel.setBorder(BorderFactory.createTitledBorder(""));
        CalcTypeChoosePanel.setLayout(gridbag);
        CalcTypeChoosePanel.setPreferredSize(new Dimension(300, 120));

        CalcTypeCbG = new CheckboxGroup();
        CalcTypeChoosePanel.add(new Checkbox("             ROI             ", CalcTypeCbG, true));
        CalcTypeChoosePanel.add(new Checkbox("            Full               ", CalcTypeCbG, false));

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gridbag.setConstraints(CalcTypeChoosePanel, gbc);
        jf.add(CalcTypeChoosePanel);

        DeconPanel = new JPanel();
        DeconPanel.setBorder(BorderFactory.createTitledBorder("Fitting"));
        DeconPanel.setLayout(gridbag);
        DeconPanel.setPreferredSize(new Dimension(300, 120));

        fitTypeCbg = new CheckboxGroup();
        gaussBox = new Checkbox("     Gaussian     ", fitTypeCbg, true);
        deconBox = new Checkbox("  Deconvolution   ", fitTypeCbg, false);
        gaussBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        deconBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DeconPanel.add(gaussBox);
        DeconPanel.add(deconBox);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gridbag.setConstraints(DeconPanel, gbc);
        jf.add(DeconPanel);

        // this panel will keep the parameter options
        SystemParPanel = new JPanel();
        SystemParPanel.setBorder(BorderFactory.createTitledBorder("Visualization"));
        SystemParPanel.setLayout(gridbag);
        SystemParPanel.setPreferredSize(new Dimension(300, 120));

        // Pixel size of  image
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 3.0;
        label02 = new Label("Pixel size at sample plane(nm)");
        gridbag.setConstraints(label02, gbc);
        SystemParPanel.add(label02);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        PixelSizeImageTF = new TextField(String.valueOf(PixelSizeImage));
        PixelSizeImageTF.setEnabled(true);
        //PixelSizeRawImageTF.addTextListener(PSTL);
        gridbag.setConstraints(PixelSizeImageTF, gbc);
        SystemParPanel.add(PixelSizeImageTF);
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gridbag.setConstraints(SystemParPanel, gbc);
        jf.add(SystemParPanel);

        // start button for starting calculation
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 5;
        StartjButton = new JButton("Start");
        LuckyProfiler_.StartListener SL = new LuckyProfiler_.StartListener();
        StartjButton.addActionListener(SL);
        gridbag.setConstraints(StartjButton, gbc);
        jf.add(StartjButton);

        jf.setLocationRelativeTo(null);
        jf.setSize(325, 210);
        jf.setVisible(true);
    }

    // respond of button "Start"
    class StartListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            PixelSizeImage = Double.parseDouble(PixelSizeImageTF.getText());


            if (PixelSizeImage <= 0) {
                IJ.showMessage("Please enter the right Pixel size of raw image in um!");
                return;
            }

            String type = CalcTypeCbG.getSelectedCheckbox().getLabel();
            String fit = fitTypeCbg.getSelectedCheckbox().getLabel();

            // resolution calculation type
            if (type.contentEquals("             ROI             ")) {
                CalcType = 0;
            } else {
                CalcType = 1;
            }

            if (fit.contentEquals("     Gaussian     ")) {
                fitType = 0;
            } else {
                fitType = 1;
            }

            StartjButton.setEnabled(false);
            new LuckyProfileRunner(imp, ip, CUDADll, PixelSizeImage, CalcType, fitType, LuckyProfiler_.this);
        }
    }
}

class LuckyProfileRunner extends Thread {

    private ImagePlus imp;
    private ImagePlus RoiImp;
    private ImageProcessor ip;
    private ImageProcessor RoiIp;
    private LuckyProfiler_ CUDADll;
    int CalcType;//0:Roi;1:Auto
    int fitType;
    // Pixel Size
    double Px;
    // image properties
    int imageW, imageH, imageRoiW, imageRoiH;
    private String line;
    LuckyProfiler_ luckyProfile_;

    LuckyProfileRunner(ImagePlus imagePlus, ImageProcessor imageProcessor, LuckyProfiler_ CUDADll, double PixelSizeImage, int CalcType, int fitType, LuckyProfiler_ luckyProfile_) {
        this.luckyProfile_ = luckyProfile_;
        this.imp = imagePlus;
        this.ip = imageProcessor;
        this.CUDADll = CUDADll;
        this.CalcType = CalcType;
        this.fitType = fitType;
        RoiIp = ip.crop();

        imageRoiW = RoiIp.getWidth();
        imageRoiH = RoiIp.getHeight();
        imageW = imp.getWidth();// width of raw PALM image
        imageH = imp.getHeight();// height of raw PALM image
        Px = PixelSizeImage;
        start();
    }

    public void run() {

        if (CalcType == 0) {
            doLuckyProfileRunner();
            luckyProfile_.StartjButton.setEnabled(true);
        } else {
            How2FWHM2Auto();
            luckyProfile_.StartjButton.setEnabled(true);
        }
    }

    private void How2FWHM2Auto() {
        ImageConverter imageConverter = new ImageConverter(imp);
        imageConverter.convertToGray16();
        ip = imp.getProcessor();
        Mat origin = array2Mat(ip.getIntArray());
        Mat RenderedImg = origin.t();
        int imgH = RenderedImg.rows();
        int imgW = RenderedImg.cols();

        Mat Offset = Mat.zeros(1, 2 * imgH + 2 * imgW, CvType.CV_16UC1);
        RenderedImg.row(0).copyTo(Offset.colRange(0, imgW));
        RenderedImg.row(imgH - 1).copyTo(Offset.colRange(imgW, 2 * imgW));
        RenderedImg.col(0).t().copyTo(Offset.colRange(2 * imgW, 2 * imgW + imgH));
        RenderedImg.col(imgW - 1).t().copyTo(Offset.colRange(2 * imgW + imgH, 2 * imgW + 2 * imgH));

        Scalar mean = Core.mean(Offset);

        Mat IIMat = new Mat();
        Core.subtract(RenderedImg, mean, IIMat);
        int[][] II = Mat2Int(IIMat);

        int n = (int) Math.ceil(20f / Px);

        Mat se = strel(n);

        Mat I = new Mat(imgH, imgW, CvType.CV_16UC1);
        IIMat.copyTo(I);
        I.convertTo(I, CvType.CV_32FC1);
        Mat Im = DenoiseFunc(I, Px);//Im 32F

        Mat RenderedImg2BS = new Mat();
        Imgproc.dilate(Im, RenderedImg2BS, se);

        Core.normalize(RenderedImg2BS, RenderedImg2BS, 0, 255, Core.NORM_MINMAX);
        RenderedImg2BS.convertTo(RenderedImg2BS, CvType.CV_8UC1);

        int blockSize = 2 * (int) Math.floor(imgH / 16f) + 1;

        Mat Bounderpre = new Mat();//8U 0/1
        Imgproc.adaptiveThreshold(RenderedImg2BS, Bounderpre, 1, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 0);

        Mat Im2selectMat = new Mat();//8u 0/1/2 -> 0/1
        Imgproc.Laplacian(Bounderpre, Im2selectMat, -1, 1, 1, 0, Core.BORDER_CONSTANT);

        Imgproc.threshold(Im2selectMat, Im2selectMat, 0, 1, Imgproc.THRESH_BINARY);

        se = strel(1);
        Imgproc.dilate(Im2selectMat, Im2selectMat, se);
        int[][] Im2select = Mat2Int(Im2selectMat);

        //Im2select = Imgcodecs.imread("Im2select.tif", CvType.CV_8UC1);

        Mat skeletontemp1 = new Mat();

        Core.normalize(Im, skeletontemp1, 0, 255, Core.NORM_MINMAX);
        skeletontemp1.convertTo(skeletontemp1, CvType.CV_8UC1);

        Mat skeletontemp = new Mat();//8U
        Imgproc.adaptiveThreshold(skeletontemp1, skeletontemp, 1, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 0);

        Mat skeletontempT = skeletontemp.t();

        //Imgcodecs.imwrite("skr.tif", skeletontempT);

        int[] skg = luckyProfile_.getSkeleton(Mat2Int1D(skeletontempT), imgH, imgW);
        int[] SkelImwithout1D = Skeleton.algbwmorph(reshape(skg, imgH, imgW, 30), imgH, imgW);
        int[][] SkelImwithout1 = reshape(SkelImwithout1D, imgH, imgW);

//        Mat skr = array2Mat(skg, imgH, 30);//1 -> 255

//        Mat SkelImwithout1Mat = Skeleton.run(skr);//255 -> 1
//        int[][] SkelImwithout1 = Mat2Int(SkelImwithout1Mat);

        List<Integer> locx_list = new ArrayList<>();
        List<Integer> locy_list = new ArrayList<>();

        for (int j = 0; j < imgW; j++) {
            for (int i = 0; i < imgH; i++) {
                if (SkelImwithout1[i][j] != 0) {
                    locx_list.add(i);
                    locy_list.add(j);
                }
            }
        }

        int[] locx = locx_list.stream().mapToInt(t -> t).toArray();
        int[] locy = locy_list.stream().mapToInt(t -> t).toArray();

        int SkelNum = locx_list.size();

        luckyProfile_.computeAngs(locx, locy, SkelImwithout1D, SkelNum, imgH, imgW);

        double[] angs = luckyProfile_.getAngs();

        int[] rr = new int[SkelNum];
        List<List<Integer>> LineProfile = new ArrayList<>(SkelNum);

        for (int i = 0; i < SkelNum; i++) {
            double angle = angs[i];
            int BoundCount = 0;
            List<Integer> LineProfile_r = new ArrayList<>();
            List<Integer> LineProfile_l = new ArrayList<>();
            int XX1 = 0, YY1 = 0, XX2 = 0, YY2 = 0, rep = 0, index1 = 0, index2 = 0;
            for (int r = 1; r <= 1000; r++) {
                int X1 = (int) Math.round(locx[i] - r * Math.sin(angle));
                int Y1 = (int) Math.round(locy[i] + r * Math.cos(angle));
                int X2 = (int) Math.round(locx[i] - r * Math.sin(Math.PI + angle));
                int Y2 = (int) Math.round(locy[i] + r * Math.cos(Math.PI + angle));
                index1 = locx[i];
                index2 = locy[i];

                if (Y1 > imgW - 1 || Y1 < 0 || X1 > imgH - 1 || X1 < 0 || Y2 > imgW - 1 || Y2 < 0 || X2 > imgH - 1 || X2 < 0)
                    break;

                if ((X1 == XX1 && Y1 == YY1) || (X2 == XX2 && Y2 == YY2)) rep = rep + 1;
                else {
                    XX1 = X1;
                    YY1 = Y1;
                    XX2 = X2;
                    YY2 = Y2;
                    BoundCount = Im2select[X1][Y1] + Im2select[X2][Y2] + BoundCount;
                    LineProfile_r.add(II[X1][Y1]);
                    LineProfile_l.add(II[X2][Y2]);
                    if (BoundCount >= 2) {
                        rr[i] = r - rep;
                        break;
                    }
                }
            }
            List<Integer> LineProfilei = new ArrayList<>();
            for (int i1 = LineProfile_l.size() - 1; i1 >= 0; i1--) {
                LineProfilei.add(LineProfile_l.get(i1));
            }
            LineProfilei.add(II[index1][index2]);
            LineProfilei.addAll(LineProfile_r);
            LineProfile.add(LineProfilei);
        }

        List<Integer> rrnozero = new ArrayList<>();
        int rrmin = Integer.MAX_VALUE, rrmax = 0;
        for (int i : rr) {
            if (i != 0) {
                rrnozero.add(i);
                rrmax = Math.max(rrmax, i);
                rrmin = Math.min(rrmin, i);
            }

        }

        double[] a2 = getEdges(rrnozero, rrmin, rrmax);
        double[] a1 = new double[a2.length - 1];

        for (Integer r : rrnozero) {
            for (int i = 0; i < a2.length - 1; i++) {
                if (a2[i] <= r && r < a2[i + 1]) {
                    a1[i]++;
                }
            }
            if ((double) r == a2[a2.length - 1]) a1[a1.length - 1]++;
        }
        double sum1 = Arrays.stream(a1).sum();
        double[] a3 = new double[a2.length - 1];
        for (int i = 0; i < a2.length - 1; i++) {
            a3[i] = (a2[i] + a2[i + 1]) / 2;
        }
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int j = 0; j < a3.length; j++) {
            obs.add(a3[j], a1[j] / sum1);
        }
        double[] fits = GaussianCurveFitter.create().fit(obs.toList());
        double T1 = fits[1] + 3 * fits[2] * Math.sqrt(2);
        double T2 = fits[1] - 3 * fits[2] * Math.sqrt(2);
        for (int i = 0; i < rr.length; i++) {
            if (rr[i] > T1 || rr[i] < T2) {
                rr[i] = 0;
            }
        }

        rrmax += 1;

        int[] SingleResult = new int[SkelNum * 2];
        int[][] LP = new int[SkelNum][2 * rrmax + 1];
        for (int j = 0; j < SkelNum; j++) {
            SingleResult[j * 2] = locx[j];
            SingleResult[j * 2 + 1] = locy[j];
            int R = rr[j];
            if (R > 0) {
                int rradd = rrmax - R;
                List<Integer> line = LineProfile.get(j);
                int len = line.size();
                for (int i = 0; i < len; i++) {
                    LP[j][rradd + i] = line.get(i);
                }
            }
        }

        int randnum = Math.min(SkelNum, 100);

        int PreProjectionNum = PreProNum(randnum, SkelImwithout1, SkelNum, SingleResult, Px, imgH, imgW, LP);

        luckyProfile_.computeRCadiAndRelatedLocIndex(SkelImwithout1D, SingleResult, rr, PreProjectionNum, imgH, imgW);

        int[] RCadi = luckyProfile_.getRCadi();
        int[] RelatedLocIndex = luckyProfile_.getRelatedLocIndex();

        double[] RCadiMean = new double[SkelNum];

        for (int i = 0; i < SkelNum; i++) {
            int[] RCadiTemp = new int[PreProjectionNum];
            System.arraycopy(RCadi, i * PreProjectionNum, RCadiTemp, 0, PreProjectionNum);
            double sum = 0;
            int cnt = 0;
            for (double v : RCadiTemp) {
                if (v != 0) {
                    cnt++;
                    sum += v;
                }
            }
            RCadiMean[i] = cnt == 0 ? 0 : sum / cnt;
        }

        List<double[]> list = new ArrayList<>();
        for (int i = 0; i < RCadiMean.length; i++) {
            list.add(new double[]{RCadiMean[i], i});
        }

        list.sort(Comparator.comparingDouble(o -> o[0]));

        List<Integer> Index = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[0] != 0) {
                Index.add(i);
            }
        }

        int N = (int) (0.04 * SkelNum);

        List<Integer> SkelId = new ArrayList<>();

        int start = Index.get(0);
        int end = Index.get(N - 1);

        for (int i = start; i <= end; i++) {
            SkelId.add((int) list.get(i)[1]);
        }

        if (this.fitType == 0) draw1(LP, N, SkelId, PreProjectionNum, RelatedLocIndex, SingleResult);
        else draw2(LP, N, SkelId, PreProjectionNum, RelatedLocIndex, SingleResult);
    }

    private void draw1(int[][] LP, int N, List<Integer> SkelId, int PreProjectionNum, int[] RelatedLocIndex, int[] SingleResult) {
        int cols = LP[0].length;
        double[] fitresult = new double[3];
        fitresult[2] = Double.MAX_VALUE;
        double[] finalL = new double[cols];
        double[][] profiles = new double[10][cols];
        int maxx = Integer.MIN_VALUE;
        int maxy = Integer.MIN_VALUE;
        int minx = Integer.MAX_VALUE;
        int miny = Integer.MAX_VALUE;

        double[] Lsize = new double[cols];
        for (int j = 0; j < Lsize.length; j++) {
            Lsize[j] = (j + 1) * Px;
        }

        for (int i = 0; i < N; i++) {
            int SI = SkelId.get(i);
            int[] LocIndex = new int[PreProjectionNum];
            System.arraycopy(RelatedLocIndex, SI * PreProjectionNum, LocIndex, 0, PreProjectionNum);
            int[][] lp = new int[PreProjectionNum][cols];
            for (int j = 0; j < PreProjectionNum; j++) {
                System.arraycopy(LP[LocIndex[j]], 0, lp[j], 0, cols);
            }

            double maxL = Integer.MIN_VALUE;
            for (int i1 = 0; i1 < PreProjectionNum; i1++) {
                for (int i2 = 0; i2 < cols; i2++) {
                    maxL = Math.max(lp[i1][i2], maxL);
                }
            }

            double[] L = new double[cols];
            for (int j = 0; j < cols; j++) {
                for (int l = 0; l < PreProjectionNum; l++) {
                    L[j] += lp[l][j];
                }
            }

            L = Normalized(L);
            WeightedObservedPoints obs1 = new WeightedObservedPoints();
            for (int j = 0; j < Lsize.length; j++) {
                obs1.add(Lsize[j], L[j]);
            }
            double[] fits1 = GaussianCurveFitter.create().fit(obs1.toList());
            if (fits1[2] < fitresult[2]) {
                fitresult = fits1;
                maxx = Integer.MIN_VALUE;
                maxy = Integer.MIN_VALUE;
                minx = Integer.MAX_VALUE;
                miny = Integer.MAX_VALUE;
                for (int i1 = 0; i1 < PreProjectionNum; i1++) {
                    int index = LocIndex[i1];
                    maxx = Math.max(maxx, SingleResult[index * 2]);
                    minx = Math.min(minx, SingleResult[index * 2]);
                    maxy = Math.max(maxy, SingleResult[index * 2 + 1]);
                    miny = Math.min(miny, SingleResult[index * 2 + 1]);
                    finalL = L;
                    for (int i2 = 0; i2 < 10; i2++) {
                        for (int i3 = 0; i3 < profiles[0].length; i3++) {
                            profiles[i2][i3] = lp[i2][i3] / maxL;
                        }
                    }
                }
            }
        }

        Gaussian gaussian = new Gaussian(fitresult[0], fitresult[1], fitresult[2]);//make x more dense

        List<Double> xdata_list = new ArrayList<>();
        List<Double> ydata_list = new ArrayList<>();
        for (double x = Lsize[0]; x <= Lsize[Lsize.length - 1]; x += 0.1) {
            xdata_list.add(x);
            ydata_list.add(gaussian.value(x));
        }

        double[] Xdata = xdata_list.stream().mapToDouble(t -> t).toArray();
        double[] Ydata = ydata_list.stream().mapToDouble(t -> t).toArray();

        Plot plot = new Plot("FWHM", "Distance (nm)", "Normalized Intensity");
        plot.setLineWidth(4);
        plot.setColor(Color.red);
        plot.add("line", Xdata, Ydata);
        plot.setLimits(0, Lsize[Lsize.length - 1], 0, 1.2);
        plot.setAxisLabelFont(Font.BOLD, 20);

        String[] iColor = {"balck", "blue", "cyan", "darkGray", "gray", "lightGray", "green", "magenta", "orange", "pink", "red", "yellow"};
        plot.setLineWidth(0.6f);
        int j = 0;
        int k = 0;
        while (j < 10) {
            if (k >= 12) {
                k = 0;
            }
            plot.setColor(iColor[k]);
            plot.add("line", Lsize, profiles[j]);
            j++;
            k++;
        }
        plot.setLineWidth(4);
        plot.setColor(Color.black);
        plot.add("line", Lsize, finalL);

        plot.setFontSize(20);
        plot.setColor(Color.red);
        plot.addLabel(0.65, 0.5, String.format("FWHM = %.3f(nm)", fitresult[2] * 2.355));

        ip.setLineWidth(3);
        ip.setColor(Color.yellow);
        ip.drawRect(miny, minx, maxy - miny, maxx - minx);
        imp.setProcessor(ip);

        plot.show();
    }

    private void draw2(int[][] LP, int N, List<Integer> SkelId, int PreProjectionNum, int[] RelatedLocIndex, int[] SingleResult) {
        MyCurveFitter.Result result = new MyCurveFitter.Result(Double.MAX_VALUE, null, null);
        int imgW = LP[0].length;
        double[] finalL = new double[imgW];
        double[][] profiles = new double[10][imgW];
        int maxx = Integer.MIN_VALUE;
        int maxy = Integer.MIN_VALUE;
        int minx = Integer.MAX_VALUE;
        int miny = Integer.MAX_VALUE;

        double[] Lsize = new double[imgW];
        for (int j = 0; j < Lsize.length; j++) {
            Lsize[j] = (j + 1) * Px;
        }

        for (int i = 0; i < N; i++) {
            int SI = SkelId.get(i);
            int[] LocIndex = new int[PreProjectionNum];
            System.arraycopy(RelatedLocIndex, SI * PreProjectionNum, LocIndex, 0, PreProjectionNum);
            int[][] lp = new int[PreProjectionNum][imgW];
            for (int j = 0; j < PreProjectionNum; j++) {
                System.arraycopy(LP[LocIndex[j]], 0, lp[j], 0, imgW);
            }

            double maxL = Integer.MIN_VALUE;
            for (int i1 = 0; i1 < PreProjectionNum; i1++) {
                for (int i2 = 0; i2 < imgW; i2++) {
                    maxL = Math.max(lp[i1][i2], maxL);
                }
            }

            double[] L = new double[imgW];
            for (int j = 0; j < imgW; j++) {
                for (int l = 0; l < PreProjectionNum; l++) {
                    L[j] += lp[l][j];
                }
            }

            L = Normalized(L);
            MyCurveFitter.Result fit = MyCurveFitter.widthSolvedFWHM1D(L, Px);
            if (fit.WithWidthFit_s < result.WithWidthFit_s) {
                result = fit;
                maxx = Integer.MIN_VALUE;
                maxy = Integer.MIN_VALUE;
                minx = Integer.MAX_VALUE;
                miny = Integer.MAX_VALUE;
                for (int i1 = 0; i1 < PreProjectionNum; i1++) {
                    int index = LocIndex[i1];
                    maxx = Math.max(maxx, SingleResult[index * 2]);
                    minx = Math.min(minx, SingleResult[index * 2]);
                    maxy = Math.max(maxy, SingleResult[index * 2 + 1]);
                    miny = Math.min(miny, SingleResult[index * 2 + 1]);
                    finalL = L;
                    for (int i2 = 0; i2 < 10; i2++) {
                        for (int i3 = 0; i3 < profiles[0].length; i3++) {
                            profiles[i2][i3] = lp[i2][i3] / maxL;
                        }
                    }
                }
            }
        }

        Plot plot = new Plot("FWHM", "Distance (nm)", "Normalized Intensity");
        plot.setLineWidth(4);
        plot.setColor(Color.red);
        plot.add("line", result.X, result.Y);
        plot.setLimits(0, result.X[result.X.length - 1], 0, 1.2);
        plot.setAxisLabelFont(Font.BOLD, 20);

        String[] iColor = {"balck", "blue", "cyan", "darkGray", "gray", "lightGray", "green", "magenta", "orange", "pink", "red", "yellow"};
        plot.setLineWidth(0.6f);
        int j = 0;
        int k = 0;
        while (j < 10) {
            if (k >= 12) {
                k = 0;
            }
            plot.setColor(iColor[k]);
            plot.add("line", Lsize, profiles[j]);
            j++;
            k++;
        }
        plot.setLineWidth(4);
        plot.setColor(Color.black);
        plot.add("line", Lsize, finalL);

        plot.setFontSize(20);
        plot.setColor(Color.red);
        plot.addLabel(0.65, 0.5, String.format("FWHM = %.3f(nm)", result.WithWidthFit_s));

        ip.setLineWidth(3);
        ip.setColor(Color.yellow);
        ip.drawRect(miny, minx, maxy - miny, maxx - minx);
        imp.setProcessor(ip);

        plot.show();
    }

    public void doLuckyProfileRunner() {
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray16();
        ip = imp.getProcessor();
        Mat origin = array2Mat(ip.getIntArray());
        Mat RenderedImg = origin.t();
        int imgH = RenderedImg.rows();
        int imgW = RenderedImg.cols();

        Mat Offset = Mat.zeros(1, 2 * imgH + 2 * imgW, CvType.CV_16UC1);
        RenderedImg.row(0).copyTo(Offset.colRange(0, imgW));
        RenderedImg.row(imgH - 1).copyTo(Offset.colRange(imgW, 2 * imgW));
        RenderedImg.col(0).t().copyTo(Offset.colRange(2 * imgW, 2 * imgW + imgH));
        RenderedImg.col(imgW - 1).t().copyTo(Offset.colRange(2 * imgW + imgH, 2 * imgW + 2 * imgH));

        Scalar mean = Core.mean(Offset);

        RoiManager roiManager = RoiManager.getRoiManager();
        Roi[] roi = roiManager.getRoisAsArray();
        ImagePlus[] RoiImp = imp.crop(roi);
        int RoiNum = roiManager.getCount();
        for (int ii = 0; ii < RoiNum; ii++) {
            RoiIp = RoiImp[ii].getProcessor();
            Mat ROI = array2Mat(RoiIp.getIntArray());
            Mat II = new Mat();
            Core.subtract(ROI, mean, II);

            int n = (int) Math.ceil(20f / Px);

            Mat se = strel(n);

            Mat I = new Mat(II.rows(), II.cols(), CvType.CV_16UC1);
            II.copyTo(I);
            I.convertTo(I, CvType.CV_32FC1);
            Mat Im = DenoiseFunc(I, Px);//Im 32F

            Mat RenderedImg2BS = new Mat();
            Imgproc.dilate(Im, RenderedImg2BS, se);

            Core.normalize(RenderedImg2BS, RenderedImg2BS, 0, 255, Core.NORM_MINMAX);
            RenderedImg2BS.convertTo(RenderedImg2BS, CvType.CV_8UC1);

            int blockSize = 2 * (int) Math.floor(RenderedImg2BS.rows() / 16f) + 1;

            Mat Bounderpre = new Mat();//8U 0/1
            Imgproc.adaptiveThreshold(RenderedImg2BS, Bounderpre, 1, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 0);

            Mat Im2select = new Mat();//8u 0/1/2 -> 0/1
            Imgproc.Laplacian(Bounderpre, Im2select, -1, 1, 1, 0, Core.BORDER_CONSTANT);

            Imgproc.threshold(Im2select, Im2select, 0, 1, Imgproc.THRESH_BINARY);

            se = strel(1);
            Imgproc.dilate(Im2select, Im2select, se);

            //Im2select = Imgcodecs.imread("Im2select.tif", CvType.CV_8UC1);

            Mat skeletontemp1 = new Mat();

            Core.normalize(Im, skeletontemp1, 0, 255, Core.NORM_MINMAX);
            skeletontemp1.convertTo(skeletontemp1, CvType.CV_8UC1);

            Mat skeletontemp = new Mat();//8U
            Imgproc.adaptiveThreshold(skeletontemp1, skeletontemp, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 0);

            Mat SkelImwithout1 = Skeleton.run(skeletontemp);//255 -> 1

            List<Integer> locx_list = new ArrayList<>();
            List<Integer> locy_list = new ArrayList<>();

            for (int j = 2; j < SkelImwithout1.cols() - 2; j++) {
                for (int i = 2; i < SkelImwithout1.rows() - 2; i++) {
                    if (SkelImwithout1.get(i, j)[0] != 0) {
                        locx_list.add(i);
                        locy_list.add(j);
                    }
                }
            }

            Integer[] locx = locx_list.toArray(new Integer[0]);
            Integer[] locy = locy_list.toArray(new Integer[0]);

            int SkelNum = locx_list.size();

            Mat rr = Mat.zeros(1, SkelNum, CvType.CV_8UC1);
            List<List<Integer>> LineProfile = new ArrayList<>(SkelNum);

            for (int i = 0; i < SkelNum; i++) {
                double angle = adjangle1(locx, locy, i, Mat2Int(SkelImwithout1));//SkelImwithout1 8U
                int BoundCount = 0;
                List<Integer> LineProfile_r = new ArrayList<>();
                List<Integer> LineProfile_l = new ArrayList<>();
                int XX1 = 0, YY1 = 0, XX2 = 0, YY2 = 0, rep = 0, index1 = 0, index2 = 0;
                for (int r = 1; r <= 1000; r++) {
                    int X1 = (int) Math.round(locx[i] - r * Math.sin(angle));
                    int Y1 = (int) Math.round(locy[i] + r * Math.cos(angle));
                    int X2 = (int) Math.round(locx[i] - r * Math.sin(Math.PI + angle));
                    int Y2 = (int) Math.round(locy[i] + r * Math.cos(Math.PI + angle));
                    index1 = locx[i];
                    index2 = locy[i];

                    if (Y1 > Im2select.cols() - 1 || Y1 < 0 || X1 > Im2select.rows() - 1 || X1 < 0 || Y2 > Im2select.cols() - 1 || Y2 < 0 || X2 > Im2select.rows() - 1 || X2 < 0)
                        break;

                    if ((X1 == XX1 && Y1 == YY1) || (X2 == XX2 && Y2 == YY2)) rep = rep + 1;
                    else {
                        XX1 = X1;
                        YY1 = Y1;
                        XX2 = X2;
                        YY2 = Y2;
                        int[] point1 = new int[]{X1, Y1};
                        int[] point2 = new int[]{X2, Y2};
                        BoundCount = (int) (Im2select.get(point1)[0] + Im2select.get(point2)[0] + BoundCount);
                        LineProfile_r.add((int) II.get(point1)[0]);
                        LineProfile_l.add((int) II.get(point2)[0]);
                        if (BoundCount >= 2) {
                            rr.put(0, i, r - rep);
                            break;
                        }
                    }
                }
                List<Integer> LineProfilei = new ArrayList<>();
                for (int i1 = LineProfile_l.size() - 1; i1 >= 0; i1--) {
                    LineProfilei.add(LineProfile_l.get(i1));
                }
                LineProfilei.add((int) II.get(index1, index2)[0]);
                LineProfilei.addAll(LineProfile_r);
                LineProfile.add(LineProfilei);
            }

            List<Integer> rrnozero = new ArrayList<>();
            int rrmin = Integer.MAX_VALUE, rrmax = 0;
            for (int j = 0; j < rr.cols(); j++) {
                int i = (int) rr.get(0, j)[0];
                if (i != 0) {
                    rrnozero.add(i);
                    rrmax = Math.max(rrmax, i);
                    rrmin = Math.min(rrmin, i);
                }
            }

            double[] a2 = getEdges(rrnozero, rrmin, rrmax);
            double[] a1 = new double[a2.length - 1];

            for (Integer r : rrnozero) {
                for (int i = 0; i < a2.length - 1; i++) {
                    if (a2[i] <= r && r < a2[i + 1]) {
                        a1[i]++;
                    }
                }
                if ((double) r == a2[a2.length - 1]) a1[a1.length - 1]++;
            }
            double sum1 = Arrays.stream(a1).sum();
            double[] a3 = new double[a2.length - 1];
            for (int i = 0; i < a2.length - 1; i++) {
                a3[i] = (a2[i] + a2[i + 1]) / 2;
            }
            WeightedObservedPoints obs = new WeightedObservedPoints();
            for (int j = 0; j < a3.length; j++) {
                obs.add(a3[j], a1[j] / sum1);
            }
            double[] fits = GaussianCurveFitter.create().fit(obs.toList());
            double T1 = fits[1] + 3 * fits[2] * Math.sqrt(2);
            double T2 = fits[1] - 3 * fits[2] * Math.sqrt(2);
            for (int i = 0; i < rr.cols(); i++) {
                int r = (int) rr.get(0, i)[0];
                if (r > T1 || r < T2) {
                    rr.put(0, i, 0);
                }
            }

            rrmax += 1;
            Mat LP = Mat.zeros(SkelNum, 2 * rrmax + 1, CvType.CV_16UC1);
            for (int j = 0; j < SkelNum; j++) {
                int R = (int) rr.get(0, j)[0];
                if (R > 0) {
                    int rradd = rrmax - R;
                    Mat row = LP.row(j);
                    List<Integer> line = LineProfile.get(j);
                    int len = line.size();
                    for (int i = 0; i < len; i++) {
                        row.put(0, rradd + i, line.get(i));
                    }
                }
            }

            double[] L = new double[LP.cols()];
            for (int j = 0; j < LP.cols(); j++) {
                for (int l = 0; l < LP.rows(); l++) {
                    L[j] += LP.get(l, j)[0];
                }
            }

            L = Normalized(L);
            double[] Lsize = new double[L.length];
            for (int j = 0; j < Lsize.length; j++) {
                Lsize[j] = (j + 1) * Px;
            }

            Plot plot = new Plot("FWHM", "Distance (nm)", "Normalized Intensity");
            plot.setLineWidth(4);
            plot.setColor(Color.red);

            if (this.fitType == 0) {//Gaussian fitting
                WeightedObservedPoints obs2 = new WeightedObservedPoints();
                for (int j = 0; j < Lsize.length; j++) {
                    obs2.add(Lsize[j], L[j]);
                }
                double[] fitresult = GaussianCurveFitter.create().fit(obs2.toList());

                Gaussian gaussian = new Gaussian(fitresult[0], fitresult[1], fitresult[2]);//make x more dense

                List<Double> xdata = new ArrayList<>();
                List<Double> ydata = new ArrayList<>();
                for (double x = Lsize[0]; x <= Lsize[Lsize.length - 1]; x += 0.1) {
                    xdata.add(x);
                    ydata.add(gaussian.value(x));
                }

                double[] Xdata = new double[xdata.size()];
                double[] Ydata = new double[ydata.size()];

                for (int i1 = 0; i1 < xdata.size(); i1++) {
                    Xdata[i1] = xdata.get(i1);
                    Ydata[i1] = ydata.get(i1);
                }

                plot.add("line", Xdata, Ydata);
                plot.setFontSize(20);
                plot.setColor(Color.red);
                plot.addLabel(0.65, 0.5, String.format("FWHM = %.3f(nm)", fitresult[2] * 2.355));
            } else {
                MyCurveFitter.Result fit = MyCurveFitter.widthSolvedFWHM1D(L, Px);
                plot.add("line", fit.X, fit.Y);
                plot.setFontSize(20);
                plot.setColor(Color.red);
                plot.addLabel(0.65, 0.5, String.format("FWHM = %.3f(nm)", fit.WithWidthFit_s));
            }

            plot.setLimits(0, Lsize[Lsize.length - 1], 0, 1.2);
            plot.setAxisLabelFont(Font.BOLD, 20);

            plot.setLineWidth(4);
            plot.setColor(Color.black);
            plot.add("line", Lsize, L);
            plot.show();
        }
    }

    public static void main(String[] args) throws IOException {
        Image image = ImageIO.read(new FileInputStream("ROI-5.tif"));
        //Image image = ImageIO.read(new FileInputStream("Loc_result2D9-16bit.tif"));
        ImagePlus imagePlus = new ImagePlus("", image);
        LuckyProfiler_ luckyProfile_ = new LuckyProfiler_();
        luckyProfile_.setup("", imagePlus);
        luckyProfile_.run(luckyProfile_.imp.getProcessor());
    }

    public int PreProNum(int randnum, int[][] SkellImWithout1, int SkelNum, int[] SingelResult, double px, int imgH, int imgW, int[][] aTry) {
        Random rand = new Random();
        int[] PreIndex1 = new int[randnum];
        for (int i = 0; i < randnum; i++) {
            PreIndex1[i] = Math.round((SkelNum - 1) * rand.nextFloat());
        }

        List<Integer> Cannot = new ArrayList<>();
        for (int i = 0; i < aTry.length; i++) {
            int sum = Arrays.stream(aTry[i]).sum();
            if (sum == 0) {
                Cannot.add(i);
            }
        }

        int lineWidth = aTry[0].length;

        List<Integer> PreIndex = new ArrayList<>();
        for (int i : PreIndex1) {
            if (!Cannot.contains(i)) {
                PreIndex.add(i);
            }
        }

        randnum = PreIndex.size();
        int[] intenum = new int[randnum];

        for (int i = 0; i < randnum; i++) {
            if (i % 10 == 0) System.out.println(i);
            int Index = PreIndex.get(i);
            int XLoc = SingelResult[Index * 2];
            int YLoc = SingelResult[Index * 2 + 1];
            List<int[]> Loc = new ArrayList<>();
            Loc.add(new int[]{XLoc, YLoc});
            double prefitg;
            double[] line = Arrays.stream(aTry[Index]).mapToDouble(Double::valueOf).toArray();
            double[] L = Normalized(line);
            double[] LSize = new double[L.length];
            for (int j = 0; j < LSize.length; j++) {
                LSize[j] = (j + 1) * px;
            }

            WeightedObservedPoints obs = new WeightedObservedPoints();
            for (int j = 0; j < LSize.length; j++) {
                obs.add(LSize[j], L[j]);
            }

            double[] fitResult = GaussianCurveFitter.create().fit(obs.toList());
            prefitg = fitResult[2] * Math.sqrt(2) * 1.665;
            if (XLoc - 1 >= 0 && YLoc - 1 >= 0 && XLoc + 1 < imgH && YLoc + 1 < imgW) {
                for (int k = 0; k < 1000; k++) {
                    Loc = aggregate(Loc, SkellImWithout1, imgH, imgW);
                    int[][] LP = new int[Loc.size()][lineWidth];
                    for (int j = 0; j < Loc.size(); j++) {
                        int Ind = 0;
                        for (int i1 = 0; i1 < SkelNum; i1++) {
                            if (SingelResult[i1 * 2] == Loc.get(j)[0] && SingelResult[i1 * 2 + 1] == Loc.get(j)[1]) {
                                Ind = i1;
                                break;
                            }
                        }
                        System.arraycopy(aTry[Ind], 0, LP[j], 0, lineWidth);
                    }

                    L = new double[lineWidth];
                    for (int j = 0; j < lineWidth; j++) {
                        for (int l = 0; l < Loc.size(); l++) {
                            L[j] += LP[l][j];
                        }
                    }

                    L = Normalized(L);
                    WeightedObservedPoints obs2 = new WeightedObservedPoints();
                    for (int j = 0; j < LSize.length; j++) {
                        obs2.add(LSize[j], L[j]);
                    }

                    double[] fitresult1 = GaussianCurveFitter.create().fit(obs2.toList());
                    double sig = fitresult1[2] * Math.sqrt(2) * 1.665;

                    Gaussian gaussian = new Gaussian(fitresult1[0], fitresult1[1], fitresult1[2]);
                    double SSE = 0;
                    for (int i1 = 0; i1 < LSize.length; i1++) {
                        SSE += Math.pow(gaussian.value(LSize[i1]) - L[i1], 2);
                    }
                    double SST = 0;
                    double sum = Arrays.stream(L).sum();
                    double mean = sum / L.length;
                    for (double v : L) {
                        SST += Math.pow(mean - v, 2);
                    }

                    double fg = 1 - SSE / SST;

                    if (fg > 0.8 && Math.abs(sig - prefitg) < 0.01) {
                        intenum[i] = Loc.size();
                        break;
                    } else {
                        prefitg = sig;
                    }
                }
            }
        }

//        MatOfFloat Intenum = new MatOfFloat(intenum);
//        List<Mat> matList = new ArrayList<>();
//        matList.add(Intenum);
//        Mat hist = new Mat();
//        Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(Intenum);
//        MatOfFloat ranges = new MatOfFloat((float) minMaxLocResult.minVal, (float) (minMaxLocResult.maxVal + 0.001));
//        MatOfInt histSize = new MatOfInt(10);
//        Imgproc.calcHist(matList, new MatOfInt(0), new Mat(), hist, histSize, ranges);
//
//        float[] centers = new float[10];
//        float stride = (float) ((minMaxLocResult.maxVal - minMaxLocResult.minVal) / 10);
//        centers[0] = (float) (minMaxLocResult.minVal + stride / 2);
//        for (int i = 1; i < centers.length; i++) {
//            centers[i] = centers[i - 1] + stride;
//        }
//
//        List<int[]> list = new ArrayList<>();
//        for (int i = 0; i < hist.rows(); i++) {
//            list.add(new int[]{(int) hist.get(i, 0)[0], i});
//        }
//
//        list.sort((o1, o2) -> Integer.compare(o2[0], o1[0]));
//
//        List<Integer> TempId = new ArrayList<>();
//        for (int i = 0; i < list.size(); i++) {
//            if (list.get(i)[1] == list.get(1)[1]) {
//                TempId.add(i);
//            }
//        }
//
//        List<Integer> tmp = new ArrayList<>();
//        tmp.add(list.get(0)[1]);
//        tmp.addAll(TempId);
//        TempId = tmp;
//
//        float sum = 0;
//        for (Integer id : TempId) {
//            sum += centers[id];
//        }

        double sum = 0;
        for (int i : intenum) {
            sum += i;
        }

        return (int) Math.ceil(sum / randnum);
    }

    public Mat strel(int radius) {
        int borderWidth;
        Mat sel;
        int m, n;
        switch (radius) {
            case 1:
            case 2:
                if (radius == 1)
                    borderWidth = 1;
                else
                    borderWidth = 2;
                sel = new Mat((2 * radius + 1), (2 * radius + 1), CvType.CV_8UC1, new Scalar(1));
                break;//radius = 1 : 3 * 3  radius = 2 : 5 * 5
            case 3:
                borderWidth = 0;
                sel = new Mat((2 * radius - 1), (2 * radius - 1), CvType.CV_8UC1, new Scalar(1));
                break;
            default:
                n = radius / 7;
                m = radius % 7;
                if (m == 0 || m >= 4)
                    borderWidth = 2 * (2 * n + 1);
                else
                    borderWidth = 2 * 2 * n;
                sel = new Mat((2 * radius - 1), (2 * radius - 1), CvType.CV_8UC1, new Scalar(1));
                break;
        }
        for (int i = 0; i < borderWidth; i++) {
            for (int j = 0; j < borderWidth - i; j++) {
                sel.row(i).col(j).setTo(new Scalar(0));
                sel.row(i).col(sel.cols() - 1 - j).setTo(new Scalar(0));
                sel.row(sel.rows() - 1 - i).col(j).setTo(new Scalar(0));
                sel.row(sel.rows() - 1 - i).col(sel.cols() - 1 - j).setTo(new Scalar(0));
            }
        }

        return sel;
    }
}
