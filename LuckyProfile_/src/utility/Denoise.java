package utility;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import utility.Converter;

import static utility.Arithmetic.*;

public class Denoise {

    public static Mat DenoiseFunc(Mat I, double PixelSizeImage) {
        int n = (int) Math.ceil(50 / PixelSizeImage);

        Imgproc.medianBlur(I, I, n);//The median filter uses #BORDER_REPLICATE internally to cope with border pixels
        Imgproc.GaussianBlur(I, I, new Size(n, n), 0.5, 0.5, Core.BORDER_CONSTANT);
        Imgproc.blur(I, I, new Size(n, n), new Point(-1, -1), Core.BORDER_CONSTANT);
        float[][] I3 = Converter.Mat2Float(I);

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

        Mat oi3 = Converter.array2Mat(oI1);
        Imgproc.medianBlur(oi3, oi3, n);
        Imgproc.GaussianBlur(oi3, oi3, new Size(n, n), 0.5, 0.5, Core.BORDER_CONSTANT);
        return oi3;
    }
}
