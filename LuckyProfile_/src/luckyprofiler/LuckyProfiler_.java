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
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class LuckyProfiler_ implements PlugInFilter {
    ImagePlus imp;
    ImageProcessor ip;
    ImageStack ims;
    int CalcType;//0:Roi;1:Auto 
    // Pixel Size
    double PixelSizeImage;
    // image properties
    int imageW, imageH;
    CheckboxGroup CalcTypeCbG;
    TextField PixelSizeImageTF;
    JFrame jf;
    //frameListener1 mFL1;
    LuckyProfiler_ CUDADll;
    JPanel CalcTypeChoosePanel;
    JPanel SystemParPanel;
    Label label02;
    JButton StartjButton;

    public native void computeAngs(int[] locx, int[] locy, short[] SkelImwithout1, int SkelNum, int imgH, int imgW);

    public native int[] getSkeleton(short[] skeletontemp, int imgH, int imgW);

    public native double[] getAngs();

    public native void computeRCadiAndRelatedLocIndex(short[] SkelImwithout1, int[] SingleResult, int[] rr, int PreProjectionNum, int imH, int imgW);

    public native short[] getRCadi();

    public native int[] getRelatedLocIndex();

    public LuckyProfiler_() {

        PixelSizeImage = 10;
        CalcType = 0;
    }

    public int setup(String arg, ImagePlus imp) {

        System.loadLibrary("opencv_java450");
        System.loadLibrary("LuckyProfiler");
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
        jf = new JFrame("LuckyProfiler");
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        jf.setFont(new Font("SansSerif", Font.PLAIN, 14));
        jf.setLayout(gridbag);

        // this panel will choose the resolution calculation(auto or roi) type
        CalcTypeChoosePanel = new JPanel();
        CalcTypeChoosePanel.setBorder(BorderFactory.createTitledBorder(""));
        CalcTypeChoosePanel.setLayout(gridbag);
        CalcTypeChoosePanel.setPreferredSize(new Dimension(300, 120));

        CalcTypeCbG = new CheckboxGroup();
        CalcTypeChoosePanel.add(new Checkbox("         ROI           ", CalcTypeCbG, true));
        CalcTypeChoosePanel.add(new Checkbox("         Full           ", CalcTypeCbG, false));


        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gridbag.setConstraints(CalcTypeChoosePanel, gbc);
        jf.add(CalcTypeChoosePanel);

        // this panel will keep the parameter options
        SystemParPanel = new JPanel();
        SystemParPanel.setBorder(BorderFactory.createTitledBorder("Parameter"));
        SystemParPanel.setLayout(gridbag);
        SystemParPanel.setPreferredSize(new Dimension(300, 120));

        // Pixel size of  image
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 3.0;
        label02 = new Label("Pixel size of image (nm)");
        gridbag.setConstraints(label02, gbc);
        SystemParPanel.add(label02);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = 1;
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
        gbc.gridy = 1;
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
        jf.setSize(300, 200);
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


            // resolution calculation type
            if (CalcTypeCbG.getSelectedCheckbox().getLabel().contentEquals("         ROI           ")) {
                CalcType = 0;
            } else {
                CalcType = 1;
            }

            StartjButton.setEnabled(false);
            new LuckyProfileRunner(imp, ip, CUDADll, PixelSizeImage, CalcType, LuckyProfiler_.this);
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
    // Pixel Size
    double Px;
    // image properties
    int imageW, imageH, imageRoiW, imageRoiH;
    private String line;
    LuckyProfiler_ luckyProfile_;

    LuckyProfileRunner(ImagePlus imagePlus, ImageProcessor imageProcessor, LuckyProfiler_ CUDADll, double PixelSizeImage, int CalcType, LuckyProfiler_ luckyProfile_) {
        this.luckyProfile_ = luckyProfile_;
        this.imp = imagePlus;
        this.ip = imageProcessor;
        this.CUDADll = CUDADll;
        this.CalcType = CalcType;
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
        imageConverter.convertToGray16();//
        ip = imp.getProcessor();
        int[][] imageData = ip.getIntArray();
        Mat origin = array2Mat(imageData);
        Mat RenderedImg = origin.t();
        int imgH = RenderedImg.rows();
        int imgW = RenderedImg.cols();

        Mat Offset = Mat.zeros(1, 2 * imgH + 2 * imgW, CvType.CV_16UC1);
        RenderedImg.row(0).copyTo(Offset.colRange(0, imgW));
        RenderedImg.row(imgH - 1).copyTo(Offset.colRange(imgW, 2 * imgW));
        RenderedImg.col(0).t().copyTo(Offset.colRange(2 * imgW, 2 * imgW + imgH));
        RenderedImg.col(imgW - 1).t().copyTo(Offset.colRange(2 * imgW + imgH, 2 * imgW + 2 * imgH));

        Scalar mean = Core.mean(Offset);

        Mat II = new Mat();
        Core.subtract(RenderedImg, mean, II);

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
        Imgproc.adaptiveThreshold(skeletontemp1, skeletontemp, 1, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, blockSize, 0);

        Mat skeletontempT = skeletontemp.t();

        int[] skg = luckyProfile_.getSkeleton(CV16U2Short(skeletontempT), imgH, imgW);

//        int[] skr = reshape(skg, imgH, imgW);
//        int[] SkelImwithout1_arr = Skeleton.algbwmorph(skr, imgH, imgW);
//        Mat SkelImwithout1 = array2Mat(SkelImwithout1_arr, imgH);

        Mat skr = array2Mat(skg, imgH, 30);//1 -> 255

        //Imgcodecs.imwrite("skr.tif", skr);

        Mat SkelImwithout1 = Skeleton.run(skr);//255 -> 1

        //Mat SkelImwithout1 = Imgcodecs.imread("SkelImwithout1.tif", CvType.CV_8UC1);//

//        Imgproc.threshold(SkelImwithout1, SkelImwithout1, 0, 255, Imgproc.THRESH_BINARY);//To observe the skeleton
//        Imgcodecs.imwrite("SkelImwithout1.tif", SkelImwithout1);

        List<Integer> locxTemp = new ArrayList<>();
        List<Integer> locyTemp = new ArrayList<>();

        for (int i = 0; i < SkelImwithout1.cols(); i++) {
            for (int i1 = 0; i1 < SkelImwithout1.rows(); i1++) {
                if (SkelImwithout1.get(i1, i)[0] != 0) {
                    locxTemp.add(i1);
                    locyTemp.add(i);
                }
            }
        }

        int[] locx = new int[locxTemp.size()];
        int[] locy = new int[locxTemp.size()];

        for (int i = 0; i < locxTemp.size(); i++) {
            locx[i] = locxTemp.get(i);
            locy[i] = locyTemp.get(i);
        }

        int SkelNum = locxTemp.size();

        short[] SkelShort = CV16U2Short(SkelImwithout1);

        luckyProfile_.computeAngs(locx, locy, SkelShort, SkelNum, imgH, imgW);

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
                double X1 = locx[i] - r * Math.sin(angle);
                double Y1 = locy[i] + r * Math.cos(angle);
                X1 = Math.round(X1);
                Y1 = Math.round(Y1);
                double X2 = locx[i] - r * Math.sin(Math.PI + angle);
                double Y2 = locy[i] + r * Math.cos(Math.PI + angle);
                X2 = Math.round(X2);
                Y2 = Math.round(Y2);
                index1 = locx[i];
                index2 = locy[i];

                if (Y1 > Im2select.cols() - 1 || Y1 < 0 || X1 > Im2select.rows() - 1 || X1 < 0 || Y2 > Im2select.cols() - 1 || Y2 < 0 || X2 > Im2select.rows() - 1 || X2 < 0)
                    break;

                if ((X1 == XX1 && Y1 == YY1) || (X2 == XX2 && Y2 == YY2)) rep = rep + 1;
                else {
                    XX1 = (int) X1;
                    YY1 = (int) Y1;
                    XX2 = (int) X2;
                    YY2 = (int) Y2;
                    int[] point1 = new int[]{(int) X1, (int) Y1};
                    int[] point2 = new int[]{(int) X2, (int) Y2};
                    BoundCount = (int) (Im2select.get(point1)[0] + Im2select.get(point2)[0] + BoundCount);
                    LineProfile_r.add((int) II.get(point1)[0]);
                    LineProfile_l.add((int) II.get(point2)[0]);
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
            LineProfilei.add((int) II.get(index1, index2)[0]);
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

        Double[] a2 = getEdges(rrnozero, rrmin, rrmax);
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
        Mat LP = Mat.zeros(SkelNum, 2 * rrmax + 1, CvType.CV_16UC1);
        for (int j = 0; j < SkelNum; j++) {
            SingleResult[j * 2] = locx[j];
            SingleResult[j * 2 + 1] = locy[j];
            int R = rr[j];
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

        int randnum = Math.min(SkelNum, 100);

        int PreProjectionNum = PreProNum(randnum, SkelImwithout1, SkelNum, SingleResult, Px, imgH, imgW, LP);

        luckyProfile_.computeRCadiAndRelatedLocIndex(SkelShort, SingleResult, rr, PreProjectionNum, imgH, imgW);

        short[] RCadi = luckyProfile_.getRCadi();
        int[] RelatedLocIndex = luckyProfile_.getRelatedLocIndex();

        double[] RCadiMean = new double[SkelNum];

        for (int i = 0; i < SkelNum; i++) {
            short[] RCadiTemp = new short[PreProjectionNum];
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

        double[] fitresult = new double[3];
        fitresult[2] = Double.MAX_VALUE;
        double[][] finalL = new double[1][LP.cols()];
        double[][] profiles = new double[10][LP.cols()];
        double maxL;
        int maxx = Integer.MIN_VALUE;
        int maxy = Integer.MIN_VALUE;
        int minx = Integer.MAX_VALUE;
        int miny = Integer.MAX_VALUE;

        double[] Lsize = new double[LP.cols()];
        for (int j = 0; j < Lsize.length; j++) {
            Lsize[j] = (j + 1) * Px;
        }

        for (int i = 0; i < N; i++) {
            int SI = SkelId.get(i);
            int[] LocIndex = new int[PreProjectionNum];
            System.arraycopy(RelatedLocIndex, SI * PreProjectionNum, LocIndex, 0, PreProjectionNum);
            Mat lp = new Mat(PreProjectionNum, LP.cols(), CvType.CV_16UC1);
            for (int j = 0; j < PreProjectionNum; j++) {
                LP.row(LocIndex[j]).copyTo(lp.row(j));
            }

            double[][] L = new double[1][lp.cols()];
            for (int j = 0; j < lp.cols(); j++) {
                for (int l = 0; l < lp.rows(); l++) {
                    L[0][j] += lp.get(l, j)[0];
                }
            }

            L = Normalized(L);
            WeightedObservedPoints obs1 = new WeightedObservedPoints();
            for (int j = 0; j < Lsize.length; j++) {
                obs1.add(Lsize[j], L[0][j]);
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
                    maxL = Core.minMaxLoc(lp).maxVal;
                    for (int i2 = 0; i2 < 10; i2++) {
                        for (int i3 = 0; i3 < profiles[0].length; i3++) {
                            profiles[i2][i3] = lp.get(i2, i3)[0] / maxL;
                        }
                    }
                }
            }
        }

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
        plot.add("line", Lsize, finalL[0]);

        plot.setFontSize(20);
        plot.setColor(Color.red);
        plot.addLabel(0.65, 0.5, String.format("FWHM = %.3f(nm)", fitresult[2] * 2.355));

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
        int[][] imageData = ip.getIntArray();
        Mat origin = array2Mat(imageData);
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
            int[][] imageRoiData = RoiIp.getIntArray();
            Mat ROI = array2Mat(imageRoiData);
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

            List<Integer> locxTemp = new ArrayList<>();
            List<Integer> locyTemp = new ArrayList<>();

            for (int i = 2; i < SkelImwithout1.cols() - 2; i++) {
                for (int i1 = 2; i1 < SkelImwithout1.rows() - 2; i1++) {
                    if (SkelImwithout1.get(i1, i)[0] != 0) {
                        locxTemp.add(i1);
                        locyTemp.add(i);
                    }
                }
            }

            Integer[] locx = locxTemp.toArray(new Integer[0]);
            Integer[] locy = locyTemp.toArray(new Integer[0]);

            int SkelNum = locxTemp.size();

            Mat rr = Mat.zeros(1, SkelNum, CvType.CV_8UC1);
            List<List<Integer>> LineProfile = new ArrayList<>(SkelNum);

            for (int i = 0; i < SkelNum; i++) {
                double angle = adjangle1(locx, locy, i, CV8U2Int(SkelImwithout1));//SkelImwithout1 8U
                int BoundCount = 0;
                List<Integer> LineProfile_r = new ArrayList<>();
                List<Integer> LineProfile_l = new ArrayList<>();
                int XX1 = 0, YY1 = 0, XX2 = 0, YY2 = 0, rep = 0, index1 = 0, index2 = 0;
                for (int r = 1; r <= 1000; r++) {
                    double X1 = locx[i] - r * Math.sin(angle);
                    double Y1 = locy[i] + r * Math.cos(angle);
                    X1 = Math.round(X1);
                    Y1 = Math.round(Y1);
                    double X2 = locx[i] - r * Math.sin(Math.PI + angle);
                    double Y2 = locy[i] + r * Math.cos(Math.PI + angle);
                    X2 = Math.round(X2);
                    Y2 = Math.round(Y2);
                    index1 = locx[i];
                    index2 = locy[i];

                    if (Y1 > Im2select.cols() - 1 || Y1 < 0 || X1 > Im2select.rows() - 1 || X1 < 0 || Y2 > Im2select.cols() - 1 || Y2 < 0 || X2 > Im2select.rows() - 1 || X2 < 0)
                        break;

                    if ((X1 == XX1 && Y1 == YY1) || (X2 == XX2 && Y2 == YY2)) rep = rep + 1;
                    else {
                        XX1 = (int) X1;
                        YY1 = (int) Y1;
                        XX2 = (int) X2;
                        YY2 = (int) Y2;
                        int[] point1 = new int[]{(int) X1, (int) Y1};
                        int[] point2 = new int[]{(int) X2, (int) Y2};
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

            Double[] a2 = getEdges(rrnozero, rrmin, rrmax);
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

            double[][] L = new double[1][LP.cols()];
            for (int j = 0; j < LP.cols(); j++) {
                for (int l = 0; l < LP.rows(); l++) {
                    L[0][j] += LP.get(l, j)[0];
                }
            }

            L = Normalized(L);
            double[] Lsize = new double[L[0].length];
            for (int j = 0; j < Lsize.length; j++) {
                Lsize[j] = (j + 1) * Px;
            }

            WeightedObservedPoints obs2 = new WeightedObservedPoints();
            for (int j = 0; j < Lsize.length; j++) {
                obs2.add(Lsize[j], L[0][j]);
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

            Plot plot = new Plot("FWHM", "Distance (nm)", "Normalized Intensity");
            plot.setLineWidth(4);
            plot.setColor(Color.red);
            plot.add("line", Xdata, Ydata);
            plot.setLimits(0, Lsize[Lsize.length - 1], 0, 1.2);
            plot.setAxisLabelFont(Font.BOLD, 20);

            plot.setFontSize(20);
            plot.setColor(Color.red);
            plot.addLabel(0.65, 0.5, String.format("FWHM = %.3f(nm)", fitresult[2] * 2.355));

            plot.setLineWidth(4);
            plot.setColor(Color.black);
            plot.add("line", Lsize, L[0]);
            plot.show();
        }
    }

    private Double[] getEdges(List<Integer> norrzero, int minx, int maxx) {
        int xrange = maxx - minx;
        if (xrange <= 50) return integerrule(minx, maxx);
        else {
            float[][] f = new float[1][norrzero.size()];
            for (int i = 0; i < norrzero.size(); i++) {
                f[0][i] = norrzero.get(i);
            }
            float std = std(f);
            return scottsrule(std, minx, maxx, norrzero.size());
        }
    }

    private Double[] scottsrule(float std, int minx, int maxx, int size) {
        double rawBinWidth = 3.5 * std / Math.pow(size, 1.0 / 3.0);
        double powOfTen = Math.pow(10, Math.floor(Math.log10(rawBinWidth)));
        double relSize = rawBinWidth / powOfTen;
        double binWidth;
        if (relSize < 1.5)
            binWidth = 1 * powOfTen;
        else if (relSize < 2.5)
            binWidth = 2 * powOfTen;
        else if (relSize < 4)
            binWidth = 3 * powOfTen;
        else if (relSize < 7.5)
            binWidth = 5 * powOfTen;
        else
            binWidth = 10 * powOfTen;

        double leftEdge = Math.min(binWidth * Math.floor(minx / binWidth), minx);
        double nbinsActual = Math.max(1, Math.ceil((maxx - leftEdge) / binWidth));
        double rightEdge = Math.max(leftEdge + nbinsActual * binWidth, maxx);

        List<Double> edges = new ArrayList<>();
        if (nbinsActual == 1) {
            return new Double[]{leftEdge, rightEdge};
        } else {
            edges.add(leftEdge);
            int i = 1;
            while (i < nbinsActual) {
                edges.add(leftEdge + i++ * binWidth);
            }
            edges.add(rightEdge);
        }

        return edges.toArray(new Double[0]);
    }

    private Double[] integerrule(int minx, int maxx) {
        double edge = minx - 0.5;
        List<Double> edges = new ArrayList<>();
        while (edge <= maxx + 0.5) {
            edges.add(edge++);
        }
        return edges.toArray(new Double[0]);
    }

    private Mat array2Mat(int[][] array) {
        Mat result = Mat.zeros(array.length, array[0].length, CvType.CV_16UC1);//pay attention to the returned type of mat
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                result.put(i, j, array[i][j]);
            }
        }
        return result;
    }

    private Mat array2Mat(int[] array, int imgH, int thres) {
        Mat result = Mat.zeros(imgH, array.length / imgH, CvType.CV_8UC1);//pay attention to the returned type of mat
        int idx = 0;
        for (int j = 0; j < result.cols(); j++) {
            for (int i = 0; i < result.rows(); i++) {
                if (array[idx] > thres)
                    result.put(i, j, 255);
                idx++;
            }
        }
        return result;
    }

    private Mat array2Mat(int[] array, int imgH) {
        Mat result = Mat.zeros(imgH, array.length / imgH, CvType.CV_8UC1);
        int idx = 0;
        for (int i = 0; i < result.rows(); i++) {
            for (int j = 0; j < result.cols(); j++) {
                result.put(i, j, array[idx++]);
            }
        }
        return result;
    }

    private int[] reshape(int[] array, int imgH, int imgW) {
        int[] result = new int[imgH * imgW];
        int idx = 0;
        for (int j = 0; j < imgW; j++) {
            for (int i = 0; i < imgH; i++) {
                result[i * imgW + j] = array[idx] > 30 ? 1 : 0;
                idx++;
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        Image image = ImageIO.read(new FileInputStream("ROI-5.tif"));
        //Image image = ImageIO.read(new FileInputStream("Loc_result2D9-16bit.tif"));
        ImagePlus imagePlus = new ImagePlus("", image);
        LuckyProfiler_ luckyProfile_ = new LuckyProfiler_();
        luckyProfile_.setup("", imagePlus);
        luckyProfile_.run(luckyProfile_.imp.getProcessor());
    }

    private short[] CV16U2Short(Mat mat) {
        short[] result = new short[mat.rows() * mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i * mat.cols() + j] = (short) mat.get(i, j)[0];
            }
        }
        return result;
    }

    public int PreProNum(int randnum, Mat SkellImWithout1, int SkelNum, int[] SingelResult, double px, int SizeIm1, int SizeIm2, Mat aTry) {
        Random rand = new Random();
        int[] PreIndex1 = new int[randnum];
        for (int i = 0; i < randnum; i++) {
            PreIndex1[i] = Math.round(SkelNum * rand.nextFloat());
        }

        List<Integer> Cannot = new ArrayList<>();
        for (int i = 0; i < aTry.rows(); i++) {
            Scalar sum = Core.sumElems(aTry.row(i));
            if (sum.val[0] == 0) {
                Cannot.add(i);
            }
        }

        List<Integer> PreIndex = new ArrayList<>();
        for (int i : PreIndex1) {
            if (!Cannot.contains(i)) {
                PreIndex.add(i);
            }
        }

        randnum = PreIndex.size();
        float[] intenum = new float[randnum];

        for (int i = 0; i < randnum; i++) {
            int Index = PreIndex.get(i);
            int XLoc = SingelResult[Index * 2];
            int YLoc = SingelResult[Index * 2 + 1];
            List<int[]> Loc = new ArrayList<>();
            Loc.add(new int[]{XLoc, YLoc});
            double prefitg;
            Mat line = aTry.row(Index);
            double[][] L = Normalized(CV16U2Double(line));
            double[] Lsize = new double[L[0].length];
            for (int j = 0; j < Lsize.length; j++) {
                Lsize[j] = (j + 1) * px;
            }

            WeightedObservedPoints obs = new WeightedObservedPoints();
            for (int j = 0; j < Lsize.length; j++) {
                obs.add(Lsize[j], L[0][j]);
            }
            double[] fitresult = GaussianCurveFitter.create().fit(obs.toList());
            prefitg = fitresult[2] * Math.sqrt(2) * 1.665;
            if (XLoc - 1 >= 0 && YLoc - 1 >= 0 && XLoc + 1 < SizeIm1 && YLoc + 1 < SizeIm2) {
                for (int k = 0; k < 1000; k++) {
                    Loc = aggregate(Loc, SkellImWithout1);
                    Mat LP = Mat.zeros(Loc.size(), aTry.cols(), CvType.CV_16UC1);
                    for (int j = 0; j < Loc.size(); j++) {
                        int Ind = 0;
                        for (int i1 = 0; i1 < SkelNum; i1++) {
                            if (SingelResult[i1 * 2] == Loc.get(j)[0] && SingelResult[i1 * 2 + 1] == Loc.get(j)[1]) {
                                Ind = i1;
                                break;
                            }
                        }
                        aTry.row(Ind).copyTo(LP.row(j));
                    }

                    L = new double[1][aTry.cols()];
                    for (int j = 0; j < aTry.cols(); j++) {
                        for (int l = 0; l < Loc.size(); l++) {
                            L[0][j] += LP.get(l, j)[0];
                        }
                    }

                    L = Normalized(L);
                    WeightedObservedPoints obs2 = new WeightedObservedPoints();
                    for (int j = 0; j < Lsize.length; j++) {
                        obs2.add(Lsize[j], L[0][j]);
                    }

                    double[] fitresult1 = GaussianCurveFitter.create().fit(obs2.toList());
                    double sig = fitresult1[2] * Math.sqrt(2) * 1.665;

                    Gaussian gaussian = new Gaussian(fitresult1[0], fitresult1[1], fitresult1[2]);
                    double SSE = 0;
                    for (int i1 = 0; i1 < Lsize.length; i1++) {
                        SSE += Math.pow(gaussian.value(Lsize[i1]) - L[0][i1], 2);
                    }
                    double SST = 0;
                    double sum = 0;
                    for (int i1 = 0; i1 < L[0].length; i1++) {
                        sum += L[0][i1];
                    }
                    double mean = sum / L[0].length;
                    for (int i1 = 0; i1 < L[0].length; i1++) {
                        SST += Math.pow(mean - L[0][i1], 2);
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
        for (float v : intenum) {
            sum += v;
        }

        return (int) Math.ceil(sum / randnum);
    }

    public void PreR(Mat SkelImwithout1, int[][] SingleResult1, Mat rr, int i, int PreProjectionNum, int SizeIm1, int SizeIm2, Mat RCadi, Mat Index) {// both uint16
        int XLoc = SingleResult1[i][1];
        int YLoc = SingleResult1[i][2];
        int[] loc = new int[]{XLoc, YLoc};
        List<int[]> Loc = new ArrayList<>();
        Loc.add(loc);
        if (XLoc - 1 >= 0 && YLoc - 1 >= 0 && XLoc + 1 < SizeIm1 && YLoc + 1 < SizeIm2) {
            for (int k = 0; k < 1000; k++) {
                Loc = aggregate(Loc, SkelImwithout1);
                if (Loc.size() >= PreProjectionNum) {
                    for (int j = 0; j < PreProjectionNum; j++) {
                        for (int i1 = 0; i1 < SingleResult1.length; i1++) {
                            if (SingleResult1[i1][1] == Loc.get(j)[0] && SingleResult1[i1][2] == Loc.get(j)[1]) {//这里不能找到就break
                                Index.put(0, j, i1);
                                RCadi.put(0, j, rr.get(0, (int) Index.get(0, j)[0])[0]);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
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

    public Mat array2Mat(float[][] array) {
        Mat result = Mat.zeros(array.length, array[0].length, CvType.CV_32FC1);
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                result.put(i, j, array[i][j]);
            }
        }
        return result;
    }

    private float[][] CV32F2Float(Mat mat) {
        float[][] result = new float[mat.rows()][mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i][j] = (float) mat.get(i, j)[0];
            }
        }
        return result;
    }

    private int[][] CV8U2Int(Mat mat) {
        int[][] result = new int[mat.rows()][mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i][j] = (int) mat.get(i, j)[0];
            }
        }
        return result;
    }

    private double[][] CV16U2Double(Mat mat) {
        double[][] result = new double[mat.rows()][mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i][j] = mat.get(i, j)[0];
            }
        }
        return result;
    }

    public double adjangle1(Integer[] Locx, Integer[] Locy, int i, int[][] SkelImwithout1) {
        int G1 = SkelImwithout1[Locx[i]][Locy[i] + 1];
        int G2 = SkelImwithout1[Locx[i] - 1][Locy[i] + 1];
        int G3 = SkelImwithout1[Locx[i] - 1][Locy[i]];
        int G4 = SkelImwithout1[Locx[i] - 1][Locy[i] - 1];
        int G5 = SkelImwithout1[Locx[i]][Locy[i] - 1];
        int G6 = SkelImwithout1[Locx[i] + 1][Locy[i] - 1];
        int G7 = SkelImwithout1[Locx[i] + 1][Locy[i]];
        int G8 = SkelImwithout1[Locx[i] + 1][Locy[i] + 1];
        int G9 = SkelImwithout1[Locx[i] + 1][Locy[i] + 2];
        int G10 = SkelImwithout1[Locx[i]][Locy[i] + 2];
        int G11 = SkelImwithout1[Locx[i] - 1][Locy[i] + 2];
        int G12 = SkelImwithout1[Locx[i] - 2][Locy[i] + 2];
        int G13 = SkelImwithout1[Locx[i] - 2][Locy[i] + 1];
        int G14 = SkelImwithout1[Locx[i] - 2][Locy[i]];
        int G15 = SkelImwithout1[Locx[i] - 2][Locy[i] - 1];
        int G16 = SkelImwithout1[Locx[i] - 2][Locy[i] - 2];
        int G17 = SkelImwithout1[Locx[i] - 1][Locy[i] - 2];
        int G18 = SkelImwithout1[Locx[i]][Locy[i] - 2];
        int G19 = SkelImwithout1[Locx[i] + 1][Locy[i] - 2];
        int G20 = SkelImwithout1[Locx[i] + 2][Locy[i] - 2];
        int G21 = SkelImwithout1[Locx[i] + 2][Locy[i] - 1];
        int G22 = SkelImwithout1[Locx[i] + 2][Locy[i]];
        int G23 = SkelImwithout1[Locx[i] + 2][Locy[i] + 1];
        int G24 = SkelImwithout1[Locx[i] + 2][Locy[i] + 2];


        int[][] G = new int[][]{{G16, G15, G14, G13, G12}, {G17, G4, G3, G2, G11}, {G18, G5, 1, G1, G10}, {G19, G6, G7, G8, G9}, {G20, G21, G22, G23, G24}};

        List<Integer> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();
        for (int i1 = 0; i1 < G.length; i1++) {
            for (int i2 = 0; i2 < G[0].length; i2++) {
                if (G[i1][i2] == 1) {
                    x.add(i1);
                    y.add(i2);
                }
            }
        }

        int N = 0;
        for (int[] ints : G) {
            N += Arrays.stream(ints).sum();
        }

        int[] xy = new int[x.size()];
        for (int i1 = 0; i1 < x.size(); i1++) {
            xy[i1] = x.get(i1) * y.get(i1);
        }

        int temp1 = Arrays.stream(xy).sum();

        double meanx = mean(x);
        double meany = mean(y);

        double temp2 = N * meanx * meany;

        int[] xx = new int[x.size()];
        for (int i1 = 0; i1 < x.size(); i1++) {
            xx[i1] = x.get(i1) * x.get(i1);
        }

        int temp3 = Arrays.stream(xx).sum();

        double temp4 = N * meanx * meanx;

        double k = (temp1 - temp2) / (temp3 - temp4);

        double ang;

        if (Double.isNaN(k)) {
            ang = Math.PI / 2;
        } else {
            ang = Math.atan(k);
        }

        return ang;
    }

    private double mean(List<Integer> list) {
        double sum = 0;
        for (Integer integer : list) {
            sum += integer;
        }

        return sum / list.size();
    }

    public Mat DenoiseFunc(Mat I, double PixelSizeImage) {
        int n = (int) Math.ceil(50 / PixelSizeImage);

        Imgproc.medianBlur(I, I, n);//The median filter uses #BORDER_REPLICATE internally to cope with border pixels
        Imgproc.GaussianBlur(I, I, new Size(n, n), 0.5, 0.5, Core.BORDER_CONSTANT);
        Imgproc.blur(I, I, new Size(n, n), new Point(-1, -1), Core.BORDER_CONSTANT);
        float[][] I3 = CV32F2Float(I);

        float[][] Gx = horizontalGradient(I3);
        float[][] Gy = verticalGradient(I3);
        float[][] F = sqrt(sumOfMatrices(dotPow(Gx, 2), dotPow(Gy, 2)));
        float[][] GF1 = horizontalGradient(F);
        float[][] GF2 = verticalGradient(F);
        float[][] GF = sqrt(sumOfMatrices(dotPow(GF1, 2), dotPow(GF2, 2)));

        float Ttemp1 = mean(GF);
        float Ttemp2 = std(GF);
        float T;

        if (Ttemp1 < Ttemp2) {
            T = Ttemp1 - Ttemp2;
        } else {
            T = Ttemp1 + Ttemp2;
        }

        boolean[][] Index = AND(matrixCompareWithANumber(GF, T), matrixCompareWithANumber(I3, Math.min(5 * stdOfARow(stdOfColumns(I3)), mean(I3))));
        int r = Index.length;
        int c = Index[0].length;
        float[][] oI1 = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (Index[i][j]) {
                    oI1[i][j] = I3[i][j];
                }
            }
        }

        Mat oi3 = array2Mat(oI1);
        Imgproc.medianBlur(oi3, oi3, n);
        Imgproc.GaussianBlur(oi3, oi3, new Size(n, n), 0.5, 0.5, Core.BORDER_CONSTANT);

        return oi3;
    }

    private float[][] horizontalGradient(float[][] input) {
        int r = input.length;
        int c = input[0].length;
        float[][] output = new float[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (j == 0) {
                    output[i][j] = input[i][j + 1] - input[i][j];
                } else if (j == c - 1) {
                    output[i][j] = input[i][j] - input[i][j - 1];
                } else {
                    output[i][j] = (input[i][j + 1] - input[i][j - 1]) / 2;
                }
            }
        }

        return output;
    }

    private float[][] verticalGradient(float[][] input) {
        int r = input.length;
        int c = input[0].length;
        float[][] output = new float[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (i == 0) {
                    output[i][j] = input[i + 1][j] - input[i][j];
                } else if (i == r - 1) {
                    output[i][j] = input[i][j] - input[i - 1][j];
                } else {
                    output[i][j] = (input[i + 1][j] - input[i - 1][j]) / 2;
                }
            }
        }

        return output;
    }

    private float[][] dotPow(float[][] input, int n) {
        int r = input.length;
        int c = input[0].length;

        float[][] output = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                output[i][j] = (float) Math.pow(input[i][j], n);
            }
        }

        return output;
    }

    private float[][] sumOfMatrices(float[][] a, float[][] b) {
        int r = a.length;
        int c = a[0].length;

        float[][] output = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                output[i][j] = a[i][j] + b[i][j];
            }
        }

        return output;
    }

    private float[][] sqrt(float[][] input) {
        int r = input.length;
        int c = input[0].length;

        float[][] output = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                output[i][j] = (float) Math.sqrt(input[i][j]);
            }
        }

        return output;
    }

    private float mean(float[][] input) {
        float val = 0;
        int cnt = input.length * input[0].length;

        for (float[] F : input) {
            for (float f : F) {
                val += f;
            }
        }

        return val / cnt;
    }

    private float std(float[][] input) {
        float val = 0;
        int cnt = input.length * input[0].length;
        float mean = mean(input);

        for (float[] F : input) {
            for (float f : F) {
                val += (f - mean) * (f - mean);
            }
        }

        return (float) Math.sqrt(val / (cnt - 1));
    }

    private boolean[][] matrixCompareWithANumber(float[][] mat, float num) {
        int r = mat.length;
        int c = mat[0].length;
        boolean[][] flags = new boolean[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                flags[i][j] = mat[i][j] > num;
            }
        }

        return flags;
    }

    private boolean[][] AND(boolean[][] a, boolean[][] b) {
        int r = a.length;
        int c = a[0].length;
        boolean[][] mat = new boolean[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                mat[i][j] = a[i][j] && b[i][j];
            }
        }

        return mat;
    }

    private float stdOfARow(float[] input) {
        float val = 0;
        int cnt = input.length;

        float sum = 0;
        for (float f : input) {
            sum += f;
        }

        float mean = sum / cnt;

        for (float f : input) {
            val += (f - mean) * (f - mean);
        }

        return (float) Math.sqrt(val / (cnt - 1));
    }

    private float[] stdOfColumns(float[][] input) {
        int r = input.length;
        int c = input[0].length;
        float[] res = new float[c];

        for (int i = 0; i < c; i++) {
            float val = 0;
            float sum = 0;
            for (float[] floats : input) {
                sum += floats[i];
            }
            float mean = sum / r;

            for (float[] f : input) {
                val += (f[i] - mean) * (f[i] - mean);
            }

            res[i] = (float) Math.sqrt(val / (r - 1));
        }

        return res;
    }

    public double[][] Normalized(double[][] I) {
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;

        for (double[] i : I) {
            for (double f : i) {
                max = Math.max(max, f);
                min = Math.min(min, f);
            }
        }

        double[][] res = new double[I.length][I[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                res[i][j] = (I[i][j] - min) / (max - min);
            }
        }

        return res;
    }

    public List<int[]> aggregate(List<int[]> Loc, Mat SkelImwithout1) {
        List<int[]> ALoc = new ArrayList<>(Loc);

        for (int k = 0; k < Loc.size(); k++) {
            int x = Loc.get(k)[0];
            int y = Loc.get(k)[1];
            int[][] L = new int[][]{{x - 1, y - 1}, {x - 1, y}, {x - 1, y + 1}, {x, y - 1}, {x, y + 1}, {x + 1, y - 1}, {x + 1, y}, {x + 1, y + 1}};
            for (int i = 0; i < 8; i++) {
                boolean[] jud = new boolean[ALoc.size()];
                for (int j = 0; j < jud.length; j++) {
                    jud[j] = (ALoc.get(j)[0] == L[i][0]) && (ALoc.get(j)[1] == L[i][1]);
                }

                if (L[i][0] >= 0 && L[i][0] < SkelImwithout1.rows() && L[i][1] >= 0 && L[i][1] < SkelImwithout1.cols()) {
                    if (SkelImwithout1.get(L[i][0], L[i][1])[0] == 1) {
                        boolean flag = false;
                        for (boolean b : jud) {
                            flag |= b;
                        }
                        if (!flag) {
                            ALoc.add(L[i]);
                        }
                    }
                }
            }
        }

        return ALoc;
    }
}
