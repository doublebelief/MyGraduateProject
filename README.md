# MyGraduateProject
Face recognition and voice match.（基于人脸识别和声纹识别的智能锁控制系统）  
MyGraduate8 project can run in any android device, while MyGraduate8b project only runs in my own arm development board(rk3399 nanoPi M4).

## MyGraduate8使用说明  
这个项目可以直接运行在Android7+的安卓手机上，即直接安装上即可。
## MyGraduate8b使用说明
这个项目必须使用我自己的开发板（rk3399 nanoPi m4），因为需要用到系统权限，而且加入了系统签名。  
想要运行这个项目需要按照以下步骤进行：
- 首先在开发板上安装gpiodemo；  
- 然后通过adb连接开发板，通过su命令进入root界面，输入“chown -R system sys”修改sys文件夹的所有者；
- 之后在开发板上运行gpiodemo这个项目，然后关闭这个项目（关闭这个项目进程）；
- 重复第二步的动作，再次修改sys文件夹的所有者；
- 最后，运行MgGrdauate8b即可。  
*注：前面四步是修改android系统中sys文件夹的所有者，目的是为了能够让系统用户修改底层文件。经过前面四步的修改，最后一步才能正常的控制GPIO口上LED灯的亮灭。*
