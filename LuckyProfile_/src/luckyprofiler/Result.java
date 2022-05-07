

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Song
 */
public class Result {
    int SI;
    int[][] ROIselected;
    double[] SkelNum;
    double WithWidthFit_s;

    public Result(int SI, int[][] ROIselected, double[] skelNum, double withWidthFit_s) {
        this.SI = SI;
        this.ROIselected = ROIselected;
        SkelNum = skelNum;
        WithWidthFit_s = withWidthFit_s;
    }
}
