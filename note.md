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

## write事件

**服务器向客户端写数据，基于TCP的控制。所以写入的数据不会一直写入，可能客户端处理数据较慢，这时候服务器写进去的字节数就是0，就有点像之前read的非阻塞的返回值，返回0就是事件没有发生。在客户端设置线程处理buffer的时候休眠一秒，服务端就会出现很多次写入0个字节。这可能就是阻塞控制吧，数据的传输速度不仅看服务端，还要看客户端被发送方的处理速度**

写事件简直就是通知拥塞控制什么时候放开了

处理完后取消key的可写事件，要不这个写事件会一直触发

只要向 channel 发送数据时，socket 缓冲可写，这个事件会频繁触发，因此应当只在 socket 缓冲区写不下时再关注可写事件，数据写完之后再取消关注

## 多线程优化

定义boss线程，worker线程。每个线程都有自己内部的selector，boss线程的selector方法处理accept事件，剩下的时间都交给worker线程。由于selector不同，比如boss线程把读事件交给worker线程注册时，如果worker线程的selector正处于select方法阻塞，那就注册不上。因此，把boss线程搞成线程池一样，维护一个任务阻塞队列，把注册这件事情的代码放在队列里面，然后调用wakeup方法唤醒select阻塞，唤醒后就拿出队列里面的注册代码运行，然后再检查是不是有读写时间发生。

## NIO与BIO

### stream与channel

stream 不会自动缓冲数据，channel 会利用系统提供的发送缓冲区、接收缓冲区（更为底层）。stream 仅支持阻塞 API，channel 同时支持阻塞、非阻塞 API，网络 channel 可配合 selector 实现多路复用二者均为全双工，即读写可以同时进行。

# Netty

依赖导入

```java
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>4.1.39.Final</version>
</dependency>
```

## 组件

#### EventLoop

EventLoop 本质是一个单线程执行器（同时维护了一个 Selector），里面有 run 方法处理 Channel 上源源不断的 io 事件。

它的继承关系比较复杂

* 一条线是继承自 j.u.c.ScheduledExecutorService 因此包含了线程池中所有的方法
* 另一条线是继承自 netty 自己的 OrderedEventExecutor，
  * 提供了 boolean inEventLoop(Thread thread) 方法判断一个线程是否属于此 EventLoop
  * 提供了 parent 方法来看看自己属于哪个 EventLoopGroup

#### EventLoopGroup

EventLoopGroup 是一组 EventLoop，Channel 一般会调用 EventLoopGroup 的 register 方法来绑定其中一个 EventLoop，后续这个 Channel 上的 io 事件都由此 EventLoop 来处理（保证了 io 事件处理时的线程安全）

* 继承自 netty 自己的 EventExecutorGroup
  * 实现了 Iterable 接口提供遍历 EventLoop 的能力
  * 另有 next 方法获取集合中下一个 EventLoop

关闭方法：

优雅关闭 `shutdownGracefully` 方法。该方法会首先切换 `EventLoopGroup` 到关闭状态从而拒绝新的任务的加入，然后在任务队列的任务都处理完成后，停止线程的运行。从而确保整体应用是在正常有序的状态下退出的

工人与 channel 之间进行了绑定：

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-23%2015.01.38.png" alt="截屏2022-12-23 15.01.38" style="zoom:50%;" />

#### DefaultEventLoopGroup

本来EventLoop是一个线程按照流水线执行任务，但是流水线的其中一个环节可以换线程池DefaultEventLoop，这样这个环节就可以多个线程池操作，处理IO事件的线程就可以空闲出来。同时处理时间的线程池也会按照轮询并且一个channel绑定一个线程的原则

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-23%2019.33.37.png" alt="截屏2022-12-23 19.33.37" style="zoom:50%;" />

执行换线程的源码

```java
static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
    final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
    // 下一个 handler 的事件循环是否与当前的事件循环是同一个线程
    EventExecutor executor = next.executor();
    
    // 是，直接调用
    if (executor.inEventLoop()) {
        next.invokeChannelRead(m);
    } 
    // 不是，将要执行的代码作为任务提交给下一个事件循环处理（换人）
    else {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                next.invokeChannelRead(m);
            }
        });
    }
}
```

#### Channel

closeFuture() 用来处理 channel 的关闭

* sync 方法作用是同步等待 channel 关闭
* 而 addListener 方法是异步等待 channel 关闭

connect方法返回的是 ChannelFuture 对象，它的作用是利用 channel() 方法来获取 Channel 对象。但是获取Channel对象之前要等Future返回结果，即等待连接建立完毕。**connect 方法是异步的，意味着不等连接建立，方法执行就返回了。因此 channelFuture 对象中不能【立刻】获得到正确的 Channel 对象**

除了用 sync 方法可以让异步操作同步以外，还可以使用回调的方式。使用Future中的addListener方法。不同的是，回调方法最终执行的线程是Netty内部的线程，sync方法后面执行的就是本线程。

channel.closeFuture();获取关闭Future对象，也有两种方法

#### 异步的好处

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-24%2009.13.46.png" alt="截屏2022-12-24 09.13.46" style="zoom:50%;" />

四个医生，每个病人看病需要20分钟，每个医生一小时看3个病人，总共12个病人

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-24%2009.16.06.png" alt="截屏2022-12-24 09.16.06" style="zoom:50%;" />

在一小时内还是能看12个病人，但是如果范围变成10分钟的话，这边20分钟内，在包括在处理和已经处理的就有12个，那边就只有三个，这边的**吞吐量就翻了四倍**。但是**每一个病人的总处理时间没有变短**，甚至可能变长。但是但从吞吐量来说是提高了的

#### Future & Promise

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-24%2009.26.01.png" alt="截屏2022-12-24 09.26.01"  />

#### Handler & Pipeline

ChannelHandler 用来处理 Channel 上的各种事件，分为入站、出站两种。所有 ChannelHandler 被连成一串，就是 Pipeline

* 入站处理器通常是 ChannelInboundHandlerAdapter 的子类，主要用来读取客户端数据，写回结果
* 出站处理器通常是 ChannelOutboundHandlerAdapter 的子类，主要对写回结果进行加工

打个比喻，每个 Channel 是一个产品的加工车间，Pipeline 是车间中的流水线，ChannelHandler 就是流水线上的各道工序，而后面要讲的 ByteBuf 是原材料，经过很多工序的加工：先经过一道道入站工序，再经过一道道出站工序最终变成产品

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-24%2009.49.41.png" alt="截屏2022-12-24 09.49.41" style="zoom:50%;" />

ctx.channel().write(msg) 从尾部开始查找出站处理器，最后一个出站处理器写这句话还能造成死循环

ctx.write(msg) 是从当前节点找上一个出站处理器

ctx.fireChannelRead(msg) 是 **调用下一个入站处理器**

#### ByteBuf

* 直接内存创建和销毁的代价昂贵，但读写性能高（少一次内存复制），适合配合池化功能一起用
* 直接内存对 GC 压力小，因为这部分内存不受 JVM 垃圾回收的管理，但也要注意及时主动释放

池化的最大意义在于可以重用 ByteBuf，优点有

* 没有池化，则每次都得创建新的 ByteBuf 实例，这个操作对直接内存代价昂贵，就算是堆内存，也会增加 GC 压力
* 有了池化，则可以重用池中 ByteBuf 实例，并且采用了与 jemalloc 类似的内存分配算法提升分配效率
* 高并发时，池化功能更节约内存，减少内存溢出的可能

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-24%2010.51.10.png" alt="截屏2022-12-24 10.51.10" style="zoom:50%;" />

这不比ByteBuffer好使

扩容规则是

* 如何写入后数据大小未超过 512，则选择下一个 16 的整数倍，例如写入后大小为 12 ，则扩容后 capacity 是 16
* 如果写入后数据大小超过 512，则选择下一个 2^n，例如写入后大小为 513，则扩容后 capacity 是 2^10=1024（2^9=512 已经不够了）
* 扩容不能超过 max capacity 会报错

Netty 这里采用了引用计数法来控制回收内存，每个 ByteBuf 都实现了 ReferenceCounted 接口

* 每个 ByteBuf 对象的初始计数为 1
* 调用 release 方法计数减 1，如果计数为 0，ByteBuf 内存被回收
* 调用 retain 方法计数加 1，表示调用者没用完之前，其它 handler 即使调用了 release 也不会造成回收
* 当计数为 0 时，底层内存会被回收，这时即使 ByteBuf 对象还在，其各个方法均无法正常使用

【零拷贝】的体现之一，对原始 ByteBuf 进行切片成多个 ByteBuf，切片后的 ByteBuf 并没有发生内存复制，还是使用原始 ByteBuf 的内存，切片后的 ByteBuf 维护独立的 read，write 指针

我最初在认识上有这样的误区，认为只有在 netty，nio 这样的多路复用 IO 模型时，读写才不会相互阻塞，才可以实现高效的双向通信，但实际上，Java Socket 是全双工的：在任意时刻，线路上存在`A 到 B` 和 `B 到 A` 的双向信号传输。即使是阻塞 IO，**读和写是可以同时进行的，只要分别采用读线程和写线程即可**，读不会阻塞写、写也不会阻塞读

## 粘包和半包的解决

<img src="assets/%E6%88%AA%E5%B1%8F2022-12-26%2018.08.27.png" alt="截屏2022-12-26 18.08.27" style="zoom:50%;" />

```java
ch.pipeline().addLast(new FixedLengthFrameDecoder(8)); // 固定长度
ch.pipeline().addLast(new LineBasedFrameDecoder(1024)); // 固定分隔符，当1024个字节还没出现分隔符就报错
```

这行代码放在pipline的第一个环节，是一个Decoder。当客户端传来的消息没有满足一帧的条件时，pipline不会往下走。当满足条件时把传来的消息打包成rebuf对象，剩下的处理流程就和之前的一致了

下面这个就很像学四层网络模型时每一层的数据帧结构了

```java
// 最大长度，长度偏移，长度占用字节，长度调整，剥离字节数
ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 1, 0, 1));
```

![截屏2022-12-29 10.42.43](assets/%E6%88%AA%E5%B1%8F2022-12-29%2010.42.43.png)

按照图翻译就是：最大长度，header1的长度，Length的长度，header2的长度，传递下一环节需不需要去掉几个字节（自己定义，可以去掉header1或者不是内容的都去掉，所以建议内容就放到帧的最后一部分）

**分清楚客户端是按照字节数填充这一帧的内容，服务端才是设计长度。同时length的长度如果是大于一个字节就要考虑位移二进制位数去填充长度这一部分**

### 自定义协议

* 魔数，用来在第一时间判定是否是无效数据包
* 版本号，可以支持协议的升级
* 序列化算法，消息正文到底采用哪种序列化反序列化方式，可以由此扩展，例如：json、protobuf、hessian、jdk
* 指令类型，是登录、注册、单聊、群聊... 跟业务相关
* 请求序号，为了双工通信，提供异步能力
* 正文长度
* 消息正文

自定义协议的类继承ByteToMessageCodec，只需要重写encode和decode就行，即设置了入站第一个完成解码，也设置了出站最后一个编码，这可能也是为什么出站要反着来的原因吧。

同时上面使用的 LengthFieldBasedFrameDecoder等类也都是重写了ByteToMessageCodec里面的方法

```java
@Slf4j
public class MessageCodec extends ByteToMessageCodec<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        // 1. 4 字节的魔数
        out.writeBytes(new byte[]{1, 2, 3, 4});
        // 2. 1 字节的版本,
        out.writeByte(1);
        // 3. 1 字节的序列化方式 jdk 0 , json 1
        out.writeByte(0);
        // 4. 1 字节的指令类型
        out.writeByte(msg.getMessageType());
        // 5. 4 个字节
        out.writeInt(msg.getSequenceId());
        // 无意义，对齐填充
        out.writeByte(0xff);
        // 6. 获取内容的字节数组
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        byte[] bytes = bos.toByteArray();
        // 7. 长度
        out.writeInt(bytes.length);
        // 8. 写入内容
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int magicNum = in.readInt();
        byte version = in.readByte();
        byte serializerType = in.readByte();
        byte messageType = in.readByte();
        int sequenceId = in.readInt();
        in.readByte();
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Message message = (Message) ois.readObject();
        log.debug("{}, {}, {}, {}, {}, {}", magicNum, version, serializerType, messageType, sequenceId, length);
        log.debug("{}", message);
        out.add(message);
    }
}
```

### 客户端假死问题的解决

1、**客户端定时发送心跳消息**。客户端可以定时向服务器端发送数据，只要这个时间间隔小于服务器定义的空闲检测的时间间隔，那么就能防止前面提到的误判，客户端可以定义如下心跳处理器

```java
// 用来判断是不是 读空闲时间过长，或 写空闲时间过长
// 3s 内如果没有向服务器写数据，会触发一个 IdleState#WRITER_IDLE 事件
ch.pipeline().addLast(new IdleStateHandler(0, 3, 0));
// ChannelDuplexHandler 可以同时作为入站和出站处理器
ch.pipeline().addLast(new ChannelDuplexHandler() {
    // 用来触发特殊事件
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
        IdleStateEvent event = (IdleStateEvent) evt;
        // 触发了写空闲事件
        if (event.state() == IdleState.WRITER_IDLE) {
            //                                log.debug("3s 没有写数据了，发送一个心跳包");
            ctx.writeAndFlush(new PingMessage());
        }
    }
});
```

2、服务端每隔一段时间就检查这段时间内是否接收到客户端数据，没有就可以判定为连接假死

```java
// 用来判断是不是 读空闲时间过长，或 写空闲时间过长
// 5s 内如果没有收到 channel 的数据，会触发一个 IdleState#READER_IDLE 事件
ch.pipeline().addLast(new IdleStateHandler(5, 0, 0));
// ChannelDuplexHandler 可以同时作为入站和出站处理器
ch.pipeline().addLast(new ChannelDuplexHandler() {
    // 用来触发特殊事件
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception{
        IdleStateEvent event = (IdleStateEvent) evt;
        // 触发了读空闲事件
        if (event.state() == IdleState.READER_IDLE) {
            log.debug("已经 5s 没有读到数据了");
            ctx.channel().close();
        }
    }
});
```

## 参数调优

#### option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)

属于 SocketChannal 参数，用在**客户端**建立连接时，如果在指定毫秒内无法连接，会抛出 timeout 异常

如果本身服务器就连不上，不用等3s，直接就返回refused的异常了

底层就是使用eventLoop安排一个定时任务，**这个任务内容就是3s后抛出一个异常**

#### SO_BACKLOG

属于 ServerSocketChannal 参数

**TCP为什么需要三次握手：**

**为了防止已经失效的连接请求报文突然又传送到了服务器，从而导致不必要的错误和资源的浪费**。

如果使用两次握手，客户端发送一次请求连接，服务端收到以后发送确认给客户端就直接连接。在这种情况下，加入客户端历史发送了一个链接，这个链接阻塞了很长时间，客户端都以为无效重新发送了。过了一段时间，这个无效链接来到服务端，这时候服务端接受发送确认，然后按照两次握手此时连接就建立了。但是客户端就不需要这个连接了，造成资源浪费。那么三次握手的第三次就是服务器端对到来的请求发送给客户端让其辨认这个链接是否有效是否过时，等客户端再次确认有效才能够真正建立连接。

**总的来说就是三次握手的第三次就是为了让客户端辨认服务端发来的链接是否有效。握手就是发送数据包的过程**

**微观来讲三次握手为了确定双方各自的随机初始序列号**（初始序列号不从0开始，由双方随机禅城），因为TCP是可靠的，所有每次数据包的序列号都要由接收方发送确认才行。

![截屏2023-01-01 14.04.50](assets/%E6%88%AA%E5%B1%8F2023-01-01%2014.04.50.png)

发送一个包就成为了SYN状态，所以客户端服务器端都要发送一次SYN，也能顺便区分三次握手的阶段，再次受到就ESTABLISHED状态，ack序号代表期待下一次接受的序列号。ACK就是一个确认符。

![image-20230101141346804](assets/image-20230101141346804.png)

1. 第一次握手，client 发送 SYN 到 server，状态修改为 SYN_SEND，server 收到，状态改变为 SYN_REVD，并将该请求**放入 sync queue 队列（半连接队列）**
2. 第二次握手，server 回复 SYN + ACK 给 client，client 收到，状态改变为 ESTABLISHED，并发送 ACK 给 server
3. 第三次握手，server 收到 ACK，状态改变为 ESTABLISHED，将该**请求从 sync queue 放入 accept queue，放入队列后不会马上使用，看server什么时候从队列中取出处理**

* sync queue - 半连接队列
  * 大小通过 /proc/sys/net/ipv4/tcp_max_syn_backlog 指定，在 `syncookies` 启用的情况下，逻辑上没有最大值限制，这个设置便被忽略
* accept queue - 全连接队列
  * 其大小通过 /proc/sys/net/core/somaxconn 指定，在使用 listen 函数时，内核会根据传入的 backlog 参数与系统参数，**取二者的较小值**
  * **如果 accpet queue 队列满了，server 将发送一个拒绝连接的错误信息到 client**

**适当调整全连接队列的大小进行调优**

## RPC框架

**只要接受数据的时候需要解决粘包等问题，编码发送的时候什么都不用管**

**rebuf的wirteInt方法就直接写4个字节长度**

**不管前面是什么数据，只要最后一个出站处理器转成buf对象就行**

**粘包和半包等问题的解码对象需要暂存状态，所以不可以共享。不用存状态的就一定可以设置为sharable**

**如果结果不正常但是不报错，看看是不是没有继承序列化接口，还是用json好**

class对象中有一个泛型，这个泛型代表着什么类的class文件

## java中使用反射的原因

首先JVM会启动，你的代码会编译成一个.**class文件，然后被类加载器加载进jvm的内存中**，你的类Object加载到方法区中，创建了Object类的class对象到堆中，注意这个不是new出来的对象，而是类的类型对象，每个类只有一个class对象，作为方法区类的数据结构的接口。jvm创建对象前，会先检查类是否加载，寻找类对应的class对象，若加载好，则为你的对象分配内存，初始化也就是代码:new Object()。

假如一个服务器上突然遇到某个请求哦要用到某个类，哎呀但没加载进jvm，是不是要停下来自己写段代码，new一下，哦启动一下服务器，（脑残）！通过反射的机制，**可以通过类的全类名让jvm在服务器中找到并加载这个类**

## JDK动态代理

jdk的动态代理通过反射实现所有的接口，implement所有接口并且extends了Proxy类。因为当你动态代理了一个类后，生成的代理类要和被代理的类是同一种类，这样才算代理，外部程序感知不到变化。如果你增强了一个对象，并且想把增强后的对象向上转型，那么这个增强的类要么实现了所有的接口或者是被代理类的子类。**因为jdk动态代理已经继承了Proxy所以只能实现被代理类的所有接口去增强，然后才能向上转型成接口类型。这样才叫做增强！！！**

1、拿到所有接口的字节文件，然后实现接口方法，在接口方法中直接调用invoke方法，invoke就是那个传入函数式接口实现类！！！

![截屏2023-01-05 13.49.24](assets/%E6%88%AA%E5%B1%8F2023-01-05%2013.49.24.png)

## cglib动态代理

原理是对指定的目标类生成一个子类，并覆盖其中方法实现增强，但因为采用的是继承，所以不能对final修饰的类进行代理。 **JDK的动态代理机制只能代理实现了接口的类，而不能实现接口的类就不能实现JDK的动态代理。**

# Netty源码解读

1、debug方式，进入bind方法

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2013.29.31.png" alt="截屏2023-01-06 13.29.31" style="zoom:50%;" />

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2013.30.23.png" alt="截屏2023-01-06 13.30.23" style="zoom:50%;" />

2、commad键再进入这个bind方法

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2013.31.27.png" alt="截屏2023-01-06 13.31.27" style="zoom:50%;" />

3、进入dobind方法，给一下方法加上断点

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2014.12.40.png" alt="截屏2023-01-06 14.12.40" style="zoom:50%;" />

initAndRegister（**init创建ssc，Register将ssc注册到selector**）会给nio线程，返回一个Future对象。如果future很快执行完就是跑到第一个doBind0，此时就是主线程。否则就跑到回调函数那里，由nio线程执行。**doBind0就是讲端口绑定**

4、进入initAndRegister方法查看

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2014.25.53.png" alt="截屏2023-01-06 14.25.53" style="zoom:50%;" />

channelFactory.newChannel就是通过反射创建Niossc对象，Niossc对象是对java里面的ssc对象的增强。进入init方法，到P.addlast，p就是pipeline，初始化handler等待调用

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2015.24.21.png" alt="截屏2023-01-06 15.24.21" style="zoom:50%;" />

进入initAndRegister方法下面的register方法，一直进入register直到下面

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2015.29.27.png" alt="截屏2023-01-06 15.29.27" style="zoom:50%;" />

切换线程，eventLoop.inEventLoop()判断当前线程是否是nio线程，所以一定会进入else分支，切换线程提交nio线程池任务。一定会是nio线程执行注册。进入register0

<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2015.32.59.png" alt="截屏2023-01-06 15.32.59" style="zoom:50%;" />

进入doRegister()方法<img src="assets/%E6%88%AA%E5%B1%8F2023-01-06%2015.35.01.png" alt="截屏2023-01-06 15.35.01" style="zoom:50%;" />

javaChannel就是ssc原生的，将niossc作为附件，开始什么事件都不关注。有事件的话就给附件niossc处理。执行完后，register()方法中的invokeHandlerAddedIfNeeded就会执行初始化的handler，初始化handler中会添加acceptor处理accept事件

**处理器不就相当于之前的原始写法里面的读事件、写事件的封装！！**
