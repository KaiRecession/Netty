# NIO

non-blocking io 非阻塞 IO

# Channel

看file的channel的read源码

![截屏2022-11-30 10.55.39](img/%E6%88%AA%E5%B1%8F2022-11-30%2010.55.39.png)

java堆中有一个Buffer，写数据的时候首先创建一个用户态的内存Buffer（堆外空间），然后把Buffer写在堆外空间的Buffer，再由DMA切换内核态把数据写到硬盘/网卡。因为堆中的Buffer收垃圾回收的影响，内存地址会变，这样操作系统写数据就会发生异常，只能在堆中创建一个DirectBuffer对象，**这个对象在堆中，但是指向的地址是在堆外的一个Buffer**，当然也可以直接在堆中创建DirectBuffer对象，就能省去一次copy。可以在源码中看到如果channel的read传进来的Buffer是DirectBuffer就不创建，否则会创建一个DirectBuffer。

**channel提供了一个map()方法，可以直接将数据映射到内存中**

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

**flip方法看源码就是把limit等于position，再让position等于0**

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

# 网络编程

一个Socket连接的主键（即不同socket之间的区分）是由一个五元组{SRC-IP, SRC-PORT, DEST-IP, DEST-PORT, PROTOCOL}组成，即{源地址，源端口，目标地址，目标端口，协议}组成，五元组任何一个不同，程序都可分辨。所以服务端的同一个端口可以连接多个客户端请求，因为源ip+端口是不一样的

accept会阻塞，read会阻塞。那么连接不断就需要一直有一个线程维护，一个链接配上一个线程就会很浪费，多个线程虽然大部分时间在阻塞，但是线程的维护和切换也很耗费资源，既然大部分时间都在阻塞，那么就考虑使用单线程来变相解决

## 非阻塞的实现

accept和read的函数不在阻塞，直接返回是否有数据。这样直接判断是否有数据就让程序往下进行，不在阻塞。缺点就是需要while true一直循环，为了改善死循环就出现了selector

## 多路复用

单线程可以配合 Selector 完成对多个 Channel 可读写事件的监控，这称之为多路复用

* **多路复用仅针对网络 IO**、普通文件 IO 没法利用多路复用
* 如果不用 Selector 的非阻塞模式，**线程大部分时间都在做无用功（死循环中读到空数据的情况）**，而 Selector 能够保证
  * 有可连接事件时才去连接
  * 有可读事件才去读取
  * 有可写事件才去写入
    * 限于网络传输能力，Channel 未必时时可写，一旦 Channel 可写，会触发 Selector 的可写事件

![截屏2022-11-30 15.33.16](img/%E6%88%AA%E5%B1%8F2022-11-30%2015.33.16.png)

* 一个线程配合 selector 就可以监控多个 channel 的事件，事件发生线程才去处理。避免非阻塞模式下所做无用功
* 让这个线程能够被充分利用
* 节约了线程的数量
* 减少了线程上下文切换

## select何时不阻塞

* 事件发生时
  * 客户端发起连接请求，会触发 accept 事件
  * 客户端发送数据过来，**客户端正常、异常关闭时，都会触发 read 事件**，另外如果发送的数据大于 buffer 缓冲区，会触发多次读取事件
  * channel 可写，会触发 write 事件
  * 在 linux 下 nio bug 发生时
* 调用 selector.wakeup()
* 调用 selector.close()
* selector 所在线程 interrupt

## 绑定事件

也称之为注册事件，绑定的事件 selector 才会关心 

绑定的事件类型可以有

* connect - 客户端连接成功时触发
* accept - 服务器端成功接受连接时触发
* read - 数据可读入时触发，有因为接收能力弱，数据暂不能读入的情况
* write - 数据可写出时触发，有因为发送能力弱，数据暂不能写出的情况

**总的来说，每一个对象都有不同的事件要关心，比如ServerSocketChannel关心的是accept事件，SocketChannel关心的是read事件，把对象和相应的事件交给selector即可**

**通过SelectionKey的 SelectableChannel的channel()方法，可以获取当前 SelectionKey的表示的通道，这就是为什么每次读写都要先拿key.channel()**

**登记表**：当我们呼叫时channel.register，其中会有一个新的项目（密钥）。仅当我们调用时key.cancel()，它将从此表中删除。

准备好选择表：**当我们调用时selector.select()，选择器将查找注册表，找到可用的键，并将它们的引用复制到该选择表中**。选择器不会清除此表中的项目（这意味着，即使我们selector.select()再次调用，它也不会清除现有项目）

这就是为什么iter.remove()当我们从选择表中获得键时必须调用的原因。如果没有，selector.selectedKeys()即使它尚未准备好使用，我们也会一次又一次地获得密钥。

因为 select 在事件发生后，就会将相关的 key 放入 selectedKeys 集合，但不会在处理完后从 selectedKeys 集合中移除，需要我们自己编码删除。例如

* 第一次触发了 ssckey 上的 accept 事件，没有移除 ssckey 
* **第二次触发了 sckey 上的 read 事件**，但这时 selectedKeys 中还有上次的 ssckey ，在处理时因为没有真正的 serverSocket 连上了，就会导致空指针异常

**不移除key的话，触发read的时候才会有空指针异常，移除的是选择表的key**

**不妨理解成，连接断开之前，每次读read都是0，所以没有事件。当连接断开后，read是-1，这样相当于一直有事件发生，而且之前这个连接已经被添加到了登记表中，就一直触发事件。这个连接拿到了，然后达不到，相当于对于达不到的连接去读，会一直读到-1。-1的返回是很有必要的，就像读文件读到结尾要是-1，表示结束。如果没有-1，buffer长度又可变，就不知道什么时候结束，结束符是最好的选择。所以每次要cancle，在网络中读到-1就相当于连接断开，也就是文件结束了**

**文件的channel也是-1当结尾，可能是规定吧，找不到解释。只有读取字节的时候，读到末尾会返回int为-1，表示结束。这个很有用，其他情况都是返回ascii表中对应的int值，肯定不会是-1**

# 处理消息边界问题（半包，粘包问题）

![截屏2022-12-07 21.23.13](../../../Library/Application%20Support/typora-user-images/%E6%88%AA%E5%B1%8F2022-12-07%2021.23.13.png)

本科毕设写的TLV格式的原因就在这里了，可以很好的处理半包粘包的问题，能够准确的分配buffer的长度。TLV 格式，即 Type 类型、Length 长度、Value 数据，类型和长度已知的情况下，就可以方便获取消息大小，分配合适的 buffer，**缺点是 buffer 需要提前分配，如果内容过大，则影响 server 吞吐量**

如果服务器端分配的buffer没有将客户端的消息一次读完，那就会触发下一次的read事件，依次类推，直到用buffer将消息读完

## 附件（attachment）

给每一个sc连接分配一个sc专属的buffer，防止多个sc连接公用buffer，**需要buffer记录历史的半包数据的话那不就乱了**。和线程的ThreadLocal类似，都是给这个对象注入一个私有变量罢了

当使用split方法后，检查position是等于limit的，就证明这是个半包，就扩容

# write事件

**服务器向客户端写数据，基于TCP的控制。所以写入的数据不会一直写入，可能客户端处理数据较慢，这时候服务器写进去的字节数就是0，就有点像之前read的非阻塞的返回值，返回0就是事件没有发生。在客户端设置线程处理buffer的时候休眠一秒，服务端就会出现很多次写入0个字节。这可能就是阻塞控制吧，数据的传输速度不仅看服务端，还要看客户端被发送方的处理速度**

写事件简直就是通知拥塞控制什么时候放开了

处理完后取消key的可写事件，要不这个写事件会一直触发

只要向 channel 发送数据时，socket 缓冲可写，这个事件会频繁触发，因此应当只在 socket 缓冲区写不下时再关注可写事件，数据写完之后再取消关注

## 多线程优化

定义boss线程，worker线程。每个线程都有自己内部的selector，boss线程的selector方法处理accept事件，剩下的时间都交给worker线程。由于selector不同，比如boss线程把读事件交给worker线程注册时，如果worker线程的selector正处于select方法阻塞，那就注册不上。因此，把boss线程搞成线程池一样，维护一个任务阻塞队列，把注册这件事情的代码放在队列里面，然后调用wakeup方法唤醒select阻塞，唤醒后就拿出队列里面的注册代码运行，然后再检查是不是有读写时间发生。

# NIO与BIO

### stream与channel

stream 不会自动缓冲数据，channel 会利用系统提供的发送缓冲区、接收缓冲区（更为底层）。stream 仅支持阻塞 API，channel 同时支持阻塞、非阻塞 API，网络 channel 可配合 selector 实现多路复用二者均为全双工，即读写可以同时进行。