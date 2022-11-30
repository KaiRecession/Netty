# NIO

non-blocking io 非阻塞 IO

# Channel

看file的channel的read源码

![截屏2022-11-30 10.55.39](img/%E6%88%AA%E5%B1%8F2022-11-30%2010.55.39.png)

java堆中有一个Buffer，写数据的时候首先创建一个用户态的内存Buffer（堆外空间），然后把Buffer写在堆外空间的Buffer，再由DMA切换内核态把数据写到硬盘/网卡。因为堆中的Buffer收垃圾回收的影响，内存地址会变，这样操作系统写数据就会发生异常，只能在堆中创建一个DirectBuffer对象，**这个对象在堆中，但是指向的地址是在堆外的一个Buffer**，当然也可以直接在堆中创建DirectBuffer对象，就能省去一次copy。可以在源码中看到如果channel的read传进来的Buffer是DirectBuffer就不创建，否则会创建一个DirectBuffer。

**要不你就关掉jvm的垃圾回收，也能行**

![截屏2022-11-30 11.00.36](img/%E6%88%AA%E5%B1%8F2022-11-30%2011.00.36.png)

## DMA

对于一个IO操作而言，都是通过CPU发出对应的指令来完成，但是相比CPU来说，IO的速度太慢了，CPU有大量的时间处于等待IO的状态。因此就产生了DMA（Direct Memory Access）直接内存访问技术，本质上来说他就是一块主板上独立的芯片，通过它来进行内存和IO设备的数据传输，从而减少CPU的等待时间
# Buffer结构

![截屏2022-11-30 11.15.48](img/%E6%88%AA%E5%B1%8F2022-11-30%2011.15.48.png)

写模式，limit等于容量，写一个字节（ByteBuffer）position移动一格

flip动作后，limit变成读限制，position变成读位置

![截屏2022-11-30 11.17.29](img/%E6%88%AA%E5%B1%8F2022-11-30%2011.17.29.png)

clear动作发生后，position归零后又变成写位置，limit变成capacity位置，如最开始的图

compact方法，把未读完的部分向前压缩，变成写模式，相当于clear的升级版

![截屏2022-11-30 11.19.28](img/%E6%88%AA%E5%B1%8F2022-11-30%2011.19.28.png)

mark和reset

mark 是在读取时，做一个标记，即使 position 改变，只要调用 reset 就能回到 mark 的位置

**注意：position指的是下一个可以放入的位置，是边界 + 1的，所以limit也是开区间**

**Buffer是非线程安全的**

# 粘包和半包

数据被进行了重新组合，例如原始数据有3条为

* Hello,world\n
* I'm zhangsan\n
* How are you?\n

变成了下面的两个 byteBuffer (黏包，半包)

* Hello,world\nI'm zhangsan\nHo
* w are you?\n