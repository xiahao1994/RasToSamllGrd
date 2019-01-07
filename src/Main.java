import java.io.*;
import java.util.HashMap;

public class Main {
        //定义预览图的长宽最大大小


    public static void main(String[] args) throws Exception {
        String inputfile= args[0];
        int accuracy = Integer.valueOf(args[1]);
        InputStream in= new FileInputStream(new File(inputfile));
        String outputfile = inputfile.split("\\.")[0]+".grd";
        String outputfilepng = inputfile.split("\\.")[0]+".png";
        OutputStream out = new FileOutputStream(new File(outputfile));
        Main.getHDFSRasviewTest(in,out,accuracy,outputfilepng);
    }

    /**
     *
     * @param in
     * @return
     */
    public static void getHDFSRasviewTest(InputStream in,OutputStream out,int accuracy,String outputfilepng){
        int Max_X=accuracy;
        int Max_Y=accuracy;
        // File rasfile = new File("E:\\RAS数据\\Africa.Ras");
        try {
            //源文件头
            HashMap map = RasHead.getRasHead(in);
            int nx=Integer.valueOf(map.get("nx").toString());
            int ny=Integer.valueOf(map.get("ny").toString());
            double x1=Double.valueOf(map.get("x1").toString());
            double x2=Double.valueOf(map.get("x2").toString());
            double y1=Double.valueOf(map.get("y1").toString());
            double y2=Double.valueOf(map.get("y2").toString());
            double z1=Double.valueOf(map.get("z1").toString());
            double z2=Double.valueOf(map.get("z2").toString());

            //缩放比例(每scala行取一行，每scala列取一列)
            int scala = ((nx-1)/Max_X>(ny-1)/Max_Y?(nx-1)/Max_X:(ny-1)/Max_Y)+1;//-1其实只是让500在里面

            //取样的数量
            int num_x = nx/scala;//{0,scala,2scala,..,(num_x-1)*scala}
            int num_y = ny/scala;//{0,scala,2scala,..,(num_y-10)*scala}
            float data[] = new float[num_x*num_y];

            int loc =0;//预览数组全局下标
            for(int row = 0;row<ny;row++) {


                //准备读第row行的数据
                float row_data[] = new float[nx];//存放一行的数据数组
                byte row_byte[] = new byte[4*nx];
                in.read(row_byte);
                for (int i = 0; i <nx; i++) {
                    int ch4 = row_byte[4 *i    ]& 0xff;
                    int ch3 = row_byte[4 *i + 1]& 0xff;
                    int ch2 = row_byte[4 *i + 2]& 0xff;
                    int ch1 = row_byte[4 *i + 3]& 0xff;
                    if ((ch1 | ch2 | ch3 | ch4) < 0)System.out.println("xx");
                    row_data[i] = Float.intBitsToFloat(((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)));
                }
                //抽取一行中的数据
                if((row+1)%scala==0){
                    for(int l =0 ;l<num_x;l++){
                        data[loc]=row_data[l*scala];
                        loc++;
                    }
                }

            }
            //写grd文件
           // OutputStream out = new FileOutputStream(new File("E:\\地理计算框架文档\\栅格格式转换\\RasToGrd\\cck.grd"));
            byte[]dsbb = {'D','S','B','B'};
            out.write(dsbb);
            //写入文件头到Grd
            TileSplitDataOutputStream o = new TileSplitDataOutputStream(new DataOutputStream(out));
            o.writeShort((short) num_x);
            o.writeShort((short) num_y);
            o.writeDouble(x1);
            o.writeDouble(x2);
            o.writeDouble(y1);
            o.writeDouble(y2);
            o.writeDouble(z1);
            o.writeDouble(z2);
            for(int i=0;i<data.length;i++){
                o.writeFloat(data[i]);
            }
            o.close();
            out.close();
            in.close();

            //写png图像
            int rgbArray[][] = new int[num_y][num_x];
            for(int i=0;i<data.length;i++){
                int value = (new Double(((double)data[i]-z1)*255/(z2-z1))).intValue();
                rgbArray[num_y-1-i/num_x][ i%num_x]=value*256*256+value*256+value+255*256*256*256;
            }
            ImageWithArray.writeImageFromArray(outputfilepng, "png", rgbArray);//这里写你要输出的绝对路径+文件名


        }catch (Exception e){
            System.out.println("处理失败");
        }
        try {
            in.close();//关闭输入流
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("处理完成");

    }


}
