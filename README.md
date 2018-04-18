# kedamaListener
a listener to record the changes of player number in The Minecraft Server

### 2018/04/05

ping 方法完成

消耗流量过多（8kB/次），拟不采用

### 2018/04/16

基本可以运行

未实现SSL

### 2018/04/17

实现简单的单向认证SSL，使用jre/lib/security/cacerts 作为trustkeystore

### 2018/04/18

使用java.time 管理时间日期（含时区）；好像有点慢；

### & 2018/04/19 00:00 

加入 slf4j & logback ；使用logback按天记录PlayerCount信息

@vesrion=0.0.1-SNAPSHOT-7 基本稳定的版本