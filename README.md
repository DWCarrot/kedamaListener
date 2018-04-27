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

### 2018/04/20
改善了命令回复
json记录增加 continuous项判断是否连续
#### 准备上线！

### 2018/04/22 -- 2018/04/24
增添了数据查询服务器 
>`https://0.0.0.0:28443`    服务器端口
>`/kedamaListener/PlayerCountRecord`    目录
>`?`    查询模式
>1.`start=&end=&dest=` 
>API:查询记录数据
>参数1:记录开始时间,%d{yyyy-MM-dd},默认为1970-01-01
>参数2:记录结束时间,%d{yyyy-MM-dd},默认为当天    参数3:返回值的名字，默认为空
>返回: json记录对象数组 \$dest\$=[\${"timestamp":\$timestamp, "time":\$time%ISO_OFFSETDATETIME, "onlineNum":\$onlinePlayerNumber, "online":[\$playerName,...],"continuous":\$ifContinuous},...]
>2.`list=list&dest=`
>API:查询记录列表
>参数1:list
>参数2:返回值的名字，默认为空
>返回: json记录对象数组 \$dest\$=[{"time":\$time,"file":\$file},...]
>3.`check=now&dest=`
>API:查询当前在线
>参数1:now
>参数2:返回值的名字，默认为空
>返回: json记录对象数组 \$dest\$=\${"timestamp":\$timestamp, "time":\$time%ISO_OFFSETDATETIME, "onlineNum":\$onlinePlayerNumber, "online":[\$playerName,...],"continuous":true} 

###2018/04/25
增添了IRC服务器不响应的自动重连功能，改变启动模式