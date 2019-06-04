package com.example.lenovo.mygraduate8b;
/**
 * 人脸特征(512维特征值)
 * 相似度取特征向量之间的欧式距离.
 */
public class FaceFeature {
    public static final int DIMS=512; //特征维度大小
    private float fea[]; //特征值
    FaceFeature(){
        fea=new float[DIMS];
    }
    public float[] getFeature(){
        return fea;
    }
    //比较当前特征和另一个特征之间的相似度
    public double compare(FaceFeature ff){
        double dist=0;
        for (int i=0;i<DIMS;i++)
            dist+=(fea[i]-ff.fea[i])*(fea[i]-ff.fea[i]);
        dist=Math.sqrt(dist);
        return dist;
    }
}
