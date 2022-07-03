import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Denoise {

    public static Mat DenoiseFunc(Mat I, double PixelSizeImage) {
        int n = (int) Math.ceil(50 / PixelSizeImage);

        Imgproc.medianBlur(I, I, n);//The median filter uses #BORDER_REPLICATE internally to cope with border pixels
        Imgproc.GaussianBlur(I, I, new Size(n, n), 0.5, 0.5, Core.BORDER_CONSTANT);
        Imgproc.blur(I, I, new Size(n, n), new Point(-1, -1), Core.BORDER_CONSTANT);
        float[][] I3 = Converter.Mat2Float(I);

        float[][] Gx = Arithmetic.horizontalGradient(I3);
        float[][] Gy = Arithmetic.verticalGradient(I3);
        float[][] F = Arithmetic.sqrt(Arithmetic.sumOfMatrices(Arithmetic.dotPow(Gx, 2), Arithmetic.dotPow(Gy, 2)));
        float[][] GF1 = Arithmetic.horizontalGradient(F);
        float[][] GF2 = Arithmetic.verticalGradient(F);
        float[][] GF = Arithmetic.sqrt(Arithmetic.sumOfMatrices(Arithmetic.dotPow(GF1, 2), Arithmetic.dotPow(GF2, 2)));

        float Ttemp1 = Arithmetic.mean(GF);
        float Ttemp2 = Arithmetic.std(GF);
        float T;

        if (Ttemp1 < Ttemp2) {
            T = Ttemp1 - Ttemp2;
        } else {
            T = Ttemp1 + Ttemp2;
        }

        boolean[][] Index = Arithmetic.and(Arithmetic.matrixCompareWithANumber(GF, T), Arithmetic.matrixCompareWithANumber(I3, Math.min(5 * Arithmetic.stdOfARow(Arithmetic.stdOfColumns(I3)), Arithmetic.mean(I3))));
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
