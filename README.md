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

### How to Obtain Runnable Jar of Tai-e?

The simplest way is to download it from [GitHub Releases](https://github.com/pascal-lab/Tai-e/releases).

Alternatively, you might build the latest Tai-e yourself from the source code. This can be simply accomplished via Gradle (be sure that Java 17 (or higher version) is available on your system).
You just need to run command `gradlew fatJar`, and then the runnable jar will be generated in `tai-e/build/`, which includes Tai-e and all its dependencies.

### Notes 11.15

a = new A()     a的集合加上这个new位点

a = b        set(a) |= set(b)

a[i]怎么看：简单的，当成a；复杂的，区分i == 0 / i != 0

a = b.f      set(a) |= set(b.f)。b.f的含义是：b所有可能指向的new位点，的f指针，可能指向的位置的并

a.f = b      set(a.f) |= set(b)  对于所有a可能指向的new位点，的f指针，全部update

Instance of: 简单的忽略，复杂的按照语义判断？

Cast: ？？？唯一需要额外考虑的是，a.f()的时候，a的类型和实际指向内容不匹配时，以谁为准

Invoke(Call):   a.f()  简单的，直接向所有可能的连边；复杂的，按照分析得到的类型连边。(实操的时候，先把所有可能的边连上，是否按边加入队列动态查看。转移方法：把传参可能的值，“并”给函数的参数)

Return: 将相应表达式的结果，并给 承接者。

Throw() / Catch() 当做特殊的jump

Monitor: ？？？



每个指针 to subset(所有new位点)，记作G。指针 = 局部变量 ∪ new位点们的字段

猜想：区分类型，可以有效降低复杂度。Testing: 按照类型rule out一些可能的point-to







算法：从所有程序的入口(叫main的方法)，开始执行。沿着控制流图传递，如果一个节点u，它的后继v。记录|G|比rem[v]大，则更新rev[v]，并将v加入更新队列。特别的，每个Benchmark.test仅在扫描该语句的时候统计答案

初始化rem[v] = -1



实现的问题

1. 高效的集合并操作，比如u64，需要元素编号成0, 1, 2, ... n
2. 所有的指针也需要重新编号
3. 需要重构IR，上下文展开、需要对于stmt的random access、以及后继、calling、returning。建议修改原始IR的class，添加字段。
