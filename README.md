# CVE-2019-2725

CVE-2019-2725(CNVD-C-2019-48814、WebLogic wls9-async)

# 命令回显

## 10.3.6
![10.0.3效果图](https://raw.githubusercontent.com/lufeirider/CVE-2019-2725/master/10.0.3%E6%95%88%E6%9E%9C%E5%9B%BE.jpg)

## 12.1.3
![12.1.3效果图](https://raw.githubusercontent.com/lufeirider/CVE-2019-2725/master/12.1.3%E6%95%88%E6%9E%9C%E5%9B%BE.jpg)


# ResultBaseExec.java
用于测试defineClass，将把恶意类从base64还原出来，执行代码，主要是比较方便（可用可不用）。

# JDK7u21.java
会生成weblogic-2019-2725_12.1.3命令执行.txt中的xml，请使用jdk6编译。

# CVE-2019-2725.py
检测命令是否会执行。
