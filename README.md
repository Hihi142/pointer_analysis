# Shared Notes

原来这个README.md里边的内容毫无卵用，我全都给删掉了。

目前发现一些比较有用的资料有：

[一些基本的class && 程序的IR ](https://tai-e.pascal-lab.net/docs/current/reference/en/program-abstraction.html)

### 关于github的远程操作

```bash
git remote add origin https://github.com/Hihi142/pointer_analysis.git
//把这个远程仓库添加进来, 将其命名为origin
git remote set-url origin git@github.com:Hihi142/pointer_analysis.git
//需要使用ssh连接，具体配置自行google/chatGPT一下

git config --global https.proxy
git config --global http.proxy
//好像需要全局开代理，push/pull失败的时候试试这两个指令

git push 
git push origin <branch-name>
//这个命令将本地当前branch推送到远程仓库<branch-name>, default = "main"

git pull
git pull origin <branch_name>
//这个命令可以拉取远程仓库<branch-name>的修改到本地当前branch, 不输入default = "main"
```

### 关于评测

似乎需要安装docker，我装上并且跑起来了，运行截图如下：

```bash
== 当前评测机负载: 0.2 / 8

== 评测环境：
...
pku-pta finishes, elapsed time: 0.05s
Tai-e finishes, elapsed time: 3.47s
    输出：{1: {1, 2, 3}, 2: {1, 2, 3}, 3: {1, 2, 3}}
    答案：{1: {1, 2}, 2: {2}, 3: {3}}
    分数：1.58 ('sound', 9, 4)

Traceback (most recent call last):
  File "/root/main.py", line 229, in <module>
    assert score>=1.99999, f'未通过公开测试用例 {b[0]}'
AssertionError: 未通过公开测试用例 Hello
```

本地下发的只有两个trivial的样例，我们恐怕需要造更多的样例 / 充分利用平台上的不公开样例优化算法

## How to Obtain Runnable Jar of Tai-e?

The simplest way is to download it from [GitHub Releases](https://github.com/pascal-lab/Tai-e/releases).

Alternatively, you might build the latest Tai-e yourself from the source code. This can be simply accomplished via Gradle (be sure that Java 17 (or higher version) is available on your system).
You just need to run command `gradlew fatJar`, and then the runnable jar will be generated in `tai-e/build/`, which includes Tai-e and all its dependencies.
