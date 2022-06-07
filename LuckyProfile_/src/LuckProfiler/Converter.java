/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
//package LuckProfiler;

/**
 *
 * @author Song
 */
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Converter {

    public static Mat array2Mat(int[][] array) {
        Mat result = Mat.zeros(array.length, array[0].length, CvType.CV_16UC1);//pay attention to the returned type of mat
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                result.put(i, j, array[i][j]);
            }
        }
        return result;
    }

    public static Mat array2Mat(int[] array, int imgH, int thres) {
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

    public static Mat array2Mat(int[] array, int imgH) {
        Mat result = Mat.zeros(imgH, array.length / imgH, CvType.CV_8UC1);
        int idx = 0;
        for (int i = 0; i < result.rows(); i++) {
            for (int j = 0; j < result.cols(); j++) {
                result.put(i, j, array[idx++]);
            }
        }
        return result;
    }

    public static int[] reshape(int[] array, int imgH, int imgW, int thres) {
        int[] result = new int[imgH * imgW];
        int idx = 0;
        for (int j = 0; j < imgW; j++) {
            for (int i = 0; i < imgH; i++) {
                result[i * imgW + j] = array[idx++] > thres ? 1 : 0;
            }
        }
        return result;
    }

    public static int[][] reshape(int[] array, int imgH, int imgW) {
        int[][] result = new int[imgH][imgW];
        int idx = 0;
        for (int i = 0; i < imgH; i++) {
            for (int j = 0; j < imgW; j++) {
                result[i][j] = array[idx++];
            }
        }
        return result;
    }

    public static Mat array2Mat(float[][] array) {
        Mat result = Mat.zeros(array.length, array[0].length, CvType.CV_32FC1);
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                result.put(i, j, array[i][j]);
            }
        }
        return result;
    }

    public static float[][] Mat2Float(Mat mat) {
        float[][] result = new float[mat.rows()][mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i][j] = (float) mat.get(i, j)[0];
            }
        }
        return result;
    }

    public static int[][] Mat2Int(Mat mat) {
        int[][] result = new int[mat.rows()][mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i][j] = (int) mat.get(i, j)[0];
            }
        }
        return result;
    }

    public static int[] Mat2Int1D(Mat mat) {
        int c = mat.cols();
        int[] result = new int[mat.rows() * mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i * c + j] = (int) mat.get(i, j)[0];
            }
        }
        return result;
    }

    public static double[][] Mat2Double(Mat mat) {
        double[][] result = new double[mat.rows()][mat.cols()];
        for (int i = 0; i < mat.rows(); i++) {
            for (int j = 0; j < mat.cols(); j++) {
                result[i][j] = mat.get(i, j)[0];
            }
        }
        return result;
    }
}

