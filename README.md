# Shared Notes

原来这个README.md里边的内容毫无卵用，我全都给删掉了。

目前发现一些比较有用的资料有：

[一些基本的class && 程序的IR ](https://tai-e.pascal-lab.net/docs/current/reference/en/program-abstraction.html)

一些**远程的**git指令：

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
git pull <remote_name> <branch_name>
//这个命令可以拉取远程仓库<branch-name>的修改到本地当前branch, 不输入default = "main"
```

Last modified: Xusheng Zhi, Nov. 10
